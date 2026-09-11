# SecureX Cryptographic Model & Ephemeral Link Design

## Zero-Knowledge Encryption Model
1. **Client Key Generation**: For every file upload, the browser generates a 256-bit AES key via `window.crypto.subtle.generateKey`.
2. **Nonce/IV Generation**: A unique 96-bit random IV is generated for every encryption using `window.crypto.getRandomValues`.
3. **Integrity Checksum**: Plaintext SHA-256 hash is computed client-side prior to encryption.
4. **Ciphertext Storage**: Only the encrypted binary blob, IV, and SHA-256 hash are stored on the server. The raw secret key NEVER touches the backend.

## Ephemeral Link Control
- **Token Generation**: High-entropy 256-bit CSPRNG token (`32 bytes` / `64 hex chars`).
- **Token Hashing**: Server stores `SHA-256(token)` to prevent database breach token enumeration.
- **Link Fragment Key Ingestion**: The share link URL includes the key in the fragment: `https://localhost:5173/share/<token>#key=<keyBase64Url>`. Browsers do NOT send URL fragment hashes (`#key=...`) to HTTP servers.
