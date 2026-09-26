import React, { useEffect, useState, useCallback } from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { Button, Card, StatusBadge, Input } from '../components/common';
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

  // "Need More" Modal / Inline form state
  const [needMoreModalId, setNeedMoreModalId] = useState(null);
  const [needMoreAmount, setNeedMoreAmount] = useState('');
  const [needMoreReason, setNeedMoreReason] = useState('');
  const [isSubmittingNeedMore, setIsSubmittingNeedMore] = useState(false);
  const [needMoreError, setNeedMoreError] = useState('');

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

    // Check frontend guard if price adjustment is requested
    if (req.status === 'ACCEPTED' && req.payment?.additionalPaymentStatus === 'REQUESTED') {
      setErrorMessage(
        `Cannot collect: awaiting customer payment for the requested price increase (৳${parseFloat(req.payment.additionalTotal).toFixed(2)}).`
      );
      return;
    }

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
      setNotesByRequest((prev) => ({ ...prev, [req.id]: '' }));
      setActiveNoteInputId(null);

      await loadDeliveries();
    } catch (err) {
      setErrorMessage(err.message || 'Failed to update order status.');
    } finally {
      setUpdatingId(null);
    }
  };

  const handleOpenNeedMore = (reqId) => {
    setNeedMoreModalId(reqId);
    setNeedMoreAmount('');
    setNeedMoreReason('');
    setNeedMoreError('');
  };

  const handleCloseNeedMore = () => {
    setNeedMoreModalId(null);
    setNeedMoreAmount('');
    setNeedMoreReason('');
    setNeedMoreError('');
  };

  const handleSubmitNeedMore = async (reqId) => {
    setNeedMoreError('');
    const amt = parseFloat(needMoreAmount);
    if (isNaN(amt) || amt < 1) {
      setNeedMoreError('Please enter a valid additional amount (at least 1 taka)');
      return;
    }
    if (!needMoreReason.trim()) {
      setNeedMoreError('Please provide a reason for the price increase');
      return;
    }

    setIsSubmittingNeedMore(true);
    try {
      await requestsApi.needMore(
        reqId,
        {
          additionalAmount: amt,
          reason: needMoreReason.trim(),
        },
        token
      );

      const fee = Math.round(amt * 0.0139 * 100) / 100;
      const total = Math.round((amt + fee) * 100) / 100;
      setSuccessMessage(
        `Price adjustment sent to customer! ৳${amt.toFixed(2)} extra requested (+ ৳${fee.toFixed(2)} fee = ৳${total.toFixed(2)}). Order is paused until customer pays.`
      );
      handleCloseNeedMore();
      await loadDeliveries();
    } catch (err) {
      setNeedMoreError(err.message || 'Failed to request additional payment');
    } finally {
      setIsSubmittingNeedMore(false);
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
            const isAwaitingCustomerPayment =
              req.status === 'ACCEPTED' && req.payment?.additionalPaymentStatus === 'REQUESTED';
            const isRemainingPaymentSubmitted =
              req.payment?.additionalPaymentStatus === 'SUBMITTED' || req.payment?.additionalPaymentStatus === 'VERIFIED';
            const isNeedMoreOpen = needMoreModalId === req.id;

            const extraAmtNum = parseFloat(needMoreAmount) || 0;
            const extraFee = Math.round(extraAmtNum * 0.0139 * 100) / 100;
            const extraTotal = Math.round((extraAmtNum + extraFee) * 100) / 100;

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

                {/* Price Adjustment Status Notices */}
                {isAwaitingCustomerPayment && (
                  <div style={{
                    marginTop: '0.75rem',
                    padding: '0.75rem',
                    background: '#fffbeb',
                    border: '1px solid #fde68a',
                    borderRadius: 'var(--radius-sm)',
                    fontSize: '0.875rem',
                    color: '#92400e'
                  }}>
                    <strong>⏳ Waiting for Customer Payment:</strong> You requested ৳{parseFloat(req.payment.additionalAmount).toFixed(2)} extra (Total with fee: ৳{parseFloat(req.payment.additionalTotal).toFixed(2)}). Collection is paused until customer pays.
                  </div>
                )}

                {isRemainingPaymentSubmitted && req.payment?.additionalPaymentStatus === 'SUBMITTED' && (
                  <div style={{
                    marginTop: '0.75rem',
                    padding: '0.75rem',
                    background: '#f0fdf4',
                    border: '1px solid #bbf7d0',
                    borderRadius: 'var(--radius-sm)',
                    fontSize: '0.875rem',
                    color: '#166534'
                  }}>
                    <strong>✅ Customer Paid:</strong> ৳{parseFloat(req.payment.additionalTotal).toFixed(2)} fulfilled via {req.payment.additionalPaymentMethod} (TrxID: <code>{req.payment.additionalTrxId}</code>). You are unblocked to collect!
                  </div>
                )}

                {/* Need More Form Drawer / Modal */}
                {isNeedMoreOpen && (
                  <div style={{
                    marginTop: '1rem',
                    padding: '1rem',
                    background: '#f8fafc',
                    border: '1px solid #cbd5e1',
                    borderRadius: 'var(--radius-md)'
                  }}>
                    <h4 style={{ fontWeight: 600, color: 'var(--color-text-main)', marginBottom: '0.5rem' }}>
                      Request Price Increase (Need More Payment)
                    </h4>
                    <p style={{ fontSize: '0.85rem', color: 'var(--color-text-muted)', marginBottom: '0.75rem' }}>
                      If the actual store price is higher than estimated budget (৳{parseFloat(req.budget).toFixed(2)}), enter the additional product cost.
                    </p>

                    {needMoreError && (
                      <div className="ui-alert ui-alert--error" style={{ marginBottom: '0.75rem' }}>
                        {needMoreError}
                      </div>
                    )}

                    <div className="form-grid">
                      <Input
                        label="Extra Product Cost Needed (৳)"
                        type="number"
                        min="1"
                        step="0.01"
                        value={needMoreAmount}
                        onChange={(e) => setNeedMoreAmount(e.target.value)}
                        placeholder="e.g. 40.00"
                        required
                      />
                      <Input
                        label="Reason for Increase"
                        type="text"
                        value={needMoreReason}
                        onChange={(e) => setNeedMoreReason(e.target.value)}
                        placeholder="e.g. Actual retail price at shop is 390 taka"
                        required
                      />
                    </div>

                    {extraAmtNum > 0 && (
                      <div style={{
                        marginTop: '0.5rem',
                        marginBottom: '0.75rem',
                        fontSize: '0.85rem',
                        background: '#eff6ff',
                        padding: '0.5rem 0.75rem',
                        borderRadius: '4px',
                        color: '#1e40af'
                      }}>
                        Calculation: Extra ৳{extraAmtNum.toFixed(2)} + MFS Fee (1.39%: ৳{extraFee.toFixed(2)}) = <strong>৳{extraTotal.toFixed(2)} Customer will be billed</strong>
                      </div>
                    )}

                    <div style={{ display: 'flex', gap: '0.5rem', marginTop: '0.75rem' }}>
                      <Button
                        variant="primary"
                        size="sm"
                        isLoading={isSubmittingNeedMore}
                        onClick={() => handleSubmitNeedMore(req.id)}
                      >
                        Submit "Need More" Request
                      </Button>
                      <Button
                        variant="outline"
                        size="sm"
                        onClick={handleCloseNeedMore}
                      >
                        Cancel
                      </Button>
                    </div>
                  </div>
                )}

                {/* Transition Action Panel */}
                <div className="delivery-action-panel">
                  {transition ? (
                    <div className="advance-flow">
                      <div className="advance-header">
                        <span className="advance-hint">
                          {isAwaitingCustomerPayment
                            ? 'Paused: Awaiting customer remaining payment.'
                            : transition.hint}
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

                      <div className="advance-btn-row" style={{ display: 'flex', gap: '0.75rem', flexWrap: 'wrap', alignItems: 'center' }}>
                        <Button
                          variant={transition.buttonVariant}
                          size="md"
                          isLoading={isUpdating}
                          disabled={isUpdating || isAwaitingCustomerPayment}
                          onClick={() => handleAdvanceStatus(req)}
                        >
                          → {transition.buttonLabel}
                        </Button>

                        {/* "Need More" button available at ACCEPTED status before collection */}
                        {req.status === 'ACCEPTED' && !isAwaitingCustomerPayment && !isNeedMoreOpen && (
                          <Button
                            variant="outline"
                            size="md"
                            style={{ borderColor: '#f59e0b', color: '#b45309' }}
                            onClick={() => handleOpenNeedMore(req.id)}
                          >
                            💰 Need More (Price Increase)
                          </Button>
                        )}
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
