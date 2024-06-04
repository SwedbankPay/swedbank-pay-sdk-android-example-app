package com.swedbankpay.exampleapp.standaloneurlconfig

import android.app.Application
import android.content.Context
import androidx.core.content.edit
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.map
import com.swedbankpay.exampleapp.util.ScanUrl
import com.swedbankpay.exampleapp.util.SwedbankPayConfig
import com.swedbankpay.mobilesdk.nativepayments.NativePayment
import com.swedbankpay.mobilesdk.nativepayments.exposedmodel.AvailableInstrument
import com.swedbankpay.mobilesdk.nativepayments.exposedmodel.PaymentAttemptInstrument


class StandaloneUrlConfigViewModel(application: Application) : AndroidViewModel(application) {
    var viewCheckoutUrl = MutableLiveData<String>()

    var baseUrl = MutableLiveData<String>()

    var completeUrl = MutableLiveData<String>()

    var cancelUrl = MutableLiveData<String>()

    var useCheckoutV3 = MutableLiveData<Boolean>()

    var paymentUrlAuthorityAndPath = MutableLiveData<String>()
    var paymentUrlScheme = MutableLiveData<String>()

    var swedbankPayConfiguration = MutableLiveData<SwedbankPayConfig>()

    // Native payments
    var sessionUrl = MutableLiveData<String>()

    var nativePaymentSessionInitiated = MutableLiveData(false)

    private val availableInstrument = MutableLiveData<List<AvailableInstrument>>()

    val abortPaymentInitiated = MutableLiveData(false)

    val isNativePaymentsLoading = MutableLiveData(false)

    // Swish
    val showSwish = availableInstrument.map {
        it.firstOrNull { instrument -> instrument is AvailableInstrument.Swish } != null
    }

    val showCreditCard = availableInstrument.map {
        it.firstOrNull { instrument -> instrument is AvailableInstrument.CreditCard } != null
    }

    val swishPrefills = availableInstrument.map {
        val swish = it.firstOrNull { instrument -> instrument is AvailableInstrument.Swish }
        if (swish != null) {
            (swish as AvailableInstrument.Swish).prefills
        } else listOf()
    }

    val creditCardPrefills = availableInstrument.map {
        val creditCard =
            it.firstOrNull { instrument -> instrument is AvailableInstrument.CreditCard }
        if (creditCard != null) {
            (creditCard as AvailableInstrument.CreditCard).prefills
        } else listOf()
    }

    val swishPaymentInitiated = MutableLiveData(false)

    var swishPhoneNumber = MutableLiveData<String?>()

    private var nativePayment: NativePayment? = null

    init {
        viewCheckoutUrl.value = ""
        baseUrl.value = getLastUsedBaseUrl()
        completeUrl.value = getLastUsedCompleteUrl()
        cancelUrl.value = getLastUsedCancelUrl()
        useCheckoutV3.value = getLastUsedV3Selection()
        paymentUrlAuthorityAndPath.value = getLastUsedPaymentUrlAuthorityAndPath()
    }

    fun onCheckoutPressed() {
        saveUrls()

        val paymentUrl = "${paymentUrlScheme.value}${paymentUrlAuthorityAndPath.value}"

        swedbankPayConfiguration.value = viewCheckoutUrl.value?.let {
            SwedbankPayConfig(
                url = it,
                baseUrl = baseUrl.value ?: "",
                completeUrl = completeUrl.value ?: "",
                cancelUrl = cancelUrl.value ?: "",
                isV3 = useCheckoutV3.value ?: true,
                paymentUrl = paymentUrl
            )
        }
    }

    fun onGetSessionPressed() {
        isNativePaymentsLoading.value = true
        nativePaymentSessionInitiated.value = true
        saveUrls()

        val paymentUrl = "${paymentUrlScheme.value}${paymentUrlAuthorityAndPath.value}"

        val configuration = SwedbankPayConfig(
            baseUrl = baseUrl.value ?: "",
            completeUrl = completeUrl.value ?: "",
            cancelUrl = cancelUrl.value ?: "",
            isV3 = useCheckoutV3.value ?: true,
            paymentUrl = paymentUrl
        )

        if (nativePayment == null) {
            nativePayment = NativePayment(configuration.orderInfo)
        }

        nativePayment?.startPaymentSession(sessionURL = sessionUrl.value ?: "")
    }

    fun setAvailableInstruments(availableInstruments: List<AvailableInstrument>) {
        availableInstrument.value = availableInstruments
    }

