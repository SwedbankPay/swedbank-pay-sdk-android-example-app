package com.swedbankpay.exampleapp.standaloneurlconfig.swishprefill

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.swedbankpay.exampleapp.R
import com.swedbankpay.exampleapp.databinding.PrefillItemBinding
import com.swedbankpay.exampleapp.standaloneurlconfig.StandaloneUrlConfigViewModel
import com.swedbankpay.mobilesdk.nativepayments.exposedmodel.SwishPrefill

class SwishPrefillAdapter(
    private val viewModel: StandaloneUrlConfigViewModel,
    private val lifecycleOwner: LifecycleOwner,
    private val onItemClicked: (String) -> Unit
) :
    ListAdapter<SwishPrefill, SwishPrefillAdapter.SwishPrefillViewHolder>(UserDiffCallBack()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SwishPrefillViewHolder {
        return SwishPrefillViewHolder(
            PrefillItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )
    }

    override fun onBindViewHolder(holder: SwishPrefillViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class SwishPrefillViewHolder(private val binding: PrefillItemBinding) :
        ViewHolder(binding.root) {

        fun bind(prefill: SwishPrefill) {
            binding.lifecycleOwner = lifecycleOwner
            binding.viewModel = viewModel
            binding.prefillTextView.text =
                binding.root.context.getString(R.string.swish_with_prefill, prefill.msisdn)

            binding.prefillTextView.setOnClickListener {
                onItemClicked.invoke(prefill.msisdn ?: "")
            }
        }
    }

    private class UserDiffCallBack : DiffUtil.ItemCallback<SwishPrefill>() {
        override fun areItemsTheSame(oldItem: SwishPrefill, newItem: SwishPrefill): Boolean =
            oldItem == newItem

        override fun areContentsTheSame(oldItem: SwishPrefill, newItem: SwishPrefill): Boolean =
            oldItem == newItem
    }

}