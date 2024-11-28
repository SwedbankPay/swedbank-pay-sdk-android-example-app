package com.swedbankpay.exampleapp.standaloneurlconfig.webbasedinstrument

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.swedbankpay.exampleapp.R
import com.swedbankpay.exampleapp.databinding.DefaultListItemBinding
import com.swedbankpay.exampleapp.standaloneurlconfig.StandaloneUrlConfigViewModel
import com.swedbankpay.mobilesdk.paymentsession.exposedmodel.AvailableInstrument

class WebBasedInstrumentAdapter(
    private val viewModel: StandaloneUrlConfigViewModel,
    private val lifecycleOwner: LifecycleOwner,
    private val onItemClicked: (AvailableInstrument) -> Unit
) :
    ListAdapter<AvailableInstrument, WebBasedInstrumentAdapter.WebBasedInstrumentViewHolder>(
        WebBasedDiffCallBack()
    ) {

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): WebBasedInstrumentViewHolder {
        return WebBasedInstrumentViewHolder(
            DefaultListItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )
    }

    override fun onBindViewHolder(holder: WebBasedInstrumentViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class WebBasedInstrumentViewHolder(private val binding: DefaultListItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(availableInstrument: AvailableInstrument) {
            binding.lifecycleOwner = lifecycleOwner
            binding.viewModel = viewModel
            binding.textView.text = binding.root.context.getString(
                R.string.web_based_instrument,
                availableInstrument.paymentMethod
            )
            binding.textView.setOnClickListener {
                onItemClicked.invoke(availableInstrument)
            }
        }
    }

    private class WebBasedDiffCallBack : DiffUtil.ItemCallback<AvailableInstrument>() {
        override fun areItemsTheSame(
            oldItem: AvailableInstrument,
            newItem: AvailableInstrument
        ): Boolean =
            oldItem == newItem

        override fun areContentsTheSame(
            oldItem: AvailableInstrument,
            newItem: AvailableInstrument
        ): Boolean =
            oldItem.paymentMethod == newItem.paymentMethod
    }

}