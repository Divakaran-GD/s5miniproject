# SecureX Threat Model & Vulnerability Analysis

## Threat Vectors & Defenses

| Threat Vector | Defense Mechanism |
|---|---|
| **Database Compromise** | Server stores only SHA-256 token hashes and ciphertext files. No plaintext files or raw encryption keys exist in DB. |
| **Share Link Brute Forcing** | Links use 256-bit CSPRNG tokens (64 hex characters) + rate limiting filter. |
| **Download Limit Bypass** | Safe atomic database queries (`incrementDownloadCountSafely`) prevent race condition bypasses under concurrency. |
| **Path Traversal Attacks** | Original filenames are sanitized; files are stored on disk using server-generated random UUID names. |
| **Man-in-the-Middle (MITM)** | Client-side encryption ensures payload is already encrypted before entering TLS transport layer. |
| **IDOR Attacks** | User ownership & authorization checks enforced on every resource ID query. |
