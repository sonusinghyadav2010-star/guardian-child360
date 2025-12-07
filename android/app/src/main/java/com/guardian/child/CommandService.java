
package com.guardian.child;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.camera2.CameraManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.IBinder;
import android.os.Vibrator;
import android.provider.MediaStore;
import android.util.Log;

public class CommandService extends Service {

    private static final String TAG = "CommandService";

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String command = intent.getStringExtra("command");
        Log.d(TAG, "Received command: " + command);
        if (command != null) {
            executeCommand(command);
        }
        return START_NOT_STICKY;
    }

    private void executeCommand(String command) {
        switch (command) {
            case "takePhoto":
                takePhoto();
                break;
            case "playAlarm":
                playAlarm();
                break;
            case "vibrateDevice":
                vibrateDevice();
                break;
            default:
                Log.w(TAG, "Unsupported command: " + command);
        }
    }

    private void takePhoto() {
        // This is a simplified version. A real implementation would be more complex.
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            // The following line will crash the app because we don't have an activity
            // context. This needs to be handled in a more sophisticated way, probably
            // by creating a transparent activity.
            // getApplicationContext().startActivity(takePictureIntent);
            Log.d(TAG, "Pretending to take a photo.");
        }
    }

    private void playAlarm() {
        try {
            Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
            Ringtone r = RingtoneManager.getRingtone(getApplicationContext(), notification);
            r.play();
        } catch (Exception e) {
            Log.e(TAG, "Error playing alarm", e);
        }
    }

    private void vibrateDevice() {
        Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        if (v != null && v.hasVibrator()) {
            v.vibrate(500);
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
