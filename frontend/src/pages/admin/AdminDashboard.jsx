import React, { useEffect, useState, useCallback } from 'react';
import { prosumerAPI, settlementAPI, matchingAPI } from '../../services/api';
import { StatCard, LoadingSpinner, ErrorAlert, PageHeader, Badge } from '../../components/UI';
import { Users, CreditCard, ShoppingCart, Activity, RefreshCw } from 'lucide-react';
import {
  BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer, Legend
} from 'recharts';

export default function AdminDashboard() {
  const [prosumers, setProsumers]     = useState([]);
  const [settlements, setSettlements] = useState([]);
  const [listings, setListings]       = useState([]);
  const [loading, setLoading]         = useState(true);
  const [error, setError]             = useState('');

  const fetchData = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      const [pRes, sRes, lRes] = await Promise.allSettled([
        prosumerAPI.getAllProfiles(),
        settlementAPI.getAllSettlements(),
        matchingAPI.getAvailableListings(),
      ]);
      if (pRes.status === 'fulfilled') setProsumers(pRes.value.data);
      if (sRes.status === 'fulfilled') setSettlements(sRes.value.data);
      if (lRes.status === 'fulfilled') setListings(lRes.value.data);
    } catch {
      setError('Failed to load admin data. Check backend services.');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { fetchData(); }, [fetchData]);

  const totalRevenue = settlements
    .filter(s => s.status === 'COMPLETED')
    .reduce((sum, s) => sum + (s.totalAmount || 0), 0);

  // Bar chart: settlements grouped by status
  const statusGroups = {};
  settlements.forEach(s => { statusGroups[s.status] = (statusGroups[s.status] || 0) + 1; });
  const barData = Object.entries(statusGroups).map(([status, count]) => ({ status, count }));

  if (loading) return <LoadingSpinner message="Loading admin dashboard…" />;

  return (
    <div className="page-wrapper">
      <PageHeader
        title="Admin Dashboard 🛡"
        subtitle="Platform overview — monitor prosumers, transactions, and statistics"
        actions={<button className="btn btn-outline btn-sm" onClick={fetchData}><RefreshCw size={14} /> Refresh</button>}
      />
      {error && <ErrorAlert message={error} onRetry={fetchData} />}

      <div className="stats-grid">
        <StatCard icon={Users}        title="Registered Prosumers" value={prosumers.length}       color="#2563eb" />
        <StatCard icon={ShoppingCart} title="Total Listings"        value={listings.length}        color="#8b5cf6" />
        <StatCard icon={CreditCard}   title="Total Settlements"     value={settlements.length}      color="#f59e0b" />
        <StatCard icon={Activity}     title="Platform Revenue (₹)"  value={totalRevenue.toFixed(2)} color="#22c55e" />
      </div>

      <div className="two-col-grid">
        {/* Settlement bar chart */}
        <div className="card">
          <div className="card-header"><h3>Settlement Status Distribution</h3></div>
          <div className="card-body">
            {barData.length === 0 ? (
              <div className="empty-state">No settlement data.</div>
            ) : (
              <ResponsiveContainer width="100%" height={220}>
                <BarChart data={barData}>
                  <CartesianGrid strokeDasharray="3 3" stroke="#e2e8f0" />
                  <XAxis dataKey="status" tick={{ fontSize: 12 }} />
                  <YAxis allowDecimals={false} tick={{ fontSize: 12 }} />
                  <Tooltip />
                  <Bar dataKey="count" fill="#2563eb" radius={[6, 6, 0, 0]} />
                </BarChart>
              </ResponsiveContainer>
            )}
          </div>
        </div>

        {/* Recent settlements */}
        <div className="card">
          <div className="card-header"><h3>Recent Settlements</h3></div>
          <div className="card-body">
            {settlements.length === 0 ? (
              <div className="empty-state">No settlements on record.</div>
            ) : (
              <table className="data-table">
                <thead>
                  <tr><th>ID</th><th>Buyer</th><th>Seller</th><th>Amount (₹)</th><th>Status</th></tr>
                </thead>
                <tbody>
                  {settlements.slice(0, 8).map(s => (
                    <tr key={s.id}>
                      <td>#{s.id}</td>
                      <td>{s.buyerId}</td>
                      <td>{s.sellerId}</td>
                      <td>₹{s.totalAmount?.toFixed(2)}</td>
                      <td><Badge status={s.status} /></td>
                    </tr>
                  ))}
                </tbody>
              </table>
            )}
          </div>
        </div>
      </div>

      {/* Prosumers table */}
      <div className="card">
        <div className="card-header"><h3>Registered Prosumers</h3></div>
        <div className="card-body">
          {prosumers.length === 0 ? (
            <div className="empty-state">No prosumer profiles registered.</div>
          ) : (
            <table className="data-table">
              <thead>
                <tr><th>Username</th><th>Full Name</th><th>Location</th><th>Capacity (kW)</th><th>Energy Type</th><th>Active</th></tr>
              </thead>
              <tbody>
                {prosumers.map(p => (
                  <tr key={p.id}>
                    <td><strong>{p.username}</strong></td>
                    <td>{p.fullName}</td>
                    <td>{p.location}</td>
                    <td>{p.installedCapacityKw}</td>
                    <td>{p.energyType}</td>
                    <td><Badge status={p.active ? 'AVAILABLE' : 'EXPIRED'} /></td>
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
