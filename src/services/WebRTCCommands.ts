import WebRTCBridge from '../modules/WebRTCBridge';
import SignalingService from './SignalingService';
import { getUniqueId } from 'react-native-device-info';

let signalingService: SignalingService | null = null;
let remoteSdpListener: (() => void) | null = null;
let remoteIceListener: (() => void) | null = null;

const getSignalingService = async () => {
  if (!signalingService) {
    const deviceId = await getUniqueId();
    signalingService = new SignalingService(deviceId);
  }
  return signalingService;
};

export const startLiveCamera = async () => {
  const service = await getSignalingService();
  await service.cleanup();

  await WebRTCBridge.initWebRTC();
  remoteSdpListener = service.listenToRemoteSDP(async (sdp) => {
    if (sdp.type === 'answer') {
      await WebRTCBridge.createAnswer(sdp);
    }
  });

  remoteIceListener = service.listenToRemoteICE(async (candidate) => {
    await WebRTCBridge.addIceCandidate(candidate);
  });

  WebRTCBridge.onIceCandidate(async (candidate) => {
    await service.sendIceCandidate(candidate);
  });

  await WebRTCBridge.startCameraStream();
  const offer = await WebRTCBridge.createOffer();
  await service.sendOffer(offer);
  return 'Camera stream started';
};

export const stopLiveCamera = async () => {
  await WebRTCBridge.stopCameraStream();
  if (remoteSdpListener) remoteSdpListener();
  if (remoteIceListener) remoteIceListener();
  WebRTCBridge.removeOnIceCandidate();
  const service = await getSignalingService();
  await service.cleanup();
  signalingService = null;
  return 'Camera stream stopped';
};

export const startScreenShare = async () => {
    const service = await getSignalingService();
    await service.cleanup();
  
    await WebRTCBridge.initWebRTC();
    remoteSdpListener = service.listenToRemoteSDP(async (sdp) => {
      if (sdp.type === 'answer') {
        await WebRTCBridge.createAnswer(sdp);
      }
    });
  
    remoteIceListener = service.listenToRemoteICE(async (candidate) => {
      await WebRTCBridge.addIceCandidate(candidate);
    });
  
    WebRTCBridge.onIceCandidate(async (candidate) => {
      await service.sendIceCandidate(candidate);
    });
  
    await WebRTCBridge.startScreenStream();
    const offer = await WebRTCBridge.createOffer();
    await service.sendOffer(offer);
    return 'Screen share started';
};

export const stopScreenShare = async () => {
    await WebRTCBridge.stopScreenStream();
    if (remoteSdpListener) remoteSdpListener();
    if (remoteIceListener) remoteIceListener();
    WebRTCBridge.removeOnIceCandidate();
    const service = await getSignalingService();
    await service.cleanup();
    signalingService = null;
    return 'Screen share stopped';
};

export const webrtcReset = async () => {
    if (remoteSdpListener) remoteSdpListener();
    if (remoteIceListener) remoteIceListener();
    WebRTCBridge.removeOnIceCandidate();
    const service = await getSignalingService();
    await service.cleanup();
    signalingService = null;
    return 'WebRTC reset';
};
