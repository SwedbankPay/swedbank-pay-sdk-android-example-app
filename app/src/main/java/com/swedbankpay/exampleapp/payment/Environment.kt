package com.swedbankpay.exampleapp.payment

import androidx.annotation.StringRes
import com.swedbankpay.exampleapp.R

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
}
