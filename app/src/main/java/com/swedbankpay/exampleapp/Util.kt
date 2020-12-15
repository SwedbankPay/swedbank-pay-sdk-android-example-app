package com.swedbankpay.exampleapp

import android.widget.EditText

fun EditText.setTextIfNeeded(text: String?) {
    if (text.orEmpty() != this.text?.toString().orEmpty()) {
        setText(text.orEmpty())
        setSelection(length())
    }
}
