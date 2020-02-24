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

    private val basketId = UUID.randomUUID().toString()

    val currency = MutableLiveData(Currency.getInstance("NOK"))

    val products = ShopItem.demoItems(app)

    val productsInCart: LiveData<List<ShopItem>> = MediatorLiveData<List<ShopItem>>().apply {
        val observer = Observer<Boolean> {
            value = products.filter { it.inCart.value == true }
        }
        for (product in products) {
            addSource(product.inCart, observer)
        }
    }


    private val shippingPrice = 120_00
    val formattedShippingPrice = Transformations.map(currency) {
        formatPrice(shippingPrice, it)
    }

    private val totalPrice = Transformations.map(productsInCart) {
        it.sumBy(ShopItem::price) + shippingPrice
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

    val optionsExpanded = MutableLiveData<Boolean>().apply { value = false }
    val useBrowser = MutableLiveData(false)

    val isUserAnonymous = MutableLiveData<Boolean>().apply { value = true }
    val userCountry = MutableLiveData<UserCountry>().apply { value =
        UserCountry.NORWAY
    }

    private val paymentFragmentConsumer = MediatorLiveData<Consumer>().apply {
        val observer = Observer<Any> {
            value = if (isUserAnonymous.value == true) {
                null
            } else {
                val country = checkNotNull(userCountry.value)
                Consumer(
                    language = country.language,
                    shippingAddressRestrictedToCountryCodes = listOf(country.code)
                )
            }
        }
        addSource(isUserAnonymous, observer)
        addSource(userCountry, observer)
    }

    private val paymentFragmentPaymentOrder = Transformations.map(productsInCart) { items ->
        val orderItems = items.map {
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

        PaymentOrder(
            currency = checkNotNull(currency.value),
            amount = amount,
            vatAmount = vatAmount,
            description = basketId,
            // Each payment needs a set of URLs, most importantly the paymentUrl.
            // The SDK can generate suitable URLs for you, provided you
            // have set proper values for the swedbankpaysdk_callback_url_scheme
            // and swedbankpaysdk_callback_host string resources.
            urls = PaymentOrderUrls(getApplication()),
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
}
