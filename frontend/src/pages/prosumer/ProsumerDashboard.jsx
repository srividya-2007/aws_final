import React, { useEffect, useState, useCallback } from 'react';
import { useAuth } from '../../context/AuthContext';
import { telemetryAPI, matchingAPI, settlementAPI } from '../../services/api';
import { StatCard, LoadingSpinner, ErrorAlert, PageHeader, Badge } from '../../components/UI';
import { Zap, TrendingUp, TrendingDown, Battery, Plus, RefreshCw } from 'lucide-react';
import {
  AreaChart, Area, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer, Legend
} from 'recharts';
import { Link } from 'react-router-dom';

export default function ProsumerDashboard() {
  const { user } = useAuth();
  const [telemetry, setTelemetry] = useState([]);
  const [latest, setLatest]       = useState(null);
  const [listings, setListings]   = useState([]);
  const [orders, setOrders]       = useState([]);
  const [loading, setLoading]     = useState(true);
  const [error, setError]         = useState('');

  const fetchData = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      const [telRes, latestRes, listRes, orderRes] = await Promise.allSettled([
        telemetryAPI.getTelemetryByProsumer(user.username),
        telemetryAPI.getLatestTelemetry(user.username),
        matchingAPI.getAvailableListings(),
        matchingAPI.getOrdersByBuyer(user.username),
      ]);

      if (telRes.status === 'fulfilled') setTelemetry(telRes.value.data.slice(-10));
      if (latestRes.status === 'fulfilled') setLatest(latestRes.value.data);
      if (listRes.status === 'fulfilled')
        setListings(listRes.value.data.filter(l => l.sellerId === user.username));
      if (orderRes.status === 'fulfilled') setOrders(orderRes.value.data);
    } catch {
      setError('Failed to load dashboard data. Make sure all backend services are running.');
    } finally {
      setLoading(false);
    }
  }, [user.username]);

  useEffect(() => { fetchData(); }, [fetchData]);

  // Build chart data from telemetry
  const chartData = telemetry.map((t, i) => ({
    name: `T${i + 1}`,
    Generated: t.energyGeneratedKwh?.toFixed(2),
    Consumed:  t.energyConsumedKwh?.toFixed(2),
    Surplus:   t.surplusEnergyKwh?.toFixed(2),
  }));

  if (loading) return <LoadingSpinner message="Loading prosumer dashboard…" />;

  return (
    <div className="page-wrapper">
      <PageHeader
        title={`Welcome, ${user.username} ⚡`}
        subtitle="Prosumer Dashboard — manage your solar generation and energy trades"
        actions={
          <button className="btn btn-outline btn-sm" onClick={fetchData}>
            <RefreshCw size={14} /> Refresh
          </button>
        }
      />

      {error && <ErrorAlert message={error} onRetry={fetchData} />}

      {/* Stat cards */}
      <div className="stats-grid">
        <StatCard icon={Zap}        title="Energy Generated" value={latest?.energyGeneratedKwh?.toFixed(2)} unit="kWh" color="#2563eb" />
        <StatCard icon={TrendingDown} title="Energy Consumed"  value={latest?.energyConsumedKwh?.toFixed(2)}  unit="kWh" color="#ef4444" />
        <StatCard icon={Battery}    title="Surplus Energy"   value={latest?.surplusEnergyKwh?.toFixed(2)}   unit="kWh" color="#22c55e" />
        <StatCard icon={TrendingUp} title="My Listings"      value={listings.length}                                  color="#8b5cf6" />
      </div>

      {/* Telemetry chart */}
      <div className="card">
        <div className="card-header">
          <h3>Energy Overview (Last {chartData.length} readings)</h3>
        </div>
        <div className="card-body">
          {chartData.length === 0 ? (
            <div className="empty-state">
              <p>No telemetry data yet.</p>
              <Link to="/telemetry" className="btn btn-primary btn-sm">Add Telemetry</Link>
            </div>
          ) : (
            <ResponsiveContainer width="100%" height={280}>
              <AreaChart data={chartData}>
                <defs>
                  <linearGradient id="gen" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="5%" stopColor="#2563eb" stopOpacity={0.3} />
                    <stop offset="95%" stopColor="#2563eb" stopOpacity={0} />
                  </linearGradient>
                  <linearGradient id="cons" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="5%" stopColor="#ef4444" stopOpacity={0.3} />
                    <stop offset="95%" stopColor="#ef4444" stopOpacity={0} />
                  </linearGradient>
                  <linearGradient id="surp" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="5%" stopColor="#22c55e" stopOpacity={0.3} />
                    <stop offset="95%" stopColor="#22c55e" stopOpacity={0} />
                  </linearGradient>
                </defs>
                <CartesianGrid strokeDasharray="3 3" stroke="#e2e8f0" />
                <XAxis dataKey="name" tick={{ fontSize: 12 }} />
                <YAxis tick={{ fontSize: 12 }} />
                <Tooltip />
                <Legend />
                <Area type="monotone" dataKey="Generated" stroke="#2563eb" fill="url(#gen)" strokeWidth={2} />
                <Area type="monotone" dataKey="Consumed"  stroke="#ef4444" fill="url(#cons)" strokeWidth={2} />
                <Area type="monotone" dataKey="Surplus"   stroke="#22c55e" fill="url(#surp)" strokeWidth={2} />
              </AreaChart>
            </ResponsiveContainer>
          )}
        </div>
      </div>

      {/* My Listings */}
      <div className="card">
        <div className="card-header">
          <h3>My Energy Listings</h3>
          <Link to="/marketplace" className="btn btn-primary btn-sm"><Plus size={14} /> New Listing</Link>
        </div>
        <div className="card-body">
          {listings.length === 0 ? (
            <div className="empty-state">No listings yet. Create one in the Marketplace.</div>
          ) : (
            <table className="data-table">
              <thead>
                <tr><th>ID</th><th>Energy (kWh)</th><th>Price/kWh (₹)</th><th>Status</th><th>Created</th></tr>
              </thead>
              <tbody>
                {listings.map(l => (
                  <tr key={l.id}>
                    <td>#{l.id}</td>
                    <td>{l.availableEnergyKwh}</td>
                    <td>₹{l.pricePerKwh}</td>
                    <td><Badge status={l.status} /></td>
                    <td>{l.createdAt ? new Date(l.createdAt).toLocaleDateString() : '—'}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>
      </div>

      {/* My Orders */}
      <div className="card">
        <div className="card-header"><h3>My Trade Orders</h3></div>
        <div className="card-body">
          {orders.length === 0 ? (
            <div className="empty-state">No orders found for your account.</div>
          ) : (
            <table className="data-table">
              <thead>
                <tr><th>ID</th><th>Requested (kWh)</th><th>Max Price (₹)</th><th>Status</th><th>Matched kWh</th></tr>
              </thead>
              <tbody>
                {orders.map(o => (
                  <tr key={o.id}>
                    <td>#{o.id}</td>
                    <td>{o.requestedEnergyKwh}</td>
                    <td>₹{o.maxPricePerKwh}</td>
                    <td><Badge status={o.status} /></td>
                    <td>{o.matchedEnergyKwh ?? '—'}</td>
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
