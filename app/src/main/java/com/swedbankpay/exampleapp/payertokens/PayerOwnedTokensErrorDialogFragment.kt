package com.swedbankpay.exampleapp.payertokens

import android.app.Dialog
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import com.swedbankpay.exampleapp.R

class PayerOwnedTokensErrorDialogFragment : DialogFragment() {
    private val vm: PayerOwnedTokensViewModel by viewModels({ requireParentFragment() })

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return AlertDialog.Builder(requireContext())
            .apply {
                vm.getTokensMessage.value?.let {
                    setTitle(it.title)
                    setMessage(it.body)
                }
            }
            .setNeutralButton(R.string.close) { _, _ ->
                vm.clearErrorMessage()
                dismiss()
            }
            .create()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        vm.getTokensMessage.observe(viewLifecycleOwner) { message ->
            message?.let {
                (dialog as? AlertDialog)?.apply {
                    setTitle(it.title)
                    setMessage(context.getString(it.body))
                }
            }
        }
    }
}
