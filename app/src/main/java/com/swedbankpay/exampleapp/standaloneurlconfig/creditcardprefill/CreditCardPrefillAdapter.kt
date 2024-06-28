package com.swedbankpay.exampleapp.standaloneurlconfig.creditcardprefill

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.swedbankpay.exampleapp.R
import com.swedbankpay.exampleapp.databinding.PrefillItemBinding
import com.swedbankpay.exampleapp.standaloneurlconfig.StandaloneUrlConfigViewModel
import com.swedbankpay.mobilesdk.paymentsession.exposedmodel.CreditCardPrefill

class CreditCardPrefillAdapter(
    private val viewModel: StandaloneUrlConfigViewModel,
    private val lifecycleOwner: LifecycleOwner,
    private val onItemClicked: (CreditCardPrefill) -> Unit
) :
    ListAdapter<CreditCardPrefill, CreditCardPrefillAdapter.CreditCardPrefillViewHolder>(
        UserDiffCallBack()
    ) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CreditCardPrefillViewHolder {
        return CreditCardPrefillViewHolder(
            PrefillItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )
    }

    override fun onBindViewHolder(holder: CreditCardPrefillViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class CreditCardPrefillViewHolder(private val binding: PrefillItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(prefill: CreditCardPrefill) {
            binding.lifecycleOwner = lifecycleOwner
            binding.viewModel = viewModel
            binding.prefillTextView.text =
                binding.root.context.getString(
                    R.string.credit_card_with_prefill,
                    prefill.cardBrand,
                    prefill.maskedPan,
                    prefill.expiryString
                )

            binding.prefillTextView.setOnClickListener {
                onItemClicked.invoke(prefill)
            }
        }
    }

    private class UserDiffCallBack : DiffUtil.ItemCallback<CreditCardPrefill>() {
        override fun areItemsTheSame(
            oldItem: CreditCardPrefill,
            newItem: CreditCardPrefill
        ): Boolean =
            oldItem == newItem

        override fun areContentsTheSame(
            oldItem: CreditCardPrefill,
            newItem: CreditCardPrefill
        ): Boolean =
            oldItem == newItem
    }

}