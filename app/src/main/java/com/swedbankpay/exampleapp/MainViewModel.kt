package com.swedbankpay.exampleapp

import android.app.Application
import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.*
import com.swedbankpay.mobilesdk.SwedbankPayProblem
import com.swedbankpay.mobilesdk.PaymentViewModel
import com.swedbankpay.mobilesdk.Problem
import com.swedbankpay.mobilesdk.UnknownProblem

private const val KEY_ERROR_MESSAGE = "KEY_ERROR_MESSAGE"

val FragmentActivity.mainViewModel get() = ViewModelProvider(this)[MainViewModel::class.java]

class MainViewModel(app: Application) : AndroidViewModel(app) {
    private val _currentErrorMessage = MutableLiveData<String>()
    val currentErrorMessage: LiveData<String> get() = _currentErrorMessage

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
                    val problem = richState.problem
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

    private fun getErrorDialogDescription(problem: Problem) =
        getApplication<Application>().getString(
            when (problem) {
                is Problem.Server -> R.string.error_server_problem
                is Problem.Client -> R.string.error_client_problem
            }
        )

    private val Problem.instance get() = when (this) {
        is SwedbankPayProblem -> instance
        is UnknownProblem -> instance
        else -> null
    }

    private fun StringBuilder.appendProblemId(problemId: String?) {
        problemId?.let {
            append("\n\n")
            append(getApplication<Application>().getString(R.string.error_problem_id_format, it))
        }
    }
}