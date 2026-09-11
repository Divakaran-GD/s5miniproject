export type Role = 'USER' | 'ADMIN';

export interface User {
  uuid: string;
  email: string;
  displayName: string;
  role: Role;
  enabled: boolean;
  createdAt: string;
  lastLoginAt?: string;
}

export interface AuthResponse {
  token: string;
  tokenType: string;
  user: User;
}

export interface FileItem {
  uuid: string;
  originalFilename: string;
  mimeType: string;
  fileSize: number;
  encryptionAlgorithm: string;
  encryptionVersion: string;
  iv: string;
  sha256Hash: string;
  createdAt: string;
  ownerUuid: string;
  ownerEmail: string;
}

export interface FileUploadResponse {
  uuid: string;
  originalFilename: string;
  fileSize: number;
  sha256Hash: string;
  status: string;
  message: string;
}

export interface ShareLink {
  uuid: string;
  fileUuid: string;
  originalFilename: string;
  fileSize: number;
  rawToken?: string;
  shareUrl?: string;
  expiresAt: string;
  maxDownloads: number;
  downloadCount: number;
  passwordProtected: boolean;
  recipientEmail?: string;
  revoked: boolean;
  expired: boolean;
  createdAt: string;
  lastAccessedAt?: string;
}

export interface PublicShareMetadata {
  shareUuid: string;
  originalFilename: string;
  mimeType: string;
  fileSize: number;
  encryptionAlgorithm: string;
  encryptionVersion: string;
  iv: string;
  sha256Hash: string;
  expiresAt: string;
  remainingDownloads: number;
  passwordProtected: boolean;
  recipientRestricted: boolean;
  valid: boolean;
  statusMessage: string;
}

export interface AuditLog {
  id: number;
  userId?: number;
  eventType: string;
  resourceType: string;
  resourceId?: string;
  timestamp: string;
  ipHash?: string;
  metadata?: string;
}

export interface AdminStats {
  totalUsers: number;
  totalFiles: number;
  totalShareLinks: number;
  activeShareLinks: number;
  expiredOrRevokedLinks: number;
  totalSuccessfulDownloads: number;
}
