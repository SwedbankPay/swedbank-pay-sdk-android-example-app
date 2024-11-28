package com.swedbankpay.exampleapp.standaloneurlconfig

import com.swedbankpay.exampleapp.util.SwedbankPayConfig
import com.swedbankpay.mobilesdk.Configuration
import com.swedbankpay.mobilesdk.PaymentFragment

class SwedbankPayConfigFragment : PaymentFragment()  {
    private val TAG = SwedbankPayConfigFragment::class.java.simpleName

    override fun getConfiguration(): Configuration {
        return arguments?.getParcelable<SwedbankPayConfig>(PaymentFragment.ARG_USER_DATA) ?: SwedbankPayConfig(url = "", baseUrl = "", completeUrl = "", cancelUrl = "", isV3 = true)
    }
}