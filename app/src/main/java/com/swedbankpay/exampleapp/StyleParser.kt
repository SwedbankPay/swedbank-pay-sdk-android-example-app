package com.swedbankpay.exampleapp

import com.google.gson.GsonBuilder

object StyleParser {
    private val gson = GsonBuilder()
        .setLenient()
        .create()

    fun parse(text: String) = gson.fromJson(text, Map::class.java)
}
