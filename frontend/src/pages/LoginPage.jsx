import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import OtpVerification from '../components/OtpVerification';
import { Zap, Eye, EyeOff, Loader2 } from 'lucide-react';

export default function LoginPage() {
  const { login, verifyLoginOtp, resendOtp } = useAuth();
  const navigate = useNavigate();

  const [step, setStep] = useState('FORM'); // 'FORM' | 'OTP'
  const [form, setForm] = useState({ username: '', password: '' });
  const [otpInfo, setOtpInfo] = useState({ email: '', username: '' });
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const [showPw, setShowPw] = useState(false);

  const dashMap = {
    PROSUMER: '/prosumer/dashboard',
    ADMIN: '/admin/dashboard',
    USER: '/consumer/dashboard'
  };

  const handleChange = (e) => {
    setForm({ ...form, [e.target.name]: e.target.value });
    setError('');
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!form.username || !form.password) {
      setError('Both fields are required.');
      return;
    }
    setLoading(true);
    setError('');
    try {
      const res = await login(form.username, form.password);
      if (res && res.otpRequired) {
        setOtpInfo({ email: res.email, username: res.username });
        setStep('OTP');
      } else {
        // Direct login fallback
        navigate(dashMap[res] || '/consumer/dashboard');
      }
    } catch (err) {
      const msg = err.response?.data?.message || err.response?.data?.error || 'Invalid username/email or password.';
      setError(typeof msg === 'string' ? msg : 'Login failed. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  const handleVerifyOtp = async (otpCode) => {
    const role = await verifyLoginOtp({
      email: otpInfo.email,
      otpCode,
      username: otpInfo.username
    });
    navigate(dashMap[role] || '/consumer/dashboard');
  };

  const handleResendOtp = async () => {
    await resendOtp({
      email: otpInfo.email,
      purpose: 'LOGIN'
    });
  };

  return (
    <div className="auth-root">
      <div className="auth-left">
        <div className="auth-brand">
          <Zap size={36} className="auth-brand-icon" />
          <h1>EcoGrid</h1>
          <p>P2P Renewable Energy Trading Platform</p>
        </div>
        <div className="auth-left-art">
          <div className="energy-orb orb1" />
          <div className="energy-orb orb2" />
          <div className="energy-orb orb3" />
        </div>
      </div>

      <div className="auth-right">
        <div className="auth-card">
          {step === 'OTP' ? (
            <OtpVerification
              email={otpInfo.email}
              purpose="LOGIN"
              title="Security Verification"
              subtitle="Enter the 6-digit code sent to your registered email"
              onVerify={handleVerifyOtp}
              onResend={handleResendOtp}
              onBack={() => setStep('FORM')}
            />
          ) : (
            <>
              <div className="auth-card-header">
                <h2>Welcome back</h2>
                <p>Sign in to your EcoGrid account</p>
              </div>

              {error && <div className="form-error-banner">{error}</div>}

              <form onSubmit={handleSubmit} className="auth-form" noValidate>
                <div className="form-group">
                  <label htmlFor="login-username">Username or Email</label>
                  <input
                    id="login-username"
                    name="username"
                    type="text"
                    value={form.username}
                    onChange={handleChange}
                    placeholder="Enter username or email"
                    autoComplete="username"
                    autoFocus
                  />
                </div>

                <div className="form-group">
                  <label htmlFor="login-password">Password</label>
                  <div className="password-input-wrap">
                    <input
                      id="login-password"
                      name="password"
                      type={showPw ? 'text' : 'password'}
                      value={form.password}
                      onChange={handleChange}
                      placeholder="Enter password"
                      autoComplete="current-password"
                    />
                    <button
                      type="button"
                      className="pw-toggle-btn"
                      onClick={() => setShowPw(!showPw)}
                      aria-label="Toggle password visibility"
                    >
                      {showPw ? <EyeOff size={16} /> : <Eye size={16} />}
                    </button>
                  </div>
                </div>

                <button
                  type="submit"
                  className="btn btn-primary btn-block"
                  disabled={loading}
                >
                  {loading ? (
                    <><Loader2 size={16} className="spin" /> Verifying Credentials…</>
                  ) : (
                    'Sign In'
                  )}
                </button>
              </form>

              <div className="auth-link">
                Don't have an account? <Link to="/register">Create one</Link>
              </div>
            </>
          )}
        </div>
      </div>
    </div>
  );
}
