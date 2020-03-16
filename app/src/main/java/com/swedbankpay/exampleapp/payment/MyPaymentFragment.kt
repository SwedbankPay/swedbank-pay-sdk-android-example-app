package com.swedbankpay.exampleapp.payment

import com.swedbankpay.mobilesdk.Configuration
import com.swedbankpay.mobilesdk.PaymentFragment
import com.swedbankpay.mobilesdk.RequestDecorator
import com.swedbankpay.mobilesdk.UserHeaders

class MyPaymentFragment : PaymentFragment() {
    companion object {
        const val ARG_ENVIRONMENT = "com.swedbankpay.exampleapp.ARG_ENVIRONMENT"
    }

    override fun getConfiguration(): Configuration {
        // N.B! In most cases you do not need this kind of dynamic
        // Configurations, and can instead set PaymentFragment.defaultConfiguration
        // and use PaymentFragment without subclassing.
        val envOrdinal = requireArguments().getInt(ARG_ENVIRONMENT)
        val env = Environment.values()[envOrdinal]
        return Configuration.Builder(env.backendUrl)
            .requestDecorator(object : RequestDecorator() {
                override suspend fun decorateAnyRequest(userHeaders: UserHeaders, method: String, url: String, body: String?) {
                    userHeaders
                        .add("x-payex-sample-apikey", "c339f53d-8a36-4ea9-9695-75048e592cc0")
                        .add("x-payex-sample-access-token", "token123")
                }
            })
            .build()
    }
}