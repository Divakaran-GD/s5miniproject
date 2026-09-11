import { describe, it, expect, beforeAll } from 'vitest';
import { webcrypto } from 'crypto';
import {
  encryptFile,
  decryptFile,
  bufferToBase64Url,
  base64UrlToBuffer,
  parseAesKey,
  bufferToHex
} from './crypto';

// Setup Web Crypto API in Node environment
beforeAll(() => {
  if (!globalThis.crypto) {
    // @ts-ignore
    globalThis.crypto = webcrypto;
  }
});

describe('SecureX Client-Side Cryptographic Engine Tests', () => {

  it('1. Generates valid 256-bit (32 bytes) AES-GCM key and encodes as Base64URL', async () => {
    const dummyFile = new File(['Hello SecureX Encryption World!'], 'sample.txt', { type: 'text/plain' });
    const result = await encryptFile(dummyFile);

    expect(result.encryptedBlob).toBeDefined();
    expect(result.ivHex.length).toBe(24); // 12 bytes = 24 hex characters
    expect(result.sha256Hex.length).toBe(64); // 32 bytes = 64 hex characters

    // Decode exported key
    const rawKeyBuffer = base64UrlToBuffer(result.keyBase64Url);
    expect(rawKeyBuffer.byteLength).toBe(32); // Must be exactly 32 bytes (256 bits)
  });

  it('2. Lossless Key Serialization & Deserialization (Base64URL)', () => {
    const originalBytes = new Uint8Array(32);
    for (let i = 0; i < 32; i++) originalBytes[i] = i * 7;

    const base64Url = bufferToBase64Url(originalBytes.buffer);
    const decodedBuffer = base64UrlToBuffer(base64Url);
    const decodedBytes = new Uint8Array(decodedBuffer);

    expect(decodedBytes.length).toBe(32);
    expect(decodedBytes).toEqual(originalBytes);
  });

  it('3. parseAesKey correctly validates 32-byte Base64URL and Hex keys', () => {
    const sample32Bytes = new Uint8Array(32).fill(0xAB);
    const base64Url = bufferToBase64Url(sample32Bytes.buffer);
    const hexString = bufferToHex(sample32Bytes.buffer);

    const parsedFromBase64 = parseAesKey(base64Url);
    expect(parsedFromBase64.byteLength).toBe(32);

    const parsedFromHex = parseAesKey(hexString);
    expect(parsedFromHex.byteLength).toBe(32);
  });

  it('4. Rejects invalid key sizes and invalid formats', () => {
    // 12-byte key (96 bits) - should be rejected
    const invalid12BytesHex = bufferToHex(new Uint8Array(12).buffer);
    expect(() => parseAesKey(invalid12BytesHex)).toThrow(/AES key data must be 256 bits \(32 bytes\)/);

    // Empty string
    expect(() => parseAesKey('')).toThrow(/Decryption key is missing/);

    // 16-byte key (128 bits) - should be rejected as SecureX requires 256-bit
    const invalid16BytesBase64 = bufferToBase64Url(new Uint8Array(16).buffer);
    expect(() => parseAesKey(invalid16BytesBase64)).toThrow(/AES key data must be 256 bits \(32 bytes\)/);
  });

  it('5. Encryption and Decryption Round-Trip with Integrity Verification', async () => {
    const plaintextContent = "Confidential Top Secret Data Content for Ephemeral Link Exchange";
    const dummyFile = new File([plaintextContent], 'top_secret.txt', { type: 'text/plain' });

    // Step A: Encrypt
    const encryptResult = await encryptFile(dummyFile);
    const encryptedArrayBuffer = await encryptResult.encryptedBlob.arrayBuffer();

    // Step B: Decrypt using exported 32-byte key
    const decryptResult = await decryptFile(
      encryptedArrayBuffer,
      encryptResult.keyBase64Url,
      encryptResult.ivHex,
      encryptResult.sha256Hex
    );

    expect(decryptResult.integrityVerified).toBe(true);

    const decryptedText = await decryptResult.decryptedBlob.text();
    expect(decryptedText).toBe(plaintextContent);
  });

});
