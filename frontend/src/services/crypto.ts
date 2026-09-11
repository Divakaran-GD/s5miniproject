/**
 * SecureX Client-Side Cryptographic Engine using Web Crypto API
 * Standard: AES-256-GCM (256-bit key = 32 bytes) with 96-bit IV (12 bytes) and SHA-256 integrity verification
 */

export interface EncryptionResult {
  encryptedBlob: Blob;
  ivHex: string;
  sha256Hex: string;
  keyBase64Url: string;
}

/**
 * Converts ArrayBuffer to Hex string
 */
export function bufferToHex(buffer: ArrayBuffer): string {
  const bytes = new Uint8Array(buffer);
  return Array.from(bytes)
    .map(b => b.toString(16).padStart(2, '0'))
    .join('');
}

/**
 * Converts Hex string to Uint8Array
 */
export function hexToBytes(hex: string): Uint8Array {
  const cleanHex = hex.trim();
  if (cleanHex.length % 2 !== 0) {
    throw new Error(`Invalid hex string length: ${cleanHex.length}`);
  }
  const bytes = new Uint8Array(cleanHex.length / 2);
  for (let i = 0; i < cleanHex.length; i += 2) {
    bytes[i / 2] = parseInt(cleanHex.substring(i, i + 2), 16);
  }
  return bytes;
}

/**
 * Convert ArrayBuffer to Base64URL string (URL fragment safe)
 */
export function bufferToBase64Url(buffer: ArrayBuffer): string {
  const bytes = new Uint8Array(buffer);
  let binary = '';
  for (let i = 0; i < bytes.byteLength; i++) {
    binary += String.fromCharCode(bytes[i]);
  }
  const base64 = btoa(binary);
  return base64.replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '');
}

/**
 * Convert Base64URL string back to ArrayBuffer
 */
export function base64UrlToBuffer(base64url: string): ArrayBuffer {
  let base64 = base64url.trim().replace(/-/g, '+').replace(/_/g, '/');
  while (base64.length % 4 !== 0) {
    base64 += '=';
  }
  const binary = atob(base64);
  const bytes = new Uint8Array(binary.length);
  for (let i = 0; i < binary.length; i++) {
    bytes[i] = binary.charCodeAt(i);
  }
  return bytes.buffer;
}

/**
 * Parses and validates an AES key string from Base64URL or Hex format.
 * Ensures the decoded raw key is EXACTLY 32 bytes (256 bits).
 */
export function parseAesKey(keyInput: string): ArrayBuffer {
  if (!keyInput || !keyInput.trim()) {
    throw new Error("Decryption key is missing.");
  }
  const trimmed = keyInput.trim();
  let buffer: ArrayBuffer;

  // Check if hex formatted (exactly 64 hex characters for 32 bytes)
  if (/^[0-9a-fA-F]{64}$/.test(trimmed)) {
    buffer = hexToBytes(trimmed).buffer as ArrayBuffer;
  } else {
    try {
      buffer = base64UrlToBuffer(trimmed);
    } catch (e) {
      throw new Error("Invalid decryption key encoding. Key must be valid Base64URL or Hex.");
    }
  }

  if (buffer.byteLength !== 32) {
    throw new Error(`AES key data must be 256 bits (32 bytes). Received ${buffer.byteLength} bytes.`);
  }

  return buffer;
}

const getCrypto = (): Crypto => {
  return typeof window !== 'undefined' ? window.crypto : (globalThis.crypto as Crypto);
};

/**
 * Computes SHA-256 hex digest of an ArrayBuffer
 */
export async function computeSha256(data: ArrayBuffer): Promise<string> {
  const hashBuffer = await getCrypto().subtle.digest('SHA-256', data);
  return bufferToHex(hashBuffer);
}

/**
 * Encrypts a raw file using CSPRNG AES-256-GCM (32-byte key)
 */
export async function encryptFile(file: File): Promise<EncryptionResult> {
  const fileArrayBuffer = await file.arrayBuffer();

  // 1. Calculate SHA-256 integrity digest of original plaintext
  const sha256Hex = await computeSha256(fileArrayBuffer);

  // 2. Generate random 256-bit (32 bytes) AES-GCM key
  const key = await getCrypto().subtle.generateKey(
    { name: 'AES-GCM', length: 256 },
    true,
    ['encrypt', 'decrypt']
  );

  // 3. Generate 96-bit (12-byte) unique random IV
  const ivBytes = getCrypto().getRandomValues(new Uint8Array(12));
  const ivHex = bufferToHex(ivBytes.buffer);

  // 4. Encrypt file using AES-256-GCM
  const encryptedBuffer = await getCrypto().subtle.encrypt(
    { name: 'AES-GCM', iv: ivBytes },
    key,
    fileArrayBuffer
  );

  // 5. Export key to raw format & encode as Base64URL (exactly 32 bytes)
  const rawKeyBuffer = await getCrypto().subtle.exportKey('raw', key);
  const keyBase64Url = bufferToBase64Url(rawKeyBuffer);

  const encryptedBlob = new Blob([encryptedBuffer], { type: 'application/octet-stream' });

  return {
    encryptedBlob,
    ivHex,
    sha256Hex,
    keyBase64Url
  };
}

/**
 * Decrypts an encrypted ArrayBuffer using AES-256-GCM and verifies SHA-256 integrity
 */
export async function decryptFile(
  encryptedArrayBuffer: ArrayBuffer,
  keyString: string,
  ivHex: string,
  expectedSha256Hex: string
): Promise<{ decryptedBlob: Blob; integrityVerified: boolean; calculatedSha256Hex: string }> {

  // 1. Parse & validate 256-bit (32 bytes) key
  const rawKeyBuffer = parseAesKey(keyString);

  const key = await getCrypto().subtle.importKey(
    'raw',
    rawKeyBuffer,
    { name: 'AES-GCM', length: 256 },
    false,
    ['decrypt']
  );

  // 2. Import IV (12 bytes)
  const ivBytes = hexToBytes(ivHex);

  // 3. Decrypt ciphertext
  const decryptedBuffer = await getCrypto().subtle.decrypt(
    { name: 'AES-GCM', iv: ivBytes as unknown as BufferSource },
    key,
    encryptedArrayBuffer
  );

  // 4. Compute SHA-256 of decrypted plaintext and verify integrity
  const calculatedSha256Hex = await computeSha256(decryptedBuffer);
  const integrityVerified = calculatedSha256Hex.toLowerCase() === expectedSha256Hex.toLowerCase();

  const decryptedBlob = new Blob([decryptedBuffer]);

  return {
    decryptedBlob,
    integrityVerified,
    calculatedSha256Hex
  };
}
