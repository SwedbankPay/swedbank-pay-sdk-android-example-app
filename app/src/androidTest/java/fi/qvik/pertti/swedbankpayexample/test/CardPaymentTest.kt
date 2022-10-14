package fi.qvik.pertti.swedbankpayexample.test

import android.app.Instrumentation
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
import com.swedbankpay.exampleapp.MainActivity
import com.swedbankpay.exampleapp.R
import fi.qvik.pertti.swedbankpayexample.test.util.*
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CardPaymentTest {
    companion object {
        const val localTimeout = 10_000L
        const val remoteTimeout = 30_000L
        
        const val noScaCardNumber1 = "4581097032723517"
        const val noScaCardNumber2 = "4925000000000004"
        const val noScaCardNumber3 = "5226600159865967"
        
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
    private val extJavaIntegrationOption get() = device.findObject(
        UiSelector().text(appString(R.string.env_ext_integration_java))
    )
    private val checkOutButton get() = device.findObject(
        byId("check_out_button")
    )
    private val cogSettingsButton get() = device.findObject(
        byId("open_settings")
    )
    private val instrumentDropDown get() = device.findObject(
        byId("instrument_mode_input_layout")
    )
    private val instrumentDropDownInput get() = device.findObject(
        byId("instrument_mode_input")
    )
    private val countrySwedenButton get() = device.findObject(
        byId("country_sweden")
    )
    private val instrumentSpinner get() = device.findObject(
        byId("instrument_spinner")
    )
    private val creditCardOption get() = device.findObject(
        UiSelector().textStartsWith("CreditCard")
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
        get() = webView.getChild(UiSelector().resourceIdMatches("cvcInput.*"))
    
    private val payButton
        get() = webView.getChild(UiSelector().className(Button::class.java).textStartsWith("Betal "))

    private val successText get() = device.findObject(
        UiSelector().text(appString(R.string.success_dialog_title))
    )
    private var mainActivity: MainActivity? = null
    
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

        val inst = InstrumentationRegistry.getInstrumentation()
        val monitor: Instrumentation.ActivityMonitor =
            inst.addMonitor("com.swedbankpay.exampleapp.MainActivity", null, false)
        
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
        mainActivity = monitor.waitForActivityWithTimeout(2000) as MainActivity
    }

    private fun fullPaymentTestAttempt(
        cardNumber: String,
        cvv: String,
        timeout: Long = localTimeout,
        paymentFlowHandler: () -> Unit
    ): Boolean {
        if (!webView.waitForExists(timeout)) {
            return false
        }

        if (!webView.waitAndScrollUntilExists(cardOption, timeout)) { return false }
        cardOption.clickUntilCheckedAndAssert(timeout)

        if (!webView.waitAndScrollUntilExists(panInput, timeout)) { return false }
        inputText(device, panInput, cardNumber)

        if (!webView.waitAndScrollUntilExists(expiryDateInput, timeout)) { return false }
        inputText(device, expiryDateInput, expiryDate)

        if (!webView.waitAndScrollUntilExists(cvvInput, timeout)) { return false }
        inputText(device, cvvInput, cvv)

        if (!webView.waitAndScrollUntilExists(payButton, remoteTimeout)) { return false }

        if (!payButton.click()) { return false }

        paymentFlowHandler()
        
        if (successText.waitForExists(remoteTimeout)) { return true }
        return false
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
        val cardNumbers = arrayOf(noScaCardNumber1, noScaCardNumber2, noScaCardNumber3)
        var success = false
        for (cardNumber in cardNumbers) {
            success = fullPaymentTestAttempt(cardNumber, cvv, paymentFlowHandler = {})
            if (success) {
                break
            }
            //try again with a new purchase
            device.pressBack()
            Assert.assertTrue(checkOutButton.click())
        }
    }

    /*
    Java backend isn't working anymore, we should remove support for it.
    
    @Test
    fun testJavaBackendNonScaPayment() {
        productsRecyclerView.waitForExists(localTimeout)
        productsRecyclerView.scrollIntoView(addItemButton)
        Assert.assertTrue(addItemButton.click())
        Assert.assertTrue(openCartButton.click())

        cartAndSettingsRecyclerView.waitForExists(localTimeout)
        cartAndSettingsRecyclerView.scrollIntoView(extJavaIntegrationOption)
        Assert.assertTrue(extJavaIntegrationOption.click())
        cartAndSettingsRecyclerView.scrollIntoView(checkOutButton)
        Assert.assertTrue(checkOutButton.click())
        
        val cardNumbers = arrayOf(noScaCardNumber1, noScaCardNumber2, noScaCardNumber3)
        var success = false
        for (cardNumber in cardNumbers) {
            success = fullPaymentTestAttempt(cardNumber, cvv, paymentFlowHandler = {})
            if (success) {
                break
            }
            //try again with a new purchase
            device.pressBack()
            Assert.assertTrue(checkOutButton.click())
        }
    }
    */

    /**
     * Test that we can select and change instruments in version 2. We don't need to do the actual payment.
     * instruments for version2 has been disabled since it does not work 100% of the time, this problem 
     * is rare but cannot be fixed in the SDK (only by recreating the paymentOrder and starting over). 
     * If you need this feature, now is a good time to upgrade to version 3. 
    @Test
    fun testInstrumentsV2() {

        //set it to useCheckoutVersion2 and wait for it to update
        val liveData = mainActivity?.productsViewModel?.useCheckoutVersion2!!
        liveData.postValue(true)
        while (liveData.value == false) {
        }
        continueInstrumentTest()
    }
     */
    
    /**
     * Test that we can select and change instruments in version 3. We don't need to do the actual payment.
     */
    @Test
    fun testInstrumentsV3() {
        continueInstrumentTest()
    }
    
    private fun continueInstrumentTest() {
        productsRecyclerView.waitForExists(localTimeout)
        productsRecyclerView.scrollIntoView(addItemButton)
        Assert.assertTrue(addItemButton.click())

        Assert.assertTrue(openCartButton.click())

        cartAndSettingsRecyclerView.waitForExists(localTimeout)
        cartAndSettingsRecyclerView.scrollIntoView(extIntegrationOption)
        Assert.assertTrue(extIntegrationOption.click())

        cartAndSettingsRecyclerView.scrollIntoView(cogSettingsButton)
        Assert.assertTrue(cogSettingsButton.click())

        //cartAndSettingsRecyclerView.scrollIntoView(countrySwedenButton)
        //countrySwedenButton.click()
        
        cartAndSettingsRecyclerView.scrollIntoView(instrumentDropDown)
        Assert.assertTrue(instrumentDropDown.click())
        instrumentDropDownInput.text = "CreditCard"
        //PayExFinancing has been removed, but this still shows that instruments exists and can be changed.
        //instrumentDropDownInput.text = "Invoice-PayExFinancingNo"

        cartAndSettingsRecyclerView.scrollIntoView(checkOutButton)
        Assert.assertTrue(checkOutButton.click())

        webView.waitForExists(localTimeout)
        
        //now see that things exists, and change to "CreditCard"
        instrumentSpinner.waitForExists(localTimeout)
        
        //spinner is also the drop-down box
        instrumentSpinner.click()
        creditCardOption.waitForExists(remoteTimeout)
        Assert.assertTrue(creditCardOption.click())

        webView.waitAndScrollFullyIntoViewAndAssertExists(panInput, remoteTimeout)
    }
}