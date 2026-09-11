import React, { useEffect, useState } from 'react';
import { Users, HardDrive, Share2, ShieldAlert, Activity } from 'lucide-react';
import { AdminStats, User, FileItem, ShareLink, AuditLog } from '../types';
import api from '../services/api';

export const AdminDashboardPage: React.FC = () => {
  const [stats, setStats] = useState<AdminStats | null>(null);
  const [users, setUsers] = useState<User[]>([]);
  const [files, setFiles] = useState<FileItem[]>([]);
  const [shares, setShares] = useState<ShareLink[]>([]);
  const [auditLogs, setAuditLogs] = useState<AuditLog[]>([]);
  const [loading, setLoading] = useState<boolean>(true);
  const [activeTab, setActiveTab] = useState<'users' | 'files' | 'shares' | 'audit'>('audit');

  useEffect(() => {
    const fetchAdminData = async () => {
      try {
        setLoading(true);
        const [statsRes, usersRes, filesRes, sharesRes, auditRes] = await Promise.all([
          api.get<AdminStats>('/admin/stats'),
          api.get<User[]>('/admin/users'),
          api.get<FileItem[]>('/admin/files'),
          api.get<ShareLink[]>('/admin/shares'),
          api.get<AuditLog[]>('/admin/audit-logs'),
        ]);

        setStats(statsRes.data);
        setUsers(usersRes.data);
        setFiles(filesRes.data);
        setShares(sharesRes.data);
        setAuditLogs(auditRes.data);
      } catch (err) {
        console.error('Error loading admin dashboard data:', err);
      } finally {
        setLoading(false);
      }
    };
    fetchAdminData();
  }, []);

  if (loading) {
    return <div className="p-8 text-center text-xs font-mono text-slate-500">Loading admin security metrics...</div>;
  }

  return (
    <div className="space-y-8">
      <div>
        <h1 className="text-2xl font-bold text-slate-100 flex items-center space-x-3">
          <ShieldAlert className="w-7 h-7 text-purple-400" />
          <span>Security Administration</span>
        </h1>
        <p className="text-xs text-slate-400">System audit logs, users, encrypted files, and active share policies</p>
      </div>

      {/* Metrics Cards */}
      {stats && (
        <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
          <div className="glass-card p-4 rounded-xl border border-slate-800">
            <p className="text-xs text-slate-400 uppercase font-mono">Total Users</p>
            <h3 className="text-2xl font-bold text-slate-100">{stats.totalUsers}</h3>
          </div>
          <div className="glass-card p-4 rounded-xl border border-slate-800">
            <p className="text-xs text-slate-400 uppercase font-mono">Encrypted Files</p>
            <h3 className="text-2xl font-bold text-emerald-400">{stats.totalFiles}</h3>
          </div>
          <div className="glass-card p-4 rounded-xl border border-slate-800">
            <p className="text-xs text-slate-400 uppercase font-mono">Active Shares</p>
            <h3 className="text-2xl font-bold text-blue-400">{stats.activeShareLinks}</h3>
          </div>
          <div className="glass-card p-4 rounded-xl border border-slate-800">
            <p className="text-xs text-slate-400 uppercase font-mono">Downloads</p>
            <h3 className="text-2xl font-bold text-purple-400">{stats.totalSuccessfulDownloads}</h3>
          </div>
        </div>
      )}

      {/* Admin Navigation Tabs */}
      <div className="flex space-x-2 border-b border-slate-800 pb-2">
        {[
          { id: 'audit', label: 'Security Audit Logs', icon: Activity },
          { id: 'users', label: 'User Directory', icon: Users },
          { id: 'files', label: 'All Encrypted Files', icon: HardDrive },
          { id: 'shares', label: 'All Share Links', icon: Share2 },
        ].map((tab) => {
          const Icon = tab.icon;
          return (
            <button
              key={tab.id}
              onClick={() => setActiveTab(tab.id as 'users' | 'files' | 'shares' | 'audit')}
              className={`px-4 py-2 rounded-lg text-xs font-semibold flex items-center space-x-2 transition-all ${
                activeTab === tab.id
                  ? 'bg-purple-500/20 text-purple-300 border border-purple-500/30'
                  : 'text-slate-400 hover:text-slate-200 hover:bg-slate-900/60'
              }`}
            >
              <Icon className="w-4 h-4" />
              <span>{tab.label}</span>
            </button>
          );
        })}
      </div>

      {/* Audit Logs View */}
      {activeTab === 'audit' && (
        <div className="glass-card rounded-2xl border border-slate-800 overflow-hidden">
          <div className="p-4 border-b border-slate-800 font-bold text-sm text-slate-200">
            System Security Audit Logs
          </div>
          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs text-slate-300">
              <thead className="bg-slate-900/60 uppercase font-mono text-[10px] text-slate-400">
                <tr>
                  <th className="p-4">Timestamp</th>
                  <th className="p-4">Event Type</th>
                  <th className="p-4">Resource</th>
                  <th className="p-4">IP Hash</th>
                  <th className="p-4">Metadata</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-800/60 font-mono">
                {auditLogs.map((log) => (
                  <tr key={log.id} className="hover:bg-slate-900/40">
                    <td className="p-4 text-slate-400">{new Date(log.timestamp).toLocaleString()}</td>
                    <td className="p-4">
                      <span className="px-2 py-0.5 rounded bg-purple-500/10 text-purple-400 border border-purple-500/20 text-[10px]">
                        {log.eventType}
                      </span>
                    </td>
                    <td className="p-4 text-slate-300">{log.resourceType}: {log.resourceId}</td>
                    <td className="p-4 text-slate-500 truncate max-w-[120px]">{log.ipHash || 'N/A'}</td>
                    <td className="p-4 text-slate-400 font-sans">{log.metadata}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {/* Users View */}
      {activeTab === 'users' && (
        <div className="glass-card rounded-2xl border border-slate-800 overflow-hidden">
          <div className="p-4 border-b border-slate-800 font-bold text-sm text-slate-200">
            User Directory
          </div>
          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs text-slate-300">
              <thead className="bg-slate-900/60 uppercase font-mono text-[10px] text-slate-400">
                <tr>
                  <th className="p-4">Display Name</th>
                  <th className="p-4">Email</th>
                  <th className="p-4">Role</th>
                  <th className="p-4">Created At</th>
                  <th className="p-4">Last Login</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-800/60">
                {users.map((user) => (
                  <tr key={user.uuid} className="hover:bg-slate-900/40">
                    <td className="p-4 font-semibold text-slate-200">{user.displayName}</td>
                    <td className="p-4 font-mono text-slate-400">{user.email}</td>
                    <td className="p-4">
                      <span className={`px-2 py-0.5 rounded text-[10px] font-mono ${user.role === 'ADMIN' ? 'bg-purple-500/20 text-purple-300 border border-purple-500/30' : 'bg-slate-800 text-slate-300'}`}>
                        {user.role}
                      </span>
                    </td>
                    <td className="p-4 text-slate-400">{new Date(user.createdAt).toLocaleString()}</td>
                    <td className="p-4 text-slate-400">{user.lastLoginAt ? new Date(user.lastLoginAt).toLocaleString() : 'Never'}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {/* Files View */}
      {activeTab === 'files' && (
        <div className="glass-card rounded-2xl border border-slate-800 overflow-hidden">
          <div className="p-4 border-b border-slate-800 font-bold text-sm text-slate-200">
            Encrypted File Inventory
          </div>
          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs text-slate-300">
              <thead className="bg-slate-900/60 uppercase font-mono text-[10px] text-slate-400">
                <tr>
                  <th className="p-4">Filename</th>
                  <th className="p-4">Owner Email</th>
                  <th className="p-4">Size</th>
                  <th className="p-4">SHA-256 Digest</th>
                  <th className="p-4">Uploaded</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-800/60 font-mono">
                {files.map((file) => (
                  <tr key={file.uuid} className="hover:bg-slate-900/40">
                    <td className="p-4 font-semibold text-slate-200 font-sans">{file.originalFilename}</td>
                    <td className="p-4 text-slate-400">{file.ownerEmail}</td>
                    <td className="p-4">{(file.fileSize / (1024 * 1024)).toFixed(2)} MB</td>
                    <td className="p-4 text-emerald-400 truncate max-w-[150px]">{file.sha256Hash}</td>
                    <td className="p-4 text-slate-400 font-sans">{new Date(file.createdAt).toLocaleString()}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {/* Shares View */}
      {activeTab === 'shares' && (
        <div className="glass-card rounded-2xl border border-slate-800 overflow-hidden">
          <div className="p-4 border-b border-slate-800 font-bold text-sm text-slate-200">
            System Share Links Policy Overview
          </div>
          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs text-slate-300">
              <thead className="bg-slate-900/60 uppercase font-mono text-[10px] text-slate-400">
                <tr>
                  <th className="p-4">Target File</th>
                  <th className="p-4">Downloads</th>
                  <th className="p-4">Expires</th>
                  <th className="p-4">Password</th>
                  <th className="p-4">Status</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-800/60">
                {shares.map((share) => (
                  <tr key={share.uuid} className="hover:bg-slate-900/40">
                    <td className="p-4 font-semibold text-slate-200">{share.originalFilename}</td>
                    <td className="p-4 font-mono">{share.downloadCount} / {share.maxDownloads}</td>
                    <td className="p-4 font-mono text-slate-400">{new Date(share.expiresAt).toLocaleString()}</td>
                    <td className="p-4">{share.passwordProtected ? 'Yes' : 'No'}</td>
                    <td className="p-4 font-mono text-[10px]">
                      {share.revoked ? (
                        <span className="text-rose-400">REVOKED</span>
                      ) : share.expired ? (
                        <span className="text-amber-400">EXPIRED</span>
                      ) : (
                        <span className="text-emerald-400">ACTIVE</span>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}
    </div>
  );
};
