package com.swedbankpay.exampleapp.standaloneurlconfig

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.swedbankpay.exampleapp.R
import com.swedbankpay.exampleapp.databinding.FragmentStandaloneUrlConfigBinding
import com.swedbankpay.exampleapp.standaloneurlconfig.camera.CameraActivity
import com.swedbankpay.exampleapp.standaloneurlconfig.camera.CameraActivity.Companion.SCANNED_URL_KEY
import com.swedbankpay.exampleapp.util.PermissionUtil
import com.swedbankpay.mobilesdk.PaymentViewModel
import com.swedbankpay.mobilesdk.paymentViewModel

class StandaloneUrlConfigFragment: Fragment(R.layout.fragment_standalone_url_config) {
    private lateinit var binding: FragmentStandaloneUrlConfigBinding
    private lateinit var viewModel: StandaloneUrlConfigViewModel

    private var lastClickedBtn: String = "none"

    companion object {
        const val payment = "payment"
        const val base = "base"
        const val complete = "complete"
        const val cancel = "cancel"
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(this)[StandaloneUrlConfigViewModel::class.java]

        binding = FragmentStandaloneUrlConfigBinding.bind(view)

        binding.lifecycleOwner = this
        binding.viewModel = viewModel

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

        binding.paymentUrlScannerButton.setOnClickListener {
            lastClickedBtn = payment
            scanQR()
        }
        binding.baseUrlScannerButton.setOnClickListener {
            lastClickedBtn = base
            scanQR()
        }
        binding.completeUrlScannerButton.setOnClickListener {
            lastClickedBtn = complete
            scanQR()
        }
        binding.cancelUrlScannerButton.setOnClickListener {
            lastClickedBtn = cancel
            scanQR()
        }
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
            when (lastClickedBtn) {
                payment -> binding.viewPaymentUrl.setText(passedUrl)
                base -> binding.baseUrl.setText(passedUrl)
                complete -> binding.completeUrl.setText(passedUrl)
                cancel -> binding.cancelUrl.setText(passedUrl)
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