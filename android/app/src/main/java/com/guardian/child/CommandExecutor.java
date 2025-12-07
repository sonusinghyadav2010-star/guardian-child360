
package com.guardian.child;

import android.content.Context;
import android.content.Intent;
import android.hardware.camera2.CameraManager;
import android.os.Vibrator;
import android.provider.MediaStore;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;

public class CommandExecutor extends ReactContextBaseJavaModule {

    private final ReactApplicationContext reactContext;

    public CommandExecutor(ReactApplicationContext reactContext) {
        super(reactContext);
        this.reactContext = reactContext;
    }

    @Override
    public String getName() {
        return "CommandExecutor";
    }

    @ReactMethod
    public void execute(String command, Promise promise) {
        Intent serviceIntent = new Intent(reactContext, CommandService.class);
        serviceIntent.putExtra("command", command);
        reactContext.startService(serviceIntent);
        promise.resolve("Command sent to service: " + command);
    }
}
