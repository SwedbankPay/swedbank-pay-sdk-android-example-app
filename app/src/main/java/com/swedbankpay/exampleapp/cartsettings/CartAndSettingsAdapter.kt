package com.swedbankpay.exampleapp.cartsettings

import android.content.res.ColorStateList
import android.transition.TransitionManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.annotation.LayoutRes
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.swedbankpay.exampleapp.R
import com.swedbankpay.exampleapp.payment.Environment
import com.swedbankpay.exampleapp.products.ProductsViewModel
import com.swedbankpay.exampleapp.products.ShopItem
import com.swedbankpay.mobilesdk.PaymentInstruments
import kotlinx.android.synthetic.main.cart_footer_cell.view.*
import kotlinx.android.synthetic.main.cart_header_cell.view.*
import kotlinx.android.synthetic.main.cart_item_cell.view.*
import kotlinx.android.synthetic.main.consumer_options_cell.view.*
import kotlinx.android.synthetic.main.settings_cell.view.*
import kotlinx.android.synthetic.main.settings_option_label.view.*
import java.util.*
import kotlin.collections.ArrayList

class CartAndSettingsAdapter(
    val lifecycleOwner: LifecycleOwner,
    val viewModel: ProductsViewModel
) : ListAdapter<CartAndSettingsAdapter.Cell, CartAndSettingsAdapter.ViewHolder>(DiffCallback) {
    init {
        viewModel.productsInCart.observe(lifecycleOwner, {
            submitList(
                ArrayList<Cell>(it.size + 5).apply {
                    add(Cell.Environment)
                    add(Cell.Header)
                    it.mapTo(this, Cell::Item)
                    add(Cell.Footer)
                    add(Cell.Consumer)
                    add(Cell.Settings)
                }
            )
        })
    }

    override fun getItemViewType(position: Int) = getItem(position).viewType.ordinal

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewType.values()[viewType].createViewHolder(this, parent)

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(this, position)

    override fun onViewRecycled(holder: ViewHolder) = holder.onRecycled()

    sealed class Cell(internal val viewType: ViewType) {
        object Environment : Cell(ViewType.ENVIRONMENT)
        object Header : Cell(ViewType.HEADER)
        class Item(val shopItem: ShopItem) : Cell(ViewType.ITEM)
        object Footer : Cell(ViewType.FOOTER)
        object Consumer : Cell(ViewType.CONSUMER)
        object Settings : Cell(ViewType.SETTINGS)
    }

    abstract class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        internal open fun bind(adapter: CartAndSettingsAdapter, position: Int) {}
        internal open fun onRecycled() {}
        val viewType get() = ViewType.values()[itemViewType]
    }

    enum class ViewType(@LayoutRes val layout: Int) {

        ENVIRONMENT(R.layout.environment_cell) {
            override fun createViewHolder(adapter: CartAndSettingsAdapter, itemView: View) =
                object : ViewHolder(itemView) {
                    init {
                        val gridLayout = itemView as GridLayout
                        for ((index, environment) in Environment.values().withIndex()) {
                            gridLayout.addEnvironmentWidget(adapter, environment, index > 1)
                        }
                    }
                }

            private val weightOne = GridLayout.spec(GridLayout.UNDEFINED, 1.0f)

            private fun GridLayout.addEnvironmentWidget(
                adapter: CartAndSettingsAdapter,
                environment: Environment,
                withTopMargin: Boolean
            ) {
                val context = context
                val resources = context.resources
                val widget = LayoutInflater
                    .from(context)
                    .inflate(R.layout.settings_option_label, this, false)
                    .apply {
                        layoutParams = (layoutParams as GridLayout.LayoutParams).apply {
                            height = resources.getDimensionPixelSize(
                                R.dimen.environment_option_height
                            )
                            columnSpec = weightOne
                            if (withTopMargin) {
                                topMargin = resources.getDimensionPixelOffset(
                                    R.dimen.environment_option_vertical_space
                                )
                            }
                        }
                    }
                initSettingWidget(adapter, widget, environment.displayName,
                    adapter.viewModel.environment, environment
                )
                addView(widget)
            }
        },

        HEADER(R.layout.cart_header_cell) {
            override fun createViewHolder(adapter: CartAndSettingsAdapter, itemView: View) =
                object : ViewHolder(itemView) {
                    init {
                        itemView.close_button.setOnClickListener {
                            adapter.viewModel.onCloseCartPressed()
                        }
                    }
                }
        },

        ITEM(R.layout.cart_item_cell) {
            override fun createViewHolder(adapter: CartAndSettingsAdapter, itemView: View) =
                object : ViewHolder(itemView) {
                    private var item: ShopItem? = null

                    private val priceFormatterObserver = Observer<(Int) -> String> {
                        this.itemView.item_price.text = item?.price?.let(it)
                    }
                    private var priceFormatter: LiveData<(Int) -> String>? = null

                    init {
                        itemView.apply {
                            item_name.typeface = ResourcesCompat.getFont(
                                context,
                                R.font.ibm_plex_mono_regular
                            )
                            item_price.typeface = ResourcesCompat.getFont(
                                context,
                                R.font.ibm_plex_mono_semibold
                            )
                            remove_button_label.typeface = ResourcesCompat.getFont(
                                context,
                                R.font.ibm_plex_mono_medium
                            )

                            remove_button.setOnClickListener {
                                item?.inCart?.value = false
                            }
                        }
                    }

                    override fun bind(adapter: CartAndSettingsAdapter, position: Int) {
                        val item = (adapter.getItem(position) as Cell.Item).shopItem
                        this.item = item
                        this.itemView.apply {
                            item_background.imageTintList =
                                ColorStateList.valueOf(item.imageBackground)
                            item_image.setImageDrawable(item.image)
                            item_name.text = item.name
                            priceFormatter?.removeObserver(priceFormatterObserver)
                            priceFormatter = adapter.viewModel.itemPriceFormatter.apply {
                                observe(adapter.lifecycleOwner, priceFormatterObserver)
                            }
                        }
                    }

                    override fun onRecycled() {
                        item = null
                        priceFormatter?.removeObserver(priceFormatterObserver)
                        priceFormatter = null
                    }
                }
        },

        FOOTER(R.layout.cart_footer_cell) {
            override fun createViewHolder(adapter: CartAndSettingsAdapter, itemView: View) =
                object : ViewHolder(itemView) {
                    private val shippingPriceObserver = Observer<String> {
                        this.itemView.shipping_price.text = it
                    }
                    private var shippingPrice: LiveData<String>? = null

                    private val priceObserver = Observer<String> {
                        this.itemView.total_price.text = it
                    }
                    private var price: LiveData<String>? = null

                    init {
                        itemView.check_out_button.setOnClickListener {
                            adapter.viewModel.onCheckOutPressed()
                        }
                        itemView.total_title.setOnClickListener {
                            adapter.viewModel.askForPrice()
                        }
                    }

                    override fun bind(adapter: CartAndSettingsAdapter, position: Int) {
                        val viewModel = adapter.viewModel
                        this.itemView.apply {
                            shippingPrice?.removeObserver(shippingPriceObserver)
                            shippingPrice = viewModel.formattedShippingPrice.apply {
                                observe(adapter.lifecycleOwner, shippingPriceObserver)
                            }
                            price?.removeObserver(priceObserver)
                            price = viewModel.formattedTotalPrice.apply {
                                observe(adapter.lifecycleOwner, priceObserver)
                            }
                        }
                    }

                    override fun onRecycled() {
                        shippingPrice?.removeObserver(shippingPriceObserver)
                        shippingPrice = null
                        price?.removeObserver(priceObserver)
                        price = null
                    }
                }
        },

        CONSUMER(R.layout.consumer_options_cell) {
            override fun createViewHolder(
                adapter: CartAndSettingsAdapter,
                itemView: View
            ) = object : ViewHolder(itemView) {
                init {
                    val vm = adapter.viewModel

                    vm.consumerOptionsExpanded.observe(adapter.lifecycleOwner, {
                        setExpandedState(it == true)
                    })

                    itemView.open_consumer.setOnClickListener {
                        vm.consumerOptionsExpanded.value = true
                    }
                    itemView.close_consumer.setOnClickListener {
                        vm.consumerOptionsExpanded.value = false
                    }

                    initSettingWidget(
                        adapter, itemView.consumer_guest, R.string.guest,
                        vm.consumerType, ProductsViewModel.ConsumerType.GUEST
                    )
                    initSettingWidget(
                        adapter, itemView.consumer_checkin, R.string.checkin,
                        vm.consumerType, ProductsViewModel.ConsumerType.CHECKIN
                    )
                    initSettingWidget(
                        adapter, itemView.consumer_prefill, R.string.prefilled,
                        vm.consumerType, ProductsViewModel.ConsumerType.PREFILL
                    )

                    bindTextView(itemView.consumer_email, vm.consumerPrefillEmail)
                    bindTextView(itemView.consumer_msisdn, vm.consumerPrefillMsisdn)
                    bindTextView(itemView.consumer_profileref, vm.consumerPrefillProfileRef)

                    val onFocusListener = View.OnFocusChangeListener { _, hasFocus ->
                        if (hasFocus) {
                            vm.consumerType.value = ProductsViewModel.ConsumerType.PREFILL
                        }
                    }
                    itemView.consumer_email.onFocusChangeListener = onFocusListener
                    itemView.consumer_msisdn.onFocusChangeListener = onFocusListener
                    itemView.consumer_profileref.onFocusChangeListener = onFocusListener
                }

                private fun setExpandedState(expanded: Boolean) {
                    this.itemView.apply {
                        (parent as? ViewGroup)?.let(TransitionManager::beginDelayedTransition)
                        layoutParams = layoutParams.apply {
                            width = if (expanded) {
                                ViewGroup.LayoutParams.MATCH_PARENT
                            } else {
                                ViewGroup.LayoutParams.WRAP_CONTENT
                            }
                        }
                        consumer_expanded_state_widgets.visibility =
                            if (expanded) View.VISIBLE else View.GONE
                    }
                }
            }
        },

        SETTINGS(R.layout.settings_cell) {
            override fun createViewHolder(
                adapter: CartAndSettingsAdapter,
                itemView: View
            ) = object : ViewHolder(itemView) {
                init {
                    itemView.apply {
                        adapter.viewModel.optionsExpanded.observe(adapter.lifecycleOwner, {
                            setExpandedState(it == true)
                        })

                        open_settings.setOnClickListener {
                            adapter.viewModel.optionsExpanded.value = true
                        }
                        close_settings.setOnClickListener {
                            adapter.viewModel.optionsExpanded.value = false
                        }
                        val boldFont = ResourcesCompat.getFont(
                            context,
                            R.font.ibm_plex_mono_bold
                        )
                        user_country_title.typeface = boldFont
                        browser_title.typeface = boldFont

                        val vm = adapter.viewModel

                        val nok = Currency.getInstance("NOK")
                        val sek = Currency.getInstance("SEK")
                        initSettingWidget(adapter, currency_nok, R.string.currency_nok,
                            vm.currency, nok,
                            { vm.currency.value = nok }
                        )
                        initSettingWidget(adapter, currency_sek, R.string.currency_sek,
                            vm.currency, sek,
                            { vm.currency.value = sek }
                        )

                        initSettingWidget(adapter, browser_no, R.string.option_no,
                            vm.useBrowser, false,
                            { vm.useBrowser.value = false }
                        )
                        initSettingWidget(adapter, browser_yes, R.string.option_yes,
                            vm.useBrowser, true,
                            { vm.useBrowser.value = true }
                        )

                        initSettingWidget(adapter, country_norway, R.string.norway,
                            vm.userCountry, ProductsViewModel.UserCountry.NORWAY,
                            { vm.userCountry.value = ProductsViewModel.UserCountry.NORWAY }
                        )
                        initSettingWidget(adapter, country_sweden, R.string.sweden,
                            vm.userCountry, ProductsViewModel.UserCountry.SWEDEN,
                            { vm.userCountry.value = ProductsViewModel.UserCountry.SWEDEN }
                        )

                        initSettingWidget(adapter, disable_payment_menu_no, R.string.disable_payment_menu_no,
                            vm.disablePaymentsMenu, false,
                            { vm.disablePaymentsMenu.value = false }
                        )
                        initSettingWidget(adapter, disable_payment_menu_yes, R.string.disable_payment_menu_yes,
                            vm.disablePaymentsMenu, true,
                            { vm.disablePaymentsMenu.value = true }
                        )

                        instruments_input.setText(vm.restrictedInstrumentsInput.value)
                        instruments_input.doAfterTextChanged {  
                            vm.restrictedInstrumentsInput.value = it?.toString()
                        }

                        initSettingWidget(adapter, use_bogus_hosturl_no, R.string.use_bogus_hosturl_no,
                            vm.useBogusHostUrl, false
                        )
                        initSettingWidget(adapter, use_bogus_hosturl_yes, R.string.use_bogus_hosturl_yes,
                            vm.useBogusHostUrl, true
                        )

                        initInstrumentModeSpinner(adapter, instrument_mode_spinner)

                        vm.payerReference.observe(adapter.lifecycleOwner, payer_reference_input::setText)
                        payer_reference_input.doAfterTextChanged {
                            val payerReference = it?.toString()?.takeUnless(String::isEmpty)
                            vm.payerReference.apply {
                                if (value != payerReference) value = payerReference
                            }
                        }
                        payer_reference_generate.setOnClickListener {
                            vm.setRandomPayerReference()
                        }
                        payer_reference_last_used.setOnClickListener {
                            vm.setPayerReferenceToLastUsed()
                        }

                        vm.paymentToken.observe(adapter.lifecycleOwner, payment_token_input::setText)
                        payment_token_input.doAfterTextChanged {
                            val paymentToken = it?.toString()?.takeUnless(String::isEmpty)
                            vm.paymentToken.apply {
                                if (value != paymentToken) value = paymentToken
                            }
                        }

                        initSettingWidget(adapter, generate_token_no, R.string.option_no,
                            vm.generatePaymentToken, false
                        )
                        initSettingWidget(adapter, generate_token_yes, R.string.option_yes,
                            vm.generatePaymentToken, true
                        )
                    }
                }

                private fun setExpandedState(expanded: Boolean) {
                    this.itemView.apply {
                        (parent as? ViewGroup)?.let(TransitionManager::beginDelayedTransition)
                        layoutParams = layoutParams.apply {
                            width = if (expanded) {
                                ViewGroup.LayoutParams.MATCH_PARENT
                            } else {
                                ViewGroup.LayoutParams.WRAP_CONTENT
                            }
                        }
                        expanded_state_widgets.visibility =
                            if (expanded) View.VISIBLE else View.GONE
                    }
                }

                private fun initInstrumentModeSpinner(adapter: CartAndSettingsAdapter, spinner: Spinner) {
                    val spinnerAdapter = object : ArrayAdapter<String>(
                        spinner.context,
                        android.R.layout.simple_spinner_item,
                        arrayOf(
                            "Disabled",
                            PaymentInstruments.CREDIT_CARD,
                            PaymentInstruments.SWISH,
                            PaymentInstruments.INVOICE
                        )
                    ) {
                        override fun getView(
                            position: Int,
                            convertView: View?,
                            parent: ViewGroup
                        ): View {
                            return super.getView(position, convertView, parent).also {
                                (it as? TextView)?.apply {
                                    setTextColor(ContextCompat.getColor(
                                        parent.context, R.color.white_text
                                    ))
                                }
                            }
                        }
                    }
                    spinner.adapter = spinnerAdapter

                    val instrumentLiveData = adapter.viewModel.paymentInstrument
                    spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                        override fun onItemSelected(
                            parent: AdapterView<*>,
                            view: View?,
                            position: Int,
                            id: Long
                        ) {
                            val instrument = if (position == 0) {
                                null
                            } else {
                                spinnerAdapter.getItem(position)
                            }
                            if (instrumentLiveData.value != instrument) {
                                instrumentLiveData.value = instrument
                            }
                        }

                        override fun onNothingSelected(parent: AdapterView<*>?) {}
                    }
                    instrumentLiveData.observe(adapter.lifecycleOwner) {
                        val position = spinnerAdapter.getPosition(it).coerceAtLeast(0)
                        spinner.setSelection(position)
                    }
                }
            }
        };

        fun createViewHolder(adapter: CartAndSettingsAdapter, parent: ViewGroup) = createViewHolder(
            adapter,
            LayoutInflater.from(parent.context).inflate(layout, parent, false)
        )

        protected abstract fun createViewHolder(adapter: CartAndSettingsAdapter, itemView: View): ViewHolder

        protected fun <T> initSettingWidget(
            adapter: CartAndSettingsAdapter,
            widget: View,
            @StringRes labelId: Int,
            setting: MutableLiveData<T>,
            settingValue: T
        ) {
            initSettingWidget<T>(adapter, widget, labelId, setting, settingValue, {
                setting.value = settingValue
            })
        }

        protected fun <T> initSettingWidget(
            adapter: CartAndSettingsAdapter,
            widget: View,
            @StringRes labelId: Int,
            setting: LiveData<T>,
            settingValue: T,
            onClick: View.OnClickListener
        ) {
            widget.text_view.setText(labelId)
            widget.setOnClickListener(onClick)
            setting.observe(adapter.lifecycleOwner, {
                val checked = it == settingValue
                widget.text_view.typeface = ResourcesCompat.getFont(
                    widget.context,
                    if (checked) R.font.ibm_plex_mono_bold else R.font.ibm_plex_mono_regular
                )
                widget.underline.visibility =
                    if (checked) View.VISIBLE else View.GONE
            })
        }

        protected fun bindTextView(
            widget: TextView,
            setting: MutableLiveData<String>
        ) {
            widget.text = setting.value
            widget.doAfterTextChanged { setting.value = it?.toString() }
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<Cell>() {
        override fun areItemsTheSame(oldItem: Cell, newItem: Cell) = when (oldItem) {
            Cell.Environment -> newItem is Cell.Environment
            Cell.Header -> newItem is Cell.Header
            is Cell.Item -> newItem is Cell.Item && oldItem.shopItem.name == newItem.shopItem.name
            Cell.Footer -> newItem is Cell.Footer
            Cell.Consumer -> newItem is Cell.Consumer
            Cell.Settings -> newItem is Cell.Settings
        }

        override fun areContentsTheSame(oldItem: Cell, newItem: Cell) = when (oldItem) {
            Cell.Environment -> newItem is Cell.Environment
            Cell.Header -> newItem is Cell.Header
            is Cell.Item -> newItem is Cell.Item && oldItem.shopItem == newItem.shopItem
            Cell.Footer -> newItem is Cell.Footer
            Cell.Consumer -> newItem is Cell.Consumer
            Cell.Settings -> newItem is Cell.Settings
        }
    }
}
