import React, { useEffect, useState, useCallback } from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { Button, Card, StatusBadge } from '../components/common';
import { requestsApi } from '../services/api';

// Fixed transition mapping mirroring the backend state machine
const NEXT_TRANSITION_MAP = {
  ACCEPTED: {
    nextStatus: 'COLLECTED',
    buttonLabel: 'Mark as Collected',
    buttonVariant: 'primary',
    promptPlaceholder: 'e.g. Purchased items from shop',
    hint: 'Advance once you have purchased/collected the items from the vendor.',
  },
  COLLECTED: {
    nextStatus: 'RETURNING',
    buttonLabel: 'Mark as Returning',
    buttonVariant: 'primary',
    promptPlaceholder: 'e.g. On the bus heading back to campus',
    hint: 'Advance when you start travelling back to KUET.',
  },
  RETURNING: {
    nextStatus: 'READY_FOR_DELIVERY',
    buttonLabel: 'Mark Ready for Delivery',
    buttonVariant: 'primary',
    promptPlaceholder: 'e.g. Arrived on campus, waiting near Hall 3',
    hint: 'Advance when you are on campus ready to meet the customer.',
  },
  READY_FOR_DELIVERY: {
    nextStatus: 'DELIVERED',
    buttonLabel: 'Mark Delivered (Complete)',
    buttonVariant: 'primary',
    promptPlaceholder: 'e.g. Handed over to student successfully',
    hint: 'Advance when the item is physically handed to the customer.',
  },
};

