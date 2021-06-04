package com.swedbankpay.exampleapp.cartsettings

import android.transition.TransitionManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.LifecycleOwner
import com.swedbankpay.exampleapp.R
import com.swedbankpay.exampleapp.databinding.StyleCellBinding
import com.swedbankpay.exampleapp.products.ProductsViewModel
import com.swedbankpay.exampleapp.setTextIfNeeded

class StyleCellViewHolder private constructor(
    adapter: CartAndSettingsAdapter,
    binding: StyleCellBinding
): CartAndSettingsAdapter.ViewHolder(binding.root) {
    constructor(adapter: CartAndSettingsAdapter, parent: ViewGroup) : this(
        adapter,
        StyleCellBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    )

    init {
        binding.setup(adapter.viewModel, adapter.lifecycleOwner)
    }

    private fun StyleCellBinding.setup(
        viewModel: ProductsViewModel,
        lifecycleOwner: LifecycleOwner
    ) {
        observeExpandedState(viewModel, lifecycleOwner)
        observeStyleError(viewModel, lifecycleOwner)
        bindStyleText(viewModel, lifecycleOwner)
        setOnClickListeners(viewModel)
    }

    private fun StyleCellBinding.observeExpandedState(
        viewModel: ProductsViewModel,
        lifecycleOwner: LifecycleOwner
    ) {
        viewModel.styleExpanded.observe(lifecycleOwner, {
            val expanded = it == true
            root.apply {
                (parent as? ViewGroup)?.let(TransitionManager::beginDelayedTransition)
                layoutParams = layoutParams.apply {
                    width = if (expanded) {
                        ViewGroup.LayoutParams.MATCH_PARENT
                    } else {
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    }
                }
            }
            styleExpandedStateWidgets.visibility =
                if (expanded) View.VISIBLE else View.GONE
        })
    }

    private fun StyleCellBinding.observeStyleError(
        viewModel: ProductsViewModel,
        lifecycleOwner: LifecycleOwner
    ) {
        viewModel.parsedStyle.observe(lifecycleOwner) {
            if (it?.isFailure == true) {
                styleError.setText(R.string.style_error)
            } else {
                styleError.text = null
            }
        }
    }

    private fun StyleCellBinding.bindStyleText(
        viewModel: ProductsViewModel,
        lifecycleOwner: LifecycleOwner
    ) {
        viewModel.styleText.observe(lifecycleOwner, styleInput::setTextIfNeeded)
        styleInput.doAfterTextChanged {
            viewModel.styleText.value = it?.toString().orEmpty()
        }
        styleInput.onFocusChangeListener = View.OnFocusChangeListener { _, _ ->
            viewModel.parseStyle()
        }
    }

    private fun StyleCellBinding.setOnClickListeners(
        viewModel: ProductsViewModel
    ) {
        openStyle.setOnClickListener {
            viewModel.styleExpanded.value = true
        }
        closeStyle.setOnClickListener {
            viewModel.styleExpanded.value = false
        }
    }
}
