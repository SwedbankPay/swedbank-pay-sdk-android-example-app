package fi.qvik.pertti.swedbankpayexample.test

import android.content.Context
import android.content.Intent
import android.webkit.WebView
import android.widget.Button
import androidx.annotation.StringRes
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.*
import com.swedbankpay.exampleapp.BuildConfig
import com.swedbankpay.exampleapp.R
import fi.qvik.pertti.swedbankpayexample.test.util.*
import fi.qvik.pertti.swedbankpayexample.test.util.clickUntilCheckedAndAssert
import fi.qvik.pertti.swedbankpayexample.test.util.clickUntilFocusedAndAssert
import fi.qvik.pertti.swedbankpayexample.test.util.waitAndScrollFullyIntoViewAndAssertExists
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CardPaymentTest {
    companion object {
        const val localTimeout = 5_000L
        const val remoteTimeout = 30_000L

        const val cardNumber = "4925000000000004"
        const val expiryDate = "1230"
        const val cvv = "111"
    }

    private fun appString(@StringRes id: Int): String =
        InstrumentationRegistry.getInstrumentation().targetContext.getString(id)

    private val device by lazy {
        UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    }

    private fun byId(id: String) = UiSelector()
        .resourceId("${BuildConfig.APPLICATION_ID}:id/$id")

    private val productsRecyclerView get() = device.findScrollable(
        byId("products_recyclerView")
    )
    private val addItemButton get() = device.findObject(
        byId("add_remove_button")
    )
    private val openCartButton get() = device.findObject(
        byId("open_cart")
    )

    private val cartAndSettingsRecyclerView get() = device.findScrollable(
        byId("cart_and_settings_recyclerView")
    )
    private val extIntegrationOption get() = device.findObject(
        UiSelector().text(appString(R.string.env_ext_integration))
    )
    private val checkOutButton get() = device.findObject(
        byId("check_out_button")
    )

    private val webView get() = device.findScrollable(
        UiSelector().className(WebView::class.java)
    )
    private val cardOption
        get() = webView.getChild(UiSelector().textStartsWith("Kort").checkable(true))
    private val panInput
        get() = webView.getChild(UiSelector().resourceId("panInput"))
    private val expiryDateInput
        get() = webView.getChild(UiSelector().resourceId("expiryInput"))
    private val cvvInput
        get() = webView.getChild(UiSelector().resourceId("cvcInput"))
    private val payButton
        get() = webView.getChild(UiSelector().className(Button::class.java).textStartsWith("Betal "))

    private val successText get() = device.findObject(
        UiSelector().text(appString(R.string.success_dialog_title))
    )

    @Before
    fun launchAppFromHomeScreen() {
        device.pressHome()

        // wait for launcher
        val launcherPackage = device.launcherPackageName
        Assert.assertNotNull("No launcher package", launcherPackage)
        device.wait(
            Until.hasObject(By.pkg(launcherPackage).depth(0)),
            localTimeout
        )

        // launch app
        val context = ApplicationProvider.getApplicationContext<Context>()
        val intent = context.packageManager.getLaunchIntentForPackage(BuildConfig.APPLICATION_ID)
        Assert.assertNotNull("Could not get launch intent", intent)
        intent!!.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)

        context.startActivity(intent)

        // wait for app
        device.wait(
            Until.hasObject(By.pkg(BuildConfig.APPLICATION_ID).depth(0)),
            localTimeout
        )
    }

    @Test
    fun testNonScaPayment() {
        productsRecyclerView.waitForExists(localTimeout)
        productsRecyclerView.scrollIntoView(addItemButton)
        Assert.assertTrue(addItemButton.click())

        Assert.assertTrue(openCartButton.click())

        cartAndSettingsRecyclerView.waitForExists(localTimeout)
        cartAndSettingsRecyclerView.scrollIntoView(extIntegrationOption)
        Assert.assertTrue(extIntegrationOption.click())
        cartAndSettingsRecyclerView.scrollIntoView(checkOutButton)
        Assert.assertTrue(checkOutButton.click())

        webView.waitForExists(localTimeout)

        webView.waitAndScrollFullyIntoViewAndAssertExists(cardOption, remoteTimeout)
        cardOption.clickUntilCheckedAndAssert(remoteTimeout)

        webView.waitAndScrollFullyIntoViewAndAssertExists(panInput, remoteTimeout)
        panInput.clickUntilFocusedAndAssert(remoteTimeout)
        inputText(device, panInput, cardNumber)

        webView.waitAndScrollFullyIntoViewAndAssertExists(expiryDateInput, remoteTimeout)

        expiryDateInput.clickUntilFocusedAndAssert(remoteTimeout)
        inputText(device, expiryDateInput, expiryDate)

        webView.waitAndScrollFullyIntoViewAndAssertExists(cvvInput, remoteTimeout)
        cvvInput.clickUntilFocusedAndAssert(remoteTimeout)
        inputText(device, cvvInput, cvv)

        webView.waitAndScrollFullyIntoViewAndAssertExists(payButton, remoteTimeout)
        Assert.assertTrue(payButton.click())

        successText.waitForExists(remoteTimeout)
    }
}