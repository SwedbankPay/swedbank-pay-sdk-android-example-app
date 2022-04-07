package fi.qvik.pertti.swedbankpayexample.test.util

import android.os.SystemClock
import android.view.KeyEvent
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject
import org.junit.Assert

private const val textChangeTimeout = 5000L
private const val timeout = 30_000L

internal fun UiObject.clickUntilCheckedAndAssert(timeout: Long) {
    Assert.assertTrue(
        "Could not check $selector",
        clickUntilTrue(timeout, ::isChecked)
    )
}

internal fun UiObject.clickUntilFocusedAndAssert(timeout: Long) {
    Assert.assertTrue(
        "Could not focus $selector",
        clickUntilTrue(timeout, ::isFocused)
    )
}

private fun UiObject.clickUntilTrue(timeout: Long, condition: () -> Boolean): Boolean {
    return retryUntilTrue(timeout) {
        click() && condition()
    }
}

fun inputText(device: UiDevice, widget: UiObject, text: String) {
    widget.text = text
    widget.clickUntilFocusedAndAssert(timeout)
    if (widget.text != null && widget.text != "") {
        // We have succeeded to input something, but can't verify its correct since JS reformatting.
        return
    }
    
    for (c in text) {
        val oldText = widget.text
        device.pressKeyCode(KeyEvent.keyCodeFromString("KEYCODE_$c"))
        Assert.assertTrue("Could not input '$c' to $widget", waitForTextChanged(widget, oldText))
    }
}

private fun waitForTextChanged(widget: UiObject, oldText: String): Boolean {
    val pollInterval = 10L
    repeat((textChangeTimeout / pollInterval).toInt()) {
        if (widget.text != oldText) {
            return true
        } else {
            SystemClock.sleep(pollInterval)
        }
    }
    return false
}
