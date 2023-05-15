package com.swedbankpay.exampleapp.selectmethod

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.swedbankpay.exampleapp.R
import com.swedbankpay.exampleapp.databinding.FragmentSelectMethodBinding

class SelectMethodFragment: Fragment(R.layout.fragment_select_method) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val binding = FragmentSelectMethodBinding.bind(view)

        binding.backendSupportedFlowButton.setOnClickListener {
            findNavController().navigate(R.id.action_selectMethodFragment_to_productsFragment)
        }

        binding.standaloneUrlButton.setOnClickListener {
            findNavController().navigate(R.id.action_selectMethodFragment_to_standaloneUrlConfigFragment)
        }
    }
}