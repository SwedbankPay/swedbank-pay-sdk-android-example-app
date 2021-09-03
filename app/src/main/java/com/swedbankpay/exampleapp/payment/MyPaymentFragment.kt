package com.swedbankpay.exampleapp.payment

import com.swedbankpay.mobilesdk.Configuration
import com.swedbankpay.mobilesdk.PaymentFragment

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
        return env.configuration
    }
}