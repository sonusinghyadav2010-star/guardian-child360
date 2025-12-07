package com.guardian.child.webrtc;

import android.app.Activity;
import android.content.Intent;
import android.media.projection.MediaProjectionManager;

import com.facebook.react.bridge.ActivityEventListener;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableMap;

public class WebRTCModule extends ReactContextBaseJavaModule implements ActivityEventListener {
    private final WebRTCEngine webRTCEngine;
    private static final int SCREEN_CAPTURE_REQUEST_CODE = 1;

    public WebRTCModule(ReactApplicationContext reactContext) {
        super(reactContext);
        this.webRTCEngine = new WebRTCEngine(reactContext);
        reactContext.addActivityEventListener(this);
    }

    @Override
    public String getName() {
        return "WebRTCModule";
    }

    @ReactMethod
    public void initWebRTC(Promise promise) {
        webRTCEngine.initWebRTC();
        promise.resolve(null);
    }

    @ReactMethod
    public void createOffer(Promise promise) {
        webRTCEngine.createOffer(promise);
    }

    @ReactMethod
    public void createAnswer(ReadableMap sdp, Promise promise) {
        webRTCEngine.createAnswer(sdp, promise);
    }

    @ReactMethod
    public void addIceCandidate(ReadableMap candidate, Promise promise) {
        webRTCEngine.addIceCandidate(candidate);
        promise.resolve(null);
    }

    @ReactMethod
    public void startCameraStream(Promise promise) {
        webRTCEngine.startCameraStream();
        promise.resolve(null);
    }

    @ReactMethod
    public void stopCameraStream(Promise promise) {
        webRTCEngine.stopCameraStream();
        promise.resolve(null);
    }

    @ReactMethod
    public void startScreenStream(Promise promise) {
        ScreenCaptureService.setPromise(promise);
        ScreenCaptureService.setContext(getReactApplicationContext());
        MediaProjectionManager mediaProjectionManager = (MediaProjectionManager) getReactApplicationContext().getSystemService(ReactApplicationContext.MEDIA_PROJECTION_SERVICE);
        Activity activity = getCurrentActivity();
        if (activity != null) {
            activity.startActivityForResult(mediaProjectionManager.createScreenCaptureIntent(), SCREEN_CAPTURE_REQUEST_CODE, null);
        }
    }

    @ReactMethod
    public void stopScreenStream(Promise promise) {
        webRTCEngine.stopScreenStream();
        promise.resolve(null);
    }

    @Override
    public void onActivityResult(Activity activity, int requestCode, int resultCode, Intent data) {
        if (requestCode == SCREEN_CAPTURE_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                webRTCEngine.startScreenStream(data);
            }
        }
    }

    @Override
    public void onNewIntent(Intent intent) {

    }
}
