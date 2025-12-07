
package com.guardian.child;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.facebook.react.HeadlessJsTaskService;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class GuardianService extends Service {

    private static final int NOTIFICATION_ID = 1;
    private static final String CHANNEL_ID = "GuardianServiceChannel";
    private static final long HEARTBEAT_INTERVAL = 5 * 60 * 1000; // 5 minutes
    private static final long UPDATE_INTERVAL = 15 * 60 * 1000; // 15 minutes
    private static final String TAG = "GuardianService";

    private final Handler handler = new Handler(Looper.getMainLooper());
    private FirebaseFirestore db;
    private String deviceId;
    private ListenerRegistration commandListener;
    private final List<Map<String, Object>> pendingUploads = new ArrayList<>();
    private boolean isNetworkAvailable = false;

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();


    @Override
    public void onCreate() {
        super.onCreate();
        db = FirebaseFirestore.getInstance();
        SharedPreferences sharedPreferences = getSharedPreferences("GuardianChildPrefs", Context.MODE_PRIVATE);
        deviceId = sharedPreferences.getString("deviceId", null);

        createNotificationChannel();
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("GuardianChild360")
                .setContentText("Protecting your child")
                .setSmallIcon(R.mipmap.ic_launcher)
                .build();
        startForeground(NOTIFICATION_ID, notification);

        registerNetworkCallback();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "GuardianService started");

        if (deviceId == null || deviceId.isEmpty()) {
            Log.e(TAG, "Device ID not found, stopping service.");
            stopSelf();
            return START_NOT_STICKY;
        }

        // Start other services
        startService(new Intent(this, MyAccessibilityService.class));

        // Schedule periodic tasks
        scheduler.scheduleAtFixedRate(this::collectAndUploadData, 0, UPDATE_INTERVAL, TimeUnit.MILLISECONDS);
        scheduler.scheduleAtFixedRate(this::uploadHeartbeat, 0, HEARTBEAT_INTERVAL, TimeUnit.MILLISECONDS);

        // Listen for remote commands
        setupCommandListener();

        return START_STICKY;
    }

    private void setupCommandListener() {
        if (commandListener != null) {
            commandListener.remove();
        }
        commandListener = db.collection("childDevices").document(deviceId).collection("commands")
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) {
                        Log.w(TAG, "Listen failed.", e);
                        return;
                    }

                    for (DocumentChange dc : snapshots.getDocumentChanges()) {
                        if (dc.getType() == DocumentChange.Type.ADDED) {
                            Log.d(TAG, "New command: " + dc.getDocument().getData());
                            // Trigger Headless JS task to handle the command
                            Intent serviceIntent = new Intent(getApplicationContext(), RemoteCommandHeadlessTaskService.class);
                            serviceIntent.putExtra("commandId", dc.getDocument().getId());
                            serviceIntent.putExtra("command", dc.getDocument().getString("command"));
                            serviceIntent.putExtra("payload", (HashMap) dc.getDocument().get("payload"));
                            getApplicationContext().startService(serviceIntent);
                             // Delete the command doc to prevent re-execution
                            dc.getDocument().getReference().delete();
                        }
                    }
                });
    }

    private void collectAndUploadData() {
        Map<String, Object> statusData = new HashMap<>();
        statusData.put("type", "deviceStatus");
        statusData.put("timestamp", System.currentTimeMillis());

        BatteryManager bm = (BatteryManager) getSystemService(BATTERY_SERVICE);
        int batteryPct = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
        statusData.put("battery", batteryPct);

        statusData.put("networkType", getNetworkType());

        UsageStatsManager usm = (UsageStatsManager) getSystemService(Context.USAGE_STATS_SERVICE);
        long endTime = System.currentTimeMillis();
        long beginTime = endTime - UPDATE_INTERVAL;
        List<UsageStats> usageStatsList = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, beginTime, endTime);
        Map<String, Object> usageData = new HashMap<>();
        for (UsageStats stats : usageStatsList) {
            if (stats.getTotalTimeInForeground() > 0) {
                Map<String, Object> appData = new HashMap<>();
                appData.put("totalTimeInForeground", stats.getTotalTimeInForeground());
                appData.put("lastTimeUsed", stats.getLastTimeUsed());
                usageData.put(stats.getPackageName().replace(".", "_"), appData);
            }
        }
        statusData.put("usageStats", usageData);

        uploadDataWithRetry("monitoring", statusData);
    }

    private void uploadHeartbeat() {
        Map<String, Object> heartbeat = new HashMap<>();
        heartbeat.put("lastSeen", System.currentTimeMillis());
        uploadDataWithRetry("status", heartbeat, true); // Use merge to avoid overwriting other status fields
    }

    private void uploadDataWithRetry(String collection, Map<String, Object> data) {
         uploadDataWithRetry(collection, data, false);
    }

    private void uploadDataWithRetry(String subCollection, Map<String, Object> data, boolean merge) {
        if (!isNetworkAvailable) {
            Log.d(TAG, "Network unavailable, queuing data for " + subCollection);
            Map<String, Object> pending = new HashMap<>();
            pending.put("collection", subCollection);
            pending.put("data", data);
            pending.put("merge", merge);
            pendingUploads.add(pending);
            return;
        }

        if (subCollection.equals("monitoring")) {
             db.collection("childDevices").document(deviceId).collection("monitoring").add(data)
                .addOnSuccessListener(documentReference -> Log.d(TAG, "Data uploaded to " + subCollection))
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error uploading to " + subCollection + ", queuing.", e);
                    Map<String, Object> pending = new HashMap<>();
                    pending.put("collection", subCollection);
                    pending.put("data", data);
                     pending.put("merge", merge);
                    pendingUploads.add(pending);
                });
        } else {
             db.collection("childDevices").document(deviceId).collection(subCollection).document("latest").set(data, merge ? com.google.firebase.firestore.SetOptions.merge() : com.google.firebase.firestore.SetOptions.of())
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Data uploaded to " + subCollection))
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error uploading to " + subCollection + ", queuing.", e);
                    Map<String, Object> pending = new HashMap<>();
                    pending.put("collection", subCollection);
                    pending.put("data", data);
                     pending.put("merge", merge);
                    pendingUploads.add(pending);
                });
        }
       
    }

    private void flushPendingUploads() {
        Log.d(TAG, "Network available, flushing " + pendingUploads.size() + " pending uploads.");
        List<Map<String, Object>> uploadsToProcess = new ArrayList<>(pendingUploads);
        pendingUploads.clear();
        for (Map<String, Object> pending : uploadsToProcess) {
            uploadDataWithRetry((String) pending.get("collection"), (Map<String, Object>) pending.get("data"), (Boolean) pending.get("merge"));
        }
    }

    private void registerNetworkCallback() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            cm.registerDefaultNetworkCallback(new ConnectivityManager.NetworkCallback() {
                @Override
                public void onAvailable(@NonNull Network network) {
                    isNetworkAvailable = true;
                    handler.post(() -> flushPendingUploads());
                }

                @Override
                public void onLost(@NonNull Network network) {
                    isNetworkAvailable = false;
                }
            });
        } else {
             // For older APIs, use a BroadcastReceiver
            IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
            registerReceiver(networkReceiver, filter);
        }
         // Initial check
        isNetworkAvailable = getNetworkType() != "NONE";
    }

     private final BroadcastReceiver networkReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            boolean previousState = isNetworkAvailable;
            isNetworkAvailable = getNetworkType() != "NONE";
            if (isNetworkAvailable && !previousState) {
                flushPendingUploads();
            }
        }
    }; 

    private String getNetworkType() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        if (activeNetwork != null && activeNetwork.isConnectedOrConnecting()) {
            return activeNetwork.getTypeName();
        }
        return "NONE";
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "GuardianService destroyed, restarting...");
        if (commandListener != null) {
            commandListener.remove();
        }
        scheduler.shutdownNow();
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            unregisterReceiver(networkReceiver);
        }
        Intent broadcastIntent = new Intent();
        broadcastIntent.setAction("restartservice");
        broadcastIntent.setClass(this, BootReceiver.class);
        this.sendBroadcast(broadcastIntent);
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Guardian Service Channel",
                    NotificationManager.IMPORTANCE_LOW
            );
            getSystemService(NotificationManager.class).createNotificationChannel(serviceChannel);
        }
    }
}