export default function MyDeliveriesPage() {
  const { token } = useAuth();

  const [deliveries, setDeliveries] = useState([]);
  const [isLoading, setIsLoading] = useState(true);
  const [updatingId, setUpdatingId] = useState(null);
  const [activeNoteInputId, setActiveNoteInputId] = useState(null);
  const [notesByRequest, setNotesByRequest] = useState({});

  const [errorMessage, setErrorMessage] = useState('');
  const [successMessage, setSuccessMessage] = useState('');

  const loadDeliveries = useCallback(async () => {
    setIsLoading(true);
    setErrorMessage('');
    try {
      const data = await requestsApi.getAssigned(token);
      setDeliveries(data || []);
    } catch (err) {
      setErrorMessage(err.message || 'Failed to load your assigned deliveries.');
    } finally {
      setIsLoading(false);
    }
  }, [token]);

  useEffect(() => {
    loadDeliveries();
  }, [loadDeliveries]);

  const handleAdvanceStatus = async (req) => {
    const transition = NEXT_TRANSITION_MAP[req.status];
    if (!transition) return;

    setUpdatingId(req.id);
    setErrorMessage('');
    setSuccessMessage('');

    try {
      const note = notesByRequest[req.id]?.trim() || null;
      await requestsApi.updateStatus(
        req.id,
        {
          status: transition.nextStatus,
          note,
        },
        token
      );

      setSuccessMessage(
        `Order "${req.productName}" updated to ${transition.nextStatus.replace(/_/g, ' ')}!`
      );
      // Clear note
      setNotesByRequest((prev) => ({ ...prev, [req.id]: '' }));
      setActiveNoteInputId(null);

      // Refresh deliveries
      await loadDeliveries();
    } catch (err) {
      setErrorMessage(err.message || 'Failed to update order status.');
    } finally {
      setUpdatingId(null);
    }
  };

  return (
    <div className="my-deliveries-page">
      <div className="page-header-row">
        <div>
          <h1 className="page-title">My Deliveries</h1>
          <p className="page-subtitle">
            Manage your accepted orders and progress them step-by-step to delivery
          </p>
        </div>
        <div className="header-actions">
          <Link to="/requests/available">
            <Button variant="outline" size="md">
              Find More Requests
            </Button>
          </Link>
          <Link to="/trips/new">
            <Button variant="primary" size="md">
              + Post New Trip
            </Button>
          </Link>
        </div>
      </div>

      {successMessage && (
        <div className="ui-alert ui-alert--success" role="status">
          {successMessage}
        </div>
      )}

      {errorMessage && (
        <div className="ui-alert ui-alert--error" role="alert">
          {errorMessage}{' '}
          <button type="button" onClick={loadDeliveries} className="auth-link">
            Retry
          </button>
        </div>
      )}

      {isLoading ? (
        <div className="ui-loading-card">
          <span className="spinner" aria-hidden="true"></span>
          <span>Loading your assigned deliveries...</span>
        </div>
      ) : deliveries.length === 0 ? (
        <Card className="empty-state-card">
          <div className="empty-state">
            <span className="empty-state__icon">🚴</span>
            <h3>No Active Deliveries</h3>
            <p>
              You haven't accepted any delivery orders yet. Post a trip or check the
              available requests page to start earning delivery fees!
            </p>
            <div className="empty-state-actions mt-2">
              <Link to="/requests/available">
                <Button variant="primary" size="md">
                  Browse Available Requests
                </Button>
              </Link>
            </div>
          </div>
        </Card>
      ) : (
        <div className="requests-list">
          {deliveries.map((req) => {
            const transition = NEXT_TRANSITION_MAP[req.status];
            const isUpdating = updatingId === req.id;
            const isDelivered = req.status === 'DELIVERED';
            const showNoteInput = activeNoteInputId === req.id;

            return (
              <Card key={req.id} className="delivery-card">
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
                    <span className="meta-val">
                      {req.pickupArea?.replace(/_/g, ' ')}
                    </span>
                  </div>
                  <div className="meta-item">
                    <span className="meta-label">Budget:</span>
                    <span className="meta-val">
                      {req.budget != null ? `৳${parseFloat(req.budget).toFixed(2)}` : 'N/A'}
                    </span>
                  </div>
                  <div className="meta-item">
                    <span className="meta-label">Trip ID:</span>
                    <span className="meta-val">#{req.matchedTripId}</span>
                  </div>
                </div>

                {req.preferredShop && (
                  <div className="request-card__sub-detail">
                    <strong>Shop:</strong> {req.preferredShop}
                  </div>
                )}

                {req.instructions && (
                  <div className="request-card__sub-detail">
                    <strong>Instructions:</strong> {req.instructions}
                  </div>
                )}

                {/* Transition Action Panel */}
                <div className="delivery-action-panel">
                  {transition ? (
                    <div className="advance-flow">
                      <div className="advance-header">
                        <span className="advance-hint">
                          {transition.hint}
                        </span>
                        {!showNoteInput && (
                          <button
                            type="button"
                            onClick={() => setActiveNoteInputId(req.id)}
                            className="note-toggle-btn"
                          >
                            + Add progress note
                          </button>
                        )}
                      </div>

                      {showNoteInput && (
                        <div className="advance-note-box">
                          <input
                            type="text"
                            placeholder={transition.promptPlaceholder}
                            value={notesByRequest[req.id] || ''}
                            onChange={(e) =>
                              setNotesByRequest((prev) => ({
                                ...prev,
                                [req.id]: e.target.value,
                              }))
                            }
                            className="ui-input ui-input--compact"
                          />
                        </div>
                      )}

                      {/* Requirement: only the single valid next status-advance button */}
                      <div className="advance-btn-row">
                        <Button
                          variant={transition.buttonVariant}
                          size="md"
                          isLoading={isUpdating}
                          disabled={isUpdating}
                          onClick={() => handleAdvanceStatus(req)}
                        >
                          → {transition.buttonLabel}
                        </Button>
                      </div>
                    </div>
                  ) : isDelivered ? (
                    <div className="delivered-banner">
                      <span className="delivered-icon">✓</span>
                      <span>Delivery completed! Order is fulfilled.</span>
                    </div>
                  ) : (
                    <div className="status-note text-muted">
                      Order status: {req.status}
                    </div>
                  )}

                  <div className="delivery-links">
                    <Link to={`/requests/${req.id}`}>
                      <Button variant="ghost" size="sm">
                        View Timeline Details →
                      </Button>
                    </Link>
                  </div>
                </div>
              </Card>
            );
          })}
        </div>
      )}
    </div>
  );
}
