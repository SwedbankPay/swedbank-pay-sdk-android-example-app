package com.swedbankpay.exampleapp.standaloneurlconfig

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.View.OnClickListener
import android.view.View.OnFocusChangeListener
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.getSystemService
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.swedbankpay.exampleapp.R
import com.swedbankpay.exampleapp.databinding.FragmentStandaloneUrlConfigBinding
import com.swedbankpay.exampleapp.standaloneurlconfig.camera.CameraActivity
import com.swedbankpay.exampleapp.standaloneurlconfig.camera.CameraActivity.Companion.SCANNED_URL_KEY
import com.swedbankpay.exampleapp.util.PermissionUtil
import com.swedbankpay.exampleapp.util.ScanUrl
import com.swedbankpay.mobilesdk.PaymentViewModel
import com.swedbankpay.mobilesdk.paymentViewModel

class StandaloneUrlConfigFragment: Fragment(R.layout.fragment_standalone_url_config) {
    private lateinit var binding: FragmentStandaloneUrlConfigBinding
    private lateinit var viewModel: StandaloneUrlConfigViewModel

    private var lastClickedBtn: ScanUrl = ScanUrl.Unknown

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
                .replace(R.id.standaloneUrlConfigFrame, paymentFrag, R.id.standaloneUrlConfigFrame.toString())
                .addToBackStack(null)
                .commit()
        }

        observeStandaloneUrlPaymentProcess()

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

        binding.checkoutButton.setOnClickListener {
            clearTextfieldsFocus()
            viewModel.onCheckoutPressed()
        }

        binding.baseUrlTextfield.onFocusChangeListener = onFocusChangeListener
        binding.completeUrlTextfield.onFocusChangeListener = onFocusChangeListener
        binding.cancelUrlTextfield.onFocusChangeListener = onFocusChangeListener
        binding.paymentUrlTextfield.onFocusChangeListener = onFocusChangeListener
    }

    private val onFocusChangeListener = OnFocusChangeListener { editTextView, hasFocus ->
        if (!hasFocus) {
            when (editTextView) {
                binding.baseUrlTextfield -> {
                    viewModel.saveUrl(binding.baseUrlTextfield.text.toString(), ScanUrl.Base)
                }
                binding.completeUrlTextfield -> {
                    viewModel.saveUrl(binding.completeUrlTextfield.text.toString(), ScanUrl.Complete)
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

    private fun clearTextfieldsFocus() {
        binding.viewCheckoutUrlTextfield.clearFocus()
        binding.baseUrlTextfield.clearFocus()
        binding.completeUrlTextfield.clearFocus()
        binding.cancelUrlTextfield.clearFocus()
        binding.paymentUrlTextfield.clearFocus()
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
                Toast.makeText(requireContext(), getString(R.string.camera_access_needed), Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(requireContext(), getString(R.string.camera_access_is_a_must), Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun observeStandaloneUrlPaymentProcess() {
        activity?.paymentViewModel?.richState?.observe(viewLifecycleOwner) { richState ->
            if (!richState.state.isFinal) {
                return@observe
            }

            childFragmentManager.popBackStack()

            binding.paymentResultImage.visibility = View.VISIBLE
            binding.paymentResultText.visibility = View.VISIBLE
            when (richState.state) {
                PaymentViewModel.State.COMPLETE -> {
                    binding.paymentResultImage.setImageResource(R.drawable.payment_success)
                    binding.paymentResultText.text = context?.getString(R.string.standalone_url_config_fragment_payment_completed)
                }
                PaymentViewModel.State.CANCELED -> {
                    binding.paymentResultImage.setImageResource(R.drawable.payment_failure)
                    binding.paymentResultText.text = context?.getString(R.string.standalone_url_config_fragment_payment_cancelled)
                }
                PaymentViewModel.State.FAILURE -> {
                    binding.paymentResultImage.setImageResource(R.drawable.payment_failure)
                    binding.paymentResultText.text = richState.exception?.message
                }
                else -> { }
            }
        }
    }
}