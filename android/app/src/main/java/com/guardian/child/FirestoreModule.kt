package com.guardian.child

import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod
import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReadableMap
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import android.util.Log

class FirestoreModule(reactContext: ReactApplicationContext) : ReactContextBaseJavaModule(reactContext) {

    override fun getName() = "FirestoreModule"

    private val db = Firebase.firestore

    private fun getDeviceId(promise: Promise): String? {
        val sharedPreferences = reactApplicationContext.getSharedPreferences("GuardianChildPrefs", 0)
        val deviceId = sharedPreferences.getString("deviceId", null)
        if (deviceId == null || deviceId.isEmpty()) {
            promise.reject("DEVICE_ID_ERROR", "Device ID is not set")
            return null
        }
        return deviceId
    }

    private fun uploadData(collection: String, doc: String? = null, data: Map<String, Any>, promise: Promise, merge: Boolean = false) {
        val deviceId = getDeviceId(promise) ?: return
        val collectionRef = db.collection("childDevices").document(deviceId).collection(collection)

        val task = if (doc != null) {
            if (merge) collectionRef.document(doc).set(data, com.google.firebase.firestore.SetOptions.merge()) else collectionRef.document(doc).set(data)
        } else {
            collectionRef.add(data)
        }

        task
            .addOnSuccessListener { promise.resolve("Data uploaded successfully to $collection") }
            .addOnFailureListener { e -> promise.reject("FIRESTORE_ERROR", "Error uploading to $collection", e) }
    }

    @ReactMethod
    fun uploadCommandResult(commandId: String, result: ReadableMap, promise: Promise) {
        uploadData("responses", commandId, result.toHashMap(), promise)
    }

    @ReactMethod
    fun uploadHeartbeat(promise: Promise) {
        val data = hashMapOf("lastSeen" to System.currentTimeMillis())
        uploadData("status", "latest", data, promise, merge = true)
    }

    @ReactMethod
    fun uploadSnapshotCollection(collectionName: String, data: ReadableMap, promise: Promise) {
         val snapshotData = hashMapOf(
            "type" to collectionName,
            "timestamp" to System.currentTimeMillis(),
            "data" to data.toHashMap()
        )
        uploadData("monitoring", null, snapshotData, promise)
    }

    @ReactMethod
    fun uploadErrorLog(error: ReadableMap, promise: Promise) {
        uploadData("errors", null, error.toHashMap(), promise)
    }
    
    @ReactMethod
    fun uploadMonitoringData(type: String, data: ReadableMap, promise: Promise) {
        val dataMap = data.toHashMap()
        dataMap["type"] = type
        dataMap["timestamp"] = System.currentTimeMillis()
        uploadData("monitoring", null, dataMap, promise)
    }

    @ReactMethod
    fun uploadUsageStats(data: ReadableMap, promise: Promise) {
        uploadMonitoringData("usageStats", data, promise)
    }

    @ReactMethod
    fun uploadNotification(data: ReadableMap, promise: Promise) {
        uploadMonitoringData("notification", data, promise)
    }

    @ReactMethod
    fun uploadAccessibility(data: ReadableMap, promise: Promise) {
        uploadMonitoringData("accessibility", data, promise)
    }

    @ReactMethod
    fun uploadActiveApp(data: ReadableMap, promise: Promise) {
        uploadMonitoringData("activeApp", data, promise)
    }
}
