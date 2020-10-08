package com.swedbankpay.exampleapp.payment

enum class Environment(val backendUrl: String) {
    STAGE("https://stage-dot-payex-merchant-samples.ey.r.appspot.com/"),
    EXTERNAL_INTEGRATION("https://payex-merchant-samples.ey.r.appspot.com")
}
