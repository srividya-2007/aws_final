import React, { useEffect, useState, useCallback } from 'react';
import { useAuth } from '../../context/AuthContext';
import { matchingAPI, settlementAPI } from '../../services/api';
import { StatCard, LoadingSpinner, ErrorAlert, PageHeader, Badge } from '../../components/UI';
import { ShoppingCart, CheckCircle, Clock, Zap, RefreshCw } from 'lucide-react';
import { PieChart, Pie, Cell, Tooltip, ResponsiveContainer, Legend } from 'recharts';

const COLORS = ['#2563eb', '#22c55e', '#ef4444', '#f59e0b'];

export default function ConsumerDashboard() {
  const { user } = useAuth();
  const [listings, setListings]     = useState([]);
  const [orders, setOrders]         = useState([]);
  const [settlements, setSettlements] = useState([]);
  const [loading, setLoading]       = useState(true);
  const [error, setError]           = useState('');

  const fetchData = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      const [listRes, orderRes, settleRes] = await Promise.allSettled([
        matchingAPI.getAvailableListings(),
        matchingAPI.getOrdersByBuyer(user.username),
        settlementAPI.getAllSettlements(),
      ]);
      if (listRes.status   === 'fulfilled') setListings(listRes.value.data);
      if (orderRes.status  === 'fulfilled') setOrders(orderRes.value.data);
      if (settleRes.status === 'fulfilled')
        setSettlements(settleRes.value.data.filter(s => s.buyerId === user.username));
    } catch {
      setError('Failed to load dashboard. Make sure all backend services are running.');
    } finally {
      setLoading(false);
    }
  }, [user.username]);

  useEffect(() => { fetchData(); }, [fetchData]);

  const pendingOrders   = orders.filter(o => o.status === 'PENDING').length;
  const matchedOrders   = orders.filter(o => o.status === 'MATCHED').length;
  const completedTrades = settlements.filter(s => s.status === 'COMPLETED').length;

  const pieData = [
    { name: 'Pending',   value: pendingOrders },
    { name: 'Matched',   value: matchedOrders },
    { name: 'Completed', value: completedTrades },
    { name: 'Available Listings', value: listings.length },
  ].filter(d => d.value > 0);

  if (loading) return <LoadingSpinner message="Loading consumer dashboard…" />;

  return (
    <div className="page-wrapper">
      <PageHeader
        title={`Welcome, ${user.username} 🛒`}
        subtitle="Consumer Dashboard — browse listings and track your energy purchases"
        actions={<button className="btn btn-outline btn-sm" onClick={fetchData}><RefreshCw size={14} /> Refresh</button>}
      />
      {error && <ErrorAlert message={error} onRetry={fetchData} />}

      <div className="stats-grid">
        <StatCard icon={Zap}          title="Available Listings" value={listings.length}    color="#2563eb" />
        <StatCard icon={Clock}        title="Pending Orders"     value={pendingOrders}       color="#f59e0b" />
        <StatCard icon={ShoppingCart} title="Matched Orders"     value={matchedOrders}       color="#8b5cf6" />
        <StatCard icon={CheckCircle}  title="Completed Trades"   value={completedTrades}     color="#22c55e" />
      </div>

      <div className="two-col-grid">
        {/* Order status chart */}
        <div className="card">
          <div className="card-header"><h3>Order Status Overview</h3></div>
          <div className="card-body">
            {pieData.length === 0 ? (
              <div className="empty-state">No order data to display.</div>
            ) : (
              <ResponsiveContainer width="100%" height={240}>
                <PieChart>
                  <Pie data={pieData} cx="50%" cy="50%" outerRadius={90} dataKey="value" label>
                    {pieData.map((_, i) => <Cell key={i} fill={COLORS[i % COLORS.length]} />)}
                  </Pie>
                  <Tooltip />
                  <Legend />
                </PieChart>
              </ResponsiveContainer>
            )}
          </div>
        </div>

        {/* Recent settlements */}
        <div className="card">
          <div className="card-header"><h3>My Settlements</h3></div>
          <div className="card-body">
            {settlements.length === 0 ? (
              <div className="empty-state">No settlements yet.</div>
            ) : (
              <table className="data-table">
                <thead>
                  <tr><th>Order</th><th>Energy</th><th>Total (₹)</th><th>Status</th></tr>
                </thead>
                <tbody>
                  {settlements.slice(0, 6).map(s => (
                    <tr key={s.id}>
                      <td>#{s.orderId}</td>
                      <td>{s.energyKwh} kWh</td>
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

      {/* My orders table */}
      <div className="card">
        <div className="card-header"><h3>My Purchase Orders</h3></div>
        <div className="card-body">
          {orders.length === 0 ? (
            <div className="empty-state">No orders placed yet. Visit the Marketplace to buy energy.</div>
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
