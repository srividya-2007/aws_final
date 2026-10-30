import React, { useState, useEffect, useRef } from 'react';
import { ShieldCheck, RefreshCw, Loader2, ArrowLeft, AlertCircle, CheckCircle2 } from 'lucide-react';

export default function OtpVerification({
  email,
  purpose = 'SIGNUP',
  onVerify,
  onResend,
  onBack,
  title = 'Email Verification',
  subtitle = 'Enter the 6-digit code sent to your email'
}) {
  const [digits, setDigits] = useState(['', '', '', '', '', '']);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [expirySeconds, setExpirySeconds] = useState(300); // 5 minutes
  const [cooldown, setCooldown] = useState(30); // 30 seconds resend cooldown
  const [resending, setResending] = useState(false);

  const inputRefs = useRef([]);

  // 5-minute countdown timer
  useEffect(() => {
    if (expirySeconds <= 0) return;
    const timer = setInterval(() => {
      setExpirySeconds(prev => Math.max(0, prev - 1));
    }, 1000);
    return () => clearInterval(timer);
  }, [expirySeconds]);

  // 30-second resend cooldown timer
  useEffect(() => {
    if (cooldown <= 0) return;
    const timer = setInterval(() => {
      setCooldown(prev => Math.max(0, prev - 1));
    }, 1000);
    return () => clearInterval(timer);
  }, [cooldown]);

  // Focus first input on mount
  useEffect(() => {
    inputRefs.current[0]?.focus();
  }, []);

  const formatTimer = (sec) => {
    const m = Math.floor(sec / 60);
    const s = sec % 60;
    return `${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}`;
  };

  const handleDigitChange = (index, val) => {
    // Only accept numeric characters
    const clean = val.replace(/\D/g, '');
    if (!clean) {
      const next = [...digits];
      next[index] = '';
      setDigits(next);
      return;
    }

    // Handle single character
    const char = clean.slice(-1);
    const next = [...digits];
    next[index] = char;
    setDigits(next);
    setError('');

    // Auto-advance to next box
    if (index < 5 && char) {
      inputRefs.current[index + 1]?.focus();
    }
  };

  const handleKeyDown = (index, e) => {
    if (e.key === 'Backspace' && !digits[index] && index > 0) {
      inputRefs.current[index - 1]?.focus();
    }
  };

  const handlePaste = (e) => {
    e.preventDefault();
    const pasted = e.clipboardData.getData('text').replace(/\D/g, '').slice(0, 6);
    if (!pasted) return;
    const next = [...digits];
    for (let i = 0; i < pasted.length; i++) {
      next[i] = pasted[i];
    }
    setDigits(next);
    setError('');
    const nextFocus = Math.min(pasted.length, 5);
    inputRefs.current[nextFocus]?.focus();
  };

  const handleSubmit = async (e) => {
    e?.preventDefault();
    const otpCode = digits.join('');
    if (otpCode.length !== 6) {
      setError('Please enter all 6 digits of the verification code.');
      return;
    }
    if (expirySeconds <= 0) {
      setError('This verification code has expired. Please request a new code.');
      return;
    }

    setLoading(true);
    setError('');
    try {
      await onVerify(otpCode);
    } catch (err) {
      const msg = err.response?.data?.message || err.response?.data?.error || 'Verification failed. Please try again.';
      setError(typeof msg === 'string' ? msg : 'Verification failed.');
    } finally {
      setLoading(false);
    }
  };

  const handleResend = async () => {
    if (cooldown > 0 || resending) return;
    setResending(true);
    setError('');
    setSuccess('');
    try {
      await onResend();
      setSuccess('A new 6-digit verification code has been sent!');
      setCooldown(30);
      setExpirySeconds(300);
      setDigits(['', '', '', '', '', '']);
      inputRefs.current[0]?.focus();
    } catch (err) {
      const msg = err.response?.data?.message || err.response?.data?.error || 'Failed to resend code.';
      setError(typeof msg === 'string' ? msg : 'Failed to resend OTP.');
    } finally {
      setResending(false);
    }
  };

  const isComplete = digits.every(d => d !== '');

  return (
    <div className="otp-container">
      <div className="otp-header">
        <div className="otp-icon-wrap">
          <ShieldCheck size={32} className="otp-shield-icon" />
        </div>
        <h2>{title}</h2>
        <p className="otp-subtitle">{subtitle}</p>
        <div className="otp-email-badge">
          <span>Sent to:</span> <strong>{email}</strong>
        </div>
      </div>

      {error && (
        <div className="form-error-banner" style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
          <AlertCircle size={16} /> {error}
        </div>
      )}

      {success && (
        <div className="form-success-banner" style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
          <CheckCircle2 size={16} /> {success}
        </div>
      )}

      <form onSubmit={handleSubmit} className="otp-form">
        <div className="otp-digits-wrapper" onPaste={handlePaste}>
          {digits.map((digit, i) => (
            <input
              key={i}
              ref={el => inputRefs.current[i] = el}
              type="text"
              inputMode="numeric"
              maxLength={1}
              value={digit}
              onChange={e => handleDigitChange(i, e.target.value)}
              onKeyDown={e => handleKeyDown(i, e)}
              className={`otp-digit-box ${digit ? 'filled' : ''}`}
              autoComplete="one-time-code"
              disabled={loading}
            />
          ))}
        </div>

        <div className="otp-meta-row">
          <div className="otp-timer">
            ⏱ Valid for:{' '}
            <span className={expirySeconds <= 60 ? 'timer-urgent' : ''}>
              {formatTimer(expirySeconds)}
            </span>
          </div>

          <button
            type="button"
            className="otp-resend-btn"
            onClick={handleResend}
            disabled={cooldown > 0 || resending}
          >
            {resending ? (
              <><Loader2 size={13} className="spin" /> Sending…</>
            ) : cooldown > 0 ? (
              <>Resend in {cooldown}s</>
            ) : (
              <><RefreshCw size={13} /> Resend Code</>
            )}
          </button>
        </div>

        <div className="otp-actions">
          <button
            type="submit"
            className="btn btn-primary"
            style={{ width: '100%', padding: '12px', fontSize: '1rem' }}
            disabled={!isComplete || loading}
          >
            {loading ? (
              <><Loader2 size={16} className="spin" /> Verifying…</>
            ) : (
              'Verify & Continue'
            )}
          </button>

          {onBack && (
            <button
              type="button"
              className="btn btn-ghost btn-sm"
              style={{ width: '100%', marginTop: '8px', color: 'var(--gray-600)' }}
              onClick={onBack}
              disabled={loading}
            >
              <ArrowLeft size={14} /> Back
            </button>
          )}
        </div>
      </form>
    </div>
  );
}
