package com.swedbankpay.exampleapp.products

import android.app.AlertDialog
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.swedbankpay.exampleapp.R
import com.swedbankpay.exampleapp.cartsettings.CartAndSettingsFragment
import kotlinx.android.synthetic.main.dialog_price_cell.view.*
import kotlinx.android.synthetic.main.fragment_products.view.*

class ProductsFragment : Fragment(R.layout.fragment_products) {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // N.B! Add observers in onCreate, not onCreateView/onViewCreated.
        // This way they only get added once, even if the view
        // is destroyed and recreated.
        val viewModel = requireActivity().productsViewModel

        viewModel.onCloseCartPressed.observe(this, {
            if (it != null) childFragmentManager.popBackStack()
        })

        viewModel.onCheckOutPressed.observe(this, {
            if (it != null) {
                navigateToPayment()
            }
        })
        viewModel.onAdjustPricePressed.observe(this, Observer {
            it ?: return@Observer
            activity?.let {
                val inflater = it.layoutInflater
                val builder = AlertDialog.Builder(it)
                val view = inflater.inflate(R.layout.dialog_price_cell, null)
                builder.setView(view)
                builder.apply { 
                    setPositiveButton("Set price") { dialog, _ ->
                        val input:String = view.input_decimal.text.toString().ifEmpty { "0" }
                        var price:Double = input.toDouble()
                        price *= 100
                        viewModel.adjustedPrice.value = price.toInt()
                        dialog.dismiss()
                    }
                    setNegativeButton("Reset") { dialog, _ ->
                        viewModel.adjustedPrice.value = null
                        dialog.dismiss()
                    }
                    setTitle("Set total price (Including VAT)")
                    
                }
                builder.create().show()
            }
        })
        viewModel.onGetPaymentTokenPressed.observe(this) {
            if (it != null) {
                findNavController()
                    .navigate(R.id.action_productsFragment_to_getPaymentTokenFragment)
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        childFragmentManager.apply {
            if (findFragmentById(R.id.cart_and_settings) == null) {
                val cartAndSettingsFragment = CartAndSettingsFragment()
                beginTransaction()
                    .add(R.id.cart_and_settings, cartAndSettingsFragment)
                    .hide(cartAndSettingsFragment)
                    .commit()
            }
        }

        view.recyclerView.adapter = ProductsAdapter(
            this,
            requireActivity().productsViewModel
        )

        view.open_cart.setOnClickListener {
            childFragmentManager.apply {
                val cartAndSettingsFragment =
                    checkNotNull(findFragmentById(R.id.cart_and_settings))

                if (cartAndSettingsFragment.isHidden) {
                    beginTransaction()
                        .addToBackStack(null)
                        .show(cartAndSettingsFragment)
                        .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                        .commit()
                }
            }
        }
    }

    private fun navigateToPayment() {
        // A MediatorLiveData only updates when it has an active observer
        val arguments = requireActivity().productsViewModel.paymentFragmentArguments.run {
            if (!hasActiveObservers()) {
                val observer = Observer<Any> {}
                observeForever(observer)
                removeObserver(observer)
            }
            value
        }
        if (arguments != null) {
            requireActivity().productsViewModel.apply {
                payerReference.value?.let(::saveLastUsedPayerReference)
            }
            findNavController().navigate(R.id.action_productsFragment_to_paymentFragment, arguments)
        }
    }
}
