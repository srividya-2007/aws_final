import React from 'react';

export function StatCard({ icon: Icon, title, value, unit = '', color = '#2563eb', trend }) {
  return (
    <div className="stat-card">
      <div className="stat-card-icon" style={{ background: `${color}18`, color }}>
        <Icon size={22} />
      </div>
      <div className="stat-card-body">
        <p className="stat-card-title">{title}</p>
        <p className="stat-card-value">
          {value !== undefined && value !== null ? value : '—'}
          {unit && <span className="stat-card-unit"> {unit}</span>}
        </p>
        {trend && <p className="stat-card-trend">{trend}</p>}
      </div>
    </div>
  );
}

export function LoadingSpinner({ message = 'Loading…' }) {
  return (
    <div className="loading-center">
      <div className="spinner" />
      <p className="loading-msg">{message}</p>
    </div>
  );
}

export function ErrorAlert({ message, onRetry }) {
  return (
    <div className="error-alert">
      <span>⚠ {message}</span>
      {onRetry && (
        <button className="btn btn-sm btn-outline" onClick={onRetry}>
          Retry
        </button>
      )}
    </div>
  );
}

export function InfoAlert({ message }) {
  return <div className="info-alert">ℹ {message}</div>;
}

export function PageHeader({ title, subtitle, actions }) {
  return (
    <div className="page-header">
      <div>
        <h1 className="page-title">{title}</h1>
        {subtitle && <p className="page-subtitle">{subtitle}</p>}
      </div>
      {actions && <div className="page-header-actions">{actions}</div>}
    </div>
  );
}

export function Badge({ status }) {
  const map = {
    AVAILABLE:   { label: 'Available',   cls: 'badge-success' },
    MATCHED:     { label: 'Matched',     cls: 'badge-info'    },
    EXPIRED:     { label: 'Expired',     cls: 'badge-muted'   },
    PENDING:     { label: 'Pending',     cls: 'badge-warning' },
    COMPLETED:   { label: 'Completed',   cls: 'badge-success' },
    FAILED:      { label: 'Failed',      cls: 'badge-danger'  },
    SETTLED:     { label: 'Settled',     cls: 'badge-success' },
    PROSUMER:    { label: 'Prosumer',    cls: 'badge-green'   },
    ADMIN:       { label: 'Admin',       cls: 'badge-warning' },
    USER:        { label: 'Consumer',    cls: 'badge-info'    },
  };
  const entry = map[status] || { label: status, cls: 'badge-muted' };
  return <span className={`badge ${entry.cls}`}>{entry.label}</span>;
}

export function Modal({ open, title, onClose, children }) {
  if (!open) return null;
  return (
    <div className="modal-backdrop" onClick={onClose}>
      <div className="modal-box" onClick={(e) => e.stopPropagation()}>
        <div className="modal-header">
          <h3 className="modal-title">{title}</h3>
          <button className="modal-close" onClick={onClose}>✕</button>
        </div>
        <div className="modal-body">{children}</div>
      </div>
    </div>
  );
}
