import React from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider } from './context/AuthContext';
import { ProtectedRoute, PublicRoute } from './routes/ProtectedRoute';
import MainLayout from './layouts/MainLayout';

// Pages
import LandingPage       from './pages/LandingPage';
import LoginPage         from './pages/LoginPage';
import RegisterPage      from './pages/RegisterPage';
import ProsumerDashboard from './pages/prosumer/ProsumerDashboard';
import ConsumerDashboard from './pages/consumer/ConsumerDashboard';
import AdminDashboard    from './pages/admin/AdminDashboard';
import AdminUsersPage    from './pages/admin/AdminUsersPage';
import AdminStatsPage    from './pages/admin/AdminStatsPage';
import MarketplacePage   from './pages/MarketplacePage';
import TelemetryPage     from './pages/TelemetryPage';
import MatchingPage      from './pages/MatchingPage';
import SettlementsPage   from './pages/SettlementsPage';
import ProfilePage       from './pages/ProfilePage';
import { AccessDeniedPage, NotFoundPage } from './pages/ErrorPages';

function ProtectedLayout({ children, allowedRoles }) {
  return (
    <ProtectedRoute allowedRoles={allowedRoles}>
      <MainLayout>{children}</MainLayout>
    </ProtectedRoute>
  );
}

export default function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <Routes>
          {/* Public routes */}
          <Route path="/" element={<LandingPage />} />
          <Route path="/login"    element={<PublicRoute><LoginPage /></PublicRoute>} />
          <Route path="/register" element={<PublicRoute><RegisterPage /></PublicRoute>} />

          {/* Prosumer only */}
          <Route path="/prosumer/dashboard" element={
            <ProtectedLayout allowedRoles={['PROSUMER']}>
              <ProsumerDashboard />
            </ProtectedLayout>
          } />

          {/* Consumer/USER */}
          <Route path="/consumer/dashboard" element={
            <ProtectedLayout allowedRoles={['USER', 'PROSUMER']}>
              <ConsumerDashboard />
            </ProtectedLayout>
          } />

          {/* Admin */}
          <Route path="/admin/dashboard" element={
            <ProtectedLayout allowedRoles={['ADMIN']}>
              <AdminDashboard />
            </ProtectedLayout>
          } />
          <Route path="/admin/users" element={
            <ProtectedLayout allowedRoles={['ADMIN']}>
              <AdminUsersPage />
            </ProtectedLayout>
          } />
          <Route path="/admin/stats" element={
            <ProtectedLayout allowedRoles={['ADMIN']}>
              <AdminStatsPage />
            </ProtectedLayout>
          } />

          {/* Shared protected pages */}
          <Route path="/marketplace" element={
            <ProtectedLayout>
              <MarketplacePage />
            </ProtectedLayout>
          } />
          <Route path="/telemetry" element={
            <ProtectedLayout>
              <TelemetryPage />
            </ProtectedLayout>
          } />
          <Route path="/matching" element={
            <ProtectedLayout>
              <MatchingPage />
            </ProtectedLayout>
          } />
          <Route path="/settlements" element={
            <ProtectedLayout>
              <SettlementsPage />
            </ProtectedLayout>
          } />
          <Route path="/profile" element={
            <ProtectedLayout>
              <ProfilePage />
            </ProtectedLayout>
          } />

          {/* Error pages */}
          <Route path="/access-denied" element={<AccessDeniedPage />} />
          <Route path="*" element={<NotFoundPage />} />
        </Routes>
      </AuthProvider>
    </BrowserRouter>
  );
}
