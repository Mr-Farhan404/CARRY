const rawBaseUrl = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080';
const API_BASE_URL = rawBaseUrl.endsWith('/') ? rawBaseUrl.slice(0, -1) : rawBaseUrl;

class ApiError extends Error {
  constructor(message, status, fields = null) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
    this.fields = fields;
  }
}

async function request(endpoint, options = {}, token = null) {
  const headers = {
    'Accept': 'application/json',
    ...(options.body ? { 'Content-Type': 'application/json' } : {}),
    ...(options.headers || {}),
  };

  if (token) {
    headers['Authorization'] = `Bearer ${token}`;
  }

  const response = await fetch(`${API_BASE_URL}${endpoint}`, {
    ...options,
    headers,
  });

  // Handle empty responses (like 204 No Content or void 200 OK)
  if (response.status === 204 || response.headers.get('content-length') === '0') {
    return null;
  }

  const contentType = response.headers.get('content-type');
  const isJson = contentType && contentType.includes('application/json');
  const data = isJson ? await response.json() : await response.text();

  if (!response.ok) {
    const message = (isJson && data?.message) || response.statusText || 'An error occurred';
    const fields = (isJson && data?.fields) || null;
    throw new ApiError(message, response.status, fields);
  }

  return data;
}

export const authApi = {
  login: (credentials) =>
    request('/api/auth/login', {
      method: 'POST',
      body: JSON.stringify(credentials),
    }),

  register: (userData) =>
    request('/api/auth/register', {
      method: 'POST',
      body: JSON.stringify(userData),
    }),

  getMe: (token) =>
    request('/api/users/me', {
      method: 'GET',
    }, token),
};

export const metaApi = {
  getZones: () => request('/api/meta/zones', { method: 'GET' }),
};

export const requestsApi = {
  create: (requestData, token) =>
    request('/api/requests', {
      method: 'POST',
      body: JSON.stringify(requestData),
    }, token),

  getMyRequests: (token) =>
    request('/api/requests/mine', {
      method: 'GET',
    }, token),

  getById: (id, token) =>
    request(`/api/requests/${id}`, {
      method: 'GET',
    }, token),

  getTimeline: (id, token) =>
    request(`/api/requests/${id}/timeline`, {
      method: 'GET',
    }, token),

  cancel: (id, token) =>
    request(`/api/requests/${id}/cancel`, {
      method: 'PUT',
    }, token),

  rate: (id, ratingData, token) =>
    request(`/api/requests/${id}/rating`, {
      method: 'POST',
      body: JSON.stringify(ratingData),
    }, token),

  getRating: (id, token) =>
    request(`/api/requests/${id}/rating`, {
      method: 'GET',
    }, token),

  getAvailable: (token) =>
    request('/api/requests/available', {
      method: 'GET',
    }, token),

  getAssigned: (token) =>
    request('/api/requests/assigned', {
      method: 'GET',
    }, token),

  accept: (id, { tripId, version }, token) =>
    request(`/api/requests/${id}/accept`, {
      method: 'PUT',
      body: JSON.stringify({ tripId, version }),
    }, token),

  updateStatus: (id, { status, note }, token) =>
    request(`/api/requests/${id}/status`, {
      method: 'PUT',
      body: JSON.stringify({ status, note }),
    }, token),

  needMore: (id, { additionalAmount, reason }, token) =>
    request(`/api/requests/${id}/need-more`, {
      method: 'POST',
      body: JSON.stringify({ additionalAmount, reason }),
    }, token),

  submitAdditionalPayment: (id, { paymentMethod, senderPhone, trxId }, token) =>
    request(`/api/requests/${id}/additional-payment`, {
      method: 'POST',
      body: JSON.stringify({ paymentMethod, senderPhone, trxId }),
    }, token),

  getPayment: (id, token) =>
    request(`/api/requests/${id}/payment`, {
      method: 'GET',
    }, token),
};

export const tripsApi = {
  create: (tripData, token) =>
    request('/api/trips', {
      method: 'POST',
      body: JSON.stringify(tripData),
    }, token),

  getMyTrips: (token) =>
    request('/api/trips/mine', {
      method: 'GET',
    }, token),

  getActiveTrips: () =>
    request('/api/trips/active', {
      method: 'GET',
    }),

  updateStatus: (id, status, token) =>
    request(`/api/trips/${id}/status`, {
      method: 'PUT',
      body: JSON.stringify({ status }),
    }, token),
};

export const adminApi = {
  getUsers: (token) =>
    request('/api/admin/users', {
      method: 'GET',
    }, token),

  getRequests: (status, token) => {
    const query = status && status !== 'ALL' ? `?status=${encodeURIComponent(status)}` : '';
    return request(`/api/admin/requests${query}`, {
      method: 'GET',
    }, token);
  },

  getComplaints: (token) =>
    request('/api/admin/complaints', {
      method: 'GET',
    }, token),

  updateComplaint: (id, { status, adminNotes }, token) =>
    request(`/api/admin/complaints/${id}`, {
      method: 'PUT',
      body: JSON.stringify({ status, adminNotes }),
    }, token),

  getPayments: (token) =>
    request('/api/admin/payments', {
      method: 'GET',
    }, token),

  verifyPayment: (id, { status, additionalPaymentStatus, adminNotes }, token) =>
    request(`/api/admin/payments/${id}/verify`, {
      method: 'PUT',
      body: JSON.stringify({ status, additionalPaymentStatus, adminNotes }),
    }, token),
};

export const complaintsApi = {
  create: (complaintData, token) =>
    request('/api/complaints', {
      method: 'POST',
      body: JSON.stringify(complaintData),
    }, token),
};

export const systemApi = {
  getHealth: () => request('/api/health', { method: 'GET' }),
};

export { ApiError, API_BASE_URL };
