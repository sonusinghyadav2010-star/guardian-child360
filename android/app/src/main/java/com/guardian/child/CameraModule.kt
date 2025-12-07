package com.guardian.child

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.ImageFormat
import android.hardware.camera2.*
import android.media.ImageReader
import android.os.Handler
import android.os.HandlerThread
import android.util.Base64
import android.util.Log
import androidx.core.app.ActivityCompat
import com.facebook.react.bridge.*
import java.util.* 

class CameraModule(reactContext: ReactApplicationContext) : ReactContextBaseJavaModule(reactContext) {

    override fun getName() = "CameraModule"
    private val TAG = "CameraModule"
    private var cameraDevice: CameraDevice? = null
    private var imageReader: ImageReader? = null
    private var cameraCaptureSession: CameraCaptureSession? = null
    private val cameraManager by lazy { reactContext.getSystemService(Context.CAMERA_SERVICE) as CameraManager }
    private val cameraThread = HandlerThread("CameraThread").apply { start() }
    private val cameraHandler = Handler(cameraThread.looper)

    @ReactMethod
    fun captureImage(promise: Promise) {
        if (ActivityCompat.checkSelfPermission(reactApplicationContext, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            promise.reject("E_PERM", "Camera permission not granted")
            return
        }

        try {
            val cameraId = cameraManager.cameraIdList[0] // Use the first camera

            imageReader = ImageReader.newInstance(640, 480, ImageFormat.JPEG, 1).apply {
                setOnImageAvailableListener({
                    val image = it.acquireLatestImage()
                    val buffer = image.planes[0].buffer
                    val bytes = ByteArray(buffer.remaining())
                    buffer.get(bytes)
                    val base64Image = Base64.encodeToString(bytes, Base64.NO_WRAP)
                    promise.resolve(base64Image)
                    image.close()
                    closeCamera()
                }, cameraHandler)
            }

            cameraManager.openCamera(cameraId, object : CameraDevice.StateCallback() {
                override fun onOpened(camera: CameraDevice) {
                    cameraDevice = camera
                    createCaptureSession()
                }

                override fun onDisconnected(camera: CameraDevice) {
                    camera.close()
                    cameraDevice = null
                }

                override fun onError(camera: CameraDevice, error: Int) {
                    camera.close()
                    cameraDevice = null
                    promise.reject("E_CAM_ERR", "Camera error: $error")
                }
            }, cameraHandler)
        } catch (e: Exception) {
            promise.reject("E_CAM_EX", e.message)
        }
    }

    private fun createCaptureSession() {
        val surface = imageReader!!.surface
        val captureRequestBuilder = cameraDevice!!.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE).apply {
            addTarget(surface)
        }

        cameraDevice!!.createCaptureSession(Collections.singletonList(surface), object : CameraCaptureSession.StateCallback() {
            override fun onConfigured(session: CameraCaptureSession) {
                cameraCaptureSession = session
                session.capture(captureRequestBuilder.build(), null, null)
            }

            override fun onConfigureFailed(session: CameraCaptureSession) {
                // Handle failure
            }
        }, cameraHandler)
    }

    private fun closeCamera() {
        cameraCaptureSession?.close()
        cameraCaptureSession = null
        cameraDevice?.close()
        cameraDevice = null
        imageReader?.close()
        imageReader = null
    }
}
