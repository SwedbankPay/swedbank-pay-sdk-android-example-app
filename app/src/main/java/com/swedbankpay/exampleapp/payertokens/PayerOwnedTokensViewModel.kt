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

    private val getTokensJob = MutableLiveData<Job>()

    private val _onUsePaymentTokenPressed = MutableLiveData<String?>()
    val onUsePaymentTokenPressed: LiveData<String?> get() = _onUsePaymentTokenPressed

    val updating = Transformations.map(getTokensJob) { it != null }

    val payerReference = MutableLiveData<String>()

    private val _paymentTokens = MutableLiveData<List<PaymentTokenInfo>>()
    val paymentTokens: LiveData<List<PaymentTokenInfo>> get() = _paymentTokens

    private val _getTokensMessage = MutableLiveData<Message>()
    val getTokensMessage: LiveData<Message> get() = _getTokensMessage

    fun getTokens(configuration: MerchantBackendConfiguration) {
        if (getTokensJob.value == null) {
            payerReference.value?.takeUnless { it.isEmpty() }?.let { payerReference ->
                getTokensJob.value = viewModelScope.launch {
                    getTokens(configuration, payerReference)
                    getTokensJob.value = null
                }
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

    fun clearErrorMessage() {
        _getTokensMessage.value = null
    }

    fun onUsePaymentTokenPressed(paymentToken: String) {
        _onUsePaymentTokenPressed.apply {
            value = paymentToken
            value = null
        }
    }
}