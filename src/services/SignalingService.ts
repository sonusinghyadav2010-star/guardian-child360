import firestore from '@react-native-firebase/firestore';
import { v4 as uuidv4 } from 'uuid';

class SignalingService {
  private deviceId: string;

  constructor(deviceId: string) {
    this.deviceId = deviceId;
  }

  private get signalingCollection() {
    return firestore().collection('childDevices').doc(this.deviceId).collection('signaling');
  }

  public async sendOffer(sdp: any) {
    await this.signalingCollection.doc('offer').set({ sdp });
  }

  public async sendAnswer(sdp: any) {
    await this.signalingCollection.doc('answer').set({ sdp });
  }

  public async sendIceCandidate(candidate: any) {
    await this.signalingCollection.collection('iceCandidates').add(candidate);
  }

  public listenToRemoteSDP(callback: (sdp: any) => void) {
    const unsubscribeOffer = this.signalingCollection.doc('offer').onSnapshot((snapshot) => {
      if (snapshot.exists) {
        callback(snapshot.data()?.sdp);
      }
    });
    const unsubscribeAnswer = this.signalingCollection.doc('answer').onSnapshot((snapshot) => {
        if (snapshot.exists) {
          callback(snapshot.data()?.sdp);
        }
      });

    return () => {
        unsubscribeOffer();
        unsubscribeAnswer();
    }
  }

  public listenToRemoteICE(callback: (candidate: any) => void) {
    const unsubscribe = this.signalingCollection.collection('iceCandidates').onSnapshot((snapshot) => {
      snapshot.docChanges().forEach((change) => {
        if (change.type === 'added') {
          callback(change.doc.data());
        }
      });
    });
    return unsubscribe;
  }

  public async cleanup() {
    const iceCandidates = await this.signalingCollection.collection('iceCandidates').get();
    iceCandidates.forEach(async (doc) => {
      await doc.ref.delete();
    });
    await this.signalingCollection.doc('offer').delete();
    await this.signalingCollection.doc('answer').delete();
  }
}

export default SignalingService;
