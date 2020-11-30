package com.swedbankpay.exampleapp

import android.widget.EditText

fun EditText.setTextIfNeeded(text: String?) {
    if (text != this.text?.toString()) {
        setText(text)
        setSelection(length())
    }
}