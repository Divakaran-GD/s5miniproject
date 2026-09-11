# SECURITY.md — SecureX Security Policy & Limitations

## Protected Against
- Unauthorized file enumeration
- Expired & revoked share link access
- Download-limit bypass via concurrent requests
- Plaintext exposure at rest on server storage
- Token guessing attacks
- SQL Injection, XSS, Path Traversal, and IDOR attacks

## Cryptographic Primitives
- **Encryption**: AES-256-GCM (Web Crypto API)
- **Integrity**: SHA-256 Checksum
- **Password Hashing**: BCrypt
- **Random Tokens**: CSPRNG (Java `SecureRandom` & Browser `crypto.getRandomValues`)

## Security Assumptions & Limitations
> [!WARNING]
> 1. **Client Browser Trust**: Client-side encryption relies on browser integrity. If the user's workstation or browser extensions are compromised, encryption keys can be intercepted in DOM memory.
> 2. **Server Trust**: A compromised web server delivering the React frontend could theoretically serve malicious JavaScript to harvest encryption keys before encryption occurs. HTTPS and Subresource Integrity (SRI) must be enforced in production.
> 3. **Metadata Visibility**: The server stores file size, MIME type, upload timestamp, and SHA-256 hashes for integrity verification.
> 4. **Educational/Demo Notice**: SecureX is designed to demonstrate modern full-stack zero-trust architecture concepts. It is not a substitute for a professionally audited commercial solution.
