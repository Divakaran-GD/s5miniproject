import React, { useState } from 'react';
import { X, Share2, Copy, Check, Lock, Clock, Download, Mail } from 'lucide-react';
import { FileItem, ShareLink } from '../types';
import api from '../services/api';

interface CreateShareModalProps {
  file: FileItem | null;
  isOpen: boolean;
  onClose: () => void;
  onSuccess?: () => void;
}

export const CreateShareModal: React.FC<CreateShareModalProps> = ({ file, isOpen, onClose, onSuccess }) => {
  const [expiresInMinutes, setExpiresInMinutes] = useState<number>(1440); // 24 hours
  const [maxDownloads, setMaxDownloads] = useState<number>(3);
  const [usePassword, setUsePassword] = useState<boolean>(false);
  const [password, setPassword] = useState<string>('');
  const [recipientEmail, setRecipientEmail] = useState<string>('');
  const [loading, setLoading] = useState<boolean>(false);
  const [createdShare, setCreatedShare] = useState<{ shareUrl: string; passkey: string; expiresAt: string } | null>(null);
  const [copiedUrl, setCopiedUrl] = useState<boolean>(false);
  const [copiedKey, setCopiedKey] = useState<boolean>(false);
  const [error, setError] = useState<string | null>(null);

  if (!isOpen || !file) return null;

  const handleCreateShare = async () => {
    try {
      setLoading(true);
      setError(null);

      const payload = {
        expiresInMinutes,
        maxDownloads,
        password: usePassword ? password : null,
        recipientEmail: recipientEmail.trim() || null,
      };

      const res = await api.post<ShareLink>(`/files/${file.uuid}/shares`, payload);
      const shareData = res.data;

      // Retrieve stored client-side 256-bit AES key for file
      const savedKey = localStorage.getItem('securex_key_' + file.uuid) || '';

      let shareUrl = shareData.shareUrl || '';

      const env = (import.meta as any).env || {};
      const publicBaseUrl = env.VITE_PUBLIC_BASE_URL || env.VITE_FRONTEND_URL;
      if (publicBaseUrl) {
        const cleanBase = publicBaseUrl.endsWith('/') ? publicBaseUrl.slice(0, -1) : publicBaseUrl;
        const tokenMatch = shareUrl.match(/\/share\/([a-zA-Z0-9_-]+)/);
        if (tokenMatch) {
          shareUrl = `${cleanBase}/share/${tokenMatch[1]}`;
        }
      } else if (window.location.protocol === 'https:' || (!window.location.hostname.includes('localhost') && !window.location.hostname.includes('127.0.0.1'))) {
        const tokenMatch = shareUrl.match(/\/share\/([a-zA-Z0-9_-]+)/);
        if (tokenMatch) {
          shareUrl = `${window.location.origin}/share/${tokenMatch[1]}`;
        }
      }

      setCreatedShare({
        shareUrl,
        passkey: savedKey,
        expiresAt: new Date(shareData.expiresAt).toLocaleString(),
      });

      if (onSuccess) onSuccess();
    } catch (err: unknown) {
      const errorMsg = (err as { response?: { data?: { message?: string } }; message?: string }).response?.data?.message
        || (err as Error).message
        || 'Failed to create share link';
      setError(errorMsg);
    } finally {
      setLoading(false);
    }
  };

  const copyUrlToClipboard = () => {
    if (createdShare?.shareUrl) {
      navigator.clipboard.writeText(createdShare.shareUrl);
      setCopiedUrl(true);
      setTimeout(() => setCopiedUrl(false), 2500);
    }
  };

  const copyPasskeyToClipboard = () => {
    if (createdShare?.passkey) {
      navigator.clipboard.writeText(createdShare.passkey);
      setCopiedKey(true);
      setTimeout(() => setCopiedKey(false), 2500);
    }
  };

  const handleReset = () => {
    setCreatedShare(null);
    setCopiedUrl(false);
    setCopiedKey(false);
    setError(null);
    onClose();
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-950/80 backdrop-blur-sm">
      <div className="bg-slate-900 border border-slate-800 rounded-2xl w-full max-w-lg overflow-hidden shadow-2xl">
        <div className="p-6 border-b border-slate-800 flex items-center justify-between">
          <div className="flex items-center space-x-3">
            <div className="p-2 rounded-lg bg-emerald-500/10 border border-emerald-500/20 text-emerald-400">
              <Share2 className="w-5 h-5" />
            </div>
            <div>
              <h3 className="text-lg font-bold text-slate-100">Create Ephemeral Share Link</h3>
              <p className="text-xs text-slate-400 font-mono truncate max-w-xs">{file.originalFilename}</p>
            </div>
          </div>
          <button onClick={handleReset} className="text-slate-400 hover:text-slate-200">
            <X className="w-5 h-5" />
          </button>
        </div>

        <div className="p-6 space-y-5">
          {error && (
            <div className="p-3 rounded-lg bg-rose-500/10 border border-rose-500/30 text-rose-400 text-xs">
              {error}
            </div>
          )}

          {createdShare ? (
            <div className="space-y-4">
              <div className="p-4 rounded-xl bg-emerald-500/10 border border-emerald-500/30 text-center space-y-2">
                <div className="w-10 h-10 rounded-full bg-emerald-500/20 text-emerald-400 mx-auto flex items-center justify-center">
                  <Check className="w-6 h-6" />
                </div>
                <h4 className="font-bold text-slate-100">Secure Link Generated</h4>
                <p className="text-xs text-slate-400">Recipient can view metadata and download encrypted blob</p>
              </div>

              <div className="space-y-2">
                <label className="text-xs text-slate-400 font-mono">Share Link</label>
                <div className="flex items-center space-x-2">
                  <input
                    type="text"
                    readOnly
                    value={createdShare.shareUrl}
                    className="flex-1 bg-slate-950 border border-slate-800 rounded-lg px-3 py-2 text-xs font-mono text-emerald-400 truncate focus:outline-none"
                  />
                  <button
                    onClick={copyUrlToClipboard}
                    className="px-4 py-2 bg-emerald-500 hover:bg-emerald-600 text-white rounded-lg text-xs font-semibold flex items-center space-x-1.5 transition-colors shrink-0"
                  >
                    {copiedUrl ? <Check className="w-4 h-4" /> : <Copy className="w-4 h-4" />}
                    <span>{copiedUrl ? 'Copied' : 'Copy Link'}</span>
                  </button>
                </div>
              </div>

              <div className="space-y-2">
                <label className="text-xs text-slate-400 font-mono">Passkey</label>
                <div className="flex items-center space-x-2">
                  <input
                    type="text"
                    readOnly
                    value={createdShare.passkey}
                    className="flex-1 bg-slate-950 border border-slate-800 rounded-lg px-3 py-2 text-xs font-mono text-emerald-400 truncate focus:outline-none"
                  />
                  <button
                    onClick={copyPasskeyToClipboard}
                    className="px-4 py-2 bg-emerald-500 hover:bg-emerald-600 text-white rounded-lg text-xs font-semibold flex items-center space-x-1.5 transition-colors shrink-0"
                  >
                    {copiedKey ? <Check className="w-4 h-4" /> : <Copy className="w-4 h-4" />}
                    <span>{copiedKey ? 'Copied' : 'Copy Passkey'}</span>
                  </button>
                </div>
              </div>

              <div className="p-3 bg-slate-950 border border-slate-800/80 rounded-lg flex items-center justify-between text-xs font-mono text-slate-400">
                <span>Expires At:</span>
                <span className="text-slate-200">{createdShare.expiresAt}</span>
              </div>
            </div>
          ) : (
            <>
              {/* Expiration Duration */}
              <div className="space-y-2">
                <label className="text-xs font-medium text-slate-300 flex items-center space-x-2">
                  <Clock className="w-4 h-4 text-emerald-400" />
                  <span>Link Expiration Duration</span>
                </label>
                <select
                  value={expiresInMinutes}
                  onChange={(e) => setExpiresInMinutes(Number(e.target.value))}
                  className="w-full bg-slate-950 border border-slate-800 rounded-lg px-3 py-2 text-sm text-slate-200 focus:outline-none focus:border-emerald-500"
                >
                  <option value={60}>1 Hour</option>
                  <option value={1440}>24 Hours (1 Day)</option>
                  <option value={10080}>7 Days</option>
                  <option value={43200}>30 Days</option>
                </select>
              </div>

              {/* Max Downloads */}
              <div className="space-y-2">
                <label className="text-xs font-medium text-slate-300 flex items-center space-x-2">
                  <Download className="w-4 h-4 text-emerald-400" />
                  <span>Maximum Download Count</span>
                </label>
                <input
                  type="number"
                  min={1}
                  max={100}
                  value={maxDownloads}
                  onChange={(e) => setMaxDownloads(Number(e.target.value))}
                  className="w-full bg-slate-950 border border-slate-800 rounded-lg px-3 py-2 text-sm text-slate-200 focus:outline-none focus:border-emerald-500 font-mono"
                />
              </div>

              {/* Password Protection Toggle */}
              <div className="space-y-3 pt-2 border-t border-slate-800">
                <div className="flex items-center justify-between">
                  <label className="text-xs font-medium text-slate-300 flex items-center space-x-2 cursor-pointer">
                    <Lock className="w-4 h-4 text-emerald-400" />
                    <span>Password Protection</span>
                  </label>
                  <input
                    type="checkbox"
                    checked={usePassword}
                    onChange={(e) => setUsePassword(e.target.checked)}
                    className="w-4 h-4 accent-emerald-500 rounded cursor-pointer"
                  />
                </div>

                {usePassword && (
                  <input
                    type="password"
                    placeholder="Enter optional share password"
                    value={password}
                    onChange={(e) => setPassword(e.target.value)}
                    className="w-full bg-slate-950 border border-slate-800 rounded-lg px-3 py-2 text-sm text-slate-200 focus:outline-none focus:border-emerald-500"
                  />
                )}
              </div>

              {/* Recipient Email */}
              <div className="space-y-2 pt-2 border-t border-slate-800">
                <label className="text-xs font-medium text-slate-300 flex items-center space-x-2">
                  <Mail className="w-4 h-4 text-emerald-400" />
                  <span>Restrict to Recipient Email (Optional)</span>
                </label>
                <input
                  type="email"
                  placeholder="recipient@example.com"
                  value={recipientEmail}
                  onChange={(e) => setRecipientEmail(e.target.value)}
                  className="w-full bg-slate-950 border border-slate-800 rounded-lg px-3 py-2 text-sm text-slate-200 focus:outline-none focus:border-emerald-500"
                />
              </div>
            </>
          )}
        </div>

        <div className="p-6 border-t border-slate-800 bg-slate-950/40 flex items-center justify-end space-x-3">
          <button
            onClick={handleReset}
            className="px-4 py-2 rounded-lg text-sm text-slate-400 hover:text-slate-200 transition-colors"
          >
            {createdShare ? 'Close' : 'Cancel'}
          </button>
          {!createdShare && (
            <button
              onClick={handleCreateShare}
              disabled={loading}
              className="px-5 py-2 rounded-lg text-sm font-semibold bg-emerald-500 hover:bg-emerald-600 text-white transition-colors flex items-center space-x-2 disabled:opacity-50 shadow-lg shadow-emerald-500/20"
            >
              <Share2 className="w-4 h-4" />
              <span>{loading ? 'Generating...' : 'Generate Secure Link'}</span>
            </button>
          )}
        </div>
      </div>
    </div>
  );
};
