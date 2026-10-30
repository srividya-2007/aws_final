import React from 'react';
import { Link } from 'react-router-dom';
import { ShieldOff } from 'lucide-react';

export function AccessDeniedPage() {
  return (
    <div className="error-page">
      <ShieldOff size={64} className="error-page-icon" />
      <h1>403 — Access Denied</h1>
      <p>You don't have permission to view this page.</p>
      <Link to="/" className="btn btn-primary">Go Home</Link>
    </div>
  );
}

export function NotFoundPage() {
  return (
    <div className="error-page">
      <span className="error-page-icon" style={{ fontSize: 64 }}>🔌</span>
      <h1>404 — Page Not Found</h1>
      <p>The page you're looking for doesn't exist.</p>
      <Link to="/" className="btn btn-primary">Go Home</Link>
    </div>
  );
}
