import React, { useEffect, useState } from 'react';
import { prosumerAPI } from '../../services/api';
import { PageHeader, LoadingSpinner, ErrorAlert, Badge, StatCard } from '../../components/UI';
import { Users, RefreshCw } from 'lucide-react';

export default function AdminUsersPage() {
  const [prosumers, setProsumers] = useState([]);
  const [loading, setLoading]     = useState(true);
  const [error, setError]         = useState('');
  const [filter, setFilter]       = useState('');

  const fetchData = async () => {
    setLoading(true);
    setError('');
    try {
      const res = await prosumerAPI.getAllProfiles();
      setProsumers(res.data);
    } catch {
      setError('Failed to load prosumers. Ensure Prosumer Service is running.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { fetchData(); }, []);

  const filtered = prosumers.filter(p =>
    p.username?.toLowerCase().includes(filter.toLowerCase()) ||
    p.location?.toLowerCase().includes(filter.toLowerCase()) ||
    p.fullName?.toLowerCase().includes(filter.toLowerCase())
  );

  if (loading) return <LoadingSpinner message="Loading registered users…" />;

  return (
    <div className="page-wrapper">
      <PageHeader
        title="Registered Prosumers"
        subtitle="All prosumer profiles registered on the platform"
        actions={<button className="btn btn-outline btn-sm" onClick={fetchData}><RefreshCw size={14} /></button>}
      />
      {error && <ErrorAlert message={error} onRetry={fetchData} />}

      <div className="stats-grid">
        <StatCard icon={Users} title="Total Prosumers" value={prosumers.length} color="#2563eb" />
        <StatCard icon={Users} title="Active"   value={prosumers.filter(p => p.active).length}  color="#22c55e" />
        <StatCard icon={Users} title="Inactive" value={prosumers.filter(p => !p.active).length} color="#ef4444" />
        <StatCard icon={Users} title="Solar"    value={prosumers.filter(p => p.energyType === 'SOLAR').length} color="#f59e0b" />
      </div>

      <div className="search-bar-wrap">
        <input className="search-bar" type="text" placeholder="Search by name, username, location…"
          value={filter} onChange={e => setFilter(e.target.value)} />
      </div>

      <div className="card">
        <div className="card-header"><h3>Prosumer Registry ({filtered.length})</h3></div>
        <div className="card-body">
          {filtered.length === 0 ? (
            <div className="empty-state">No prosumers found.</div>
          ) : (
            <table className="data-table">
              <thead>
                <tr>
                  <th>ID</th><th>Username</th><th>Full Name</th><th>Location</th>
                  <th>Capacity (kW)</th><th>Energy Type</th><th>Active</th><th>Joined</th>
                </tr>
              </thead>
              <tbody>
                {filtered.map(p => (
                  <tr key={p.id}>
                    <td>{p.id}</td>
                    <td><strong>{p.username}</strong></td>
                    <td>{p.fullName}</td>
                    <td>{p.location}</td>
                    <td>{p.installedCapacityKw} kW</td>
                    <td>{p.energyType}</td>
                    <td><Badge status={p.active ? 'AVAILABLE' : 'EXPIRED'} /></td>
                    <td>{p.createdAt ? new Date(p.createdAt).toLocaleDateString() : '—'}</td>
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
