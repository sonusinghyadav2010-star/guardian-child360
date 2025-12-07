
package com.guardian.child;

import android.accessibilityservice.AccessibilityService;
import android.content.Context;
import android.content.SharedPreferences;
import android.view.accessibility.AccessibilityEvent;
import android.util.Log;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;

public class MyAccessibilityService extends AccessibilityService {

    private static final String TAG = "MyAccessibilityService";
    private FirebaseFirestore db;
    private String deviceId;
    private String currentApp = "";
    private long startTime = 0;

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED && event.getPackageName() != null) {
            String packageName = event.getPackageName().toString();
            if (!packageName.equals(currentApp)) {
                if (!currentApp.isEmpty() && startTime > 0) {
                    long endTime = System.currentTimeMillis();
                    long duration = endTime - startTime;
                    uploadAppUsage(currentApp, duration);
                }
                currentApp = packageName;
                startTime = System.currentTimeMillis();
                uploadActiveApp(currentApp);
            }
        }
    }

    private void uploadActiveApp(String packageName) {
        if (deviceId == null || deviceId.isEmpty()) {
            return;
        }
        Map<String, Object> data = new HashMap<>();
        data.put("packageName", packageName);
        data.put("timestamp", System.currentTimeMillis());

        db.collection("childDevices").document(deviceId).collection("monitoring")
                .add(data)
                .addOnSuccessListener(documentReference -> Log.d(TAG, "Active app saved"))
                .addOnFailureListener(e -> Log.e(TAG, "Error saving active app", e));
    }
    
    private void uploadAppUsage(String packageName, long duration) {
        if (deviceId == null || deviceId.isEmpty()) {
            return;
        }
        Map<String, Object> data = new HashMap<>();
        data.put("packageName", packageName);
        data.put("duration", duration);
        data.put("timestamp", System.currentTimeMillis());

        db.collection("childDevices").document(deviceId).collection("monitoring")
                .add(data)
                .addOnSuccessListener(documentReference -> Log.d(TAG, "App usage saved"))
                .addOnFailureListener(e -> Log.e(TAG, "Error saving app usage", e));
    }

    @Override
    public void onInterrupt() {
        Log.w(TAG, "onInterrupt: Accessibility service interrupted.");
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        db = FirebaseFirestore.getInstance();
        SharedPreferences sharedPreferences = getSharedPreferences("GuardianChildPrefs", Context.MODE_PRIVATE);
        deviceId = sharedPreferences.getString("deviceId", null);
        Log.i(TAG, "onServiceConnected: Accessibility service has been connected.");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.w(TAG, "onDestroy: Accessibility service has been destroyed.");
    }
}
