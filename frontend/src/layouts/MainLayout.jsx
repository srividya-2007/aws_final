import React, { useState } from 'react';
import { NavLink, useNavigate, useLocation } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import {
  Zap, LayoutDashboard, Activity, ShoppingCart, Layers,
  CreditCard, Users, BarChart2, User, LogOut, Menu, X, ChevronRight
} from 'lucide-react';

function buildNavLinks(role) {
  const common = [
    { to: '/marketplace',  label: 'Marketplace',  icon: ShoppingCart },
    { to: '/telemetry',    label: 'Telemetry',    icon: Activity },
    { to: '/matching',     label: 'Matching',     icon: Layers },
    { to: '/settlements',  label: 'Settlements',  icon: CreditCard },
    { to: '/profile',      label: 'My Profile',   icon: User },
  ];

  if (role === 'PROSUMER') {
    return [
      { to: '/prosumer/dashboard', label: 'Dashboard',  icon: LayoutDashboard },
      ...common,
    ];
  }
  if (role === 'ADMIN') {
    return [
      { to: '/admin/dashboard',  label: 'Dashboard',  icon: LayoutDashboard },
      { to: '/admin/users',      label: 'Users',       icon: Users },
      { to: '/admin/stats',      label: 'Statistics',  icon: BarChart2 },
      ...common,
    ];
  }
  // USER / CONSUMER / OBSERVER
  return [
    { to: '/consumer/dashboard', label: 'Dashboard',  icon: LayoutDashboard },
    ...common,
  ];
}

export default function MainLayout({ children }) {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const [collapsed, setCollapsed] = useState(false);
  const [mobileOpen, setMobileOpen] = useState(false);

  const navLinks = buildNavLinks(user?.role);

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  const roleBadge = {
    PROSUMER: { label: 'Prosumer', color: '#22c55e' },
    ADMIN:    { label: 'Admin',    color: '#f59e0b' },
    USER:     { label: 'Consumer', color: '#3b82f6' },
  }[user?.role] || { label: user?.role, color: '#6b7280' };

  return (
    <div className={`layout-root ${collapsed ? 'sidebar-collapsed' : ''}`}>
      {/* Overlay for mobile */}
      {mobileOpen && (
        <div className="sidebar-overlay" onClick={() => setMobileOpen(false)} />
      )}

      {/* ─── Sidebar ─────────────────────────────── */}
      <aside className={`sidebar ${mobileOpen ? 'sidebar-mobile-open' : ''}`}>
        {/* Logo */}
        <div className="sidebar-logo">
          <div className="logo-icon"><Zap size={22} /></div>
          {!collapsed && <span className="logo-text">EcoGrid</span>}
        </div>

        {/* Toggle (desktop) */}
        <button className="sidebar-toggle" onClick={() => setCollapsed(!collapsed)} title="Toggle sidebar">
          <ChevronRight size={16} className={collapsed ? '' : 'rotate-180'} />
        </button>

        {/* Navigation */}
        <nav className="sidebar-nav">
          {navLinks.map(({ to, label, icon: Icon }) => (
            <NavLink
              key={to}
              to={to}
              className={({ isActive }) => `nav-item ${isActive ? 'nav-item-active' : ''}`}
              title={collapsed ? label : ''}
            >
              <Icon size={18} className="nav-icon" />
              {!collapsed && <span className="nav-label">{label}</span>}
            </NavLink>
          ))}
        </nav>

        {/* User footer */}
        <div className="sidebar-footer">
          {!collapsed && (
            <div className="sidebar-user-info">
              <div className="user-avatar">{user?.username?.[0]?.toUpperCase()}</div>
              <div className="user-meta">
                <span className="user-name">{user?.username}</span>
                <span className="user-role-badge" style={{ background: roleBadge.color }}>
                  {roleBadge.label}
                </span>
              </div>
            </div>
          )}
          <button className="logout-btn" onClick={handleLogout} title="Logout">
            <LogOut size={18} />
            {!collapsed && <span>Logout</span>}
          </button>
        </div>
      </aside>

      {/* ─── Main content ─────────────────────────── */}
      <div className="main-content">
        {/* Top bar */}
        <header className="topbar">
          <button className="mobile-menu-btn" onClick={() => setMobileOpen(true)}>
            <Menu size={20} />
          </button>
          <div className="topbar-title">EcoGrid P2P Energy Trading</div>
          <div className="topbar-right">
            <div className="topbar-user">
              <span className="topbar-username">{user?.username}</span>
              <span className="topbar-role-pill" style={{ background: roleBadge.color }}>
                {roleBadge.label}
              </span>
            </div>
          </div>
        </header>

        <main className="page-content">
          {children}
        </main>
      </div>
    </div>
  );
}
