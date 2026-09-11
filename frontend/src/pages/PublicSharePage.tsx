import React, { useEffect, useState } from 'react';
import { useParams, useLocation } from 'react-router-dom';
import { Shield, Lock, Download, CheckCircle2, AlertCircle, Cpu, FileText, Key } from 'lucide-react';
import { PublicShareMetadata } from '../types';
import { decryptFile } from '../services/crypto';
import api from '../services/api';

export const PublicSharePage: React.FC = () => {
  const { token } = useParams<{ token: string }>();
  const location = useLocation();

  const [metadata, setMetadata] = useState<PublicShareMetadata | null>(null);
  const [loading, setLoading] = useState<boolean>(true);
  const [password, setPassword] = useState<string>('');
  const [downloading, setDownloading] = useState<boolean>(false);
  const [downloadProgress, setDownloadProgress] = useState<number>(0);
  const [decrypting, setDecrypting] = useState<boolean>(false);
  const [integrityStatus, setIntegrityStatus] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  // Extract key fragment from hash `#key=...` or prompt user
  const getHashKey = (): string => {
    const hash = location.hash.startsWith('#') ? location.hash.substring(1) : location.hash;
    if (!hash) return '';
    const params = new URLSearchParams(hash);
    const rawKey = params.get('key') || (hash.startsWith('key=') ? hash.substring(4) : hash);
    try {
      return decodeURIComponent(rawKey);
    } catch {
      return rawKey;
    }
  };

  const [decryptionKey, setDecryptionKey] = useState<string>(getHashKey());

  useEffect(() => {
    const fetchMetadata = async () => {
      if (!token) return;
      try {
        setLoading(true);
        const res = await api.get<PublicShareMetadata>(`/public/shares/${token}`);
        setMetadata(res.data);
      } catch (err: unknown) {
        const errorMsg = (err as { response?: { data?: { message?: string } }; message?: string }).response?.data?.message
          || 'Invalid or expired share link';
        setError(errorMsg);
      } finally {
        setLoading(false);
      }
    };
    fetchMetadata();
  }, [token]);

  const handleDownload = async () => {
    if (!token || !metadata) return;

    if (!decryptionKey.trim()) {
      setError('Decryption key is required. Please check your share URL fragment or enter key manually.');
      return;
    }

    try {
      setError(null);
      setDownloading(true);
      setDownloadProgress(20);

      // Step 1: Download raw encrypted file stream from backend
      const response = await api.get(`/public/shares/${token}/download`, {
        params: { password: metadata.passwordProtected ? password : null },
        responseType: 'arraybuffer',
        onDownloadProgress: (progressEvent) => {
          if (progressEvent.total) {
            const percent = Math.round((progressEvent.loaded * 100) / progressEvent.total);
            setDownloadProgress(percent);
          }
        },
      });

      setDownloading(false);
      setDecrypting(true);

      // Read IV & SHA256 headers or metadata
      const ivHex = response.headers['x-encrypted-iv'] || metadata.iv;
      const expectedSha256 = response.headers['x-encrypted-sha256'] || metadata.sha256Hash;

      // Step 2: Web Crypto API AES-256-GCM local browser decryption
      const decryptResult = await decryptFile(
        response.data,
        decryptionKey.trim(),
        ivHex,
        expectedSha256
      );

      setDecrypting(false);

      if (!decryptResult.integrityVerified) {
        setIntegrityStatus('FAILED');
        setError('CRITICAL: SHA-256 Integrity Verification Failed! File content may have been tampered with.');
        return;
      }

      setIntegrityStatus('VERIFIED');

      // Step 3: Trigger browser save of decrypted plaintext file
      const blobUrl = URL.createObjectURL(decryptResult.decryptedBlob);
      const a = document.createElement('a');
      a.href = blobUrl;
      a.download = metadata.originalFilename;
      document.body.appendChild(a);
      a.click();
      document.body.removeChild(a);
      URL.revokeObjectURL(blobUrl);

    } catch (err: unknown) {
      setDownloading(false);
      setDecrypting(false);
      const axiosErr = err as { response?: { data?: ArrayBuffer }; message?: string };
      if (axiosErr.response && axiosErr.response.data) {
        try {
          const text = new TextDecoder().decode(axiosErr.response.data);
          const parsed = JSON.parse(text);
          setError(parsed.message || 'Download denied');
        } catch {
          setError('Download denied');
        }
      } else {
        setError(axiosErr.message || 'Decryption or download failed');
      }
    }
  };

  if (loading) {
    return (
      <div className="min-h-screen bg-slate-950 flex flex-col items-center justify-center p-4">
        <div className="flex flex-col items-center space-y-3">
          <div className="w-10 h-10 border-4 border-emerald-500/30 border-t-emerald-500 rounded-full animate-spin"></div>
          <span className="text-sm font-mono text-slate-400">Verifying ephemeral link token...</span>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-slate-950 flex flex-col items-center justify-center p-4">
      <div className="w-full max-w-lg space-y-6">
        <div className="text-center space-y-2">
          <div className="w-14 h-14 rounded-2xl bg-emerald-500/10 border border-emerald-500/30 flex items-center justify-center text-emerald-400 mx-auto shadow-xl shadow-emerald-500/10">
            <Shield className="w-7 h-7" />
          </div>
          <h1 className="text-2xl font-bold text-slate-100">SecureX Shared File</h1>
          <p className="text-xs text-slate-400 font-mono">End-to-End Encrypted Exchange Portal</p>
        </div>

        {error && (
          <div className="p-4 rounded-xl bg-rose-500/10 border border-rose-500/30 text-rose-400 text-xs flex items-start space-x-3">
            <AlertCircle className="w-5 h-5 shrink-0 mt-0.5" />
            <div>
              <p className="font-semibold">{error}</p>
            </div>
          </div>
        )}

        {metadata && (
          <div className="glass-card p-6 rounded-2xl border border-slate-800 space-y-6 shadow-2xl">
            {/* File Metadata Header */}
            <div className="flex items-center space-x-4 p-4 rounded-xl bg-slate-900/60 border border-slate-800">
              <FileText className="w-10 h-10 text-emerald-400 shrink-0" />
              <div className="overflow-hidden">
                <h3 className="font-bold text-slate-100 text-sm truncate">{metadata.originalFilename}</h3>
                <div className="flex items-center space-x-3 text-xs text-slate-400 font-mono mt-1">
                  <span>{(metadata.fileSize / (1024 * 1024)).toFixed(2)} MB</span>
                  <span>•</span>
                  <span>{metadata.mimeType}</span>
                </div>
              </div>
            </div>

            {/* Share Status Badges */}
            <div className="grid grid-cols-2 gap-3 text-xs font-mono">
              <div className="p-3 rounded-lg bg-slate-950 border border-slate-800/80">
                <span className="text-slate-500 block uppercase text-[10px]">Expires</span>
                <span className="text-slate-200">{new Date(metadata.expiresAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}</span>
              </div>
              <div className="p-3 rounded-lg bg-slate-950 border border-slate-800/80">
                <span className="text-slate-500 block uppercase text-[10px]">Downloads Remaining</span>
                <span className="text-emerald-400 font-bold">{metadata.remainingDownloads}</span>
              </div>
            </div>

            {/* Password Prompt */}
            {metadata.passwordProtected && (
              <div className="space-y-2 pt-2 border-t border-slate-800">
                <label className="text-xs font-medium text-slate-300 flex items-center space-x-2">
                  <Lock className="w-4 h-4 text-emerald-400" />
                  <span>Password Required</span>
                </label>
                <input
                  type="password"
                  placeholder="Enter share password"
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  className="w-full bg-slate-950 border border-slate-800 rounded-lg px-3 py-2 text-sm text-slate-200 focus:outline-none focus:border-emerald-500"
                />
              </div>
            )}

            {/* Decryption Key Field */}
            {!decryptionKey && (
              <div className="space-y-2 pt-2 border-t border-slate-800">
                <label className="text-xs font-medium text-slate-300 flex items-center space-x-2">
                  <Key className="w-4 h-4 text-emerald-400" />
                  <span>Decryption Key (Base64Url / Hex)</span>
                </label>
                <input
                  type="text"
                  placeholder="Enter file decryption key"
                  value={decryptionKey}
                  onChange={(e) => setDecryptionKey(e.target.value)}
                  className="w-full bg-slate-950 border border-slate-800 rounded-lg px-3 py-2 text-xs font-mono text-emerald-400 focus:outline-none focus:border-emerald-500"
                />
              </div>
            )}

            {/* Progress and Integrity Status */}
            {downloading && (
              <div className="space-y-2">
                <div className="flex items-center justify-between text-xs text-slate-300 font-mono">
                  <span>Downloading Encrypted Blob...</span>
                  <span>{downloadProgress}%</span>
                </div>
                <div className="w-full h-2 bg-slate-800 rounded-full overflow-hidden">
                  <div className="h-full bg-blue-500 transition-all duration-300" style={{ width: `${downloadProgress}%` }}></div>
                </div>
              </div>
            )}

            {decrypting && (
              <div className="p-4 rounded-xl bg-emerald-500/10 border border-emerald-500/30 space-y-2">
                <div className="flex items-center space-x-2 text-xs font-mono text-emerald-400">
                  <Cpu className="w-4 h-4 animate-spin" />
                  <span>Decrypting using Web Crypto API (AES-256-GCM)...</span>
                </div>
                <div className="flex items-center space-x-2 text-xs font-mono text-slate-300">
                  <CheckCircle2 className="w-4 h-4 text-emerald-400" />
                  <span>Calculating SHA-256 checksum verification...</span>
                </div>
              </div>
            )}

            {integrityStatus === 'VERIFIED' && (
              <div className="p-3 rounded-lg bg-emerald-500/20 border border-emerald-500/40 text-emerald-300 text-xs font-mono flex items-center space-x-2">
                <CheckCircle2 className="w-4 h-4 text-emerald-400" />
                <span>✓ SHA-256 Integrity Verified — File Decrypted</span>
              </div>
            )}

            {metadata.valid ? (
              <button
                onClick={handleDownload}
                disabled={downloading || decrypting}
                className="w-full py-3 bg-emerald-500 hover:bg-emerald-600 text-white font-semibold rounded-xl text-sm transition-all flex items-center justify-center space-x-2 shadow-lg shadow-emerald-500/20 disabled:opacity-50"
              >
                <Download className="w-4 h-4" />
                <span>
                  {downloading
                    ? 'Downloading Blob...'
                    : decrypting
                      ? 'Decrypting...'
                      : 'Verify & Download'}
                </span>
              </button>
            ) : (
              <div className="p-4 rounded-xl bg-rose-500/10 border border-rose-500/30 text-rose-400 text-sm text-center">
                <div className="font-semibold">🔒 This share link has expired</div>
                <div className="text-xs text-slate-400 mt-1">
                  This file is no longer available for download.
                </div>
              </div>
            )}
          </div>
        )}
      </div>
    </div>
  );
};
