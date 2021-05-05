package com.swedbankpay.exampleapp.products

import android.content.res.ColorStateList
import android.transition.TransitionManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import com.swedbankpay.exampleapp.R
import com.swedbankpay.exampleapp.databinding.ProductsHeaderCellBinding
import com.swedbankpay.exampleapp.databinding.ProductsItemCellBinding

class ProductsAdapter(
    val lifecycleOwner: LifecycleOwner,
    val viewModel: ProductsViewModel
) : RecyclerView.Adapter<ProductsAdapter.ViewHolder>() {
    private val items = viewModel.products

    override fun getItemCount() = 1 + items.size

    override fun getItemViewType(position: Int) = when (position) {
        0 -> ViewType.HEADER.ordinal
        else -> ViewType.ITEM.ordinal
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewType.values()[viewType].createViewHolder(parent)

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(this, position)

    override fun onViewRecycled(holder: ViewHolder) = holder.onRecycled()

    private fun getItem(position: Int) = items[position - 1]

    open class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        open fun bind(adapter: ProductsAdapter, position: Int) {}
        open fun onRecycled() {}
    }

    private enum class ViewType {
        HEADER {
            override fun createViewHolder(parent: ViewGroup): ViewHolder {
                val binding = inflate(parent, ProductsHeaderCellBinding::inflate).apply {
                    title.typeface = ResourcesCompat.getFont(
                        title.context,
                        R.font.ibm_plex_mono_medium
                    )
                }
                return ViewHolder(binding.root)
            }
        },
        ITEM {
            override fun createViewHolder(parent: ViewGroup): ViewHolder {
                val binding = inflate(parent, ProductsItemCellBinding::inflate).apply {
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
                }

                return object : ViewHolder(binding.root) {
                    private val inCartObserver = Observer<Boolean> {
                        TransitionManager.beginDelayedTransition(this.itemView as ViewGroup)
                        setButtonState(
                            when (it) {
                                true -> AddRemoveButtonState.REMOVE
                                else -> AddRemoveButtonState.ADD
                            }, false
                        )
                    }

                    private var item: ShopItem? = null
                    private var buttonState =
                        AddRemoveButtonState.ADD

                    private val priceFormatterObserver = Observer<(Int) -> String> {
                        binding.itemPrice.text = item?.price?.let(it)
                    }
                    private var priceFormatter: LiveData<(Int) -> String>? = null

                    init {
                        binding.addRemoveButton.setOnClickListener {
                            item?.inCart?.apply {
                                value = value != true
                            }
                        }
                    }

                    override fun bind(adapter: ProductsAdapter, position: Int) {
                        val item = adapter.getItem(position)
                        binding.apply {
                            itemBackground.imageTintList =
                                ColorStateList.valueOf(item.imageBackground)
                            itemImage.setImageDrawable(item.image)
                            itemName.text = item.name

                            setButtonState(
                                when (item.inCart.value) {
                                    true -> AddRemoveButtonState.REMOVE
                                    else -> AddRemoveButtonState.ADD
                                }, true
                            )
                        }

                        this.item?.inCart?.removeObserver(inCartObserver)
                        this.item = item
                        item.inCart.observe(adapter.lifecycleOwner, inCartObserver)

                        priceFormatter?.removeObserver(priceFormatterObserver)
                        priceFormatter = adapter.viewModel.itemPriceFormatter.apply {
                            observe(adapter.lifecycleOwner, priceFormatterObserver)
                        }
                    }

                    override fun onRecycled() {
                        item?.inCart?.removeObserver(inCartObserver)
                        item = null
                        priceFormatter?.removeObserver(priceFormatterObserver)
                        priceFormatter = null
                    }

                    private fun setButtonState(state: AddRemoveButtonState, force: Boolean) {
                        if (force || state != buttonState) {
                            buttonState = state

                            binding.apply {
                                val context = root.context
                                addRemoveButton.contentDescription =
                                    context.getString(state.descriptionId)
                                addRemoveButton.backgroundTintList = ColorStateList.valueOf(
                                    ContextCompat.getColor(
                                        context,
                                        state.tintId
                                    )
                                )
                                addRemoveButtonIcon.setImageResource(state.iconId)
                                removeButtonLabel.visibility = state.removeButtonVisibility
                            }
                        }
                    }
                }
            }
        };

        abstract fun createViewHolder(parent: ViewGroup): ViewHolder
        protected inline fun <B : ViewBinding> inflate(parent: ViewGroup, inflater: (LayoutInflater, ViewGroup?, Boolean) -> B): B {
            return inflater(LayoutInflater.from(parent.context), parent, false)
        }

        private enum class AddRemoveButtonState(
            @StringRes val descriptionId: Int,
            @ColorRes val tintId: Int,
            @DrawableRes val iconId: Int,
            val removeButtonVisibility: Int
        ) {
            ADD(
                R.string.add_to_cart_description,
                R.color.add_to_cart_button,
                R.drawable.ic_add_to_cart,
                View.GONE
            ),
            REMOVE(
                R.string.remove_from_cart_description,
                R.color.remove_from_cart_button,
                R.drawable.ic_remove_from_cart,
                View.VISIBLE
            )
        }
    }
}