import React from 'react';
import { AlertCircle, CheckCircle2, Info, X } from 'lucide-react';

interface AlertProps {
  type?: 'success' | 'error' | 'warning' | 'info';
  title?: string;
  message: string;
  onClose?: () => void;
}

export const Alert: React.FC<AlertProps> = ({ type = 'info', title, message, onClose }) => {
  const styles = {
    success: 'bg-emerald-500/10 border-emerald-500/30 text-emerald-400',
    error: 'bg-rose-500/10 border-rose-500/30 text-rose-400',
    warning: 'bg-amber-500/10 border-amber-500/30 text-amber-400',
    info: 'bg-blue-500/10 border-blue-500/30 text-blue-400',
  };

  const icons = {
    success: CheckCircle2,
    error: AlertCircle,
    warning: AlertCircle,
    info: Info,
  };

  const Icon = icons[type];

  return (
    <div className={`p-4 rounded-xl border flex items-start justify-between ${styles[type]} mb-4`}>
      <div className="flex items-start space-x-3">
        <Icon className="w-5 h-5 mt-0.5 shrink-0" />
        <div>
          {title && <h4 className="font-semibold text-sm mb-0.5">{title}</h4>}
          <p className="text-xs leading-relaxed opacity-90">{message}</p>
        </div>
      </div>
      {onClose && (
        <button onClick={onClose} className="p-1 hover:opacity-75 transition-opacity">
          <X className="w-4 h-4" />
        </button>
      )}
    </div>
  );
};
