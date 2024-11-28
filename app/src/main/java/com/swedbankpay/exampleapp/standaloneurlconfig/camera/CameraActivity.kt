package com.swedbankpay.exampleapp.standaloneurlconfig.camera

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.mlkit.vision.MlKitAnalyzer
import androidx.camera.view.CameraController.COORDINATE_SYSTEM_VIEW_REFERENCED
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.swedbankpay.exampleapp.R
import com.swedbankpay.exampleapp.databinding.ActivityCameraBinding

class CameraActivity : AppCompatActivity(R.layout.activity_camera) {

    companion object {
        const val SCANNED_URL_KEY = "scanned_url"
    }

    private lateinit var binding: ActivityCameraBinding
    private lateinit var barcodeScanner: BarcodeScanner

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCameraBinding.inflate(layoutInflater)
        setContentView(binding.root)

        startCamera()
    }

    private fun startCamera() {
        val cameraController = LifecycleCameraController(this)
        val previewView: PreviewView = binding.cameraView

        val options = BarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
            .build()
        barcodeScanner = BarcodeScanning.getClient(options)

        cameraController.setImageAnalysisAnalyzer(
            ContextCompat.getMainExecutor(this),
            MlKitAnalyzer(
                listOf(barcodeScanner),
                COORDINATE_SYSTEM_VIEW_REFERENCED,
                ContextCompat.getMainExecutor(this)
            ) { result: MlKitAnalyzer.Result? ->
                val barcodeResult = result?.getValue(barcodeScanner)
                if (barcodeResult == null || barcodeResult.size == 0 || barcodeResult.first() == null) {
                    return@MlKitAnalyzer
                }
                when (barcodeResult.first().valueType) {
                    Barcode.TYPE_URL -> {
                        scannedBarcodeReturn(
                            barcodeResult.first().url?.url
                                ?: getString(R.string.scanner_no_url_found)
                        )
                    }

                    Barcode.TYPE_TEXT -> {
                        scannedBarcodeReturn(
                            barcodeResult.first().rawValue
                                ?: getString(R.string.scanner_no_number_found)
                        )
                    }
                }
            }
        )
        cameraController.bindToLifecycle(this)
        previewView.controller = cameraController
    }

    private fun scannedBarcodeReturn(text: String) {
        val intent = Intent()
        intent.putExtra(SCANNED_URL_KEY, text)
        setResult(RESULT_OK, intent)
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        barcodeScanner.close()
    }
}