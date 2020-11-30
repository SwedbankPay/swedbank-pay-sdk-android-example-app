package com.swedbankpay.exampleapp.payment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import com.swedbankpay.exampleapp.R
import com.swedbankpay.mobilesdk.PaymentViewModel
import com.swedbankpay.mobilesdk.merchantbackend.InvalidInstrumentException
import com.swedbankpay.mobilesdk.paymentViewModel
import kotlinx.android.synthetic.main.fragment_payment.view.*

class PaymentContainerFragment : Fragment(R.layout.fragment_payment) {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return super.onCreateView(inflater, container, savedInstanceState).also {
            val fm = childFragmentManager
            if (fm.findFragmentById(R.id.sdk_payment_fragment) == null) {
                val paymentFragment = MyPaymentFragment()
                paymentFragment.arguments = arguments
                fm.beginTransaction()
                    .add(R.id.sdk_payment_fragment, paymentFragment)
                    .commit()
            }

            val vm = requireActivity().paymentViewModel
            vm.observeInstruments()
            vm.observeUpdating()
            vm.observeUpdateError()
        }
    }

    private fun PaymentViewModel.observeInstruments() {
        val observer = Observer<Any?> {
            refreshInstrumentUI()
        }
        richState.observe(viewLifecycleOwner, observer)
        showingPaymentMenu.observe(viewLifecycleOwner, observer)
    }

    private fun PaymentViewModel.observeUpdating() {
        state.observe(viewLifecycleOwner) {
            val view = requireView()
            if (it == PaymentViewModel.State.UPDATING_PAYMENT_ORDER) {
                view.updating_background.visibility = View.VISIBLE
                view.updating_indicator.show()
            } else {
                view.updating_background.visibility = View.INVISIBLE
                view.updating_indicator.hide()
            }
        }
    }

    private fun refreshInstrumentUI() {
        val view = requireView()
        val title = view.instrument_title
        val spinner = view.instrument_spinner

        spinner.onItemSelectedListener = null

        val activity = requireActivity()
        val vm = activity.paymentViewModel
        val info = vm.richState.value?.viewPaymentOrderInfo
        val instruments = info?.availableInstruments.orEmpty()
        val showInstrumentUI = instruments.size > 1 && vm.showingPaymentMenu.value == true

        if (!showInstrumentUI) {
            title.visibility = View.GONE
            spinner.visibility = View.GONE
            spinner.adapter = null
        } else {
            title.visibility = View.VISIBLE
            spinner.visibility = View.VISIBLE

            spinner.adapter = ArrayAdapter(
                activity,
                android.R.layout.simple_spinner_item,
                instruments
            )
            val currentInstrumentIndex = instruments.indexOf(info?.instrument)
            if (currentInstrumentIndex > 0) {
                spinner.setSelection(currentInstrumentIndex)
            }

            spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    setInstrument(instruments[position])
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }
        }
    }

    private fun setInstrument(instrument: String) {
        val vm = requireActivity().paymentViewModel
        vm.richState.value?.viewPaymentOrderInfo?.let {
            if (it.instrument != instrument) {
                vm.updatePaymentOrder(instrument)
            }
        }
    }

    private fun PaymentViewModel.observeUpdateError() {
        richState.observe(viewLifecycleOwner) {
            it.updateException?.let {
                val message = getUpdateErrorMessage(it)
                val fm = childFragmentManager
                if (fm.findFragmentByTag(UpdatePaymentOrderErrorDialogFragment.TAG) == null) {
                    UpdatePaymentOrderErrorDialogFragment
                        .newInstance(message)
                        .show(fm, UpdatePaymentOrderErrorDialogFragment.TAG)
                }
            }
        }
    }

    private fun getUpdateErrorMessage(exception: Exception): String {
        return if (
            exception is InvalidInstrumentException
        ) {
            requireContext().getString(
                R.string.invalid_instrument_format,
                exception.instrument
            )
        } else {
            requireContext().getString(R.string.update_instrument_failed)
        }
    }

    internal class UpdatePaymentOrderErrorDialogFragment : DialogFragment() {
        companion object {
            const val ARG_MESSAGE = "M"
            const val TAG = "com.swedbankpay.exampleapp.updatealert"

            fun newInstance(message: String) = UpdatePaymentOrderErrorDialogFragment().apply {
                arguments = Bundle(1).apply {
                    putString(ARG_MESSAGE, message)
                }
            }
        }

        override fun onCreateDialog(savedInstanceState: Bundle?) =
            AlertDialog.Builder(requireContext(), theme)
                .setTitle(R.string.swedbankpaysdk_error_dialog_title)
                .setMessage(requireArguments().getString(ARG_MESSAGE))
                .setNeutralButton(R.string.swedbankpaysdk_dialog_close) { dialog, _ ->
                    dialog.dismiss()
                }
                .create()
    }

}