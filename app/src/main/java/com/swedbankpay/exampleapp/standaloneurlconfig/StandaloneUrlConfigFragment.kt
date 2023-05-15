package com.swedbankpay.exampleapp.standaloneurlconfig

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.swedbankpay.exampleapp.R
import com.swedbankpay.exampleapp.databinding.FragmentStandaloneUrlConfigBinding
import com.swedbankpay.mobilesdk.PaymentViewModel
import com.swedbankpay.mobilesdk.paymentViewModel

class StandaloneUrlConfigFragment: Fragment(R.layout.fragment_standalone_url_config) {
    private lateinit var binding: FragmentStandaloneUrlConfigBinding
    private lateinit var viewModel: StandaloneUrlConfigViewModel

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