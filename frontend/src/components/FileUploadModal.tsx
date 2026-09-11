import React, { useState, useRef } from 'react';
import { Upload, X, Shield, Cpu, Lock, FileText, AlertCircle } from 'lucide-react';
import { encryptFile } from '../services/crypto';
import api from '../services/api';

interface FileUploadModalProps {
  isOpen: boolean;
  onClose: () => void;
  onSuccess: () => void;
}

export const FileUploadModal: React.FC<FileUploadModalProps> = ({ isOpen, onClose, onSuccess }) => {
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [isDragOver, setIsDragOver] = useState(false);
  const [encrypting, setEncrypting] = useState(false);
  const [encryptionProgress, setEncryptionProgress] = useState(0);
  const [uploading, setUploading] = useState(false);
  const [uploadProgress, setUploadProgress] = useState(0);
  const [sha256, setSha256] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const fileInputRef = useRef<HTMLInputElement>(null);

  if (!isOpen) return null;

  const handleFileChange = (file: File) => {
    if (file.size > 100 * 1024 * 1024) {
      setError('File size exceeds maximum limit of 100MB');
      return;
    }
    setSelectedFile(file);
    setError(null);
    setSha256(null);
    setEncryptionProgress(0);
    setUploadProgress(0);
  };

  const handleDrop = (e: React.DragEvent) => {
    e.preventDefault();
    setIsDragOver(false);
    if (e.dataTransfer.files && e.dataTransfer.files[0]) {
      handleFileChange(e.dataTransfer.files[0]);
    }
  };

  const handleUpload = async () => {
    if (!selectedFile) return;

    try {
      setError(null);
      setEncrypting(true);
      setEncryptionProgress(25);

      // Step 1: Web Crypto API AES-256-GCM encryption & SHA-256 computation
      setEncryptionProgress(50);
      const cryptoResult = await encryptFile(selectedFile);
      setEncryptionProgress(100);
      setEncrypting(false);
      setSha256(cryptoResult.sha256Hex);

      // Step 2: Upload encrypted blob to backend
      setUploading(true);
      const formData = new FormData();
      formData.append('file', cryptoResult.encryptedBlob, selectedFile.name);
      formData.append('iv', cryptoResult.ivHex);
      formData.append('sha256Hash', cryptoResult.sha256Hex);

      const uploadRes = await api.post<{ uuid: string }>('/files', formData, {
        headers: {
          'Content-Type': 'multipart/form-data',
        },
        onUploadProgress: (progressEvent) => {
          if (progressEvent.total) {
            const percent = Math.round((progressEvent.loaded * 100) / progressEvent.total);
            setUploadProgress(percent);
          }
        },
      });

      if (uploadRes.data && uploadRes.data.uuid) {
        localStorage.setItem('securex_key_' + uploadRes.data.uuid, cryptoResult.keyBase64Url);
      }

      setUploading(false);
      onSuccess();
      onClose();
    } catch (err: unknown) {
      setEncrypting(false);
      setUploading(false);
      const errorMsg = (err as { response?: { data?: { message?: string } }; message?: string }).response?.data?.message
        || (err as Error).message
        || 'Failed to encrypt and upload file';
      setError(errorMsg);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-950/80 backdrop-blur-sm">
      <div className="bg-slate-900 border border-slate-800 rounded-2xl w-full max-w-lg overflow-hidden shadow-2xl">
        <div className="p-6 border-b border-slate-800 flex items-center justify-between">
          <div className="flex items-center space-x-3">
            <div className="p-2 rounded-lg bg-emerald-500/10 border border-emerald-500/20 text-emerald-400">
              <Lock className="w-5 h-5" />
            </div>
            <div>
              <h3 className="text-lg font-bold text-slate-100">Secure Encrypted Upload</h3>
              <p className="text-xs text-slate-400">Client-side AES-256-GCM zero-trust encryption</p>
            </div>
          </div>
          <button onClick={onClose} className="text-slate-400 hover:text-slate-200">
            <X className="w-5 h-5" />
          </button>
        </div>

        <div className="p-6 space-y-6">
          {error && (
            <div className="p-3 rounded-lg bg-rose-500/10 border border-rose-500/30 text-rose-400 text-xs flex items-center space-x-2">
              <AlertCircle className="w-4 h-4 shrink-0" />
              <span>{error}</span>
            </div>
          )}

          {/* Drag and Drop Zone */}
          <div
            onDragOver={(e) => { e.preventDefault(); setIsDragOver(true); }}
            onDragLeave={() => setIsDragOver(false)}
            onDrop={handleDrop}
            onClick={() => fileInputRef.current?.click()}
            className={`border-2 border-dashed rounded-xl p-8 text-center cursor-pointer transition-all ${
              isDragOver
                ? 'border-emerald-500 bg-emerald-500/5'
                : selectedFile
                ? 'border-slate-700 bg-slate-800/40'
                : 'border-slate-800 hover:border-slate-700 hover:bg-slate-800/20'
            }`}
          >
            <input
              ref={fileInputRef}
              type="file"
              onChange={(e) => e.target.files?.[0] && handleFileChange(e.target.files[0])}
              className="hidden"
            />
            {selectedFile ? (
              <div className="flex items-center justify-center space-x-3">
                <FileText className="w-8 h-8 text-emerald-400 shrink-0" />
                <div className="text-left">
                  <p className="font-semibold text-sm text-slate-200 truncate max-w-xs">{selectedFile.name}</p>
                  <p className="text-xs text-slate-400 font-mono">{(selectedFile.size / (1024 * 1024)).toFixed(2)} MB</p>
                </div>
              </div>
            ) : (
              <div className="space-y-2">
                <Upload className="w-10 h-10 text-slate-500 mx-auto" />
                <p className="text-sm text-slate-300 font-medium">Drag and drop file here, or <span className="text-emerald-400 underline">browse</span></p>
                <p className="text-xs text-slate-500">Maximum file size: 100 MB</p>
              </div>
            )}
          </div>

          {/* Progress Indicators */}
          {encrypting && (
            <div className="space-y-2">
              <div className="flex items-center justify-between text-xs text-slate-300 font-mono">
                <span className="flex items-center space-x-2">
                  <Cpu className="w-4 h-4 text-emerald-400 animate-spin" />
                  <span>Web Crypto API AES-256-GCM Encryption...</span>
                </span>
                <span>{encryptionProgress}%</span>
              </div>
              <div className="w-full h-2 bg-slate-800 rounded-full overflow-hidden">
                <div className="h-full bg-emerald-500 transition-all duration-300" style={{ width: `${encryptionProgress}%` }}></div>
              </div>
            </div>
          )}

          {sha256 && (
            <div className="p-3 bg-slate-950 border border-slate-800 rounded-lg space-y-1">
              <span className="text-[10px] text-slate-400 uppercase tracking-wider font-mono">SHA-256 Checksum</span>
              <p className="text-xs font-mono text-emerald-400 truncate">{sha256}</p>
            </div>
          )}

          {uploading && (
            <div className="space-y-2">
              <div className="flex items-center justify-between text-xs text-slate-300 font-mono">
                <span className="flex items-center space-x-2">
                  <Upload className="w-4 h-4 text-blue-400 animate-bounce" />
                  <span>Uploading Ciphertext Blob...</span>
                </span>
                <span>{uploadProgress}%</span>
              </div>
              <div className="w-full h-2 bg-slate-800 rounded-full overflow-hidden">
                <div className="h-full bg-blue-500 transition-all duration-300" style={{ width: `${uploadProgress}%` }}></div>
              </div>
            </div>
          )}
        </div>

        <div className="p-6 border-t border-slate-800 bg-slate-950/40 flex items-center justify-end space-x-3">
          <button
            onClick={onClose}
            disabled={encrypting || uploading}
            className="px-4 py-2 rounded-lg text-sm text-slate-400 hover:text-slate-200 transition-colors disabled:opacity-50"
          >
            Cancel
          </button>
          <button
            onClick={handleUpload}
            disabled={!selectedFile || encrypting || uploading}
            className="px-5 py-2 rounded-lg text-sm font-semibold bg-emerald-500 hover:bg-emerald-600 text-white transition-colors flex items-center space-x-2 disabled:opacity-50 shadow-lg shadow-emerald-500/20"
          >
            <Shield className="w-4 h-4" />
            <span>Encrypt & Upload</span>
          </button>
        </div>
      </div>
    </div>
  );
};
