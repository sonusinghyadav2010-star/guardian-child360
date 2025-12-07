
package com.guardian.child;

import android.app.Notification;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;

public class MyNotificationListenerService extends NotificationListenerService {

    private static final String TAG = "MyNotificationListener";
    private FirebaseFirestore db;
    private String deviceId;

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        if (sbn == null) return;
        Notification notification = sbn.getNotification();
        if (notification == null) return;

        Bundle extras = notification.extras;
        String title = extras.getString(Notification.EXTRA_TITLE);
        CharSequence text = extras.getCharSequence(Notification.EXTRA_TEXT);

        if (title == null || title.isEmpty() || text == null || text.length() == 0) {
            return;
        }

        String packageName = sbn.getPackageName();
        uploadNotification(title, text.toString(), packageName);
    }

    private void uploadNotification(String title, String text, String packageName) {
        if (deviceId == null || deviceId.isEmpty()) {
            return;
        }
        Map<String, Object> data = new HashMap<>();
        data.put("title", title);
        data.put("text", text);
        data.put("packageName", packageName);
        data.put("timestamp", System.currentTimeMillis());

        db.collection("childDevices").document(deviceId).collection("monitoring")
                .add(data)
                .addOnSuccessListener(documentReference -> Log.d(TAG, "Notification saved"))
                .addOnFailureListener(e -> Log.e(TAG, "Error saving notification", e));
    }

    @Override
    public void onListenerConnected() {
        super.onListenerConnected();
        db = FirebaseFirestore.getInstance();
        SharedPreferences sharedPreferences = getSharedPreferences("GuardianChildPrefs", Context.MODE_PRIVATE);
        deviceId = sharedPreferences.getString("deviceId", null);
        Log.i(TAG, "onListenerConnected: Notification listener has been connected.");
    }

    @Override
    public void onListenerDisconnected() {
        super.onListenerDisconnected();
        Log.w(TAG, "onListenerDisconnected: Notification listener has been disconnected.");
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        return START_STICKY;
    }
}
