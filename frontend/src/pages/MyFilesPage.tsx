import React, { useEffect, useState } from 'react';
import { HardDrive, Upload, Share2, Trash2, Search } from 'lucide-react';
import { FileItem } from '../types';
import { FileUploadModal } from '../components/FileUploadModal';
import { CreateShareModal } from '../components/CreateShareModal';
import api from '../services/api';

export const MyFilesPage: React.FC = () => {
  const [files, setFiles] = useState<FileItem[]>([]);
  const [loading, setLoading] = useState<boolean>(true);
  const [search, setSearch] = useState<string>('');
  const [isUploadOpen, setIsUploadOpen] = useState<boolean>(false);
  const [selectedShareFile, setSelectedShareFile] = useState<FileItem | null>(null);

  const fetchFiles = async () => {
    try {
      setLoading(true);
      const res = await api.get<FileItem[]>('/files');
      setFiles(res.data);
    } catch (err) {
      console.error('Error fetching files:', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchFiles();
  }, []);

  const handleDelete = async (uuid: string) => {
    if (!window.confirm('Are you sure you want to delete this file blob permanently?')) return;
    try {
      await api.delete(`/files/${uuid}`);
      fetchFiles();
    } catch (err) {
      alert('Failed to delete file');
    }
  };

  const filteredFiles = files.filter(f =>
    f.originalFilename.toLowerCase().includes(search.toLowerCase()) ||
    f.sha256Hash.toLowerCase().includes(search.toLowerCase())
  );

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-slate-100">My Encrypted Files</h1>
          <p className="text-xs text-slate-400">Encrypted blobs stored on server (AES-256-GCM)</p>
        </div>
        <button
          onClick={() => setIsUploadOpen(true)}
          className="px-4 py-2 bg-emerald-500 hover:bg-emerald-600 text-white rounded-xl font-semibold text-sm flex items-center space-x-2 transition-colors shadow-lg shadow-emerald-500/20"
        >
          <Upload className="w-4 h-4" />
          <span>Upload File</span>
        </button>
      </div>

      <div className="glass-card p-4 rounded-xl border border-slate-800 flex items-center space-x-3">
        <Search className="w-5 h-5 text-slate-500" />
        <input
          type="text"
          placeholder="Search files by filename or SHA-256 checksum..."
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          className="w-full bg-transparent border-none text-sm text-slate-100 placeholder-slate-500 focus:outline-none"
        />
      </div>

      <div className="glass-card rounded-2xl border border-slate-800 overflow-hidden">
        {loading ? (
          <div className="p-8 text-center text-xs font-mono text-slate-500">Loading files...</div>
        ) : filteredFiles.length === 0 ? (
          <div className="p-12 text-center space-y-3">
            <HardDrive className="w-10 h-10 text-slate-600 mx-auto" />
            <p className="text-sm text-slate-400">No files found.</p>
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs text-slate-300">
              <thead className="bg-slate-900/60 uppercase font-mono text-[10px] text-slate-400">
                <tr>
                  <th className="p-4">Filename</th>
                  <th className="p-4">MIME Type</th>
                  <th className="p-4">Size</th>
                  <th className="p-4">Algorithm</th>
                  <th className="p-4">SHA-256 Digest</th>
                  <th className="p-4">Upload Date</th>
                  <th className="p-4 text-right">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-800/60">
                {filteredFiles.map((file) => (
                  <tr key={file.uuid} className="hover:bg-slate-900/40">
                    <td className="p-4 font-semibold text-slate-200">{file.originalFilename}</td>
                    <td className="p-4 font-mono text-slate-400">{file.mimeType}</td>
                    <td className="p-4 font-mono">{(file.fileSize / (1024 * 1024)).toFixed(2)} MB</td>
                    <td className="p-4 font-mono text-emerald-400">{file.encryptionAlgorithm}</td>
                    <td className="p-4 font-mono text-emerald-400 truncate max-w-[140px]">{file.sha256Hash}</td>
                    <td className="p-4 text-slate-400">{new Date(file.createdAt).toLocaleString()}</td>
                    <td className="p-4 text-right space-x-2">
                      <button
                        onClick={() => setSelectedShareFile(file)}
                        className="px-2.5 py-1 bg-emerald-500/10 hover:bg-emerald-500/20 text-emerald-400 border border-emerald-500/30 rounded-lg font-medium transition-colors"
                        title="Create Share Link"
                      >
                        <Share2 className="w-3.5 h-3.5" />
                      </button>
                      <button
                        onClick={() => handleDelete(file.uuid)}
                        className="px-2.5 py-1 bg-rose-500/10 hover:bg-rose-500/20 text-rose-400 border border-rose-500/30 rounded-lg font-medium transition-colors"
                        title="Delete File"
                      >
                        <Trash2 className="w-3.5 h-3.5" />
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
        onSuccess={fetchFiles}
      />

      <CreateShareModal
        file={selectedShareFile}
        isOpen={!!selectedShareFile}
        onClose={() => setSelectedShareFile(null)}
        onSuccess={fetchFiles}
      />
    </div>
  );
};
