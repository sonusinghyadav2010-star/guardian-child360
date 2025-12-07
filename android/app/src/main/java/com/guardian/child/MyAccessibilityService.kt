package com.guardian.child

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.facebook.react.HeadlessJsTaskService
import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.ReactContext
import com.facebook.react.modules.core.DeviceEventManagerModule

class MyAccessibilityService : AccessibilityService() {

    private val TAG = "MyAccessibilityService"

    private fun sendEvent(reactContext: ReactContext, eventName: String, params: Arguments?) {
        reactContext
            .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter::class.java)
            .emit(eventName, params)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val packageName = event.packageName?.toString()
            if (packageName != null) {
                val eventData = Arguments.createMap().apply {
                    putString("packageName", packageName)
                }
                sendEvent(this.application as ReactContext, "onAppOpened", eventData)
            }
        }
    }

    override fun onInterrupt() {
        Log.e(TAG, "Accessibility service interrupted.")
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d(TAG, "Accessibility service connected.")
    }
}
