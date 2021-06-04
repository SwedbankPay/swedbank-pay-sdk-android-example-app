package com.swedbankpay.exampleapp.cartsettings

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.swedbankpay.exampleapp.R
import com.swedbankpay.exampleapp.products.productsViewModel

class CartAndSettingsFragment : Fragment(R.layout.fragment_cart_and_settings) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val recyclerView = view.findViewById<RecyclerView>(R.id.recyclerView)
        recyclerView.adapter = CartAndSettingsAdapter(
            this,
            requireActivity().productsViewModel
        )
        recyclerView.addItemDecoration(CartAndSettingsItemDecoration(requireContext()))
    }

    override fun onStart() {
        super.onStart()
        parseStyle()
    }

    override fun onResume() {
        super.onResume()
        parseStyle()
    }

    private fun parseStyle() {
        requireActivity().productsViewModel.parseStyle()
    }
}
