package com.swedbankpay.exampleapp.util

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.webkit.PermissionRequest
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.swedbankpay.exampleapp.MainActivity

object PermissionUtil {
    private const val REQUEST_CODE_PERMISSIONS = 10
    private val REQUIRED_PERMISSIONS =
        mutableListOf (
            Manifest.permission.CAMERA
        ).toTypedArray()

    fun allPermissionsGranted(context: Context) =  REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            context, it) == PackageManager.PERMISSION_GRANTED
    }

    fun requestCameraPermission(activity: Activity) {
        ActivityCompat.requestPermissions(
            activity,
            REQUIRED_PERMISSIONS,
            REQUEST_CODE_PERMISSIONS
        )
    }
}