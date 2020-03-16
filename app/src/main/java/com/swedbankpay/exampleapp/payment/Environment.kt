package com.swedbankpay.exampleapp.payment

enum class Environment(val backendUrl: String) {
    STAGE("https://stage-dot-payex-merchant-samples.appspot.com/"),
    EXTERNAL_INTEGRATION("https://payex-merchant-samples.appspot.com")
}