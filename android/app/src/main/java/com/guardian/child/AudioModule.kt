package com.guardian.child

import android.content.Intent
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod

class AudioModule(reactContext: ReactApplicationContext) : ReactContextBaseJavaModule(reactContext) {

    override fun getName() = "AudioModule"

    @ReactMethod
    fun startAudioRecording() {
        val intent = Intent(reactApplicationContext, AudioService::class.java)
        reactApplicationContext.startService(intent)
    }

    @ReactMethod
    fun stopAudioRecording() {
        val intent = Intent(reactApplicationContext, AudioService::class.java)
        reactApplicationContext.stopService(intent)
    }
}
