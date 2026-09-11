import React from 'react';
import { NavLink } from 'react-router-dom';
import { LayoutDashboard, HardDrive, Share2, ShieldAlert } from 'lucide-react';
import { useAuth } from '../context/AuthContext';

export const Sidebar: React.FC = () => {
  const { user } = useAuth();

  const navItems = [
    { to: '/dashboard', label: 'Dashboard', icon: LayoutDashboard },
    { to: '/files', label: 'My Encrypted Files', icon: HardDrive },
    { to: '/shares', label: 'Active Share Links', icon: Share2 },
  ];

  if (user?.role === 'ADMIN') {
    navItems.push({ to: '/admin', label: 'Security Admin', icon: ShieldAlert });
  }

  return (
    <aside className="w-64 border-r border-slate-800 bg-slate-950 min-h-[calc(100vh-4rem)] p-4 flex flex-col justify-between">
      <nav className="space-y-1">
        {navItems.map((item) => {
          const Icon = item.icon;
          return (
            <NavLink
              key={item.to}
              to={item.to}
              className={({ isActive }) =>
                `flex items-center space-x-3 px-3 py-2.5 rounded-lg text-sm font-medium transition-all ${
                  isActive
                    ? 'bg-emerald-500/10 text-emerald-400 border border-emerald-500/30'
                    : 'text-slate-400 hover:text-slate-200 hover:bg-slate-900/60'
                }`
              }
            >
              <Icon className="w-5 h-5" />
              <span>{item.label}</span>
            </NavLink>
          );
        })}
      </nav>

      <div className="p-3 bg-slate-900/40 border border-slate-800/80 rounded-xl">
        <div className="flex items-center space-x-2 text-xs font-mono text-emerald-400 mb-1">
          <span className="w-2 h-2 rounded-full bg-emerald-400 animate-pulse"></span>
          <span>Zero-Knowledge Server</span>
        </div>
        <p className="text-[11px] text-slate-400 leading-relaxed">
          Files are encrypted client-side using Web Crypto API. Server stores only ciphertext.
        </p>
      </div>
    </aside>
  );
};
