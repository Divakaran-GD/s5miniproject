import React, { useEffect, useState } from 'react';
import { Share2, Ban } from 'lucide-react';
import { ShareLink } from '../types';
import api from '../services/api';

export const SharesPage: React.FC = () => {
  const [shares, setShares] = useState<ShareLink[]>([]);
  const [loading, setLoading] = useState<boolean>(true);

  const fetchShares = async () => {
    try {
      setLoading(true);
      const res = await api.get<ShareLink[]>('/shares');
      setShares(res.data);
    } catch (err) {
      console.error('Error fetching share links:', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchShares();
  }, []);

  const handleRevoke = async (uuid: string) => {
    if (!window.confirm('Are you sure you want to revoke this share link immediately?')) return;
    try {
      await api.delete(`/shares/${uuid}`);
      fetchShares();
    } catch (err) {
      alert('Failed to revoke share link');
    }
  };

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-slate-100">Share Links Management</h1>
        <p className="text-xs text-slate-400">Track downloads, expiration policies, and manual revocation</p>
      </div>

      <div className="glass-card rounded-2xl border border-slate-800 overflow-hidden">
        {loading ? (
          <div className="p-8 text-center text-xs font-mono text-slate-500">Loading share links...</div>
        ) : shares.length === 0 ? (
          <div className="p-12 text-center space-y-3">
            <Share2 className="w-10 h-10 text-slate-600 mx-auto" />
            <p className="text-sm text-slate-400">No share links generated yet.</p>
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs text-slate-300">
              <thead className="bg-slate-900/60 uppercase font-mono text-[10px] text-slate-400">
                <tr>
                  <th className="p-4">Target File</th>
                  <th className="p-4">Downloads</th>
                  <th className="p-4">Expiration</th>
                  <th className="p-4">Protection</th>
                  <th className="p-4">Status</th>
                  <th className="p-4">Created At</th>
                  <th className="p-4 text-right">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-800/60">
                {shares.map((share) => (
                  <tr key={share.uuid} className="hover:bg-slate-900/40">
                      <td className="p-4 font-semibold text-slate-200">{share.originalFilename}</td>
                      <td className="p-4 font-mono">
                        <span className={share.downloadCount >= share.maxDownloads ? 'text-rose-400 font-bold' : 'text-slate-200'}>
                          {share.downloadCount}
                        </span>{' '}
                        / {share.maxDownloads}
                      </td>
                      <td className="p-4 text-slate-400">{new Date(share.expiresAt).toLocaleString()}</td>
                      <td className="p-4">
                        {share.passwordProtected ? (
                          <span className="px-2 py-0.5 rounded bg-emerald-500/10 text-emerald-400 border border-emerald-500/20 font-mono text-[10px]">
                            Password Protected
                          </span>
                        ) : (
                          <span className="text-slate-500">None</span>
                        )}
                      </td>
                      <td className="p-4">
                        {share.revoked ? (
                          <span className="px-2 py-0.5 rounded bg-rose-500/10 text-rose-400 border border-rose-500/20 font-mono text-[10px]">
                            REVOKED
                          </span>
                        ) : share.expired ? (
                          <span className="px-2 py-0.5 rounded bg-amber-500/10 text-amber-400 border border-amber-500/20 font-mono text-[10px]">
                            EXPIRED
                          </span>
                        ) : share.downloadCount >= share.maxDownloads ? (
                          <span className="px-2 py-0.5 rounded bg-rose-500/10 text-rose-400 border border-rose-500/20 font-mono text-[10px]">
                            LIMIT REACHED
                          </span>
                        ) : (
                          <span className="px-2 py-0.5 rounded bg-emerald-500/10 text-emerald-400 border border-emerald-500/20 font-mono text-[10px]">
                            ACTIVE
                          </span>
                        )}
                      </td>
                      <td className="p-4 text-slate-400">{new Date(share.createdAt).toLocaleString()}</td>
                      <td className="p-4 text-right">
                        {!share.revoked && (
                          <button
                            onClick={() => handleRevoke(share.uuid)}
                            className="px-3 py-1 bg-rose-500/10 hover:bg-rose-500/20 text-rose-400 border border-rose-500/30 rounded-lg font-medium text-xs transition-colors inline-flex items-center space-x-1"
                          >
                            <Ban className="w-3.5 h-3.5" />
                            <span>Revoke</span>
                          </button>
                        )}
                      </td>
                    </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  );
};
