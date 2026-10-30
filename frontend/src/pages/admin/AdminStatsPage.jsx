import React, { useEffect, useState } from 'react';
import { settlementAPI, matchingAPI, prosumerAPI } from '../../services/api';
import { PageHeader, LoadingSpinner, ErrorAlert, StatCard } from '../../components/UI';
import { BarChart2, TrendingUp, Zap, CreditCard, RefreshCw } from 'lucide-react';
import {
  BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer,
  LineChart, Line, Legend
} from 'recharts';

export default function AdminStatsPage() {
  const [settlements, setSettlements] = useState([]);
  const [listings, setListings]       = useState([]);
  const [prosumers, setProsumers]     = useState([]);
  const [loading, setLoading]         = useState(true);
  const [error, setError]             = useState('');

  const fetchData = async () => {
    setLoading(true);
    setError('');
    try {
      const [sRes, lRes, pRes] = await Promise.allSettled([
        settlementAPI.getAllSettlements(),
        matchingAPI.getAvailableListings(),
        prosumerAPI.getAllProfiles(),
      ]);
      if (sRes.status === 'fulfilled') setSettlements(sRes.value.data);
      if (lRes.status === 'fulfilled') setListings(lRes.value.data);
      if (pRes.status === 'fulfilled') setProsumers(pRes.value.data);
    } catch {
      setError('Failed to load statistics.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { fetchData(); }, []);

  const totalRevenue = settlements.filter(s => s.status === 'COMPLETED').reduce((a, b) => a + (b.totalAmount || 0), 0);
  const totalEnergy  = settlements.filter(s => s.status === 'COMPLETED').reduce((a, b) => a + (b.energyKwh  || 0), 0);

  // Chart: Settlements by day
  const byDay = {};
  settlements.forEach(s => {
    const d = s.createdAt ? new Date(s.createdAt).toLocaleDateString() : 'Unknown';
    byDay[d] = (byDay[d] || 0) + 1;
  });
  const dayData = Object.entries(byDay).map(([date, count]) => ({ date, count }));

  // Chart: Capacity distribution
  const capData = prosumers.map(p => ({ name: p.username, capacity: p.installedCapacityKw }));

  if (loading) return <LoadingSpinner message="Loading statistics…" />;

  return (
    <div className="page-wrapper">
      <PageHeader
        title="Platform Statistics"
        subtitle="Aggregated energy trading analytics across all users"
        actions={<button className="btn btn-outline btn-sm" onClick={fetchData}><RefreshCw size={14} /></button>}
      />
      {error && <ErrorAlert message={error} onRetry={fetchData} />}

      <div className="stats-grid">
        <StatCard icon={CreditCard}  title="Total Revenue (₹)"   value={totalRevenue.toFixed(2)} color="#22c55e" />
        <StatCard icon={Zap}         title="Total Energy Traded"  value={totalEnergy.toFixed(2)}  unit="kWh"     color="#2563eb" />
        <StatCard icon={TrendingUp}  title="Total Settlements"    value={settlements.length}                     color="#8b5cf6" />
        <StatCard icon={BarChart2}   title="Active Listings"      value={listings.filter(l => l.status === 'AVAILABLE').length} color="#f59e0b" />
      </div>

      <div className="two-col-grid">
        <div className="card">
          <div className="card-header"><h3>Daily Settlement Activity</h3></div>
          <div className="card-body">
            {dayData.length === 0 ? (
              <div className="empty-state">No data.</div>
            ) : (
              <ResponsiveContainer width="100%" height={230}>
                <BarChart data={dayData}>
                  <CartesianGrid strokeDasharray="3 3" stroke="#e2e8f0" />
                  <XAxis dataKey="date" tick={{ fontSize: 10 }} />
                  <YAxis allowDecimals={false} tick={{ fontSize: 12 }} />
                  <Tooltip />
                  <Bar dataKey="count" fill="#2563eb" radius={[4, 4, 0, 0]} name="Settlements" />
                </BarChart>
              </ResponsiveContainer>
            )}
          </div>
        </div>

        <div className="card">
          <div className="card-header"><h3>Prosumer Installed Capacity (kW)</h3></div>
          <div className="card-body">
            {capData.length === 0 ? (
              <div className="empty-state">No prosumer data.</div>
            ) : (
              <ResponsiveContainer width="100%" height={230}>
                <BarChart data={capData}>
                  <CartesianGrid strokeDasharray="3 3" stroke="#e2e8f0" />
                  <XAxis dataKey="name" tick={{ fontSize: 10 }} />
                  <YAxis tick={{ fontSize: 12 }} />
                  <Tooltip />
                  <Bar dataKey="capacity" fill="#22c55e" radius={[4, 4, 0, 0]} name="Capacity (kW)" />
                </BarChart>
              </ResponsiveContainer>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}
