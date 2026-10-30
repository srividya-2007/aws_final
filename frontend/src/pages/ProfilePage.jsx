import React, { useEffect, useState, useCallback } from 'react';
import { useAuth } from '../context/AuthContext';
import { prosumerAPI } from '../services/api';
import { PageHeader, LoadingSpinner, ErrorAlert, Badge } from '../components/UI';
import { User, MapPin, Zap, Calendar, Save, Loader2, Edit2 } from 'lucide-react';

export default function ProfilePage() {
  const { user } = useAuth();
  const [profile, setProfile]   = useState(null);
  const [loading, setLoading]   = useState(true);
  const [error, setError]       = useState('');
  const [editing, setEditing]   = useState(false);
  const [saving, setSaving]     = useState(false);
  const [saveErr, setSaveErr]   = useState('');
  const [saveOk, setSaveOk]     = useState('');
  const [creating, setCreating] = useState(false);
  const [form, setForm] = useState({
    username: user.username,
    fullName: '',
    location: '',
    installedCapacityKw: '',
    energyType: 'SOLAR',
  });

  const fetchProfile = useCallback(async () => {
    if (user?.role !== 'PROSUMER') {
      setLoading(false);
      return;
    }
    setLoading(true);
    setError('');
    try {
      const res = await prosumerAPI.getProfile(user.username);
      setProfile(res.data);
      setForm({
        username: res.data.username,
        fullName: res.data.fullName,
        location: res.data.location,
        installedCapacityKw: res.data.installedCapacityKw,
        energyType: res.data.energyType,
      });
    } catch (err) {
      // 404 or 500 (ProsumerNotFoundException) means the prosumer hasn't filled profile details yet
      if (err.response?.status === 404 || err.response?.status === 500) {
        setProfile(null);
      } else {
        setError('Failed to load profile. Check Prosumer Service.');
      }
    } finally {
      setLoading(false);
    }
  }, [user?.username, user?.role]);

  useEffect(() => { fetchProfile(); }, [fetchProfile]);

  const handleSave = async (e) => {
    e.preventDefault();
    setSaveErr('');
    setSaveOk('');
    if (!form.fullName || !form.location || !form.installedCapacityKw) {
      setSaveErr('All fields are required.');
      return;
    }
    setSaving(true);
    try {
      if (creating || !profile) {
        await prosumerAPI.createProfile({ ...form, installedCapacityKw: parseFloat(form.installedCapacityKw) });
      } else {
        await prosumerAPI.updateProfile(user.username, { ...form, installedCapacityKw: parseFloat(form.installedCapacityKw) });
      }
      setSaveOk('Profile saved successfully.');
      setEditing(false);
      setCreating(false);
      fetchProfile();
    } catch (err) {
      setSaveErr(err.response?.data?.message || 'Failed to save profile.');
    } finally {
      setSaving(false);
    }
  };

  if (loading) return <LoadingSpinner message="Loading profile…" />;

  const roleInfo = {
    PROSUMER: { label: 'Prosumer', color: '#22c55e' },
    ADMIN:    { label: 'Admin',    color: '#f59e0b' },
    USER:     { label: 'Consumer', color: '#2563eb' },
  }[user.role] || { label: user.role, color: '#6b7280' };

  return (
    <div className="page-wrapper">
      <PageHeader title="My Profile" subtitle="View and manage your account information" />
      {error && <ErrorAlert message={error} />}

      {/* Account card */}
      <div className="card">
        <div className="profile-hero">
          <div className="profile-avatar">{user.username[0]?.toUpperCase()}</div>
          <div className="profile-hero-info">
            <h2>{user.username}</h2>
            <Badge status={user.role} />
          </div>
        </div>
        <div className="card-body">
          <div className="profile-info-grid">
            <div className="profile-info-item"><User size={16} /> <span>Username:</span> <strong>{user.username}</strong></div>
            <div className="profile-info-item"><Zap size={16} />  <span>Role:</span>
              <strong style={{ color: roleInfo.color }}>{roleInfo.label}</strong>
            </div>
          </div>
        </div>
      </div>

      {/* Prosumer profile */}
      {user.role === 'PROSUMER' && (
        <div className="card">
          <div className="card-header">
            <h3>Prosumer Profile</h3>
            {profile && !editing && (
              <button className="btn btn-outline btn-sm" onClick={() => { setEditing(true); setSaveErr(''); setSaveOk(''); }}>
                <Edit2 size={14} /> Edit
              </button>
            )}
          </div>
          <div className="card-body">
            {saveOk && <div className="form-success-banner">{saveOk}</div>}
            {saveErr && <div className="form-error-banner">{saveErr}</div>}

            {!profile && !creating ? (
              <div className="empty-state">
                <p>No prosumer profile found for your account.</p>
                <button className="btn btn-primary btn-sm" onClick={() => setCreating(true)}>
                  Create Prosumer Profile
                </button>
              </div>
            ) : editing || creating ? (
              <form onSubmit={handleSave} className="auth-form">
                <div className="form-group">
                  <label>Full Name</label>
                  <input type="text" placeholder="Your full name" value={form.fullName}
                    onChange={e => setForm({ ...form, fullName: e.target.value })} />
                </div>
                <div className="form-group">
                  <label>Location / City</label>
                  <input type="text" placeholder="e.g. Hyderabad" value={form.location}
                    onChange={e => setForm({ ...form, location: e.target.value })} />
                </div>
                <div className="form-group">
                  <label>Installed Capacity (kW)</label>
                  <input type="number" step="0.1" min="0.1" placeholder="e.g. 10.0" value={form.installedCapacityKw}
                    onChange={e => setForm({ ...form, installedCapacityKw: e.target.value })} />
                </div>
                <div className="form-group">
                  <label>Energy Source Type</label>
                  <select value={form.energyType} onChange={e => setForm({ ...form, energyType: e.target.value })}>
                    <option>SOLAR</option>
                    <option>WIND</option>
                    <option>HYDRO</option>
                  </select>
                </div>
                <div className="form-actions">
                  <button type="submit" className="btn btn-primary" disabled={saving}>
                    {saving ? <><Loader2 size={14} className="spin" /> Saving…</> : <><Save size={14} /> Save Profile</>}
                  </button>
                  <button type="button" className="btn btn-ghost" onClick={() => { setEditing(false); setCreating(false); }}>Cancel</button>
                </div>
              </form>
            ) : (
              <div className="profile-info-grid">
                <div className="profile-info-item"><User size={16} />    <span>Full Name:</span>   <strong>{profile.fullName}</strong></div>
                <div className="profile-info-item"><MapPin size={16} />  <span>Location:</span>    <strong>{profile.location}</strong></div>
                <div className="profile-info-item"><Zap size={16} />     <span>Capacity:</span>    <strong>{profile.installedCapacityKw} kW</strong></div>
                <div className="profile-info-item"><Zap size={16} />     <span>Energy Type:</span> <strong>{profile.energyType}</strong></div>
                <div className="profile-info-item"><Calendar size={16} /><span>Joined:</span>
                  <strong>{profile.createdAt ? new Date(profile.createdAt).toLocaleDateString() : '—'}</strong>
                </div>
                <div className="profile-info-item"><span>Active:</span>  <Badge status={profile.active ? 'AVAILABLE' : 'EXPIRED'} /></div>
              </div>
            )}
          </div>
        </div>
      )}

      {user.role === 'USER' && (
        <div className="card">
          <div className="card-body">
            <div className="info-alert">
              ℹ You are registered as a <strong>Consumer</strong>. Contact an administrator if you wish to register generation capacity and upgrade to PROSUMER status.
            </div>
          </div>
        </div>
      )}

      {user.role === 'ADMIN' && (
        <div className="card">
          <div className="card-body">
            <div className="info-alert" style={{ background: '#fffbeb', borderColor: '#fef3c7', borderLeftColor: '#f59e0b', color: '#92400e' }}>
              🛡 You have <strong>Administrator</strong> privileges with full access to user management, trade matching, and platform analytics.
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
