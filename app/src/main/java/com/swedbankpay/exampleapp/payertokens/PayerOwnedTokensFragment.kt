package com.swedbankpay.exampleapp.payertokens

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import androidx.core.widget.ContentLoadingProgressBar
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.swedbankpay.exampleapp.R
import com.swedbankpay.exampleapp.products.ProductsViewModel
import com.swedbankpay.exampleapp.setTextIfNeeded

class PayerOwnedTokensFragment : Fragment(R.layout.fragment_get_payment_token) {

    companion object {
        private const val ALERT_TAG = "com.swedbankpay.exampleapp.payertokens.alert"
    }

    private val vm: PayerOwnedTokensViewModel by viewModels()
    private val productsVm: ProductsViewModel by viewModels({ requireActivity() })

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.apply {
            findViewById<Button>(R.id.payer_reference_current).setOnClickListener {
                vm.payerReference.value = productsVm.payerReference.value
            }
            findViewById<Button>(R.id.payer_reference_last_used).setOnClickListener {
                vm.payerReference.value = productsVm.getLastUsedPayerReference()
            }
            findViewById<Button>(R.id.payment_token_get).setOnClickListener {
                val environment = checkNotNull(productsVm.environment.value)
                vm.getTokens(environment.configuration)
            }

            findViewById<EditText>(R.id.payer_reference_input).apply {
                vm.payerReference.observe(viewLifecycleOwner, ::setTextIfNeeded)
                doAfterTextChanged {
                    vm.payerReference.value = it?.toString()?.takeUnless(String::isEmpty)
                }
            }

            findViewById<RecyclerView>(R.id.payment_tokens_recycler_view)
                .adapter = PayerOwnedTokensAdapter(this@PayerOwnedTokensFragment, vm)

            findViewById<ContentLoadingProgressBar>(R.id.updating_indicator).apply {
                vm.updating.observe(viewLifecycleOwner) {
                    if (it == true) {
                        show()
                    } else {
                        hide()
                    }
                }
            }
        }

        vm.onUsePaymentTokenPressed.observe(viewLifecycleOwner) {
            if (it != null) {
                productsVm.paymentToken.value = it
                findNavController().popBackStack()
            }
        }

        vm.onDeletePaymentTokenPressed.observe(viewLifecycleOwner) {
            if (it != null) {
                val environment = checkNotNull(productsVm.environment.value)
                vm.deleteToken(environment.configuration, it)
            }
        }

        vm.getTokensMessage.observe(viewLifecycleOwner) {
            if (it != null) {
                childFragmentManager.apply {
                    if (findFragmentByTag(ALERT_TAG) == null) {
                        PayerOwnedTokensErrorDialogFragment().show(this, ALERT_TAG)
                    }
                }
            }
        }
    }
}