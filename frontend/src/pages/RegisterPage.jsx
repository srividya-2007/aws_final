import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { Zap, Eye, EyeOff, Loader2, CheckCircle } from 'lucide-react';

const ROLES = [
  { value: 'USER',     label: 'Consumer (USER)',   desc: 'Browse and buy energy from listings',      color: '#2563eb' },
  { value: 'PROSUMER', label: 'Prosumer',           desc: 'Generate solar power and sell surplus',   color: '#16a34a' },
  { value: 'ADMIN',    label: 'Administrator',      desc: 'Manage platform, users and transactions', color: '#d97706' },
];

export default function RegisterPage() {
  const { register } = useAuth();
  const navigate      = useNavigate();

  const [form, setForm] = useState({
    username: '', email: '', password: '', confirmPassword: '', role: 'USER'
  });
  const [errors, setErrors]   = useState({});
  const [loading, setLoading] = useState(false);
  const [success, setSuccess] = useState('');
  const [showPw, setShowPw]   = useState(false);

  const validate = () => {
    const e = {};
    if (!form.username || form.username.length < 3)
      e.username = 'Username must be at least 3 characters.';
    if (!form.email || !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.email))
      e.email = 'Enter a valid email address.';
    if (!form.password || form.password.length < 6)
      e.password = 'Password must be at least 6 characters.';
    if (form.password !== form.confirmPassword)
      e.confirmPassword = 'Passwords do not match.';
    return e;
  };

  const handleChange = (e) => {
    setForm({ ...form, [e.target.name]: e.target.value });
    setErrors({ ...errors, [e.target.name]: '' });
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    const v = validate();
    if (Object.keys(v).length) { setErrors(v); return; }
    setLoading(true);
    try {
      const { username, email, password, role } = form;
      await register({ username, email, password, role });
      const roleLabel = ROLES.find(r => r.value === role)?.label || role;
      setSuccess(`Account created as ${roleLabel}! Redirecting to login…`);
      setTimeout(() => navigate('/login'), 2000);
    } catch (err) {
      const msg = err.response?.data?.message || err.response?.data || 'Registration failed.';
      setErrors({ form: typeof msg === 'string' ? msg : 'Registration failed. Try again.' });
    } finally {
      setLoading(false);
    }
  };

  const selectedRole = ROLES.find(r => r.value === form.role);

  return (
    <div className="auth-root">
      <div className="auth-left">
        <div className="auth-brand">
          <Zap size={36} className="auth-brand-icon" />
          <h1>EcoGrid</h1>
          <p>Join the clean energy revolution</p>
        </div>
        <div className="auth-left-art">
          <div className="energy-orb orb1" />
          <div className="energy-orb orb2" />
          <div className="energy-orb orb3" />
        </div>
      </div>

      <div className="auth-right">
        <div className="auth-card">
          <div className="auth-card-header">
            <h2>Create Account</h2>
            <p>Start trading renewable energy today</p>
          </div>

          {success && (
            <div className="form-success-banner">
              <CheckCircle size={16} /> {success}
            </div>
          )}
          {errors.form && <div className="form-error-banner">{errors.form}</div>}

          <form onSubmit={handleSubmit} className="auth-form" noValidate>

            {/* Role selector */}
            <div className="form-group">
              <label>Account Role</label>
              <div className="role-selector">
                {ROLES.map(r => (
                  <label
                    key={r.value}
                    className={`role-option ${form.role === r.value ? 'role-option-active' : ''}`}
                    style={form.role === r.value ? { borderColor: r.color, background: `${r.color}10` } : {}}
                  >
                    <input
                      type="radio"
                      name="role"
                      value={r.value}
                      checked={form.role === r.value}
                      onChange={handleChange}
                    />
                    <div className="role-dot" style={{ background: r.color }} />
                    <div className="role-info">
                      <span className="role-label" style={form.role === r.value ? { color: r.color } : {}}>
                        {r.label}
                      </span>
                      <span className="role-desc">{r.desc}</span>
                    </div>
                  </label>
                ))}
              </div>
              {selectedRole && (
                <div className="role-selected-note" style={{ borderColor: selectedRole.color, color: selectedRole.color }}>
                  You are registering as: <strong>{selectedRole.label}</strong>
                </div>
              )}
            </div>

            <div className="form-group">
              <label htmlFor="reg-username">Username</label>
              <input
                id="reg-username"
                name="username"
                type="text"
                value={form.username}
                onChange={handleChange}
                placeholder="Choose a username (min 3 chars)"
                autoComplete="username"
              />
              {errors.username && <span className="field-error">{errors.username}</span>}
            </div>

            <div className="form-group">
              <label htmlFor="reg-email">Email Address</label>
              <input
                id="reg-email"
                name="email"
                type="email"
                value={form.email}
                onChange={handleChange}
                placeholder="you@example.com"
                autoComplete="email"
              />
              {errors.email && <span className="field-error">{errors.email}</span>}
            </div>

            <div className="form-group">
              <label htmlFor="reg-password">Password</label>
              <div className="input-password-wrap">
                <input
                  id="reg-password"
                  name="password"
                  type={showPw ? 'text' : 'password'}
                  value={form.password}
                  onChange={handleChange}
                  placeholder="Min 6 characters"
                  autoComplete="new-password"
                />
                <button type="button" className="pw-toggle" onClick={() => setShowPw(!showPw)}>
                  {showPw ? <EyeOff size={16} /> : <Eye size={16} />}
                </button>
              </div>
              {errors.password && <span className="field-error">{errors.password}</span>}
            </div>

            <div className="form-group">
              <label htmlFor="reg-confirm">Confirm Password</label>
              <input
                id="reg-confirm"
                name="confirmPassword"
                type="password"
                value={form.confirmPassword}
                onChange={handleChange}
                placeholder="Repeat your password"
                autoComplete="new-password"
              />
              {errors.confirmPassword && <span className="field-error">{errors.confirmPassword}</span>}
            </div>

            <button type="submit" className="btn btn-primary btn-full" disabled={loading}>
              {loading
                ? <><Loader2 size={16} className="spin" /> Creating account…</>
                : `Create ${selectedRole?.label || 'Account'}`}
            </button>
          </form>

          <p className="auth-link">
            Already have an account? <Link to="/login">Sign in</Link>
          </p>
        </div>
      </div>
    </div>
  );
}
