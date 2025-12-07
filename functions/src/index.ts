
import * as functions from 'firebase-functions';
import * as admin from 'firebase-admin';
import { generateKeyPair, exportKey, signPayload, importKey, verifySignature } from './cryptoService';

admin.initializeApp();

const db = admin.firestore();

// Securely store and retrieve keys from Firestore (for demonstration)
// In production, use a secure secret manager.
const KEYS_COLLECTION = 'signingKeys';

// Generates and stores a new key pair, should be called once for setup
export const generateAndStoreKeyPair = functions.https.onCall(async (data, context) => {
  // Add authentication to ensure only authorized users can call this
  // if (!context.auth || !isAdmin(context.auth.uid)) { 
  //   throw new functions.https.HttpsError('permission-denied', 'Must be an admin to call this function.');
  // }

  const { publicKey, privateKey } = await generateKeyPair();

  const publicKeyJwk = await exportKey(publicKey);
  const privateKeyJwk = await exportKey(privateKey);

  const keyPairRef = db.collection(KEYS_COLLECTION).doc('defaultPair');

  await keyPairRef.set({
    publicKey: publicKeyJwk,
    privateKey: privateKeyJwk, // Storing private key in Firestore is NOT recommended for production
    createdAt: admin.firestore.FieldValue.serverTimestamp(),
  });

  return { status: 'success', message: 'Key pair generated and stored.' };
});

// Function to retrieve the public key
export const getPublicKey = functions.https.onCall(async (data, context) => {
  const keyPairRef = await db.collection(KEYS_COLLECTION).doc('defaultPair').get();
  if (!keyPairRef.exists) {
    throw new functions.https.HttpsError('not-found', 'Signing keys have not been generated yet.');
  }
  return keyPairRef.data()?.publicKey;
});


export const generatePairingRequest = functions.https.onCall(async (data, context) => {
  if (!context.auth) {
    throw new functions.https.HttpsError('unauthenticated', 'The request must be authenticated.');
  }

  const parentUid = context.auth.uid;
  const { planId, deviceLimit } = data;

  if (!planId || typeof deviceLimit === 'undefined') {
    throw new functions.https.HttpsError('invalid-argument', 'Missing planId or deviceLimit.');
  }

  const nonce = db.collection('pairingRequests').doc().id;
  const timestamp = Date.now();

  const pairingData = { parentUid, timestamp, nonce, planId, deviceLimit };

  // Retrieve private key and sign the data
  const keyPairDoc = await db.collection(KEYS_COLLECTION).doc('defaultPair').get();
  if (!keyPairDoc.exists) {
    throw new functions.https.HttpsError('failed-precondition', 'Signing keys not found.');
  }
  const privateKeyJwk = keyPairDoc.data()?.privateKey;
  const privateKey = await importKey(privateKeyJwk, 'ES256');
  const signature = await signPayload(privateKey, pairingData);

  const qrData = { ...pairingData, signature };

  await db.collection('pairingRequests').doc(nonce).set({
    ...pairingData,
    status: 'pending',
    createdAt: admin.firestore.FieldValue.serverTimestamp(),
  });

  return qrData;
});


export const linkChildDevice = functions.https.onCall(async (data, context) => {
  if (!context.auth) {
    throw new functions.https.HttpsError('unauthenticated', 'The request must be authenticated.');
  }

  const childUid = context.auth.uid;
  const { qrData, childDeviceName } = data;
  const { parentUid, timestamp, nonce, planId, deviceLimit, signature } = qrData;

  if (!parentUid || !timestamp || !nonce || !planId || typeof deviceLimit === 'undefined' || !signature) {
    throw new functions.https.HttpsError('invalid-argument', 'Missing required QR data fields.');
  }

  // 1. Validate Nonce and Pairing Request
  const pairingRef = db.collection('pairingRequests').doc(nonce);
  const pairingDoc = await pairingRef.get();
  if (!pairingDoc.exists || pairingDoc.data()?.status !== 'pending') {
    throw new functions.https.HttpsError('not-found', 'Pairing request not found or has expired.');
  }

  // 2. Verify Signature
  const keyPairDoc = await db.collection(KEYS_COLLECTION).doc('defaultPair').get();
  if (!keyPairDoc.exists) {
    throw new functions.https.HttpsError('failed-precondition', 'Signing keys not found.');
  }
  const publicKeyJwk = keyPairDoc.data()?.publicKey;
  const publicKey = await importKey(publicKeyJwk, 'ES256');

  const payloadToVerify = { parentUid, timestamp, nonce, planId, deviceLimit };

  const verifiedPayload = await verifySignature(publicKey, signature, payloadToVerify);

  if (!verifiedPayload) {
    throw new functions.https.HttpsError('unauthenticated', 'Invalid signature.');
  }
  
  // 3. Enforce Device Limit
  const childrenCollectionRef = db.collection('users').doc(parentUid).collection('children');
  const existingChildrenSnapshot = await childrenCollectionRef.get();
  if (existingChildrenSnapshot.size >= deviceLimit) {
    throw new functions.https.HttpsError('resource-exhausted', 'Device limit reached.');
  }

  // 4. Link Device
  const batch = db.batch();
  const parentChildRef = childrenCollectionRef.doc(childUid);
  batch.set(parentChildRef, {
    childUid, deviceName: childDeviceName || 'Unknown Device', pairedAt: admin.firestore.FieldValue.serverTimestamp(), planId
  });

  const childMetaRef = db.collection('children').doc(childUid);
  batch.set(childMetaRef, { parentUid, pairedAt: admin.firestore.FieldValue.serverTimestamp(), planId });

  batch.update(pairingRef, { status: 'linked', linkedChildUid: childUid, linkedAt: admin.firestore.FieldValue.serverTimestamp() });

  await batch.commit();

  return { status: 'success', message: 'Device successfully linked.' };
});



