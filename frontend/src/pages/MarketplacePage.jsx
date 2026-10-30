import React, { useEffect, useState, useCallback } from 'react';
import { useAuth } from '../context/AuthContext';
import { matchingAPI } from '../services/api';
import { PageHeader, LoadingSpinner, ErrorAlert, Badge, StatCard } from '../components/UI';
import { ShoppingCart, Tag, Zap, Clock, Plus, RefreshCw, Loader2 } from 'lucide-react';

export default function MarketplacePage() {
  const { user } = useAuth();
  const [listings, setListings]     = useState([]);
  const [orders, setOrders]         = useState([]);
  const [loading, setLoading]       = useState(true);
  const [error, setError]           = useState('');
  const [showListingForm, setShowListingForm] = useState(false);
  const [showOrderForm, setShowOrderForm]     = useState(false);
  const [selectedListing, setSelectedListing] = useState(null);
  const [submitting, setSubmitting] = useState(false);
  const [filter, setFilter]         = useState('');

  const [listingForm, setListingForm] = useState({
    sellerId: user.username,
    availableEnergyKwh: '',
    pricePerKwh: '',
  });
  const [orderForm, setOrderForm] = useState({
    buyerId: user.username,
    requestedEnergyKwh: '',
    maxPricePerKwh: '',
  });
  const [formErr, setFormErr] = useState('');

  const fetchData = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      const [lRes, oRes] = await Promise.allSettled([
        matchingAPI.getAvailableListings(),
        matchingAPI.getOrdersByBuyer(user.username),
      ]);
      if (lRes.status === 'fulfilled') setListings(lRes.value.data);
      if (oRes.status === 'fulfilled') setOrders(oRes.value.data);
    } catch {
      setError('Failed to load marketplace. Ensure Matching Service is running.');
    } finally {
      setLoading(false);
    }
  }, [user.username]);

  useEffect(() => { fetchData(); }, [fetchData]);

  const handleCreateListing = async (e) => {
    e.preventDefault();
    setFormErr('');
    if (!listingForm.availableEnergyKwh || !listingForm.pricePerKwh) {
      setFormErr('All fields are required.');
      return;
    }
    setSubmitting(true);
    try {
      await matchingAPI.createListing({
        sellerId: user.username,
        availableEnergyKwh: parseFloat(listingForm.availableEnergyKwh),
        pricePerKwh: parseFloat(listingForm.pricePerKwh),
      });
      setShowListingForm(false);
      setListingForm({ sellerId: user.username, availableEnergyKwh: '', pricePerKwh: '' });
      fetchData();
    } catch (err) {
      setFormErr(err.response?.data?.message || 'Failed to create listing.');
    } finally {
      setSubmitting(false);
    }
  };

  const handlePlaceOrder = async (e) => {
    e.preventDefault();
    setFormErr('');
    if (!orderForm.requestedEnergyKwh || !orderForm.maxPricePerKwh) {
      setFormErr('All fields are required.');
      return;
    }
    setSubmitting(true);
    try {
      const res = await matchingAPI.placeOrder({
        buyerId: user.username,
        requestedEnergyKwh: parseFloat(orderForm.requestedEnergyKwh),
        maxPricePerKwh: parseFloat(orderForm.maxPricePerKwh),
      });
      // Try auto-match immediately
      try { await matchingAPI.matchOrder(res.data.id); } catch {}
      setShowOrderForm(false);
      setSelectedListing(null);
      setOrderForm({ buyerId: user.username, requestedEnergyKwh: '', maxPricePerKwh: '' });
      fetchData();
    } catch (err) {
      setFormErr(err.response?.data?.message || 'Failed to place order.');
    } finally {
      setSubmitting(false);
    }
  };

  const openOrderForm = (listing) => {
    setSelectedListing(listing);
    setOrderForm({
      buyerId: user.username,
      requestedEnergyKwh: listing.availableEnergyKwh,
      maxPricePerKwh: listing.pricePerKwh,
    });
    setShowOrderForm(true);
    setFormErr('');
  };

  const filtered = listings.filter(l =>
    l.sellerId?.toLowerCase().includes(filter.toLowerCase()) ||
    String(l.pricePerKwh).includes(filter) ||
    l.status?.toLowerCase().includes(filter.toLowerCase())
  );

  if (loading) return <LoadingSpinner message="Loading marketplace…" />;

  return (
    <div className="page-wrapper">
      <PageHeader
        title="Energy Marketplace"
        subtitle="Browse and trade surplus solar energy from prosumers in your area"
        actions={
          <div className="header-actions">
            <button className="btn btn-outline btn-sm" onClick={fetchData}><RefreshCw size={14} /></button>
            {user.role === 'PROSUMER' && (
              <button className="btn btn-primary btn-sm" onClick={() => { setShowListingForm(!showListingForm); setFormErr(''); }}>
                <Plus size={14} /> Create Listing
              </button>
            )}
          </div>
        }
      />

      {error && <ErrorAlert message={error} onRetry={fetchData} />}

      <div className="stats-grid">
        <StatCard icon={Tag}          title="Available Listings" value={listings.filter(l => l.status === 'AVAILABLE').length} color="#2563eb" />
        <StatCard icon={Zap}          title="Total Listings"     value={listings.length}  color="#8b5cf6" />
        <StatCard icon={ShoppingCart} title="My Orders"          value={orders.length}    color="#22c55e" />
        <StatCard icon={Clock}        title="Pending Orders"      value={orders.filter(o => o.status === 'PENDING').length} color="#f59e0b" />
      </div>

      {/* Create Listing Form */}
      {showListingForm && (
        <div className="card">
          <div className="card-header"><h3>Create New Energy Listing</h3></div>
          <div className="card-body">
            {formErr && <div className="form-error-banner">{formErr}</div>}
            <form onSubmit={handleCreateListing} className="inline-form">
              <div className="form-row">
                <div className="form-group">
                  <label>Available Energy (kWh)</label>
                  <input type="number" step="0.01" min="0.01" placeholder="e.g. 10.0"
                    value={listingForm.availableEnergyKwh}
                    onChange={e => setListingForm({ ...listingForm, availableEnergyKwh: e.target.value })} />
                </div>
                <div className="form-group">
                  <label>Price per kWh (₹)</label>
                  <input type="number" step="0.01" min="0.01" placeholder="e.g. 4.5"
                    value={listingForm.pricePerKwh}
                    onChange={e => setListingForm({ ...listingForm, pricePerKwh: e.target.value })} />
                </div>
              </div>
              <div className="form-actions">
                <button type="submit" className="btn btn-primary" disabled={submitting}>
                  {submitting ? <><Loader2 size={14} className="spin" /> Creating…</> : 'Create Listing'}
                </button>
                <button type="button" className="btn btn-ghost" onClick={() => setShowListingForm(false)}>Cancel</button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Place Order Form */}
      {showOrderForm && (
        <div className="card card-highlight">
          <div className="card-header">
            <h3>Place Order{selectedListing ? ` — Listing #${selectedListing.id} by ${selectedListing.sellerId}` : ''}</h3>
          </div>
          <div className="card-body">
            {formErr && <div className="form-error-banner">{formErr}</div>}
            <form onSubmit={handlePlaceOrder} className="inline-form">
              <div className="form-row">
                <div className="form-group">
                  <label>Requested Energy (kWh)</label>
                  <input type="number" step="0.01" min="0.01"
                    value={orderForm.requestedEnergyKwh}
                    onChange={e => setOrderForm({ ...orderForm, requestedEnergyKwh: e.target.value })} />
                </div>
                <div className="form-group">
                  <label>Max Price / kWh (₹)</label>
                  <input type="number" step="0.01" min="0.01"
                    value={orderForm.maxPricePerKwh}
                    onChange={e => setOrderForm({ ...orderForm, maxPricePerKwh: e.target.value })} />
                </div>
              </div>
              <div className="form-actions">
                <button type="submit" className="btn btn-primary" disabled={submitting}>
                  {submitting ? <><Loader2 size={14} className="spin" /> Placing…</> : 'Place & Match Order'}
                </button>
                <button type="button" className="btn btn-ghost" onClick={() => setShowOrderForm(false)}>Cancel</button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Search */}
      <div className="search-bar-wrap">
        <input
          type="text"
          className="search-bar"
          placeholder="Search by seller, price, or status…"
          value={filter}
          onChange={e => setFilter(e.target.value)}
        />
      </div>

      {/* Listings grid */}
      <div className="card">
        <div className="card-header"><h3>Available Energy Listings ({filtered.length})</h3></div>
        <div className="card-body">
          {filtered.length === 0 ? (
            <div className="empty-state">No listings match your search.</div>
          ) : (
            <div className="listings-grid">
              {filtered.map(l => (
                <div className="listing-card" key={l.id}>
                  <div className="listing-card-header">
                    <span className="listing-seller">⚡ {l.sellerId}</span>
                    <Badge status={l.status} />
                  </div>
                  <div className="listing-detail">
                    <span className="listing-energy">{l.availableEnergyKwh} kWh</span>
                    <span className="listing-price">₹{l.pricePerKwh} / kWh</span>
                  </div>
                  <div className="listing-total">
                    Total: ₹{(l.availableEnergyKwh * l.pricePerKwh).toFixed(2)}
                  </div>
                  {l.status === 'AVAILABLE' && l.sellerId !== user.username && (
                    <button className="btn btn-primary btn-sm btn-full mt-1" onClick={() => openOrderForm(l)}>
                      <ShoppingCart size={14} /> Buy Energy
                    </button>
                  )}
                  {l.sellerId === user.username && (
                    <span className="own-listing-badge">Your Listing</span>
                  )}
                </div>
              ))}
            </div>
          )}
        </div>
      </div>

      {/* My orders */}
      <div className="card">
        <div className="card-header"><h3>My Orders ({orders.length})</h3></div>
        <div className="card-body">
          {orders.length === 0 ? (
            <div className="empty-state">No orders yet. Click "Buy Energy" on a listing.</div>
          ) : (
            <table className="data-table">
              <thead>
                <tr><th>ID</th><th>Requested (kWh)</th><th>Max Price (₹)</th><th>Status</th><th>Matched kWh</th><th>Listing #</th></tr>
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
