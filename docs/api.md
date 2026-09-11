# SecureX REST API Reference

## Auth Endpoints
- `POST /api/auth/register` — Register a new account
- `POST /api/auth/login` — Authenticate credentials and receive Bearer JWT
- `GET /api/auth/me` — Retrieve active user session profile
- `POST /api/auth/logout` — End user session

## File Management Endpoints
- `POST /api/files` — Upload ciphertext blob + IV + SHA-256
- `GET /api/files` — List user's encrypted files
- `GET /api/files/{uuid}` — Get encrypted file metadata
- `GET /api/files/{uuid}/download` — Download encrypted blob
- `DELETE /api/files/{uuid}` — Soft delete file

## Ephemeral Share Endpoints
- `POST /api/files/{fileUuid}/shares` — Create share link with expiration & download limits
- `GET /api/files/{fileUuid}/shares` — List share links for file
- `GET /api/shares` — List all share links created by current user
- `DELETE /api/shares/{shareUuid}` — Revoke share link immediately

## Public Recipient Endpoints
- `GET /api/public/shares/{token}` — Evaluate share token validity and metadata
- `POST /api/public/shares/{token}/verify` — Verify share password
- `GET /api/public/shares/{token}/download` — Stream encrypted file blob

## Admin Endpoints (RBAC `ROLE_ADMIN`)
- `GET /api/admin/stats` — High-level system activity metrics
- `GET /api/admin/users` — List registered users
- `GET /api/admin/files` — Inventory of all stored files
- `GET /api/admin/shares` — Inventory of all share links
- `GET /api/admin/audit-logs` — System security audit trail
