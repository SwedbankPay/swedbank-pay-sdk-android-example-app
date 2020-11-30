package com.swedbankpay.exampleapp.payertokens

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.swedbankpay.exampleapp.R
import com.swedbankpay.mobilesdk.merchantbackend.PaymentTokenInfo

class PayerOwnedTokensAdapter(
    lifecycleOwner: LifecycleOwner,
    private val vm: PayerOwnedTokensViewModel
) : ListAdapter<PaymentTokenInfo, PayerOwnedTokensAdapter.ViewHolder>(DiffCallback) {

    init {
        vm.paymentTokens.observe(lifecycleOwner) {
            it?.let(::submitList)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.get_payment_token_cell, parent, false)
        ).also { viewHolder ->
            viewHolder.itemView.findViewById<Button>(R.id.use).setOnClickListener {
                onUsePressed(viewHolder)
            }
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val info = getItem(position)
        holder.name.text = info.instrument
        holder.number.text = info.instrumentDisplayName
    }

    private fun onUsePressed(holder: ViewHolder) {
        vm.onUsePaymentTokenPressed(getItem(holder.adapterPosition).paymentToken)
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val name = itemView.findViewById<TextView>(R.id.name)
        val number = itemView.findViewById<TextView>(R.id.number)
    }

    object DiffCallback : DiffUtil.ItemCallback<PaymentTokenInfo>() {
        override fun areItemsTheSame(
            oldItem: PaymentTokenInfo,
            newItem: PaymentTokenInfo
        ) = oldItem.paymentToken == newItem.paymentToken

        override fun areContentsTheSame(
            oldItem: PaymentTokenInfo,
            newItem: PaymentTokenInfo
        ) = oldItem == newItem
    }
}