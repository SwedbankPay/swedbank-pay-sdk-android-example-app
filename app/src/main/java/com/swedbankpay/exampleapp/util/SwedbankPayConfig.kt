package com.swedbankpay.exampleapp.util

import android.content.Context
import android.os.Parcelable
import com.swedbankpay.mobilesdk.*
import kotlinx.android.parcel.Parcelize

@Parcelize
class SwedbankPayConfig(
    val url: String,
    val baseUrl: String,
    val completeUrl: String,
    val cancelUrl: String,
    val paymentUrl: String = "https://consid.mobi/payment/android",
    val isV3: Boolean,
) : Configuration(), Parcelable {

    val orderInfo: ViewPaymentOrderInfo
        get() {
            return ViewPaymentOrderInfo(
                viewPaymentLink = url,
                webViewBaseUrl = baseUrl,
                completeUrl = completeUrl,
                cancelUrl = cancelUrl,
                paymentUrl = paymentUrl,
                isV3 = isV3
            )
        }

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
        return orderInfo
    }
}