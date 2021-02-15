package com.swedbankpay.exampleapp

import android.app.Application
import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModelProvider
import com.swedbankpay.mobilesdk.PaymentViewModel
import com.swedbankpay.mobilesdk.merchantbackend.MerchantBackendProblem
import com.swedbankpay.mobilesdk.merchantbackend.RequestProblemException

private const val KEY_ERROR_MESSAGE = "KEY_ERROR_MESSAGE"

val FragmentActivity.mainViewModel get() = ViewModelProvider(this)[MainViewModel::class.java]

class MainViewModel(app: Application) : AndroidViewModel(app) {
    private val _currentErrorMessage = MutableLiveData<String?>()
    val currentErrorMessage: LiveData<String?> get() = _currentErrorMessage

    fun saveState(bundle: Bundle) {
        bundle.putString(KEY_ERROR_MESSAGE, _currentErrorMessage.value)
    }

    fun resumeFromSavedState(bundle: Bundle) {
        _currentErrorMessage.value = bundle.getString(KEY_ERROR_MESSAGE)
    }

    fun setErrorMessageFromState(richState: PaymentViewModel.RichState) {
        _currentErrorMessage.value =
            if (richState.state == PaymentViewModel.State.FAILURE) {
                StringBuilder().apply {
                    val exception = richState.exception as? RequestProblemException
                    val problem = exception?.problem as? MerchantBackendProblem
                    if (problem != null) {
                        append(getErrorDialogDescription(problem))
                        appendProblemId(problem.instance)
                    } else {
                        richState.terminalFailure?.let {
                            append(getApplication<Application>().getString(R.string.error_terminal_failure))
                            appendProblemId(it.messageId)
                        }
                    }
                }.toString()
            } else {
                null
            }
    }
    fun clearErrorMessage() {
        _currentErrorMessage.value = null
    }

    private fun getErrorDialogDescription(problem: MerchantBackendProblem) =
        getApplication<Application>().getString(
            when (problem) {
                is MerchantBackendProblem.Server -> R.string.error_server_problem
                is MerchantBackendProblem.Client -> R.string.error_client_problem
            }
        )

    private fun StringBuilder.appendProblemId(problemId: String?) {
        problemId?.let {
            append("\n\n")
            append(getApplication<Application>().getString(R.string.error_problem_id_format, it))
        }
    }
}