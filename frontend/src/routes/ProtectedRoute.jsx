import React from 'react';
import { Navigate, useLocation } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

// Redirect unauthenticated users to /login
export function ProtectedRoute({ children, allowedRoles }) {
  const { isAuthenticated, user, loading } = useAuth();
  const location = useLocation();

  if (loading) {
    return (
      <div className="splash-loading">
        <div className="spinner" />
        <span>Loading…</span>
      </div>
    );
  }

  if (!isAuthenticated) {
    return <Navigate to="/login" state={{ from: location }} replace />;
  }

  if (allowedRoles && !allowedRoles.includes(user.role)) {
    return <Navigate to="/access-denied" replace />;
  }

  return children;
}

// Redirect already-logged-in users away from auth pages
export function PublicRoute({ children }) {
  const { isAuthenticated, user, loading } = useAuth();

  if (loading) return null;

  if (isAuthenticated) {
    const dashMap = {
      PROSUMER: '/prosumer/dashboard',
      ADMIN: '/admin/dashboard',
      USER: '/consumer/dashboard',
    };
    return <Navigate to={dashMap[user.role] || '/consumer/dashboard'} replace />;
  }

  return children;
}
