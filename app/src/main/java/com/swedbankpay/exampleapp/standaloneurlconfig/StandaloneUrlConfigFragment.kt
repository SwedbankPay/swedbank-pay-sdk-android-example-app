package com.swedbankpay.exampleapp.standaloneurlconfig

import android.Manifest
import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.View.OnFocusChangeListener
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.activity.addCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.getSystemService
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.swedbankpay.exampleapp.R
import com.swedbankpay.exampleapp.databinding.FragmentStandaloneUrlConfigBinding
import com.swedbankpay.exampleapp.standaloneurlconfig.camera.CameraActivity
import com.swedbankpay.exampleapp.standaloneurlconfig.camera.CameraActivity.Companion.SCANNED_URL_KEY
import com.swedbankpay.exampleapp.standaloneurlconfig.creditcardprefill.CreditCardPrefillAdapter
import com.swedbankpay.exampleapp.standaloneurlconfig.swishprefill.SwishPrefillAdapter
import com.swedbankpay.exampleapp.util.PermissionUtil
import com.swedbankpay.exampleapp.util.ScanUrl
import com.swedbankpay.mobilesdk.PaymentViewModel
import com.swedbankpay.mobilesdk.paymentViewModel
import com.swedbankpay.mobilesdk.paymentsession.PaymentSession
import com.swedbankpay.mobilesdk.paymentsession.PaymentSessionState
import com.swedbankpay.mobilesdk.paymentsession.api.model.SwedbankPayAPIError
import com.swedbankpay.mobilesdk.paymentsession.exposedmodel.PaymentAttemptInstrument
import com.swedbankpay.mobilesdk.paymentsession.exposedmodel.PaymentSessionProblem
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class StandaloneUrlConfigFragment : Fragment(R.layout.fragment_standalone_url_config) {
    private lateinit var binding: FragmentStandaloneUrlConfigBinding
    private lateinit var viewModel: StandaloneUrlConfigViewModel

    private var lastClickedBtn: ScanUrl = ScanUrl.Unknown

    private var swishPrefillAdapter: SwishPrefillAdapter? = null
    private var creditCardPrefillAdapter: CreditCardPrefillAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        activity?.onBackPressedDispatcher?.addCallback(this) {
            hideResultImage()
            if (childFragmentManager.backStackEntryCount > 0) {
                childFragmentManager.popBackStack()
                viewModel.resetNativePaymentsInitiatedState()
            } else {
                findNavController().navigateUp()
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(this)[StandaloneUrlConfigViewModel::class.java]

        binding = FragmentStandaloneUrlConfigBinding.bind(view)
        binding.lifecycleOwner = this
        binding.viewModel = viewModel

        viewModel.paymentUrlScheme.value = binding.paymentUrl.prefixText.toString()

        viewModel.swedbankPayConfiguration.observe(viewLifecycleOwner) { configuration ->
            val paymentFrag = SwedbankPayConfigFragment()

            paymentFrag.arguments = com.swedbankpay.mobilesdk.PaymentFragment.ArgumentsBuilder()
                .useBrowser(false)
                .userData(configuration)
                .build()

            childFragmentManager
                .beginTransaction()
                .replace(
                    R.id.standaloneUrlConfigFrame,
                    paymentFrag,
                    R.id.standaloneUrlConfigFrame.toString()
                )
                .addToBackStack(null)
                .commit()
        }

        observeStandaloneUrlPaymentProcess()
        observeStandaloneUrlPaymentSessionState()

        createScannerButtonListeners()
        createButtonListeners()

        observePrefills()

        binding.baseUrlTextfield.onFocusChangeListener = onFocusChangeListener
        binding.completeUrlTextfield.onFocusChangeListener = onFocusChangeListener
        binding.cancelUrlTextfield.onFocusChangeListener = onFocusChangeListener
        binding.paymentUrlTextfield.onFocusChangeListener = onFocusChangeListener

        swishPrefillAdapter = SwishPrefillAdapter(viewModel, this) { msisdn ->
            viewModel.startPaymentWith(PaymentAttemptInstrument.Swish(msisdn))
        }

        creditCardPrefillAdapter = CreditCardPrefillAdapter(viewModel, this) { creditCardPrefill ->
            viewModel.startPaymentWith(
                PaymentAttemptInstrument.CreditCard(
                    creditCardPrefill,
                    requireContext()
                )
            )
        }

        binding.swishPrefillRecyclerView.adapter = swishPrefillAdapter
        binding.creditCardPrefillRecyclerView.adapter = creditCardPrefillAdapter
    }

    private val onFocusChangeListener = OnFocusChangeListener { editTextView, hasFocus ->
        if (!hasFocus) {
            when (editTextView) {
                binding.baseUrlTextfield -> {
                    viewModel.saveUrl(binding.baseUrlTextfield.text.toString(), ScanUrl.Base)
                }

                binding.completeUrlTextfield -> {
                    viewModel.saveUrl(
                        binding.completeUrlTextfield.text.toString(),
                        ScanUrl.Complete
                    )
                }

                binding.cancelUrlTextfield -> {
                    viewModel.saveUrl(binding.cancelUrlTextfield.text.toString(), ScanUrl.Cancel)
                }

                binding.paymentUrlTextfield -> {
                    viewModel.saveUrl(binding.paymentUrlTextfield.text.toString(), ScanUrl.Payment)
                }
            }
        }
    }

    private fun createScannerButtonListeners() {
        binding.checkoutUrlScannerButton.setOnClickListener {
            lastClickedBtn = ScanUrl.Checkout
            clearTextfieldsFocus()
            scanQR()
        }
        binding.baseUrlScannerButton.setOnClickListener {
            lastClickedBtn = ScanUrl.Base
            clearTextfieldsFocus()
            scanQR()
        }
        binding.completeUrlScannerButton.setOnClickListener {
            lastClickedBtn = ScanUrl.Complete
            clearTextfieldsFocus()
            scanQR()
        }
        binding.cancelUrlScannerButton.setOnClickListener {
            lastClickedBtn = ScanUrl.Cancel
            clearTextfieldsFocus()
            scanQR()
        }

        binding.sessionUrlScannerButton.setOnClickListener {
            lastClickedBtn = ScanUrl.Session
            clearTextfieldsFocus()
            scanQR()
        }

        binding.swishPhoneNumberScannerButton.setOnClickListener {
            lastClickedBtn = ScanUrl.SwishPhoneNumber
            clearTextfieldsFocus()
            scanQR()
        }
    }

    private fun createButtonListeners() {
        binding.checkoutButton.setOnClickListener {
            hideResultImage()
            clearTextfieldsFocus()
            viewModel.onCheckoutPressed()
        }

        binding.getSessionButton.setOnClickListener {
            hideResultImage()
            clearTextfieldsFocus()
            viewModel.onGetSessionPressed()
        }

        binding.openSwishOnThisPhoneButton.setOnClickListener {
            clearTextfieldsFocus()
            viewModel.startPaymentWith(PaymentAttemptInstrument.Swish(localStartContext = context))
        }

        binding.openSwishOnAnotherPhoneButton.setOnClickListener {
            clearTextfieldsFocus()
            viewModel.startPaymentWith(PaymentAttemptInstrument.Swish(msisdn = viewModel.swishPhoneNumber.value))
        }

        binding.newCreditCardButton.setOnClickListener {
            clearTextfieldsFocus()
            viewModel.startPaymentWith(PaymentAttemptInstrument.NewCreditCard(enabledPaymentDetailsConsentCheckbox = true))
        }

        binding.getPaymentMenu.setOnClickListener {
            clearTextfieldsFocus()
            viewModel.getPaymentMenu()
        }

        binding.abortNativePaymentButton.setOnClickListener {
            clearTextfieldsFocus()
            viewModel.abortNativePayment()
        }
    }

    private fun clearTextfieldsFocus() {
        binding.viewCheckoutUrlTextfield.clearFocus()
        binding.baseUrlTextfield.clearFocus()
        binding.completeUrlTextfield.clearFocus()
        binding.cancelUrlTextfield.clearFocus()
        binding.paymentUrlTextfield.clearFocus()
        binding.sessionUrlTextfield.clearFocus()
        binding.swishPhoneNumberTextfield.clearFocus()
        hideKeyboard()
    }

    private fun hideKeyboard() {
        val imm = context?.getSystemService<InputMethodManager>()
        imm?.hideSoftInputFromWindow(view?.windowToken, 0)
    }

    private fun scanQR() {
        if (PermissionUtil.allPermissionsGranted(requireContext())) {
            val cameraScanIntent = Intent(activity, CameraActivity::class.java)
            scanForQRActivityLauncher.launch(cameraScanIntent)
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private val scanForQRActivityLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        it.data?.extras?.getString(SCANNED_URL_KEY)?.let { passedUrl ->
            viewModel.saveUrl(passedUrl, lastClickedBtn)
            when (lastClickedBtn) {
                ScanUrl.Checkout -> binding.viewCheckoutUrlTextfield.setText(passedUrl)
                ScanUrl.Base -> binding.baseUrlTextfield.setText(passedUrl)
                ScanUrl.Complete -> binding.completeUrlTextfield.setText(passedUrl)
                ScanUrl.Cancel -> binding.cancelUrlTextfield.setText(passedUrl)
                ScanUrl.Session -> binding.sessionUrlTextfield.setText(passedUrl)
                ScanUrl.SwishPhoneNumber -> binding.swishPhoneNumberTextfield.setText(passedUrl)
                else -> {}
            }
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            val cameraScanIntent = Intent(activity, CameraActivity::class.java)
            scanForQRActivityLauncher.launch(cameraScanIntent)
        } else {
            if (shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)) {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.camera_access_needed),
                    Toast.LENGTH_LONG
                ).show()
            } else {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.camera_access_is_a_must),
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun observeStandaloneUrlPaymentProcess() {
        activity?.paymentViewModel?.richState?.observe(viewLifecycleOwner) { richState ->
            if (!richState.state.isFinal) {
                return@observe
            }

            childFragmentManager.popBackStack()
            when (richState.state) {
                PaymentViewModel.State.COMPLETE -> {
                    setSuccess()
                }

                PaymentViewModel.State.CANCELED -> {
                    setError(context?.getString(R.string.standalone_url_config_fragment_payment_cancelled))
                }

                PaymentViewModel.State.FAILURE -> {
                    setError(richState.exception?.message)
                }

                else -> {}
            }
        }
    }

    private fun observeStandaloneUrlPaymentSessionState() {
        PaymentSession.paymentSessionState.observe(viewLifecycleOwner) { paymentState ->
            when (paymentState) {
                is PaymentSessionState.PaymentSessionFetched -> {
                    viewModel.setAvailableInstruments(paymentState.availableInstruments)
                }

                is PaymentSessionState.Show3DSecureFragment -> {
                    childFragmentManager.beginTransaction()
                        .replace(R.id.fragmentContainer, paymentState.fragment)
                        .addToBackStack(null)
                        .commit()
                }

                is PaymentSessionState.Dismiss3DSecureFragment -> {
                    childFragmentManager.popBackStack()
                }

                is PaymentSessionState.ShowPaymentFragment -> {
                    childFragmentManager.beginTransaction()
                        .add(R.id.fragmentContainer, paymentState.fragment)
                        .addToBackStack(null)
                        .commit()
                }

                is PaymentSessionState.PaymentSessionComplete -> {
                    setSuccess()
                }

                is PaymentSessionState.PaymentSessionCanceled -> {
                    setError(getString(R.string.payment_was_canceled))
                }

                is PaymentSessionState.SessionProblemOccurred -> {
                    openAlertDialog(
                        title = paymentState.problem.title ?: "",
                        message = getString(
                            R.string.payment_session_problem_occurred_detail,
                            paymentState.problem.status,
                            paymentState.problem.detail,
                            paymentState.problem.type
                        )
                    )
                    viewModel.resetNativePaymentsInitiatedState()
                }

                is PaymentSessionState.SdkProblemOccurred -> {
                    when (paymentState.problem) {
                        PaymentSessionProblem.ClientAppLaunchFailed -> {
                            openAlertDialog(getString(R.string.client_app_launch_failed), "")
                            viewModel.resetNativePaymentsInitiatedState()
                        }

                        is PaymentSessionProblem.PaymentSessionAPIRequestFailed -> {
                            val swedbankPayAPIError =
                                (paymentState.problem as PaymentSessionProblem.PaymentSessionAPIRequestFailed).error
                            val retry =
                                (paymentState.problem as PaymentSessionProblem.PaymentSessionAPIRequestFailed).retry

                            when (swedbankPayAPIError) {
                                is SwedbankPayAPIError.Error -> {
                                    openAlertDialogWithRetryFunctionality(
                                        title = getString(R.string.payment_session_api_request_failed),
                                        message = getString(
                                            R.string.payment_session_api_request_failed_message,
                                            swedbankPayAPIError.responseCode,
                                            swedbankPayAPIError.message
                                        ),
                                        retry = retry
                                    )
                                }

                                SwedbankPayAPIError.InvalidUrl -> {
                                    openAlertDialogWithRetryFunctionality(
                                        title = getString(R.string.payment_session_api_request_failed),
                                        message = getString(R.string.invalid_url),
                                        retry = retry
                                    )
                                }

                                SwedbankPayAPIError.Unknown -> {
                                    openAlertDialogWithRetryFunctionality(
                                        title = getString(R.string.payment_session_api_request_failed),
                                        message = getString(R.string.unknown_error),
                                        retry = retry
                                    )
                                }
                            }

                            viewModel.resetNativePaymentsInitiatedState()
                        }

                        PaymentSessionProblem.PaymentSessionEndReached -> {
                            setError(getString(R.string.payment_session_end_reached))
                        }

                        PaymentSessionProblem.InternalInconsistencyError -> {
                            setError(getString(R.string.payment_session_internal_inconsistency_error))
                        }

                        PaymentSessionProblem.AutomaticConfigurationFailed -> {
                            setError(getString(R.string.payment_session_internal_inconsistency_error))
                        }

                        is PaymentSessionProblem.PaymentSession3DSecureFragmentLoadFailed -> {
                            val error =
                                (paymentState.problem as PaymentSessionProblem.PaymentSession3DSecureFragmentLoadFailed).error

                            val retry =
                                (paymentState.problem as PaymentSessionProblem.PaymentSession3DSecureFragmentLoadFailed).retry
                            openAlertDialogWithRetryFunctionality(
                                title = getString(R.string.payment_session_3d_secure_fragment_load_failed),
                                message = getString(
                                    R.string.payment_session_3d_secure_fragment_load_failed_message,
                                    error.responseCode,
                                    error.message
                                ),
                                positiveButton = {
                                    childFragmentManager.popBackStack()
                                },
                                retry = retry
                            )
                        }
                    }
                }

                else -> {}
            }

        }
    }

    private fun observePrefills() {
        viewModel.swishPrefills.observe(viewLifecycleOwner) {
            swishPrefillAdapter?.submitList(it)
            scroll(false)
        }

        viewModel.creditCardPrefills.observe(viewLifecycleOwner) {
            creditCardPrefillAdapter?.submitList(it)
            scroll(false)
        }

    }

    private fun hideResultImage() {
        binding.paymentResultImage.visibility = View.GONE
        binding.paymentResultText.visibility = View.GONE
    }

    private fun setSuccess() {
        viewModel.resetPayment()

        binding.paymentResultImage.visibility = View.VISIBLE
        binding.paymentResultText.visibility = View.VISIBLE

        binding.paymentResultImage.setImageResource(R.drawable.payment_success)
        binding.paymentResultText.text =
            context?.getString(R.string.standalone_url_config_fragment_payment_completed)

        lifecycleScope.launch {
            delay(500)
            scroll(true)
        }
    }

    private fun setError(message: String?) {
        viewModel.resetPayment()

        binding.paymentResultImage.visibility = View.VISIBLE
        binding.paymentResultText.visibility = View.VISIBLE

        binding.paymentResultImage.setImageResource(R.drawable.payment_failure)
        binding.paymentResultText.text = message

        lifecycleScope.launch {
            delay(500)
            scroll(true)
        }
    }

    private fun openAlertDialog(title: String, message: String?) {
        AlertDialog.Builder(requireContext())
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton(getString(R.string.option_ok), null)
            .show()
    }

    private fun openAlertDialogWithRetryFunctionality(
        title: String? = null,
        message: String? = null,
        positiveButton: (() -> Unit)? = null,
        retry: () -> Unit
    ) {
        AlertDialog.Builder(requireContext())
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton(getString(R.string.option_ok)) { _, _ ->
                positiveButton?.invoke()
            }
            .setNegativeButton(getString(R.string.option_retry)) { _, _ ->
                retry.invoke()
            }
            .show()
    }

    private fun scroll(toTop: Boolean) {
        binding.standaloneUrlConfigScrollView.post {
            val animator = ObjectAnimator.ofInt(
                binding.standaloneUrlConfigScrollView,
                "scrollY",
                if (toTop) 0 else binding.standaloneUrlConfigScrollView.bottom
            )

            animator.duration = 500
            animator.interpolator = AccelerateDecelerateInterpolator()
            animator.start()
        }
    }

}