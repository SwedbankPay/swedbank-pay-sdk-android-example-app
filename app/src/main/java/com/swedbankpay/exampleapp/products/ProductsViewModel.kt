package com.swedbankpay.exampleapp.products

import android.app.Application
import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.*
import androidx.lifecycle.Observer
import com.swedbankpay.mobilesdk.*
import java.math.BigDecimal
import java.text.DecimalFormat
import java.util.*

val FragmentActivity.productsViewModel get() = ViewModelProviders.of(this)[ProductsViewModel::class.java]

class ProductsViewModel(app: Application) : AndroidViewModel(app) {
    private val currencyFormat get() = DecimalFormat("#,##0 ¤¤").apply {
        minimumFractionDigits = 0
    }

    private val _onCloseCardPressed = MutableLiveData<Unit?>()
    val onCloseCartPressed: LiveData<Unit?> get() = _onCloseCardPressed

    private val _onCheckOutPressed = MutableLiveData<Unit?>()
    val onCheckOutPressed: LiveData<Unit?> get() = _onCheckOutPressed
    
    private val _onAdjustPricePressed = MutableLiveData<Unit?>()
    val onAdjustPricePressed: LiveData<Unit?> get() = _onAdjustPricePressed

    private val basketId = UUID.randomUUID().toString()

    val currency = MutableLiveData(Currency.getInstance("NOK"))

    val disablePaymentsMenu = MutableLiveData(false)
    val restrictedInstrumentsInput = MutableLiveData<String?>(null)
    private val restrictedInstrumentsList: LiveData<List<String>?> = Transformations.map(restrictedInstrumentsInput) {
        it?.replace(" ", "")
            ?.split(",")
            ?.filter(String::isNotBlank)
    }

    val products = ShopItem.demoItems(app)

    val productsInCart: LiveData<List<ShopItem>> = MediatorLiveData<List<ShopItem>>().apply {
        val observer = Observer<Boolean> {
            value = products.filter { it.inCart.value == true }
        }
        for (product in products) {
            addSource(product.inCart, observer)
        }
    }

    val adjustedPrice = MutableLiveData<Int?>(null)

    private val shippingPrice = 120_00
    val formattedShippingPrice = Transformations.map(currency) {
        formatPrice(shippingPrice, it)
    }
    
    private val totalPrice = MediatorLiveData<Int>().apply {
        val observer = Observer<Any?> {
            value = if(adjustedPrice.value != null) {
                adjustedPrice.value
            }
            else {
                shippingPrice + (productsInCart.value?.sumBy(ShopItem::price) ?: 0)
            }
        }
        addSource(adjustedPrice, observer)
        addSource(productsInCart, observer)
    }

    val formattedTotalPrice = MediatorLiveData<String>().apply {
        val observer = Observer<Any> {
            value = totalPrice.value?.let { price ->
                currency.value?.let { currency ->
                    formatPrice(price, currency)
                }
            }
        }
        addSource(currency, observer)
        addSource(totalPrice, observer)
    }

    val itemPriceFormatter = Transformations.map(currency) {
        { price: Int -> formatPrice(price, it) }
    }


    val consumerOptionsExpanded = MutableLiveData(false)
    val optionsExpanded = MutableLiveData(false)

    val useBrowser = MutableLiveData(false)
    val useBogusHostUrl = MutableLiveData(false)

    val consumerType = MutableLiveData(ConsumerType.ANONYMOUS)
    val consumerPrefillEmail = MutableLiveData("")
    val consumerPrefillMsisdn = MutableLiveData("")
    val consumerPrefillProfileRef = MutableLiveData("")

    val userCountry = MutableLiveData(UserCountry.NORWAY)

    private val paymentFragmentConsumer = MediatorLiveData<Consumer>().apply {
        val observer = Observer<Any> {
            value = if (consumerType.value == ConsumerType.CHECKIN) {
                val country = checkNotNull(userCountry.value)
                Consumer(
                    language = country.language,
                    shippingAddressRestrictedToCountryCodes = listOf(country.code)
                )
            } else {
                null
            }
        }
        addSource(consumerType, observer)
        addSource(userCountry, observer)
    }

    private val paymentFragmentPayerPrefill = MediatorLiveData<PaymentOrderPayer>().apply {
        val observer = Observer<Any> {
            value = if (consumerType.value == ConsumerType.PREFILL) {
                PaymentOrderPayer(
                    consumerProfileRef = consumerPrefillProfileRef.value?.takeUnless(String::isEmpty),
                    email = consumerPrefillEmail.value?.takeUnless(String::isEmpty),
                    msisdn = consumerPrefillMsisdn.value?.takeUnless(String::isEmpty)
                )
            } else {
                null
            }
        }
        addSource(consumerPrefillProfileRef, observer)
        addSource(consumerPrefillEmail, observer)
        addSource(consumerPrefillMsisdn, observer)
    }

    private val paymentFragmentPaymentOrder = MediatorLiveData<PaymentOrder>().apply {

        val productsObserver = Observer<List<ShopItem>> {
            value = createPaymentOrder(it)
        }
        val payerPrefillObserver = Observer<PaymentOrderPayer?> {
            value = value?.copy(payer = it)
        }
        val restrictionsObserver = Observer<List<String>?> {
            value = value?.copy(restrictedToInstruments = it)
        }
        val toggleObserver = Observer<Boolean> {
            value = value?.copy(disablePaymentMenu = it)
        }
        val adjustedObserver = Observer<Int?> {
            value = createPaymentOrder(productsInCart.value ?: emptyList())
        }
        val countryObserver = Observer<UserCountry> {
            value = value?.copy(language = checkNotNull(it.language))
        }
        val useBogusHostUrlObserver = Observer<Boolean> {
            value = value?.copy(urls = buildPaymentOrderUrls())
        }
        
        addSource(productsInCart, productsObserver)
        addSource(paymentFragmentPayerPrefill, payerPrefillObserver)
        addSource(restrictedInstrumentsList, restrictionsObserver)
        addSource(disablePaymentsMenu, toggleObserver)
        addSource(adjustedPrice, adjustedObserver)
        addSource(userCountry, countryObserver)
        addSource(useBogusHostUrl, useBogusHostUrlObserver)
    }

