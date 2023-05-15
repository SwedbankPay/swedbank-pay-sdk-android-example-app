package com.swedbankpay.exampleapp.util

import android.content.Context
import android.os.Parcelable
import com.swedbankpay.mobilesdk.*
import kotlinx.android.parcel.Parcelize

@Parcelize
class SwedbankPayConfig(
    private val url: String,
    private val baseUrl: String,
    private val completeUrl: String,
    private val cancelUrl: String,
    private val isV3: Boolean
) : Configuration(), Parcelable {

    override suspend fun postConsumers(
        context: Context,
        consumer: Consumer?,
        userData: Any?
    ): ViewConsumerIdentificationInfo {
        throw Exception()
    }

    override suspend fun postPaymentorders(
        context: Context,
        paymentOrder: PaymentOrder?,
        userData: Any?,
        consumerProfileRef: String?
    ): ViewPaymentOrderInfo {
        return ViewPaymentOrderInfo(
            viewPaymentLink = url,
            webViewBaseUrl = baseUrl,
            completeUrl = completeUrl,
            cancelUrl = cancelUrl,
            paymentUrl = null,
            isV3 = isV3
        )
    }
}