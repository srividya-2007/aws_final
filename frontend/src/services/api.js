import axios from 'axios';

const BASE_URL = 'http://localhost:8080';

const api = axios.create({
  baseURL: BASE_URL,
  headers: { 'Content-Type': 'application/json' },
  timeout: 10000,
});

// Attach JWT token to every request
api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

// Handle 401 globally
api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      localStorage.clear();
      window.location.href = '/login';
    }
    return Promise.reject(error);
  }
);

// ─── Auth ────────────────────────────────────────────────
export const authAPI = {
  register: (data) => api.post('/api/auth/register', data),
  login: (data) => api.post('/api/auth/login', data),
  health: () => api.get('/api/auth/health'),
};

// ─── Prosumer ────────────────────────────────────────────
export const prosumerAPI = {
  createProfile: (data) => api.post('/api/prosumers', data),
  getProfile: (username) => api.get(`/api/prosumers/${username}`),
  getAllProfiles: () => api.get('/api/prosumers'),
  updateProfile: (username, data) => api.put(`/api/prosumers/${username}`, data),
  deleteProfile: (username) => api.delete(`/api/prosumers/${username}`),
  health: () => api.get('/api/prosumers/health'),
};

// ─── Telemetry ───────────────────────────────────────────
export const telemetryAPI = {
  saveTelemetry: (data) => api.post('/api/telemetry', data),
  getTelemetryByProsumer: (prosumerId) => api.get(`/api/telemetry/prosumer/${prosumerId}`),
  getLatestTelemetry: (prosumerId) => api.get(`/api/telemetry/prosumer/${prosumerId}/latest`),
  getTelemetryById: (id) => api.get(`/api/telemetry/${id}`),
  health: () => api.get('/api/telemetry/health'),
};

// ─── Matching ────────────────────────────────────────────
export const matchingAPI = {
  createListing: (data) => api.post('/api/matching/listings', data),
  getAvailableListings: () => api.get('/api/matching/listings'),
  placeOrder: (data) => api.post('/api/matching/orders', data),
  matchOrder: (orderId) => api.post(`/api/matching/orders/${orderId}/match`),
  getOrdersByBuyer: (buyerId) => api.get(`/api/matching/orders/buyer/${buyerId}`),
  health: () => api.get('/api/matching/health'),
};

// ─── Settlement ──────────────────────────────────────────
export const settlementAPI = {
  createSettlement: (data) => api.post('/api/settlements', data),
  getSettlement: (id) => api.get(`/api/settlements/${id}`),
  getAllSettlements: () => api.get('/api/settlements'),
  completeSettlement: (id) => api.post(`/api/settlements/${id}/complete`),
  failSettlement: (id, reason) => api.post(`/api/settlements/${id}/fail?reason=${encodeURIComponent(reason)}`),
  health: () => api.get('/api/settlements/health'),
};

export default api;
