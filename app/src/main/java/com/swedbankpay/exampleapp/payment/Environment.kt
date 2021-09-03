package com.swedbankpay.exampleapp.payment

import androidx.annotation.StringRes
import com.swedbankpay.exampleapp.R
import com.swedbankpay.mobilesdk.merchantbackend.RequestDecorator
import com.swedbankpay.mobilesdk.merchantbackend.MerchantBackendConfiguration

enum class Environment(
    val backendUrl: String,
    @StringRes val displayName: Int
) {
    STAGE(
        "https://stage-dot-payex-merchant-samples.ey.r.appspot.com/",
        R.string.env_stage
    ),
    EXTERNAL_INTEGRATION(
        "https://payex-merchant-samples.ey.r.appspot.com",
        R.string.env_ext_integration
    ),
    PAYMENTPAGES_EXTERNAL_INTEGRATION(
        "https://pp-dot-payex-merchant-samples.ey.r.appspot.com",
        R.string.env_pp_ext_integration
    );

    val configuration = MerchantBackendConfiguration.Builder(backendUrl)
        .requestDecorator(
            RequestDecorator.withHeaders(
                "x-payex-sample-apikey", "c339f53d-8a36-4ea9-9695-75048e592cc0",
                "x-payex-sample-access-token", "token123")
        )
        .build()
}
