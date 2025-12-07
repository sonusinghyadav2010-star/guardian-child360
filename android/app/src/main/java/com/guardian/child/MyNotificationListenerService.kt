package com.guardian.child

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.ReactContext
import com.facebook.react.modules.core.DeviceEventManagerModule

class MyNotificationListenerService : NotificationListenerService() {

    private val TAG = "NotificationListener"

    private fun sendEvent(reactContext: ReactContext, eventName: String, params: Arguments?) {
        reactContext
            .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter::class.java)
            .emit(eventName, params)
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        val packageName = sbn?.packageName
        val notification = sbn?.notification
        val title = notification?.extras?.getString("android.title")
        val text = notification?.extras?.getCharSequence("android.text")?.toString()

        if (packageName != null && title != null && text != null) {
            val eventData = Arguments.createMap().apply {
                putString("packageName", packageName)
                putString("title", title)
                putString("text", text)
            }
            sendEvent(this.application as ReactContext, "onNotificationReceived", eventData)
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
        // Optional: Handle notification removal
    }
}
