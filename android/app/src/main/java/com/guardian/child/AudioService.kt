package com.guardian.child

import android.app.Service
import android.content.Intent
import android.media.MediaRecorder
import android.os.Build
import android.os.Environment
import android.os.IBinder
import android.util.Log
import java.io.File
import java.io.IOException

class AudioService : Service() {

    private var mediaRecorder: MediaRecorder? = null
    private var isRecording = false
    private var outputFilePath: String? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!isRecording) {
            startRecording()
        }
        return START_STICKY
    }

    private fun startRecording() {
        outputFilePath = "${externalCacheDir?.absolutePath}/audiorecord.3gp"

        mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(this)
        } else {
            MediaRecorder()
        }

        mediaRecorder?.apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
            setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
            setOutputFile(outputFilePath)
            try {
                prepare()
                start()
                isRecording = true
                Log.d("AudioService", "Recording started")
            } catch (e: IOException) {
                Log.e("AudioService", "prepare() failed")
            }
        }
    }

    private fun stopRecording() {
        mediaRecorder?.apply {
            stop()
            release()
        }
        mediaRecorder = null
        isRecording = false
        Log.d("AudioService", "Recording stopped. File saved to: $outputFilePath")
    }

    override fun onDestroy() {
        if (isRecording) {
            stopRecording()
        }
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}
