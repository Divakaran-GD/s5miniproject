import React, { useEffect, useState } from 'react';
import { HardDrive, Share2, Upload, Clock, ShieldCheck } from 'lucide-react';
import { FileItem, ShareLink } from '../types';
import { FileUploadModal } from '../components/FileUploadModal';
import { CreateShareModal } from '../components/CreateShareModal';
import api from '../services/api';

export const DashboardPage: React.FC = () => {
  const [files, setFiles] = useState<FileItem[]>([]);
  const [shares, setShares] = useState<ShareLink[]>([]);
  const [loading, setLoading] = useState<boolean>(true);
  const [isUploadOpen, setIsUploadOpen] = useState<boolean>(false);
  const [selectedShareFile, setSelectedShareFile] = useState<FileItem | null>(null);

  const fetchData = async () => {
    try {
      setLoading(true);
      const [filesRes, sharesRes] = await Promise.all([
        api.get<FileItem[]>('/files'),
        api.get<ShareLink[]>('/shares'),
      ]);
      setFiles(filesRes.data);
      setShares(sharesRes.data);
    } catch (err) {
      console.error('Error fetching dashboard data:', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchData();
  }, []);

  const activeSharesCount = shares.filter(s => !s.revoked && !s.expired).length;
  const expiredSharesCount = shares.filter(s => s.revoked || s.expired).length;

  return (
    <div className="space-y-8">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-slate-100">Security Dashboard</h1>
          <p className="text-xs text-slate-400">Zero-Trust AES-256-GCM encrypted portal</p>
        </div>
        <button
          onClick={() => setIsUploadOpen(true)}
          className="px-4 py-2 bg-emerald-500 hover:bg-emerald-600 text-white rounded-xl font-semibold text-sm flex items-center space-x-2 transition-colors shadow-lg shadow-emerald-500/20"
        >
          <Upload className="w-4 h-4" />
          <span>Upload Encrypted File</span>
        </button>
      </div>

      {/* Metrics Cards */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
        <div className="glass-card p-6 rounded-2xl border border-slate-800 flex items-center space-x-4">
          <div className="p-3 rounded-xl bg-emerald-500/10 border border-emerald-500/20 text-emerald-400">
            <HardDrive className="w-6 h-6" />
          </div>
          <div>
            <p className="text-xs text-slate-400 uppercase tracking-wider font-mono">Encrypted Files</p>
            <h3 className="text-2xl font-bold text-slate-100">{files.length}</h3>
          </div>
        </div>

        <div className="glass-card p-6 rounded-2xl border border-slate-800 flex items-center space-x-4">
          <div className="p-3 rounded-xl bg-blue-500/10 border border-blue-500/20 text-blue-400">
            <Share2 className="w-6 h-6" />
          </div>
          <div>
            <p className="text-xs text-slate-400 uppercase tracking-wider font-mono">Active Shares</p>
            <h3 className="text-2xl font-bold text-slate-100">{activeSharesCount}</h3>
          </div>
        </div>

        <div className="glass-card p-6 rounded-2xl border border-slate-800 flex items-center space-x-4">
          <div className="p-3 rounded-xl bg-purple-500/10 border border-purple-500/20 text-purple-400">
            <Clock className="w-6 h-6" />
          </div>
          <div>
            <p className="text-xs text-slate-400 uppercase tracking-wider font-mono">Expired / Revoked</p>
            <h3 className="text-2xl font-bold text-slate-100">{expiredSharesCount}</h3>
          </div>
        </div>
      </div>

      {/* Recent Encrypted Files */}
      <div className="glass-card rounded-2xl border border-slate-800 overflow-hidden">
        <div className="p-6 border-b border-slate-800 flex items-center justify-between">
          <div className="flex items-center space-x-2">
            <ShieldCheck className="w-5 h-5 text-emerald-400" />
            <h2 className="font-bold text-slate-100">Recent Encrypted Storage Blobs</h2>
          </div>
          <span className="text-xs font-mono text-slate-400">AES-256-GCM + SHA-256</span>
        </div>

        {loading ? (
          <div className="p-8 text-center text-xs font-mono text-slate-500">Loading encrypted files...</div>
        ) : files.length === 0 ? (
          <div className="p-12 text-center space-y-3">
            <HardDrive className="w-10 h-10 text-slate-600 mx-auto" />
            <p className="text-sm text-slate-400">No encrypted files stored yet.</p>
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs text-slate-300">
              <thead className="bg-slate-900/60 uppercase font-mono text-[10px] text-slate-400">
                <tr>
                  <th className="p-4">Filename</th>
                  <th className="p-4">Size</th>
                  <th className="p-4">SHA-256 Hash</th>
                  <th className="p-4">Created At</th>
                  <th className="p-4 text-right">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-800/60">
                {files.slice(0, 5).map((file) => (
                  <tr key={file.uuid} className="hover:bg-slate-900/40">
                    <td className="p-4 font-semibold text-slate-200">{file.originalFilename}</td>
                    <td className="p-4 font-mono">{(file.fileSize / (1024 * 1024)).toFixed(2)} MB</td>
                    <td className="p-4 font-mono text-emerald-400 truncate max-w-[150px]">{file.sha256Hash}</td>
                    <td className="p-4 text-slate-400">{new Date(file.createdAt).toLocaleString()}</td>
                    <td className="p-4 text-right">
                      <button
                        onClick={() => setSelectedShareFile(file)}
                        className="px-3 py-1 bg-emerald-500/10 hover:bg-emerald-500/20 text-emerald-400 border border-emerald-500/30 rounded-lg font-medium text-xs transition-colors inline-flex items-center space-x-1"
                      >
                        <Share2 className="w-3.5 h-3.5" />
                        <span>Create Share</span>
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      <FileUploadModal
        isOpen={isUploadOpen}
        onClose={() => setIsUploadOpen(false)}
        onSuccess={fetchData}
      />

      <CreateShareModal
        file={selectedShareFile}
        isOpen={!!selectedShareFile}
        onClose={() => setSelectedShareFile(null)}
        onSuccess={fetchData}
      />
    </div>
  );
};
