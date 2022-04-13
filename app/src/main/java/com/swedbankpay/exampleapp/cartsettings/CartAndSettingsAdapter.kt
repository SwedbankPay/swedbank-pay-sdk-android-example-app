package com.swedbankpay.exampleapp.cartsettings

import android.content.res.ColorStateList
import android.transition.TransitionManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.annotation.StringRes
import androidx.core.content.res.ResourcesCompat
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import com.swedbankpay.exampleapp.R
import com.swedbankpay.exampleapp.all
import com.swedbankpay.exampleapp.databinding.*
import com.swedbankpay.exampleapp.hideSoftKeyboard
import com.swedbankpay.exampleapp.payment.Environment
import com.swedbankpay.exampleapp.products.ProductsViewModel
import com.swedbankpay.exampleapp.products.ShopItem
import com.swedbankpay.exampleapp.setTextIfNeeded
import com.swedbankpay.mobilesdk.PaymentInstruments
import java.util.*
import kotlin.collections.ArrayList

class CartAndSettingsAdapter(
    val lifecycleOwner: LifecycleOwner,
    val viewModel: ProductsViewModel
) : ListAdapter<CartAndSettingsAdapter.Cell, CartAndSettingsAdapter.ViewHolder>(DiffCallback) {
    init {
        viewModel.productsInCart.observe(lifecycleOwner) {
            submitList(
                ArrayList<Cell>(it.size + 6).apply {
                    add(Cell.Environment)
                    add(Cell.Header)
                    it.mapTo(this, Cell::Item)
                    add(Cell.Footer)
                    add(Cell.Consumer)
                    add(Cell.Settings)
                    add(Cell.Style)
                }
            )
        }
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
        object Style : Cell(ViewType.STYLE)
    }

    open class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        internal open fun bind(adapter: CartAndSettingsAdapter, position: Int) {}
        internal open fun onRecycled() {}
        val viewType get() = ViewType.values()[itemViewType]
    }

    enum class ViewType {

        ENVIRONMENT {
            override fun createViewHolder(adapter: CartAndSettingsAdapter, parent: ViewGroup) =
                createViewHolder(parent, EnvironmentCellBinding::inflate) {
                    for ((index, environment) in Environment.enabledEnvironments.withIndex()) {
                        root.addEnvironmentWidget(adapter, environment, index > 1)
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
                val widget = SettingsOptionLabelBinding.inflate(
                    LayoutInflater.from(context),
                    this,
                    false
                )
                widget.root.apply {
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
                addView(widget.root)
            }
        },

        HEADER {
            override fun createViewHolder(adapter: CartAndSettingsAdapter, parent: ViewGroup) =
                createViewHolder(parent, CartHeaderCellBinding::inflate) {
                    closeButton.setOnClickListener {
                        adapter.viewModel.onCloseCartPressed()
                    }
                }
        },

        ITEM {
            override fun createViewHolder(
                adapter: CartAndSettingsAdapter, parent: ViewGroup
            ): ViewHolder {
                val binding = inflate(parent, CartItemCellBinding::inflate)
                return object : ViewHolder(binding.root) {
                    private var item: ShopItem? = null

                    private val priceFormatterObserver = Observer<(Int) -> String> {
                        binding.itemPrice.text = item?.price?.let(it)
                    }
                    private var priceFormatter: LiveData<(Int) -> String>? = null

                    init {
                        binding.apply {
                            val context = root.context
                            itemName.typeface = ResourcesCompat.getFont(
                                context,
                                R.font.ibm_plex_mono_regular
                            )
                            itemPrice.typeface = ResourcesCompat.getFont(
                                context,
                                R.font.ibm_plex_mono_semibold
                            )
                            removeButtonLabel.typeface = ResourcesCompat.getFont(
                                context,
                                R.font.ibm_plex_mono_medium
                            )

                            removeButton.setOnClickListener {
                                item?.inCart?.value = false
                            }
                        }
                    }

                    override fun bind(adapter: CartAndSettingsAdapter, position: Int) {
                        val item = (adapter.getItem(position) as Cell.Item).shopItem
                        this.item = item
                        binding.apply {
                            itemBackground.imageTintList =
                                ColorStateList.valueOf(item.imageBackground)
                            itemImage.setImageDrawable(item.image)
                            itemName.text = item.name
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
            }
        },

        FOOTER {
            override fun createViewHolder(
                adapter: CartAndSettingsAdapter, parent: ViewGroup
            ): ViewHolder {
                val binding = inflate(parent, CartFooterCellBinding::inflate)
                binding.apply {
                    checkOutButton.setOnClickListener {
                        adapter.viewModel.onCheckOutPressed()
                    }
                    totalTitle.setOnClickListener {
                        adapter.viewModel.askForPrice()
                    }
                }
                val shippingPriceObserver = Observer<String> {
                    binding.shippingPrice.text = it
                }
                val priceObserver = Observer<String?> {
                    binding.totalPrice.text = it
                }
                return object : ViewHolder(binding.root) {
                    private var shippingPrice: LiveData<String>? = null
                    private var price: LiveData<String?>? = null

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
            }
        },

        CONSUMER {
            override fun createViewHolder(adapter: CartAndSettingsAdapter, parent: ViewGroup) =
                createViewHolder(parent, ConsumerOptionsCellBinding::inflate) {
                    val vm = adapter.viewModel

                    vm.consumerOptionsExpanded.observe(adapter.lifecycleOwner, {
                        val expanded = it == true
                        (root.parent as? ViewGroup)?.let(TransitionManager::beginDelayedTransition)
                        root.layoutParams = root.layoutParams.apply {
                            width = if (expanded) {
                                ViewGroup.LayoutParams.MATCH_PARENT
                            } else {
                                ViewGroup.LayoutParams.WRAP_CONTENT
                            }
                        }
                        consumerExpandedStateWidgets.visibility =
                            if (expanded) View.VISIBLE else View.GONE
                    })

                    openConsumer.setOnClickListener {
                        vm.consumerOptionsExpanded.value = true
                    }
                    closeConsumer.setOnClickListener {
                        vm.consumerOptionsExpanded.value = false
                    }

                    initSettingWidget(
                        adapter, consumerGuest, R.string.guest,
                        vm.consumerType, ProductsViewModel.ConsumerType.GUEST
                    )
                    initSettingWidget(
                        adapter, consumerCheckin, R.string.checkin,
                        vm.consumerType, ProductsViewModel.ConsumerType.CHECKIN
                    )
                    initSettingWidget(
                        adapter, consumerPrefill, R.string.prefilled,
                        vm.consumerType, ProductsViewModel.ConsumerType.PREFILL
                    )

                    bindTextView(consumerEmail, vm.consumerPrefillEmail)
                    bindTextView(consumerMsisdn, vm.consumerPrefillMsisdn)
                    bindTextView(consumerProfileref, vm.consumerPrefillProfileRef)

                    val onFocusListener = View.OnFocusChangeListener { _, hasFocus ->
                        if (hasFocus) {
                            vm.consumerType.value = ProductsViewModel.ConsumerType.PREFILL
                        }
                    }
                    consumerEmail.onFocusChangeListener = onFocusListener
                    consumerMsisdn.onFocusChangeListener = onFocusListener
                    consumerProfileref.onFocusChangeListener = onFocusListener
                }
        },

        SETTINGS {
            override fun createViewHolder(adapter: CartAndSettingsAdapter, parent: ViewGroup) =
                createViewHolder(parent, SettingsCellBinding::inflate) {
                    adapter.viewModel.optionsExpanded.observe(adapter.lifecycleOwner, {
                        val expanded = it == true
                        (root.parent as? ViewGroup)?.let(TransitionManager::beginDelayedTransition)
                        root.layoutParams = root.layoutParams.apply {
                            width = if (expanded) {
                                ViewGroup.LayoutParams.MATCH_PARENT
                            } else {
                                ViewGroup.LayoutParams.WRAP_CONTENT
                            }
                        }
                        expandedStateWidgets.visibility =
                            if (expanded) View.VISIBLE else View.GONE
                    })

                    openSettings.setOnClickListener {
                        adapter.viewModel.optionsExpanded.value = true
                    }
                    closeSettings.setOnClickListener {
                        adapter.viewModel.optionsExpanded.value = false
                    }
                    val boldFont = ResourcesCompat.getFont(
                        root.context,
                        R.font.ibm_plex_mono_bold
                    )
                    userCountryTitle.typeface = boldFont
                    browserTitle.typeface = boldFont

                    val vm = adapter.viewModel

                    val nok = Currency.getInstance("NOK")
                    val sek = Currency.getInstance("SEK")
                    initSettingWidget(adapter, currencyNok, R.string.currency_nok,
                        vm.currency, nok,
                        { vm.currency.value = nok }
                    )
                    initSettingWidget(adapter, currencySek, R.string.currency_sek,
                        vm.currency, sek,
                        { vm.currency.value = sek }
                    )

                    initSettingWidget(adapter, browserNo, R.string.option_no,
                        vm.useBrowser, false,
                        { vm.useBrowser.value = false }
                    )
                    initSettingWidget(adapter, browserYes, R.string.option_yes,
                        vm.useBrowser, true,
                        { vm.useBrowser.value = true }
                    )

                    initSettingWidget(adapter, countryNorway, R.string.norway,
                        vm.userCountry, ProductsViewModel.UserCountry.NORWAY,
                        { vm.userCountry.value = ProductsViewModel.UserCountry.NORWAY }
                    )
                    initSettingWidget(adapter, countrySweden, R.string.sweden,
                        vm.userCountry, ProductsViewModel.UserCountry.SWEDEN,
                        { vm.userCountry.value = ProductsViewModel.UserCountry.SWEDEN }
                    )

                    initSettingWidget(adapter, disablePaymentMenuNo, R.string.disable_payment_menu_no,
                        vm.disablePaymentsMenu, false,
                        { vm.disablePaymentsMenu.value = false }
                    )
                    initSettingWidget(adapter, disablePaymentMenuYes, R.string.disable_payment_menu_yes,
                        vm.disablePaymentsMenu, true,
                        { vm.disablePaymentsMenu.value = true }
                    )

                    instrumentsInput.setText(vm.restrictedInstrumentsInput.value)
                    instrumentsInput.doAfterTextChanged {
                        vm.restrictedInstrumentsInput.value = it?.toString()
                    }

                    initSettingWidget(adapter, useBogusHosturlNo, R.string.use_bogus_hosturl_no,
                        vm.useBogusHostUrl, false
                    )
                    initSettingWidget(adapter, useBogusHosturlYes, R.string.use_bogus_hosturl_yes,
                        vm.useBogusHostUrl, true
                    )

                    initInstrumentModeInput(adapter, instrumentModeInput)

                    vm.payerReference.observe(adapter.lifecycleOwner, payerReferenceInput::setTextIfNeeded)
                    payerReferenceInput.doAfterTextChanged {
                        vm.payerReference.value = it?.toString()?.takeUnless(String::isEmpty)
                    }
                    payerReferenceGenerate.setOnClickListener {
                        vm.setRandomPayerReference()
                    }
                    payerReferenceLastUsed.setOnClickListener {
                        vm.setPayerReferenceToLastUsed()
                    }

                    vm.paymentToken.observe(adapter.lifecycleOwner, paymentTokenInput::setTextIfNeeded)
                    paymentTokenInput.doAfterTextChanged {
                        vm.paymentToken.value = it?.toString()?.takeUnless(String::isEmpty)
                    }

                    initSettingWidget(adapter, generateTokenNo, R.string.option_no,
                        vm.generatePaymentToken, false
                    )
                    initSettingWidget(adapter, generateTokenYes, R.string.option_yes,
                        vm.generatePaymentToken, true
                    )

                    paymentTokenGet.setOnClickListener {
                        vm.onGetPaymentTokenPressed()
                    }

                    vm.subsite.observe(adapter.lifecycleOwner, subsiteInput::setTextIfNeeded)
                    subsiteInput.doAfterTextChanged {
                        vm.subsite.value = it?.toString()?.takeUnless(String::isEmpty)
                    }
                }

            private fun initInstrumentModeInput(
                adapter: CartAndSettingsAdapter,
                input: AutoCompleteTextView
            ) {
                val instruments = PaymentInstruments.all
                val context = input.context
                val options = listOf("") + instruments
                val inputAdapter = object : ArrayAdapter<String>(
                    context,
                    android.R.layout.simple_spinner_item,
                    options
                ) {
                    override fun getView(
                        position: Int,
                        convertView: View?,
                        parent: ViewGroup
                    ): View {
                        return super.getView(position, convertView, parent).also {
                            if (position == 0) {
                                (it as? TextView)?.setText(R.string.instrument_none)
                            }
                        }
                    }
                }
                input.setAdapter(inputAdapter)
                input.onItemClickListener = AdapterView.OnItemClickListener { _, view, _, _ ->
                    view.hideSoftKeyboard()
                }

                val vm = adapter.viewModel
                vm.paymentInstrument.observe(adapter.lifecycleOwner, input::setTextIfNeeded)
                input.doAfterTextChanged {
                    vm.paymentInstrument.value = it?.toString()?.takeUnless(String::isEmpty)
                }
            }
        },

        STYLE {
            override fun createViewHolder(
                adapter: CartAndSettingsAdapter,
                parent: ViewGroup
            ) = StyleCellViewHolder(adapter, parent)
        };

        abstract fun createViewHolder(
            adapter: CartAndSettingsAdapter,
            parent: ViewGroup
        ): ViewHolder

        protected inline fun <B : ViewBinding> createViewHolder(
            parent: ViewGroup,
            inflater: (LayoutInflater, ViewGroup?, Boolean) -> B,
            setup: B.() -> Unit
        ): ViewHolder {
            val binding = inflate(parent, inflater)
            binding.setup()
            return ViewHolder(binding.root)
        }

        protected inline fun <B : ViewBinding> inflate(parent: ViewGroup, inflater: (LayoutInflater, ViewGroup?, Boolean) -> B): B {
            return inflater(LayoutInflater.from(parent.context), parent, false)
        }

        protected fun <T> initSettingWidget(
            adapter: CartAndSettingsAdapter,
            widget: SettingsOptionLabelBinding,
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
            widget: SettingsOptionLabelBinding,
            @StringRes labelId: Int,
            setting: LiveData<T>,
            settingValue: T,
            onClick: View.OnClickListener
        ) {
            widget.textView.setText(labelId)
            widget.root.setOnClickListener(onClick)
            setting.observe(adapter.lifecycleOwner, {
                val checked = it == settingValue
                widget.textView.typeface = ResourcesCompat.getFont(
                    widget.textView.context,
                    if (checked) R.font.ibm_plex_mono_bold else R.font.ibm_plex_mono_regular
                )
                widget.underline.visibility =
                    if (checked) View.VISIBLE else View.GONE
            })
        }

        protected fun bindTextView(
            widget: TextView,
            setting: MutableLiveData<String?>
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
            Cell.Style -> newItem is Cell.Style
        }

        override fun areContentsTheSame(oldItem: Cell, newItem: Cell) = when (oldItem) {
            Cell.Environment -> newItem is Cell.Environment
            Cell.Header -> newItem is Cell.Header
            is Cell.Item -> newItem is Cell.Item && oldItem.shopItem == newItem.shopItem
            Cell.Footer -> newItem is Cell.Footer
            Cell.Consumer -> newItem is Cell.Consumer
            Cell.Settings -> newItem is Cell.Settings
            Cell.Style -> newItem is Cell.Style
        }
    }
}
