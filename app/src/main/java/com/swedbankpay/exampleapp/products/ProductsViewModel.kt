package com.swedbankpay.exampleapp.products

import android.app.Application
import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.*
import androidx.lifecycle.Observer
import com.swedbankpay.mobilesdk.Consumer
import com.swedbankpay.mobilesdk.PaymentFragment
import java.math.BigDecimal
import java.text.DecimalFormat
import java.util.*

val FragmentActivity.productsViewModel get() = ViewModelProviders.of(this)[ProductsViewModel::class.java]

class ProductsViewModel(app: Application) : AndroidViewModel(app) {
    private val languageCode = "en-US"

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

    val isUserAnonymous = MutableLiveData<Boolean>().apply { value = true }
    val userCountry = MutableLiveData<UserCountry>().apply { value =
        UserCountry.NORWAY
    }

    private val paymentFragmentConsumer = MediatorLiveData<Consumer>().apply {
        val observer = Observer<Any> {
            value = if (isUserAnonymous.value == true) {
                Consumer.ANONYMOUS
            } else {
                Consumer.Identified(checkNotNull(userCountry.value).code)
            }
        }
        addSource(isUserAnonymous, observer)
        addSource(userCountry, observer)
    }

    // Here we create the "merchantData" expected by our backend.
    // This is only a very simple example; your actual implementation
    // will be specific to your ecommerce system.
    //
    // The SDK itself imposes no other limitations for this value,
    // expect that is must be convertible to JSON (using gson).
    // Here we use Maps, but custom classes can make for more
    // maintainable code.
    private val paymentFragmentMerchantData = Transformations.map(productsInCart) { items ->
        mapOf(
            "basketId" to basketId,
            "currency" to currency.value!!.currencyCode,
            "languageCode" to languageCode,
            "items" to items.map {
                mapOf(
                    "itemId" to it.name,
                    "quantity" to 1,
                    "price" to it.price,
                    "vat" to it.price / 5
                )
            }.plus(
                mapOf(
                    "itemId" to "Shipping",
                    "quantity" to 1,
                    "price" to shippingPrice,
                    "vat" to shippingPrice / 5
                )
            )
        )
    }

    val paymentFragmentArguments = MediatorLiveData<Bundle>().apply {
        val observer = Observer<Any> {
            value = PaymentFragment.ArgumentsBuilder()
                .consumer(checkNotNull(paymentFragmentConsumer.value))
                .merchantData(paymentFragmentMerchantData.value)
                .build()
        }
        addSource(paymentFragmentConsumer, observer)
        addSource(paymentFragmentMerchantData, observer)
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
    enum class UserCountry(val code: String) {
        NORWAY("NO"), SWEDEN("SE")
    }
}