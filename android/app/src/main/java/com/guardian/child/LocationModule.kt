package com.guardian.child

import android.content.Intent
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod

class LocationModule(reactContext: ReactApplicationContext) : ReactContextBaseJavaModule(reactContext) {

    override fun getName() = "LocationModule"

    @ReactMethod
    fun startLocationTracking() {
        val intent = Intent(reactApplicationContext, LocationService::class.java)
        reactApplicationContext.startService(intent)
    }

    @ReactMethod
    fun stopLocationTracking() {
        val intent = Intent(reactApplicationContext, LocationService::class.java)
        reactApplicationContext.stopService(intent)
    }
}