    private fun buildPaymentOrderUrls() = if (useBogusHostUrl.value == true) {
        PaymentOrderUrls(
            context = getApplication(),
            hostUrl = "https://bogus-hosturl-for-testing.swedbankpay.com/",
            callbackUrl = null,
            termsOfServiceUrl = null
        )
    } else {
        // Each payment needs a set of URLs, most importantly the paymentUrl.
        // The SDK can generate suitable URLs for you, provided you
        // have set proper values for the swedbankpaysdk_callback_url_scheme
        // and swedbankpaysdk_callback_host string resources.
        PaymentOrderUrls(getApplication())
    }
    
    private fun createPaymentOrder(items: List<ShopItem>): PaymentOrder { 
        var orderItems = items.map {
            OrderItem(
                reference = it.orderItemReference,
                name = it.name,
                type = ItemType.PRODUCT,
                `class` = "Shoe",
                quantity = 1,
                quantityUnit = "pair",
                unitPrice = it.price.toLong(),
                vatPercent = 2500,
                amount = it.price.toLong(),
                vatAmount = (it.price / 5).toLong()
            )
        }.plus(OrderItem(
            reference = "shipping",
            name = "Shipping",
            type = ItemType.SHIPPING_FEE,
            `class` = "Shipping",
            quantity = 1,
            quantityUnit = "pc",
            unitPrice = shippingPrice.toLong(),
            vatPercent = 2500,
            amount = shippingPrice.toLong(),
            vatAmount = (shippingPrice / 5).toLong()
        ))

        var amount = 0L
        var vatAmount = 0L
        for (orderItem in orderItems) {
            amount += orderItem.amount
            vatAmount += orderItem.vatAmount
        }
        adjustedPrice.value?.let {

            orderItems = listOf(OrderItem(
                reference = "shipping",
                name = "Shipping",
                type = ItemType.SHIPPING_FEE,
                `class` = "Shipping",
                quantity = 1,
                quantityUnit = "pc",
                unitPrice = it.toLong(),
                vatPercent = 2500,
                amount = it.toLong(),
                vatAmount = (it / 5).toLong()
            )) 
            
            amount = it.toLong()
            vatAmount = (amount / 5)
        }

        return PaymentOrder(
            currency = checkNotNull(currency.value),
            disablePaymentMenu = checkNotNull(disablePaymentsMenu.value),
            restrictedToInstruments = restrictedInstrumentsList.value,
            amount = amount,
            vatAmount = vatAmount,
            description = basketId,
            language = checkNotNull(userCountry.value).language,
            urls = buildPaymentOrderUrls(),
            payeeInfo = PayeeInfo(
                // It is unwise to expose your merchant id in a shipping app.
                // It is better to have the backend fill in your merchant id here;
                // the example backend does this. In the interest of having
                // the Android SDK API mirror the Swedbank Pay API,
                // payeeId is still made a required parameter, but it also defaults
                // to the empty string to facilitate this common pattern.
                payeeId = "not-the-real-merchant-id",
                // PayeeReference must be unique to this payment order.
                // In a real application you would get it from your backend.
                // If you don't need the payeeReference in your app,
                // you can also generate it in your backend. The example backend
                // does this. PayeeReference is still a required field, so we
                // must set it to a valid value. The empty string is fine;
                // indeed, similarly to payeeId above, it defaults to the empty string.
                payeeReference = ""
            ),
            payer = paymentFragmentPayerPrefill.value,
            orderItems = orderItems
        )
    }

    val paymentFragmentArguments = MediatorLiveData<Bundle>().apply {
        val observer = Observer<Any> {
            value = paymentFragmentPaymentOrder.value?.let {
                PaymentFragment.ArgumentsBuilder()
                    .consumer(paymentFragmentConsumer.value)
                    .paymentOrder(it)
                    .useBrowser(useBrowser.value ?: false)
                    .build()
            }
        }
        addSource(useBrowser, observer)
        addSource(paymentFragmentConsumer, observer)
        addSource(paymentFragmentPaymentOrder, observer)
    }

    private fun formatPrice(price: Int, currency: Currency) = currencyFormat.run {
        this.currency = currency// this@ProductsViewModel.currency.value
        format(BigDecimal(price).movePointLeft(2))
    }

    fun onCloseCartPressed() = fireEvent(_onCloseCardPressed)

    fun onCheckOutPressed() = fireEvent(_onCheckOutPressed)

    fun askForPrice() = fireEvent(_onAdjustPricePressed)

    fun clearCart() {
        for (product in products) {
            product.inCart.value = false
        }
    }

    private fun fireEvent(event: MutableLiveData<Unit?>) {
        // This is an event, not a state, so set it back to null immediately.
        event.value = Unit
        event.value = null
    }


    // These are not defined in the SDK for easier extensibility.
    enum class UserCountry(val code: String, val language: Language) {
        NORWAY("NO", Language.NORWEGIAN), SWEDEN("SE", Language.SWEDISH)
    }

    enum class ConsumerType {
        ANONYMOUS, CHECKIN, PREFILL
    }
}
