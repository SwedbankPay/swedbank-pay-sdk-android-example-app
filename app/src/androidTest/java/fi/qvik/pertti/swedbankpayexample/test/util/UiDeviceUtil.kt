package fi.qvik.pertti.swedbankpayexample.test.util

import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiScrollable
import androidx.test.uiautomator.UiSelector

@Suppress("unused")
fun UiDevice.findScrollable(selector: UiSelector): UiScrollable {
    // UiScrollable actually requires an initialized UiDevice to function,
    // but does not take one as an argument.
    return UiScrollable(selector)
}