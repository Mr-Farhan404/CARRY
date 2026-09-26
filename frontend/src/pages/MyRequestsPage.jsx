import React, { useEffect, useState } from 'react';
import { Link, useLocation } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { Button, Card, StatusBadge } from '../components/common';
import { requestsApi } from '../services/api';

export default function MyRequestsPage() {
  const { token } = useAuth();
  const location = useLocation();

  const [requests, setRequests] = useState([]);
  const [isLoading, setIsLoading] = useState(true);
  const [errorMessage, setErrorMessage] = useState('');
  const [successMessage, setSuccessMessage] = useState(location.state?.message || '');

  const fetchRequests = async () => {
    setIsLoading(true);
    setErrorMessage('');
    try {
      const data = await requestsApi.getMyRequests(token);
      setRequests(data || []);
    } catch (err) {
      setErrorMessage(err.message || 'Failed to load your requests.');
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    fetchRequests();
  }, [token]);

  return (
    <div className="my-requests-page">
      <div className="page-header-row">
        <div>
          <h1 className="page-title">My Delivery Requests</h1>
          <p className="page-subtitle">Track, manage, and rate your campus delivery orders</p>
        </div>
        <Link to="/requests/new">
          <Button variant="primary" size="md">
            + New Request
          </Button>
        </Link>
      </div>

      {successMessage && (
        <div className="ui-alert ui-alert--success" role="status">
          {successMessage}
        </div>
      )}

      {errorMessage && (
        <div className="ui-alert ui-alert--error" role="alert">
          {errorMessage}{' '}
          <button type="button" onClick={fetchRequests} className="auth-link">
            Retry
          </button>
        </div>
      )}

      {isLoading ? (
        <div className="ui-loading-card">
          <span className="spinner" aria-hidden="true"></span>
          <span>Loading your requests...</span>
        </div>
      ) : requests.length === 0 ? (
        <Card className="empty-state-card">
          <div className="empty-state">
            <span className="empty-state__icon">📦</span>
            <h3>No requests yet</h3>
            <p>You haven't posted any product delivery requests yet.</p>
            <Link to="/requests/new">
              <Button variant="primary" size="md" className="mt-2">
                Create First Request
              </Button>
            </Link>
          </div>
        </Card>
      ) : (
        <div className="requests-list">
          {requests.map((req) => (
            <Card key={req.id} className="request-card">
              <div className="request-card__header">
                <div className="request-card__title-box">
                  <h3 className="request-card__title">
                    <Link to={`/requests/${req.id}`} className="request-link">
                      {req.productName}
                    </Link>
                  </h3>
                  {req.category && (
                    <span className="request-card__category">{req.category}</span>
                  )}
                </div>
                <StatusBadge status={req.status} size="md" />
              </div>

              <div className="request-card__meta-grid">
                <div className="meta-item">
                  <span className="meta-label">Quantity:</span>
                  <span className="meta-val">{req.quantity}</span>
                </div>
                <div className="meta-item">
                  <span className="meta-label">Pickup Zone:</span>
                  <span className="meta-val">{req.pickupArea?.replace(/_/g, ' ')}</span>
                </div>
                <div className="meta-item">
                  <span className="meta-label">Budget:</span>
                  <span className="meta-val">
                    {req.budget != null ? `৳${parseFloat(req.budget).toFixed(2)}` : 'N/A'}
                  </span>
                </div>
                {req.payment ? (
                  <div className="meta-item">
                    <span className="meta-label">Total Payment:</span>
                    <span className="meta-val" style={{ fontWeight: 600, color: 'var(--color-primary)' }}>
                      ৳{parseFloat(req.payment.total).toFixed(2)} ({req.payment.status})
                    </span>
                  </div>
                ) : (
                  <div className="meta-item">
                    <span className="meta-label">Requested On:</span>
                    <span className="meta-val">
                      {req.createdAt ? new Date(req.createdAt).toLocaleDateString() : 'N/A'}
                    </span>
                  </div>
                )}
              </div>

              {req.payment?.additionalPaymentStatus === 'REQUESTED' && (
                <div style={{
                  margin: '0.75rem 0 0.5rem 0',
                  padding: '0.5rem 0.75rem',
                  background: '#fffbeb',
                  border: '1px solid #fde68a',
                  borderRadius: 'var(--radius-sm)',
                  fontSize: '0.85rem',
                  color: '#92400e',
                  fontWeight: 600
                }}>
                  ⚠️ Payment Action Required: Partner requested ৳{parseFloat(req.payment.additionalAmount).toFixed(2)} extra (Remaining to pay: ৳{parseFloat(req.payment.additionalTotal).toFixed(2)}). Click details below to fulfill.
                </div>
              )}

              {req.preferredShop && (
                <div className="request-card__sub-detail" style={{ marginTop: '0.5rem' }}>
                  <strong>Shop:</strong> {req.preferredShop}
                </div>
              )}

              <div className="request-card__footer">
                <Link to={`/requests/${req.id}`}>
                  <Button variant="outline" size="sm">
                    View Details & Payment →
                  </Button>
                </Link>
              </div>
            </Card>
          ))}
        </div>
      )}
    </div>
  );
}
