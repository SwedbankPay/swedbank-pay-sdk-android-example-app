package com.swedbankpay.exampleapp.standaloneurlconfig

import android.app.Application
import android.content.Context
import android.view.View
import android.widget.LinearLayout
import androidx.camera.mlkit.vision.MlKitAnalyzer
import androidx.camera.view.CameraController.COORDINATE_SYSTEM_VIEW_REFERENCED
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.swedbankpay.exampleapp.util.SwedbankPayConfig
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class StandaloneUrlConfigViewModel(application: Application): AndroidViewModel(application) {
    var viewPaymentUrl = MutableLiveData<String>()

    var baseUrl = MutableLiveData<String>()

    var completeUrl = MutableLiveData<String>()

    var cancelUrl = MutableLiveData<String>()

    var useCheckoutV3 = MutableLiveData<Boolean>()

    var swedbankPayConfiguration = MutableLiveData<SwedbankPayConfig>()

    init {
        viewPaymentUrl.value = ""
        baseUrl.value = getLastUsedBaseUrl()
        completeUrl.value = getLastUsedCompleteUrl()
        cancelUrl.value = getLastUsedCancelUrl()
        useCheckoutV3.value = getLastUsedV3Selection()
    }

    fun onCheckoutPressed() {
        baseUrl.value?.let { saveLastUsedBaseUrl(it) }
        completeUrl.value?.let { saveLastUsedCompleteUrl(it) }
        cancelUrl.value?.let { saveLastUsedCancelUrl(it) }
        useCheckoutV3.value?.let {saveLastUsedV3Selection(it) }

        swedbankPayConfiguration.value = viewPaymentUrl.value?.let {
            SwedbankPayConfig(
                url = it,
                baseUrl = baseUrl.value ?: "",
                completeUrl = completeUrl.value ?: "",
                cancelUrl = cancelUrl.value ?: "",
                isV3 = useCheckoutV3.value ?: true
            )
        }
    }

    private fun getLastUsedBaseUrl(): String {
        return getApplication<Application>().getSharedPreferences(
            "StandaloneUrlConfig",
            Context.MODE_PRIVATE
        )?.getString("BASE_URL_CONFIG", null) ?: ""
    }

    private fun saveLastUsedBaseUrl(url: String) {
        getApplication<Application>().getSharedPreferences(
            "StandaloneUrlConfig",
            Context.MODE_PRIVATE
        )?.edit {
            putString("BASE_URL_CONFIG", url)
        }
    }

    private fun getLastUsedCompleteUrl(): String {
        return getApplication<Application>().getSharedPreferences(
            "StandaloneUrlConfig",
            Context.MODE_PRIVATE
        )?.getString("COMPLETE_URL_CONFIG", null) ?: ""
    }

    private fun saveLastUsedCompleteUrl(url: String) {
        getApplication<Application>().getSharedPreferences(
            "StandaloneUrlConfig",
            Context.MODE_PRIVATE
        )?.edit {
            putString("COMPLETE_URL_CONFIG", url)
        }
    }

    private fun getLastUsedCancelUrl(): String {
        return getApplication<Application>().getSharedPreferences(
            "StandaloneUrlConfig",
            Context.MODE_PRIVATE
        )?.getString("CANCEL_URL_CONFIG", null) ?: ""
    }

    private fun saveLastUsedCancelUrl(url: String) {
        getApplication<Application>().getSharedPreferences(
            "StandaloneUrlConfig",
            Context.MODE_PRIVATE
        )?.edit {
            putString("CANCEL_URL_CONFIG", url)
        }
    }

    private fun getLastUsedV3Selection(): Boolean {
        return getApplication<Application>().getSharedPreferences(
            "StandaloneUrlConfig",
            Context.MODE_PRIVATE
        )?.getBoolean("V3_CONFIG", true) ?: true
    }

    private fun saveLastUsedV3Selection(isChecked: Boolean) {
        getApplication<Application>().getSharedPreferences(
            "StandaloneUrlConfig",
            Context.MODE_PRIVATE
        )?.edit {
            putBoolean("V3_CONFIG", isChecked)
        }
    }
}