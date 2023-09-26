package com.swedbankpay.exampleapp.payertokens

import android.app.Application
import androidx.annotation.StringRes
import androidx.lifecycle.*
import com.swedbankpay.exampleapp.R
import com.swedbankpay.mobilesdk.merchantbackend.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class PayerOwnedTokensViewModel(app: Application) : AndroidViewModel(app) {
    class Message(
        @StringRes val title: Int,
        @StringRes val body: Int
    )

    private val updateJob = MutableLiveData<Job?>()

    private val _onUsePaymentTokenPressed = MutableLiveData<String?>()
    val onUsePaymentTokenPressed: LiveData<String?> get() = _onUsePaymentTokenPressed

    private val _onDeletePaymentTokenPressed = MutableLiveData<Int?>()
    val onDeletePaymentTokenPressed: LiveData<Int?> get() = _onDeletePaymentTokenPressed

    val updating = updateJob.map { it != null }

    val payerReference = MutableLiveData<String?>()

    private val _paymentTokens = MutableLiveData<List<PaymentTokenInfo>?>()
    val paymentTokens: LiveData<List<PaymentTokenInfo>?> get() = _paymentTokens

    private val _getTokensMessage = MutableLiveData<Message?>()
    val getTokensMessage: LiveData<Message?> get() = _getTokensMessage

    fun getTokens(configuration: MerchantBackendConfiguration) {
        payerReference.value?.takeUnless { it.isEmpty() }?.let { payerReference ->
            updateJob.value?.cancel()
            updateJob.value = viewModelScope.launch {
                getTokens(configuration, payerReference)
                updateJob.value = null
            }
        }
    }

    fun deleteToken(configuration: MerchantBackendConfiguration, index: Int) {
        paymentTokens.value?.get(index)?.let {
            updateJob.value?.cancel()
            updateJob.value = viewModelScope.launch {
                deleteToken(configuration, index, it)
                updateJob.value = null
            }
        }
    }

    private suspend fun getTokens(
        configuration: MerchantBackendConfiguration,
        payerReference: String
    ) {
        try {
            val response = MerchantBackend.getPayerOwnedPaymentTokens(
                getApplication(),
                configuration,
                payerReference
            )
            val tokens = response.payerOwnedPaymentTokens.paymentTokens
            _paymentTokens.value = tokens
            if (tokens.isEmpty()) {
                _getTokensMessage.value = Message(
                    R.string.no_tokens_note_title,
                    R.string.no_tokens_note_body
                )
            }
        } catch (e: RequestProblemException) {
            val errorMessage = when (e.problem) {
                is MerchantBackendProblem.Client.MobileSDK.Unauthorized ->
                    R.string.get_tokens_error_not_supported
                else ->
                    R.string.get_tokens_error_other
            }
            _getTokensMessage.value = Message(
                R.string.get_tokens_error_title,
                errorMessage
            )
        } catch (e: Exception) {
            _getTokensMessage.value = Message(
                R.string.get_tokens_error_title,
                R.string.get_tokens_error_other
            )
        }
    }

    private suspend fun deleteToken(
        configuration: MerchantBackendConfiguration,
        index: Int,
        info: PaymentTokenInfo
    ) {
        try {
            MerchantBackend.deletePayerOwnerPaymentToken(
                getApplication(),
                configuration,
                info,
                "User deleted from example app"
            )
            _paymentTokens.apply {
                value = value?.toMutableList()?.also {
                    it.removeAt(index)
                }
            }
        } catch (e: Exception) {}
    }

    fun clearErrorMessage() {
        _getTokensMessage.value = null
    }

    fun onUsePaymentTokenPressed(paymentToken: String) {
        _onUsePaymentTokenPressed.apply {
            value = paymentToken
            value = null
        }
    }

    fun onDeletePaymentTokenPressed(index: Int) {
        _onDeletePaymentTokenPressed.apply {
            value = index
            value = null
        }
    }
}