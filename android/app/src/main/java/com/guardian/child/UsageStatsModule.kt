
package com.guardian.child

import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.PackageManager
import com.facebook.react.bridge.*
import com.google.firebase.firestore.FirebaseFirestore
import java.util.*

class UsageStatsModule(reactContext: ReactApplicationContext) : ReactContextBaseJavaModule(reactContext) {

    override fun getName() = "UsageStatsModule"

    private val db = FirebaseFirestore.getInstance()

    @ReactMethod
    fun checkUsageStatsPermission(promise: Promise) {
        val appOps = reactApplicationContext.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            android.os.Process.myUid(),
            reactApplicationContext.packageName
        )
        promise.resolve(mode == AppOpsManager.MODE_ALLOWED)
    }

    @ReactMethod
    fun getRecentApps(promise: Promise) {
        val usageStatsManager = reactApplicationContext.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val endTime = System.currentTimeMillis()
        val beginTime = endTime - 1000 * 60 * 60 * 24 // 24 hours
        val usageStatsList = usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, beginTime, endTime)
        val recentApps = Arguments.createArray()
        usageStatsList.sortByDescending { it.lastTimeUsed }
        for (usageStats in usageStatsList) {
            val app = Arguments.createMap()
            app.putString("packageName", usageStats.packageName)
            app.putDouble("lastTimeUsed", usageStats.lastTimeUsed.toDouble())
            recentApps.pushMap(app)
        }
        promise.resolve(recentApps)
    }
    
    @ReactMethod
    fun getUsageDuration(promise: Promise) {
        val usageStatsManager = reactApplicationContext.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val endTime = System.currentTimeMillis()
        val beginTime = endTime - 1000 * 60 * 60 * 24 // 24 hours
        val usageStatsList = usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, beginTime, endTime)
        val appUsage = Arguments.createMap()
        for (usageStats in usageStatsList) {
            appUsage.putInt(usageStats.packageName, (usageStats.totalTimeInForeground / 1000).toInt())
        }
        promise.resolve(appUsage)
    }

    @ReactMethod
    fun uploadUsageStats(deviceId: String, promise: Promise) {
        if (deviceId.isEmpty()) {
            promise.reject("DEVICE_ID_NULL", "Device ID is null or empty")
            return
        }
        val usageStatsManager = reactApplicationContext.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val endTime = System.currentTimeMillis()
        val beginTime = endTime - 1000 * 60 * 60 * 24 
        val usageStatsList = usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, beginTime, endTime)
        val usageData = mutableMapOf<String, Any>()
        for (usageStats in usageStatsList) {
            if(usageStats.totalTimeInForeground > 0) {
                val appData = mutableMapOf<String, Any>()
                appData["totalTimeInForeground"] = usageStats.totalTimeInForeground
                appData["lastTimeUsed"] = usageStats.lastTimeUsed
                usageData[usageStats.packageName] = appData
            }
        }
        
        val data = hashMapOf(
            "timestamp" to System.currentTimeMillis(),
            "usageStats" to usageData
        )

        db.collection("childDevices").document(deviceId).collection("monitoring")
            .add(data)
            .addOnSuccessListener { promise.resolve("Usage stats uploaded successfully") }
            .addOnFailureListener { e -> promise.reject("FIRESTORE_ERROR", "Error uploading usage stats", e) }
    }
}
