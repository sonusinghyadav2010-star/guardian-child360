
package com.guardian.child

import android.Manifest
import android.app.AppOpsManager
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Process
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import com.facebook.react.bridge.*
import com.facebook.react.modules.core.PermissionAwareActivity
import com.facebook.react.modules.core.PermissionListener

class PermissionsModule(reactContext: ReactApplicationContext) : ReactContextBaseJavaModule(reactContext), PermissionListener {

    private var promise: Promise? = null
    private val PERMISSION_REQUEST_CODE = 123

    override fun getName() = "PermissionsModule"

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray): Boolean {
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                promise?.resolve(true)
            } else {
                promise?.resolve(false)
            }
            promise = null
        }
        return true
    }

    @ReactMethod
    fun checkPermission(permissionId: String, promise: Promise) {
        when (permissionId) {
            "accessibility" -> isAccessibilityPermissionGranted(promise)
            "notification" -> isNotificationListenerPermissionGranted(promise)
            "usage" -> isUsageAccessPermissionGranted(promise)
            "overlay" -> isOverlayPermissionGranted(promise)
            "camera" -> isPermissionGranted(Manifest.permission.CAMERA, promise)
            "microphone" -> isPermissionGranted(Manifest.permission.RECORD_AUDIO, promise)
            "location" -> isPermissionGranted(Manifest.permission.ACCESS_FINE_LOCATION, promise)
            "backgroundLocation" -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    isPermissionGranted(Manifest.permission.ACCESS_BACKGROUND_LOCATION, promise)
                } else {
                    promise.resolve(true)
                }
            }
            else -> promise.reject("UNKNOWN_PERMISSION", "Unknown permission ID: $permissionId")
        }
    }

    @ReactMethod
    fun requestPermission(permissionId: String, promise: Promise) {
        this.promise = promise
        val activity = currentActivity as? PermissionAwareActivity
        if (activity == null) {
            promise.reject("ACTIVITY_NOT_FOUND", "Activity not found")
            return
        }

        when (permissionId) {
            "camera" -> activity.requestPermissions(arrayOf(Manifest.permission.CAMERA), PERMISSION_REQUEST_CODE, this)
            "microphone" -> activity.requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), PERMISSION_REQUEST_CODE, this)
            "location" -> activity.requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), PERMISSION_REQUEST_CODE, this)
            "backgroundLocation" -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    activity.requestPermissions(arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION), PERMISSION_REQUEST_CODE, this)
                } else {
                    promise.resolve(true)
                }
            }
            else -> openSettings(permissionId)
        }
    }

    @ReactMethod
    fun openSettings(permissionId: String) {
        val intent = when (permissionId) {
            "accessibility" -> Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            "notification" -> Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
            "usage" -> Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
            "overlay" -> Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:${reactApplicationContext.packageName}")
            )
            else -> Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.parse("package:${reactApplicationContext.packageName}")
            )
        }

        if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            reactApplicationContext.startActivity(intent)
        }
    }

    private fun isPermissionGranted(permission: String, promise: Promise) {
        val granted = reactApplicationContext.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED
        promise.resolve(granted)
    }

    private fun isAccessibilityPermissionGranted(promise: Promise) {
        val accessibilityEnabled = try {
            Settings.Secure.getInt(reactApplicationContext.contentResolver, Settings.Secure.ACCESSIBILITY_ENABLED)
        } catch (e: Settings.SettingNotFoundException) {
            0
        }
        if (accessibilityEnabled == 1) {
            val enabledServices = Settings.Secure.getString(reactApplicationContext.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
            val serviceId = "${reactApplicationContext.packageName}/${MyAccessibilityService::class.java.canonicalName}"
            promise.resolve(enabledServices?.contains(serviceId) == true)
        } else {
            promise.resolve(false)
        }
    }

    private fun isNotificationListenerPermissionGranted(promise: Promise) {
        val enabledListeners = NotificationManagerCompat.getEnabledListenerPackages(reactApplicationContext)
        promise.resolve(enabledListeners.contains(reactApplicationContext.packageName))
    }

    private fun isUsageAccessPermissionGranted(promise: Promise) {
        val appOps = reactApplicationContext.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                reactApplicationContext.packageName
            )
        } else {
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                reactApplicationContext.packageName
            )
        }
        promise.resolve(mode == AppOpsManager.MODE_ALLOWED)
    }

    private fun isOverlayPermissionGranted(promise: Promise) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            promise.resolve(Settings.canDrawOverlays(reactApplicationContext))
        } else {
            promise.resolve(true)
        }
    }
}
