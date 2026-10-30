import React, { useEffect, useState, useCallback } from 'react';
import { settlementAPI } from '../services/api';
import { PageHeader, LoadingSpinner, ErrorAlert, Badge, StatCard } from '../components/UI';
import { CreditCard, CheckCircle, XCircle, Clock, RefreshCw, Loader2 } from 'lucide-react';

export default function SettlementsPage() {
  const [settlements, setSettlements] = useState([]);
  const [loading, setLoading]         = useState(true);
  const [error, setError]             = useState('');
  const [actionErr, setActionErr]     = useState('');
  const [processing, setProcessing]   = useState(null);
  const [filter, setFilter]           = useState('');

  const fetchData = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      const res = await settlementAPI.getAllSettlements();
      setSettlements(res.data);
    } catch {
      setError('Failed to load settlements. Ensure Settlement Service is running.');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { fetchData(); }, [fetchData]);

  const handleComplete = async (id) => {
    setProcessing(id);
    setActionErr('');
    try {
      await settlementAPI.completeSettlement(id);
      fetchData();
    } catch (err) {
      setActionErr(err.response?.data?.message || `Failed to complete settlement #${id}.`);
    } finally {
      setProcessing(null);
    }
  };

  const handleFail = async (id) => {
    setProcessing(id);
    setActionErr('');
    try {
      await settlementAPI.failSettlement(id, 'Manually failed by user');
      fetchData();
    } catch (err) {
      setActionErr(err.response?.data?.message || `Failed to update settlement #${id}.`);
    } finally {
      setProcessing(null);
    }
  };

  const pending   = settlements.filter(s => s.status === 'PENDING').length;
  const completed = settlements.filter(s => s.status === 'COMPLETED').length;
  const failed    = settlements.filter(s => s.status === 'FAILED').length;
  const total     = settlements.reduce((sum, s) => sum + (s.totalAmount || 0), 0);

  const filtered = settlements.filter(s =>
    s.buyerId?.toLowerCase().includes(filter.toLowerCase()) ||
    s.sellerId?.toLowerCase().includes(filter.toLowerCase()) ||
    s.status?.toLowerCase().includes(filter.toLowerCase()) ||
    String(s.orderId).includes(filter)
  );

  if (loading) return <LoadingSpinner message="Loading settlements…" />;

  return (
    <div className="page-wrapper">
      <PageHeader
        title="Settlement & Transaction History"
        subtitle="View, complete or fail energy trade settlements"
        actions={<button className="btn btn-outline btn-sm" onClick={fetchData}><RefreshCw size={14} /></button>}
      />
      {error     && <ErrorAlert message={error} onRetry={fetchData} />}
      {actionErr && <div className="form-error-banner">{actionErr}</div>}

      <div className="stats-grid">
        <StatCard icon={CreditCard}  title="Total Settlements"  value={settlements.length} color="#2563eb" />
        <StatCard icon={Clock}       title="Pending"            value={pending}            color="#f59e0b" />
        <StatCard icon={CheckCircle} title="Completed"          value={completed}          color="#22c55e" />
        <StatCard icon={XCircle}     title="Failed"             value={failed}             color="#ef4444" />
      </div>

      <div className="card">
        <div className="card-header">
          <h3>Platform Revenue: ₹{total.toFixed(2)}</h3>
        </div>
      </div>

      <div className="search-bar-wrap">
        <input className="search-bar" type="text" placeholder="Search by buyer, seller, status, order ID…"
          value={filter} onChange={e => setFilter(e.target.value)} />
      </div>

      <div className="card">
        <div className="card-header"><h3>Settlements ({filtered.length})</h3></div>
        <div className="card-body">
          {filtered.length === 0 ? (
            <div className="empty-state">No settlements found.</div>
          ) : (
            <div className="table-scroll">
              <table className="data-table">
                <thead>
                  <tr>
                    <th>ID</th><th>Order</th><th>Buyer</th><th>Seller</th>
                    <th>Energy (kWh)</th><th>Price (₹/kWh)</th><th>Total (₹)</th>
                    <th>Status</th><th>TX Hash</th><th>Actions</th>
                  </tr>
                </thead>
                <tbody>
                  {filtered.map(s => (
                    <tr key={s.id}>
                      <td>#{s.id}</td>
                      <td>#{s.orderId}</td>
                      <td>{s.buyerId}</td>
                      <td>{s.sellerId}</td>
                      <td>{s.energyKwh}</td>
                      <td>₹{s.pricePerKwh}</td>
                      <td><strong>₹{s.totalAmount?.toFixed(2)}</strong></td>
                      <td><Badge status={s.status} /></td>
                      <td>
                        {s.transactionHash ? (
                          <span className="hash-pill" title={s.transactionHash}>
                            {s.transactionHash.slice(0, 12)}…
                          </span>
                        ) : '—'}
                      </td>
                      <td>
                        {s.status === 'PENDING' && (
                          <div className="action-btns">
                            <button className="btn btn-success btn-xs" onClick={() => handleComplete(s.id)}
                              disabled={processing === s.id}>
                              {processing === s.id ? <Loader2 size={11} className="spin" /> : <CheckCircle size={11} />} Done
                            </button>
                            <button className="btn btn-danger btn-xs" onClick={() => handleFail(s.id)}
                              disabled={processing === s.id}>
                              <XCircle size={11} /> Fail
                            </button>
                          </div>
                        )}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
