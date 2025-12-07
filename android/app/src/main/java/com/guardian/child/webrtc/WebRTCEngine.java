package com.guardian.child.webrtc;

import android.content.Intent;
import android.media.projection.MediaProjection;
import android.util.Log;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.modules.core.DeviceEventManagerModule;

import org.webrtc.AudioSource;
import org.webrtc.AudioTrack;
import org.webrtc.Camera1Enumerator;
import org.webrtc.Camera2Enumerator;
import org.webrtc.CameraEnumerator;
import org.webrtc.DataChannel;
import org.webrtc.DefaultVideoDecoderFactory;
import org.webrtc.DefaultVideoEncoderFactory;
import org.webrtc.EglBase;
import org.webrtc.IceCandidate;
import org.webrtc.MediaConstraints;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnection;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.RtpReceiver;
import org.webrtc.SdpObserver;
import org.webrtc.SessionDescription;
import org.webrtc.ScreenCapturerAndroid;
import org.webrtc.SurfaceTextureHelper;
import org.webrtc.VideoCapturer;
import org.webrtc.VideoSource;
import org.webrtc.VideoTrack;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class WebRTCEngine {
    private static final String TAG = "WebRTCEngine";
    private final ReactApplicationContext reactContext;
    private final EglBase eglBase;
    private final PeerConnectionFactory peerConnectionFactory;
    private PeerConnection peerConnection;
    private VideoTrack localVideoTrack;
    private AudioTrack localAudioTrack;
    private VideoSource videoSource;
    private AudioSource audioSource;
    private VideoCapturer videoCapturer;
    private SurfaceTextureHelper surfaceTextureHelper;

    public WebRTCEngine(ReactApplicationContext reactContext) {
        this.reactContext = reactContext;
        this.eglBase = EglBase.create();

        PeerConnectionFactory.initialize(PeerConnectionFactory.InitializationOptions.builder(reactContext).createInitializationOptions());

        org.webrtc.VideoEncoderFactory encoderFactory = new DefaultVideoEncoderFactory(eglBase.getEglBaseContext(), true, true);
        org.webrtc.VideoDecoderFactory decoderFactory = new DefaultVideoDecoderFactory(eglBase.getEglBaseContext());

        peerConnectionFactory = PeerConnectionFactory.builder()
                .setVideoEncoderFactory(encoderFactory)
                .setVideoDecoderFactory(decoderFactory)
                .createPeerConnectionFactory();

        Log.d(TAG, "WebRTC Engine Initialized");
    }

    public void initWebRTC() {
        List<PeerConnection.IceServer> iceServers = new ArrayList<>();
        iceServers.add(PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer());

        PeerConnection.Observer observer = new PeerConnection.Observer() {
            @Override
            public void onSignalingChange(PeerConnection.SignalingState signalingState) {}

            @Override
            public void onIceConnectionChange(PeerConnection.IceConnectionState iceConnectionState) {}

            @Override
            public void onIceConnectionReceivingChange(boolean b) {}

            @Override
            public void onIceGatheringChange(PeerConnection.IceGatheringState iceGatheringState) {}

            @Override
            public void onIceCandidate(IceCandidate iceCandidate) {
                WritableMap params = Arguments.createMap();
                params.putString("candidate", iceCandidate.sdp);
                params.putInt("sdpMLineIndex", iceCandidate.sdpMLineIndex);
                params.putString("sdpMid", iceCandidate.sdpMid);
                reactContext.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                        .emit("onIceCandidate", params);
            }

            @Override
            public void onIceCandidatesRemoved(IceCandidate[] iceCandidates) {}

            @Override
            public void onAddStream(MediaStream mediaStream) {}

            @Override
            public void onRemoveStream(MediaStream mediaStream) {}

            @Override
            public void onDataChannel(DataChannel dataChannel) {}

            @Override
            public void onRenegotiationNeeded() {}

            @Override
            public void onAddTrack(RtpReceiver rtpReceiver, MediaStream[] mediaStreams) {}
        };

        peerConnection = peerConnectionFactory.createPeerConnection(iceServers, observer);

        // Create audio source and track
        audioSource = peerConnectionFactory.createAudioSource(new MediaConstraints());
        localAudioTrack = peerConnectionFactory.createAudioTrack(UUID.randomUUID().toString(), audioSource);
        peerConnection.addTrack(localAudioTrack);
    }

    public void createOffer(Promise promise) {
        MediaConstraints constraints = new MediaConstraints();
        constraints.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"));

        peerConnection.createOffer(new SdpObserver() {
            @Override
            public void onCreateSuccess(SessionDescription sessionDescription) {
                peerConnection.setLocalDescription(this, sessionDescription);
                WritableMap sdp = Arguments.createMap();
                sdp.putString("type", sessionDescription.type.canonicalForm());
                sdp.putString("sdp", sessionDescription.description);
                promise.resolve(sdp);
            }

            @Override
            public void onSetSuccess() {}

            @Override
            public void onCreateFailure(String s) {
                promise.reject("CREATE_OFFER_ERROR", s);
            }

            @Override
            public void onSetFailure(String s) {}
        }, constraints);
    }

    public void createAnswer(ReadableMap remoteSdp, Promise promise) {
        SessionDescription sdp = new SessionDescription(
                SessionDescription.Type.fromCanonicalForm(remoteSdp.getString("type")),
                remoteSdp.getString("sdp")
        );
        peerConnection.setRemoteDescription(new SdpObserver() {
            @Override
            public void onCreateSuccess(SessionDescription sessionDescription) {}

            @Override
            public void onSetSuccess() {
                peerConnection.createAnswer(new SdpObserver() {
                    @Override
                    public void onCreateSuccess(SessionDescription sessionDescription) {
                        peerConnection.setLocalDescription(this, sessionDescription);
                        WritableMap sdp = Arguments.createMap();
                        sdp.putString("type", sessionDescription.type.canonicalForm());
                        sdp.putString("sdp", sessionDescription.description);
                        promise.resolve(sdp);
                    }

                    @Override
                    public void onSetSuccess() {}

                    @Override
                    public void onCreateFailure(String s) {}

                    @Override
                    public void onSetFailure(String s) {}
                }, new MediaConstraints());
            }

            @Override
            public void onCreateFailure(String s) {}

            @Override
            public void onSetFailure(String s) {
                promise.reject("SET_REMOTE_DESCRIPTION_ERROR", s);
            }
        }, sdp);
    }

    public void addIceCandidate(ReadableMap iceCandidate) {
        peerConnection.addIceCandidate(new IceCandidate(
                iceCandidate.getString("sdpMid"),
                iceCandidate.getInt("sdpMLineIndex"),
                iceCandidate.getString("candidate")
        ));
    }

    public void startCameraStream() {
        videoCapturer = createCameraCapturer();
        if (videoCapturer != null) {
            surfaceTextureHelper = SurfaceTextureHelper.create("CaptureThread", eglBase.getEglBaseContext());
            videoSource = peerConnectionFactory.createVideoSource(videoCapturer.isScreencast());
            videoCapturer.initialize(surfaceTextureHelper, reactContext, videoSource.getCapturerObserver());
            videoCapturer.startCapture(1280, 720, 30);

            localVideoTrack = peerConnectionFactory.createVideoTrack(UUID.randomUUID().toString(), videoSource);
            peerConnection.addTrack(localVideoTrack);
        }
    }

    public void stopCameraStream() {
        if (videoCapturer != null) {
            try {
                videoCapturer.stopCapture();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            videoCapturer.dispose();
            videoCapturer = null;
        }
        if (surfaceTextureHelper != null) {
            surfaceTextureHelper.dispose();
            surfaceTextureHelper = null;
        }
        if (localVideoTrack != null) {
            peerConnection.removeTrack(localVideoTrack);
        }
    }

    public void startScreenStream(Intent data) {
        videoCapturer = new ScreenCapturerAndroid(data, new MediaProjection.Callback() {
            @Override
            public void onStop() {
                super.onStop();
            }
        });

        surfaceTextureHelper = SurfaceTextureHelper.create("CaptureThread", eglBase.getEglBaseContext());
        videoSource = peerConnectionFactory.createVideoSource(videoCapturer.isScreencast());
        videoCapturer.initialize(surfaceTextureHelper, reactContext, videoSource.getCapturerObserver());
        videoCapturer.startCapture(1280, 720, 30);

        localVideoTrack = peerConnectionFactory.createVideoTrack(UUID.randomUUID().toString(), videoSource);
        peerConnection.addTrack(localVideoTrack);
    }

    public void stopScreenStream() {
         if (videoCapturer != null) {
            try {
                videoCapturer.stopCapture();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            videoCapturer.dispose();
            videoCapturer = null;
        }
        if (surfaceTextureHelper != null) {
            surfaceTextureHelper.dispose();
            surfaceTextureHelper = null;
        }
        if (localVideoTrack != null) {
            peerConnection.removeTrack(localVideoTrack);
        }
    }

    private VideoCapturer createCameraCapturer() {
        CameraEnumerator enumerator;
        if (Camera2Enumerator.isSupported(reactContext)) {
            enumerator = new Camera2Enumerator(reactContext);
        } else {
            enumerator = new Camera1Enumerator(true);
        }

        for (String deviceName : enumerator.getDeviceNames()) {
            if (enumerator.isFrontFacing(deviceName)) {
                return enumerator.createCapturer(deviceName, null);
            }
        }
        for (String deviceName : enumerator.getDeviceNames()) {
            if (!enumerator.isFrontFacing(deviceName)) {
                return enumerator.createCapturer(deviceName, null);
            }
        }
        return null;
    }
}
