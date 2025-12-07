
import * as jose from 'jose';

// Generate an Elliptic Curve key pair for P-256
export async function generateKeyPair() {
  const { publicKey, privateKey } = await jose.generateKeyPair('ES256');
  return { publicKey, privateKey };
}

// Export a public key in JWK format
export async function exportPublicKey(publicKey: jose.KeyLike) {
  return await jose.exportJWK(publicKey);
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
export async function verifySignature(publicKey: jose.KeyLike, jws: string) {
  try {
    const { payload } = await jose.compactVerify(jws, publicKey);
    return JSON.parse(new TextDecoder().decode(payload));
  } catch (error) {
    console.error('Signature verification failed:', error);
    return null;
  }
}
