import React, { useEffect, useState, useCallback } from 'react';
import { useAuth } from '../context/AuthContext';
import { matchingAPI, settlementAPI } from '../services/api';
import { PageHeader, LoadingSpinner, ErrorAlert, Badge, StatCard, Modal } from '../components/UI';
import { Layers, CheckCircle, XCircle, Clock, RefreshCw, Loader2, Hash } from 'lucide-react';

export default function MatchingPage() {
  const { user } = useAuth();
  const [orders, setOrders]       = useState([]);
  const [loading, setLoading]     = useState(true);
  const [error, setError]         = useState('');
  const [matching, setMatching]   = useState(null);
  const [settling, setSettling]   = useState(null);
  const [actionErr, setActionErr] = useState('');
  const [settleModal, setSettleModal] = useState(null);
  const [settleForm, setSettleForm]   = useState({ sellerId: '', pricePerKwh: '' });
  const [submitting, setSubmitting]   = useState(false);

  const fetchOrders = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      const res = await matchingAPI.getOrdersByBuyer(user.username);
      setOrders(res.data);
    } catch {
      setError('Failed to load orders. Ensure Matching Service is running.');
    } finally {
      setLoading(false);
    }
  }, [user.username]);

  useEffect(() => { fetchOrders(); }, [fetchOrders]);

  const handleMatch = async (orderId) => {
    setMatching(orderId);
    setActionErr('');
    try {
      await matchingAPI.matchOrder(orderId);
      fetchOrders();
    } catch (err) {
      setActionErr(err.response?.data?.message || `Failed to match order #${orderId}.`);
    } finally {
      setMatching(null);
    }
  };

  const openSettleModal = (order) => {
    setSettleModal(order);
    setSettleForm({ sellerId: '', pricePerKwh: order.maxPricePerKwh || '' });
    setActionErr('');
  };

  const handleSettle = async (e) => {
    e.preventDefault();
    if (!settleForm.sellerId || !settleForm.pricePerKwh) {
      setActionErr('Seller ID and price are required.');
      return;
    }
    setSubmitting(true);
    try {
      await settlementAPI.createSettlement({
        orderId: settleModal.id,
        buyerId: settleModal.buyerId,
        sellerId: settleForm.sellerId,
        energyKwh: settleModal.matchedEnergyKwh || settleModal.requestedEnergyKwh,
        pricePerKwh: parseFloat(settleForm.pricePerKwh),
      });
      setSettleModal(null);
      fetchOrders();
    } catch (err) {
      setActionErr(err.response?.data?.message || 'Failed to create settlement.');
    } finally {
      setSubmitting(false);
    }
  };

  const pending  = orders.filter(o => o.status === 'PENDING').length;
  const matched  = orders.filter(o => o.status === 'MATCHED').length;
  const total    = orders.length;

  if (loading) return <LoadingSpinner message="Loading matching engine…" />;

  return (
    <div className="page-wrapper">
      <PageHeader
        title="Energy Matching"
        subtitle="Match your pending orders with available energy listings"
        actions={<button className="btn btn-outline btn-sm" onClick={fetchOrders}><RefreshCw size={14} /></button>}
      />
      {error && <ErrorAlert message={error} onRetry={fetchOrders} />}
      {actionErr && <div className="form-error-banner">{actionErr}</div>}

      <div className="stats-grid">
        <StatCard icon={Layers}      title="Total Orders"   value={total}   color="#2563eb" />
        <StatCard icon={Clock}       title="Pending"        value={pending}  color="#f59e0b" />
        <StatCard icon={CheckCircle} title="Matched"        value={matched}  color="#22c55e" />
        <StatCard icon={XCircle}     title="Unmatched"      value={total - pending - matched} color="#ef4444" />
      </div>

      <div className="card">
        <div className="card-header"><h3>My Orders &amp; Matching Actions</h3></div>
        <div className="card-body">
          {orders.length === 0 ? (
            <div className="empty-state">No orders to show. Place orders from the Marketplace.</div>
          ) : (
            <table className="data-table">
              <thead>
                <tr>
                  <th>ID</th><th>Requested (kWh)</th><th>Max Price (₹)</th>
                  <th>Status</th><th>Matched kWh</th><th>Listing</th><th>Actions</th>
                </tr>
              </thead>
              <tbody>
                {orders.map(o => (
                  <tr key={o.id}>
                    <td>#{o.id}</td>
                    <td>{o.requestedEnergyKwh}</td>
                    <td>₹{o.maxPricePerKwh}</td>
                    <td><Badge status={o.status} /></td>
                    <td>{o.matchedEnergyKwh ?? '—'}</td>
                    <td>{o.matchedListingId ? `#${o.matchedListingId}` : '—'}</td>
                    <td>
                      <div className="action-btns">
                        {o.status === 'PENDING' && (
                          <button className="btn btn-primary btn-xs" onClick={() => handleMatch(o.id)}
                            disabled={matching === o.id}>
                            {matching === o.id ? <Loader2 size={12} className="spin" /> : <Layers size={12} />} Match
                          </button>
                        )}
                        {o.status === 'MATCHED' && (
                          <button className="btn btn-success btn-xs" onClick={() => openSettleModal(o)}
                            disabled={settling === o.id}>
                            <Hash size={12} /> Settle
                          </button>
                        )}
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>
      </div>

      {/* Settle Modal */}
      <Modal open={!!settleModal} title={`Create Settlement for Order #${settleModal?.id}`} onClose={() => setSettleModal(null)}>
        {actionErr && <div className="form-error-banner">{actionErr}</div>}
        <form onSubmit={handleSettle} className="auth-form">
          <div className="form-group">
            <label>Seller Username / ID</label>
            <input type="text" placeholder="Enter seller's username"
              value={settleForm.sellerId}
              onChange={e => setSettleForm({ ...settleForm, sellerId: e.target.value })} />
          </div>
          <div className="form-group">
            <label>Agreed Price / kWh (₹)</label>
            <input type="number" step="0.01" min="0.01"
              value={settleForm.pricePerKwh}
              onChange={e => setSettleForm({ ...settleForm, pricePerKwh: e.target.value })} />
          </div>
          <div className="form-actions">
            <button type="submit" className="btn btn-primary" disabled={submitting}>
              {submitting ? <><Loader2 size={14} className="spin" /> Creating…</> : 'Create Settlement'}
            </button>
            <button type="button" className="btn btn-ghost" onClick={() => setSettleModal(null)}>Cancel</button>
          </div>
        </form>
      </Modal>
    </div>
  );
}
