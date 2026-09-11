# SecureX System Architecture

## High-Level Diagram

```mermaid
flowchart TD
    Client["React Frontend (Vite + TypeScript)"] -->|Web Crypto API| CryptoEngine["AES-256-GCM + SHA-256"]
    Client -->|HTTPS REST APIs| SpringBoot["Spring Boot 3.x Backend"]
    SpringBoot -->|Spring Security RBAC| SecurityModule["JWT Auth & Security Filters"]
    SpringBoot -->|Data JPA / Flyway| Database[("PostgreSQL / H2 Database")]
    SpringBoot -->|Storage Service| FileBlobStore["Encrypted Storage (.enc Blobs)"]
    SpringBoot -->|Audit Logger| AuditSystem["Security Audit Logs"]
```

## Core Components
1. **Frontend**: React 18 SPA built with Vite, TypeScript, and Tailwind CSS.
2. **Backend**: Java 17/21 Spring Boot REST application with Spring Security, Data JPA, and Flyway.
3. **Database**: PostgreSQL (production) or H2 (local zero-config demo).
4. **File Storage**: Server disk abstraction layer storing raw encrypted ciphertext blobs.