    fun startPaymentWith(instrument: PaymentAttemptInstrument) {
        isNativePaymentsLoading.value = true
        if (instrument is PaymentAttemptInstrument.Swish) {
            swishPaymentInitiated.value = true
        }
        nativePayment?.makePaymentAttempt(instrument = instrument)
    }

    fun abortNativePayment() {
        isNativePaymentsLoading.value = true
        abortPaymentInitiated.value = true
        nativePayment?.abortPaymentSession()
    }

    fun resetPayment() {
        viewCheckoutUrl.value = ""
        nativePaymentSessionInitiated.value = false
        availableInstrument.value = listOf()
        resetNativePaymentsInitiatedState()
        abortPaymentInitiated.value = false
        sessionUrl.value = ""
        swishPhoneNumber.value = ""
    }

    fun resetNativePaymentsInitiatedState() {
        swishPaymentInitiated.value = false
    }

    fun stopNativePaymentsLoading() {
        isNativePaymentsLoading.value = false
    }

    private fun saveUrls() {
        baseUrl.value?.let { saveLastUsedBaseUrl(it) }
        completeUrl.value?.let { saveLastUsedCompleteUrl(it) }
        cancelUrl.value?.let { saveLastUsedCancelUrl(it) }
        useCheckoutV3.value?.let { saveLastUsedV3Selection(it) }
    }

    fun saveUrl(url: String, type: ScanUrl) {
        when (type) {
            ScanUrl.Base -> {
                saveLastUsedBaseUrl(url)
            }

            ScanUrl.Complete -> {
                saveLastUsedCompleteUrl(url)
            }

            ScanUrl.Cancel -> {
                saveLastUsedCancelUrl(url)
            }

            ScanUrl.Payment -> {
                saveLastUsedPaymentUrlAuthorityAndPath(url)
            }

            else -> {}
        }
    }

    private fun getLastUsedBaseUrl(): String {
        return getApplication<Application>().getSharedPreferences(
            "StandaloneUrlConfig",
            Context.MODE_PRIVATE
        )?.getString("BASE_URL_CONFIG", null) ?: ""
    }

    private fun saveLastUsedBaseUrl(url: String) {
        getApplication<Application>().getSharedPreferences(
            "StandaloneUrlConfig",
            Context.MODE_PRIVATE
        )?.edit {
            putString("BASE_URL_CONFIG", url)
        }
    }

    private fun getLastUsedCompleteUrl(): String {
        return getApplication<Application>().getSharedPreferences(
            "StandaloneUrlConfig",
            Context.MODE_PRIVATE
        )?.getString("COMPLETE_URL_CONFIG", null) ?: ""
    }

    private fun saveLastUsedCompleteUrl(url: String) {
        getApplication<Application>().getSharedPreferences(
            "StandaloneUrlConfig",
            Context.MODE_PRIVATE
        )?.edit {
            putString("COMPLETE_URL_CONFIG", url)
        }
    }

    private fun getLastUsedCancelUrl(): String {
        return getApplication<Application>().getSharedPreferences(
            "StandaloneUrlConfig",
            Context.MODE_PRIVATE
        )?.getString("CANCEL_URL_CONFIG", null) ?: ""
    }

    private fun saveLastUsedCancelUrl(url: String) {
        getApplication<Application>().getSharedPreferences(
            "StandaloneUrlConfig",
            Context.MODE_PRIVATE
        )?.edit {
            putString("CANCEL_URL_CONFIG", url)
        }
    }

    private fun getLastUsedV3Selection(): Boolean {
        return getApplication<Application>().getSharedPreferences(
            "StandaloneUrlConfig",
            Context.MODE_PRIVATE
        )?.getBoolean("V3_CONFIG", true) ?: true
    }

    private fun saveLastUsedV3Selection(isChecked: Boolean) {
        getApplication<Application>().getSharedPreferences(
            "StandaloneUrlConfig",
            Context.MODE_PRIVATE
        )?.edit {
            putBoolean("V3_CONFIG", isChecked)
        }
    }

    private fun getLastUsedPaymentUrlAuthorityAndPath(): String {
        return getApplication<Application>().getSharedPreferences(
            "StandaloneUrlConfig",
            Context.MODE_PRIVATE
        )?.getString("PAYMENT_URL_CONFIG", null) ?: ""
    }

    private fun saveLastUsedPaymentUrlAuthorityAndPath(url: String) {
        getApplication<Application>().getSharedPreferences(
            "StandaloneUrlConfig",
            Context.MODE_PRIVATE
        )?.edit {
            putString("PAYMENT_URL_CONFIG", url)
        }
    }
}