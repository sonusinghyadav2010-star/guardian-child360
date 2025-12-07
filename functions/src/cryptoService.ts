
import * as jose from 'jose';

// Generate an Elliptic Curve key pair for P-256
export async function generateKeyPair() {
  const { publicKey, privateKey } = await jose.generateKeyPair('ES256');
  return { publicKey, privateKey };
}

// Export a key in JWK format
export async function exportKey(key: jose.KeyLike) {
  return await jose.exportJWK(key);
}

// Import a key from JWK format
export async function importKey(jwk: jose.JWK, alg: string) {
  return await jose.importJWK(jwk, alg);
}

// Sign a payload with a private key
export async function signPayload(privateKey: jose.KeyLike, payload: any) {
  const jws = await new jose.CompactSign(
    new TextEncoder().encode(JSON.stringify(payload))
  )
    .setProtectedHeader({ alg: 'ES256' })
    .sign(privateKey);

  return jws;
}

// Verify a signature with a public key
export async function verifySignature(publicKey: jose.KeyLike, jws: string, payload: any) {
  try {
    const { payload: verifiedPayload } = await jose.compactVerify(jws, publicKey);
    const decodedOriginalPayload = new TextEncoder().encode(JSON.stringify(payload));

    // Extra check: Compare the payload to ensure it hasn't been tampered with
    // This is often handled by the application logic, but can be an extra layer of security.
    // jose.compactVerify already validates the signature against the payload encoded in the JWS.
    const decodedVerifiedPayload = new TextDecoder().decode(verifiedPayload);
    const parsedVerifiedPayload = JSON.parse(decodedVerifiedPayload);

    // You might want to compare specific fields
    // For now, we return the parsed payload from the JWS
    return parsedVerifiedPayload;

  } catch (error) {
    console.error('Signature verification failed:', error);
    return null;
  }
}
