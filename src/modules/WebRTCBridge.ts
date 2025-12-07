import { NativeModules, NativeEventEmitter } from 'react-native';

const { WebRTCModule } = NativeModules;

const eventEmitter = new NativeEventEmitter(WebRTCModule);

export default {
  initWebRTC: () => WebRTCModule.initWebRTC(),
  createOffer: () => WebRTCModule.createOffer(),
  createAnswer: (sdp) => WebRTCModule.createAnswer(sdp),
  addIceCandidate: (candidate) => WebRTCModule.addIceCandidate(candidate),
  startCameraStream: () => WebRTCModule.startCameraStream(),
  stopCameraStream: () => WebRTCModule.stopCameraStream(),
  startScreenStream: () => WebRTCModule.startScreenStream(),
  stopScreenStream: () => WebRTCModule.stopScreenStream(),
  onIceCandidate: (callback) => eventEmitter.addListener('onIceCandidate', callback),
  removeOnIceCandidate: () => eventEmitter.removeAllListeners('onIceCandidate'),
};
