import React, { useEffect, useState, useCallback } from 'react';
import { useAuth } from '../context/AuthContext';
import { telemetryAPI } from '../services/api';
import { PageHeader, LoadingSpinner, ErrorAlert, StatCard, Badge } from '../components/UI';
import { Activity, Zap, TrendingDown, Battery, RefreshCw, Plus, Loader2 } from 'lucide-react';
import {
  LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer, Legend
} from 'recharts';

export default function TelemetryPage() {
  const { user } = useAuth();
  const [data, setData]         = useState([]);
  const [latest, setLatest]     = useState(null);
  const [loading, setLoading]   = useState(true);
  const [error, setError]       = useState('');
  const [showForm, setShowForm] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [formErr, setFormErr]   = useState('');
  const [form, setForm] = useState({
    prosumerId: user.username,
    energyGeneratedKwh: '',
    energyConsumedKwh: '',
    sourceType: 'SOLAR',
  });

  const fetchData = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      const [allRes, latestRes] = await Promise.allSettled([
        telemetryAPI.getTelemetryByProsumer(user.username),
        telemetryAPI.getLatestTelemetry(user.username),
      ]);
      if (allRes.status    === 'fulfilled') setData(allRes.value.data.slice(-20));
      if (latestRes.status === 'fulfilled') setLatest(latestRes.value.data);
    } catch {
      setError('Failed to load telemetry. Make sure Telemetry Service is running.');
    } finally {
      setLoading(false);
    }
  }, [user.username]);

  useEffect(() => { fetchData(); }, [fetchData]);

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!form.energyGeneratedKwh || !form.energyConsumedKwh) {
      setFormErr('All fields are required.');
      return;
    }
    setSubmitting(true);
    setFormErr('');
    try {
      await telemetryAPI.saveTelemetry({
        ...form,
        energyGeneratedKwh: parseFloat(form.energyGeneratedKwh),
        energyConsumedKwh:  parseFloat(form.energyConsumedKwh),
        surplusEnergyKwh:   parseFloat(form.energyGeneratedKwh) - parseFloat(form.energyConsumedKwh),
      });
      setShowForm(false);
      fetchData();
    } catch (err) {
      setFormErr(err.response?.data?.message || 'Failed to save telemetry.');
    } finally {
      setSubmitting(false);
    }
  };

  const chartData = data.map((t, i) => ({
    name: `R${i + 1}`,
    Generated: parseFloat(t.energyGeneratedKwh?.toFixed(2) || 0),
    Consumed:  parseFloat(t.energyConsumedKwh?.toFixed(2)  || 0),
    Surplus:   parseFloat(t.surplusEnergyKwh?.toFixed(2)   || 0),
  }));

  if (loading) return <LoadingSpinner message="Loading telemetry data…" />;

  return (
    <div className="page-wrapper">
      <PageHeader
        title="Telemetry Monitoring"
        subtitle="Real-time energy generation and consumption tracking"
        actions={
          <div className="header-actions">
            <button className="btn btn-outline btn-sm" onClick={fetchData}><RefreshCw size={14} /></button>
            <button className="btn btn-primary btn-sm" onClick={() => setShowForm(!showForm)}>
              <Plus size={14} /> Add Reading
            </button>
          </div>
        }
      />

      {error && <ErrorAlert message={error} onRetry={fetchData} />}

      {/* Stats */}
      <div className="stats-grid">
        <StatCard icon={Zap}          title="Generated (Latest)"  value={latest?.energyGeneratedKwh?.toFixed(2)} unit="kWh" color="#2563eb" />
        <StatCard icon={TrendingDown} title="Consumed (Latest)"   value={latest?.energyConsumedKwh?.toFixed(2)}  unit="kWh" color="#ef4444" />
        <StatCard icon={Battery}      title="Surplus (Latest)"    value={latest?.surplusEnergyKwh?.toFixed(2)}   unit="kWh" color="#22c55e" />
        <StatCard icon={Activity}     title="Total Readings"      value={data.length}                             color="#8b5cf6" />
      </div>

      {/* Add reading form */}
      {showForm && (
        <div className="card">
          <div className="card-header"><h3>Add Telemetry Reading</h3></div>
          <div className="card-body">
            {formErr && <div className="form-error-banner">{formErr}</div>}
            <form onSubmit={handleSubmit} className="inline-form">
              <div className="form-row">
                <div className="form-group">
                  <label>Energy Generated (kWh)</label>
                  <input type="number" step="0.01" min="0" placeholder="e.g. 12.5"
                    value={form.energyGeneratedKwh}
                    onChange={e => setForm({ ...form, energyGeneratedKwh: e.target.value })} />
                </div>
                <div className="form-group">
                  <label>Energy Consumed (kWh)</label>
                  <input type="number" step="0.01" min="0" placeholder="e.g. 7.2"
                    value={form.energyConsumedKwh}
                    onChange={e => setForm({ ...form, energyConsumedKwh: e.target.value })} />
                </div>
                <div className="form-group">
                  <label>Source Type</label>
                  <select value={form.sourceType} onChange={e => setForm({ ...form, sourceType: e.target.value })}>
                    <option>SOLAR</option>
                    <option>WIND</option>
                    <option>HYDRO</option>
                  </select>
                </div>
              </div>
              <div className="form-actions">
                <button type="submit" className="btn btn-primary" disabled={submitting}>
                  {submitting ? <><Loader2 size={14} className="spin" /> Saving…</> : 'Save Reading'}
                </button>
                <button type="button" className="btn btn-ghost" onClick={() => setShowForm(false)}>Cancel</button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Line chart */}
      <div className="card">
        <div className="card-header"><h3>Energy Trend (Last {chartData.length} Readings)</h3></div>
        <div className="card-body">
          {chartData.length === 0 ? (
            <div className="empty-state">No telemetry data. Add readings using the button above.</div>
          ) : (
            <ResponsiveContainer width="100%" height={300}>
              <LineChart data={chartData}>
                <CartesianGrid strokeDasharray="3 3" stroke="#e2e8f0" />
                <XAxis dataKey="name" tick={{ fontSize: 12 }} />
                <YAxis tick={{ fontSize: 12 }} />
                <Tooltip />
                <Legend />
                <Line type="monotone" dataKey="Generated" stroke="#2563eb" strokeWidth={2} dot={false} />
                <Line type="monotone" dataKey="Consumed"  stroke="#ef4444" strokeWidth={2} dot={false} />
                <Line type="monotone" dataKey="Surplus"   stroke="#22c55e" strokeWidth={2} dot={false} />
              </LineChart>
            </ResponsiveContainer>
          )}
        </div>
      </div>

      {/* Data table */}
      <div className="card">
        <div className="card-header"><h3>Telemetry Records</h3></div>
        <div className="card-body">
          {data.length === 0 ? (
            <div className="empty-state">No records found.</div>
          ) : (
            <table className="data-table">
              <thead>
                <tr><th>#</th><th>Generated (kWh)</th><th>Consumed (kWh)</th><th>Surplus (kWh)</th><th>Source</th><th>Timestamp</th></tr>
              </thead>
              <tbody>
                {data.map((t, i) => (
                  <tr key={t.id}>
                    <td>{i + 1}</td>
                    <td>{t.energyGeneratedKwh?.toFixed(2)}</td>
                    <td>{t.energyConsumedKwh?.toFixed(2)}</td>
                    <td><span style={{ color: t.surplusEnergyKwh >= 0 ? '#22c55e' : '#ef4444', fontWeight: 600 }}>
                      {t.surplusEnergyKwh?.toFixed(2)}
                    </span></td>
                    <td><Badge status={t.sourceType || 'SOLAR'} /></td>
                    <td>{t.timestamp ? new Date(t.timestamp).toLocaleString() : '—'}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>
      </div>
    </div>
  );
}
