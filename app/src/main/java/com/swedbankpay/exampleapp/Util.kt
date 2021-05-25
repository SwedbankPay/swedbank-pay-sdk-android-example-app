package com.swedbankpay.exampleapp

import android.content.Context
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import com.swedbankpay.mobilesdk.PaymentInstruments
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.javaField

fun View.hideSoftKeyboard() {
    val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    imm.hideSoftInputFromWindow(windowToken, InputMethodManager.HIDE_IMPLICIT_ONLY)
}

fun EditText.setTextIfNeeded(text: String?) {
    if (text.orEmpty() != this.text?.toString().orEmpty()) {
        setText(text.orEmpty())
        setSelection(length())
    }
}

val PaymentInstruments.all: List<String>
    get() = PaymentInstruments::class.memberProperties.mapNotNull { property ->
        // const properties become static jvm fields:
        // https://kotlinlang.org/docs/java-to-kotlin-interop.html#static-fields
        // This can be done in "pure" kotlin reflection,
        // but becomes a bit hairy. Punting over to jvm reflection
        // makes for cleaner code.
        property.takeIf { it.isConst }?.javaField?.get(null) as? String
    }