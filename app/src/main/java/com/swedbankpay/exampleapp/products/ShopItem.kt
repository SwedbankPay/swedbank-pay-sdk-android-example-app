package com.swedbankpay.exampleapp.products

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.Drawable
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.appcompat.content.res.AppCompatResources
import androidx.lifecycle.MutableLiveData
import com.swedbankpay.exampleapp.BuildConfig
import com.swedbankpay.exampleapp.R

data class ShopItem(
    val orderItemReference: String,
    val name: String,
    @ColorInt val imageBackground: Int,
    val image: Drawable?,
    val price: Int
) {
    val inCart = MutableLiveData<Boolean>().apply { value = false }

    companion object {
        fun demoItems(context: Context): List<ShopItem> {
            fun item(
                orderItemReference: String,
                name: String,
                @ColorInt imageBackground: Int,
                @DrawableRes imageId: Int,
                price: Int
            ) = ShopItem(
                orderItemReference,
                name,
                imageBackground,
                AppCompatResources.getDrawable(context, imageId),
                if (BuildConfig.ENABLE_PROD_DEMO) 1_00 else price
            )

            return listOf(
                item(
                    "pinksneak",
                    "Pink sneakers",
                    Color.rgb(255, 207, 207),
                    R.drawable.pink_sneakers,
                    1599_00
                ),
                item(
                    "redskate",
                    "Red skate shoes",
                    Color.rgb(154, 45, 58),
                    R.drawable.red_skate_shoes,
                    999_00
                ),
                item(
                    "redsneak",
                    "Red sneakers",
                    Color.rgb(240, 49, 45),
                    R.drawable.red_sneakers,
                    1899_00
                ),
                item(
                    "yellowskate",
                    "Yellow skate shoes",
                    Color.rgb(244, 184, 0),
                    R.drawable.yellow_skate_shoes,
                    899_00
                ),
                item(
                    "greysneak",
                    "Grey sneakers",
                    Color.rgb(208, 208, 208),
                    R.drawable.grey_sneakers,
                    2499_00
                )
            )
        }
    }
}