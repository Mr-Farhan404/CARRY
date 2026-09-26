import React, { useEffect, useState, useCallback } from 'react';
import { useParams, Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { Button, Card, StatusBadge, Input } from '../components/common';
import { requestsApi } from '../services/api';

export default function RequestDetailPage() {
  const { id } = useParams();
  const { token, user } = useAuth();

  const [request, setRequest] = useState(null);
  const [timeline, setTimeline] = useState([]);
  const [existingRating, setExistingRating] = useState(null);
  const [isLoading, setIsLoading] = useState(true);
  const [errorMessage, setErrorMessage] = useState('');
  const [successMessage, setSuccessMessage] = useState('');

  // Rating Form State
  const [ratingScore, setRatingScore] = useState(5);
  const [ratingComment, setRatingComment] = useState('');
  const [isSubmittingRating, setIsSubmittingRating] = useState(false);
  const [ratingError, setRatingError] = useState('');

  // Cancel action state
  const [isCancelling, setIsCancelling] = useState(false);
  const [cancelError, setCancelError] = useState('');

  // Additional Payment Form State (for customer)
  const [addPaymentMethod, setAddPaymentMethod] = useState('BKASH');
  const [addSenderPhone, setAddSenderPhone] = useState('');
  const [addTrxId, setAddTrxId] = useState('');
  const [isSubmittingAddPayment, setIsSubmittingAddPayment] = useState(false);
  const [addPaymentError, setAddPaymentError] = useState('');
  const [addPaymentSuccess, setAddPaymentSuccess] = useState('');

  const loadData = useCallback(async () => {
    setIsLoading(true);
    setErrorMessage('');
    try {
      const [reqData, timelineData] = await Promise.all([
        requestsApi.getById(id, token),
        requestsApi.getTimeline(id, token),
      ]);
      setRequest(reqData);
      setTimeline(timelineData || []);

      // If delivered, check if rating exists
      if (reqData.status === 'DELIVERED') {
        try {
          const ratingData = await requestsApi.getRating(id, token);
          setExistingRating(ratingData);
        } catch (err) {
          // If 404, not rated yet - expected behavior
          setExistingRating(null);
        }
      }
    } catch (err) {
      setErrorMessage(err.message || 'Failed to load request details.');
    } finally {
      setIsLoading(false);
    }
  }, [id, token]);

  useEffect(() => {
    loadData();
  }, [loadData]);

  const handleCancelRequest = async () => {
    if (!window.confirm('Are you sure you want to cancel this request?')) return;
    setIsCancelling(true);
    setCancelError('');
    try {
      await requestsApi.cancel(id, token);
      setSuccessMessage('Request has been cancelled.');
      await loadData();
    } catch (err) {
      setCancelError(err.message || 'Failed to cancel request.');
    } finally {
      setIsCancelling(false);
    }
  };

  const handleRatingSubmit = async (e) => {
    e.preventDefault();
    setRatingError('');
    setIsSubmittingRating(true);
    try {
      const payload = {
        score: parseInt(ratingScore, 10),
        comment: ratingComment.trim() || null,
      };
      const createdRating = await requestsApi.rate(id, payload, token);
      setExistingRating(createdRating);
      setSuccessMessage('Thank you! Your rating has been recorded and partner average updated.');
    } catch (err) {
      if (err.status === 409) {
        setRatingError('This request has already been rated.');
      } else {
        setRatingError(err.message || 'Failed to submit rating. Please try again.');
      }
    } finally {
      setIsSubmittingRating(false);
    }
  };

  const handleSubmitAdditionalPayment = async (e) => {
    e.preventDefault();
    setAddPaymentError('');
    setAddPaymentSuccess('');

    if (!addSenderPhone.trim()) {
      setAddPaymentError('Sender phone number is required');
      return;
    }
    if (!/^01[3-9]\d{8}$/.test(addSenderPhone.trim())) {
      setAddPaymentError('Please enter a valid 11-digit mobile number');
      return;
    }
    if (!addTrxId.trim()) {
      setAddPaymentError('Transaction ID (TrxID) is required');
      return;
    }

    setIsSubmittingAddPayment(true);
    try {
      await requestsApi.submitAdditionalPayment(
        id,
        {
          paymentMethod: addPaymentMethod,
          senderPhone: addSenderPhone.trim(),
          trxId: addTrxId.trim().toUpperCase(),
        },
        token
      );
      setAddPaymentSuccess('Remaining payment submitted successfully! The partner can now proceed.');
      setAddSenderPhone('');
      setAddTrxId('');
      await loadData();
    } catch (err) {
      setAddPaymentError(err.message || 'Failed to submit remaining payment.');
    } finally {
      setIsSubmittingAddPayment(false);
    }
  };

  const isCustomer = user && request && request.customerId === user.id;
  const canCancel =
    isCustomer &&
    (request?.status === 'REQUESTED' || request?.status === 'ACCEPTED');

  if (isLoading) {
    return (
      <div className="ui-loading-card">
        <span className="spinner" aria-hidden="true"></span>
        <span>Loading request details...</span>
      </div>
    );
  }

  if (errorMessage || !request) {
    return (
      <div className="request-detail-page">
        <Link to="/requests" className="back-link">
          ← Back to My Requests
        </Link>
        <div className="ui-alert ui-alert--error mt-2" role="alert">
          {errorMessage || 'Request not found'}
        </div>
      </div>
    );
  }

  const payment = request.payment;

  return (
    <div className="request-detail-page">
      <div className="page-header">
        <Link to="/requests" className="back-link">
          ← Back to My Requests
        </Link>
      </div>

      {successMessage && (
        <div className="ui-alert ui-alert--success" role="status">
          {successMessage}
        </div>
      )}

      {cancelError && (
        <div className="ui-alert ui-alert--error" role="alert">
          {cancelError}
        </div>
      )}

      {/* Overview Card */}
      <Card
        title={request.productName}
        subtitle={request.category ? `Category: ${request.category}` : 'General item'}
        headerAction={<StatusBadge status={request.status} size="md" />}
        className="detail-card"
      >
        <div className="detail-meta-grid">
          <div className="detail-meta-item">
            <span className="detail-meta-label">Quantity</span>
            <span className="detail-meta-val">{request.quantity}</span>
          </div>
          <div className="detail-meta-item">
            <span className="detail-meta-label">Pickup Zone</span>
            <span className="detail-meta-val">{request.pickupArea?.replace(/_/g, ' ')}</span>
          </div>
          <div className="detail-meta-item">
            <span className="detail-meta-label">Budget</span>
            <span className="detail-meta-val">
              {request.budget != null ? `৳${parseFloat(request.budget).toFixed(2)}` : 'N/A'}
            </span>
          </div>
          <div className="detail-meta-item">
            <span className="detail-meta-label">Matched Trip ID</span>
            <span className="detail-meta-val">
              {request.matchedTripId ? `#${request.matchedTripId}` : 'Not yet matched'}
            </span>
          </div>
        </div>

        {request.preferredShop && (
          <div className="detail-section">
            <h4 className="detail-section-title">Preferred Shop / Store</h4>
            <p className="detail-section-body">{request.preferredShop}</p>
          </div>
        )}

        {request.instructions && (
          <div className="detail-section">
            <h4 className="detail-section-title">Special Instructions</h4>
            <p className="detail-section-body">{request.instructions}</p>
          </div>
        )}

        {canCancel && (
          <div className="detail-card-actions">
            <Button
              variant="danger"
              size="sm"
              onClick={handleCancelRequest}
              isLoading={isCancelling}
            >
              Cancel This Request
            </Button>
            <span className="cancel-note">
              Cancellation is permitted before partner purchases and collects the item.
            </span>
          </div>
        )}
      </Card>

      {/* Price Adjustment "Need More" Alert & Submission Form */}
      {payment?.additionalPaymentStatus === 'REQUESTED' && (
        <Card
          title="⚠️ Price Adjustment Required (Need More Payment)"
          className="detail-card"
          style={{ border: '2px solid #f59e0b', background: '#fffbeb' }}
        >
          <div style={{ color: '#92400e', marginBottom: '1rem' }}>
            <p style={{ fontWeight: 600, fontSize: '1rem' }}>
              The delivery partner found that the product costs more at the store.
            </p>
            <div style={{
              background: '#fff',
              border: '1px solid #fde68a',
              borderRadius: 'var(--radius-sm)',
              padding: '0.85rem',
              margin: '0.75rem 0'
            }}>
              <div><strong>Additional Product Cost:</strong> ৳{parseFloat(payment.additionalAmount).toFixed(2)}</div>
              <div><strong>MFS Fee (1.39%):</strong> ৳{parseFloat(payment.additionalFee).toFixed(2)}</div>
              <div style={{ fontSize: '1.1rem', fontWeight: 700, color: '#b45309', marginTop: '0.35rem' }}>
                Remaining Amount to Pay: ৳{parseFloat(payment.additionalTotal).toFixed(2)}
              </div>
              <div style={{ marginTop: '0.5rem', fontStyle: 'italic' }}>
                <strong>Reason:</strong> "{payment.needMoreReason}"
              </div>
            </div>
            {!isCustomer && (
              <p style={{ fontSize: '0.9rem', color: '#b45309' }}>
                ⏳ Waiting for customer to fulfill the remaining payment. Collection is paused until payment is submitted.
              </p>
            )}
          </div>

          {/* If the current user is customer, show payment fulfillment form */}
          {isCustomer && (
            <div style={{
              background: '#fff',
              padding: '1.25rem',
              borderRadius: 'var(--radius-sm)',
              border: '1px solid #fde68a'
            }}>
              <h4 style={{ fontWeight: 600, marginBottom: '0.5rem', color: '#1e3a8a' }}>
                Fulfill Remaining Payment (৳{parseFloat(payment.additionalTotal).toFixed(2)})
              </h4>
              <p style={{ fontSize: '0.85rem', color: 'var(--color-text-muted)', marginBottom: '0.75rem' }}>
                Send Money <strong>৳{parseFloat(payment.additionalTotal).toFixed(2)}</strong> to Carry Campus Account:
                <strong style={{ color: 'var(--color-primary)' }}> 01700000000</strong> (bKash / Nagad Personal)
              </p>

              {addPaymentSuccess && (
                <div className="ui-alert ui-alert--success" style={{ marginBottom: '1rem' }}>
                  {addPaymentSuccess}
                </div>
              )}
              {addPaymentError && (
                <div className="ui-alert ui-alert--error" style={{ marginBottom: '1rem' }}>
                  {addPaymentError}
                </div>
              )}

              <form onSubmit={handleSubmitAdditionalPayment}>
                <div style={{ marginBottom: '0.75rem' }}>
                  <label style={{ display: 'block', fontSize: '0.875rem', fontWeight: 600, marginBottom: '0.35rem' }}>
                    Payment Method:
                  </label>
                  <div style={{ display: 'flex', gap: '1rem' }}>
                    <label style={{ display: 'flex', alignItems: 'center', gap: '0.4rem', cursor: 'pointer' }}>
                      <input
                        type="radio"
                        name="addPaymentMethod"
                        value="BKASH"
                        checked={addPaymentMethod === 'BKASH'}
                        onChange={(e) => setAddPaymentMethod(e.target.value)}
                      />
                      <span>bKash</span>
                    </label>
                    <label style={{ display: 'flex', alignItems: 'center', gap: '0.4rem', cursor: 'pointer' }}>
                      <input
                        type="radio"
                        name="addPaymentMethod"
                        value="NAGAD"
                        checked={addPaymentMethod === 'NAGAD'}
                        onChange={(e) => setAddPaymentMethod(e.target.value)}
                      />
                      <span>Nagad</span>
                    </label>
                  </div>
                </div>

                <div className="form-grid">
                  <Input
                    label="Sender Phone"
                    type="text"
                    value={addSenderPhone}
                    onChange={(e) => setAddSenderPhone(e.target.value)}
                    placeholder="017xxxxxxxx"
                    required
                  />
                  <Input
                    label="Transaction ID (TrxID)"
                    type="text"
                    value={addTrxId}
                    onChange={(e) => setAddTrxId(e.target.value)}
                    placeholder="e.g. 9J8K2LA19"
                    required
                  />
                </div>

                <div style={{ marginTop: '1rem' }}>
                  <Button
                    type="submit"
                    variant="primary"
                    size="md"
                    isLoading={isSubmittingAddPayment}
                  >
                    Submit Remaining Payment (৳{parseFloat(payment.additionalTotal).toFixed(2)})
                  </Button>
                </div>
              </form>
            </div>
          )}
        </Card>
      )}

      {/* Remaining Payment Submitted Banner */}
      {payment?.additionalPaymentStatus === 'SUBMITTED' && (
        <div className="ui-alert ui-alert--success" style={{ marginBottom: '1.5rem' }}>
          ✅ <strong>Remaining Payment Submitted:</strong> ৳{parseFloat(payment.additionalTotal).toFixed(2)} via {payment.additionalPaymentMethod} (TrxID: {payment.additionalTrxId}). Delivery partner has been unblocked to proceed with collection.
        </div>
      )}

      {/* Full Payment Breakdown Card */}
      {payment && (
        <Card
          title="Payment Breakdown & MFS Details"
          subtitle="Transparent financial tracking for item cost, campus delivery, and gateway fee"
          className="detail-card"
        >
          <div className="detail-meta-grid">
            <div className="detail-meta-item">
              <span className="detail-meta-label">Product Cost</span>
              <span className="detail-meta-val">৳{parseFloat(payment.productCost).toFixed(2)}</span>
            </div>
            <div className="detail-meta-item">
              <span className="detail-meta-label">Campus Delivery Fee</span>
              <span className="detail-meta-val">৳{parseFloat(payment.deliveryFee).toFixed(2)}</span>
            </div>
            <div className="detail-meta-item">
              <span className="detail-meta-label">MFS Fee (1.39%)</span>
              <span className="detail-meta-val">৳{parseFloat(payment.gatewayFee || 0).toFixed(2)}</span>
            </div>
            <div className="detail-meta-item">
              <span className="detail-meta-label">Initial Total</span>
              <span className="detail-meta-val" style={{ fontWeight: 700, color: 'var(--color-primary)' }}>
                ৳{parseFloat(payment.total).toFixed(2)}
              </span>
            </div>
          </div>

          <div style={{
            marginTop: '1rem',
            paddingTop: '0.75rem',
            borderTop: '1px solid var(--color-border)',
            display: 'flex',
            flexWrap: 'wrap',
            gap: '1.5rem',
            fontSize: '0.9rem'
          }}>
            <div>
              <span style={{ color: 'var(--color-text-muted)' }}>Payment Method: </span>
              <strong>{payment.paymentMethod || 'Simulated / Direct'}</strong>
            </div>
            {payment.senderPhone && (
              <div>
                <span style={{ color: 'var(--color-text-muted)' }}>Sender Phone: </span>
                <strong>{payment.senderPhone}</strong>
              </div>
            )}
            {payment.trxId && (
              <div>
                <span style={{ color: 'var(--color-text-muted)' }}>Initial TrxID: </span>
                <code style={{ background: 'var(--color-bg-app)', padding: '0.2rem 0.4rem', borderRadius: '4px' }}>
                  {payment.trxId}
                </code>
              </div>
            )}
            <div>
              <span style={{ color: 'var(--color-text-muted)' }}>Payment Status: </span>
              <span style={{
                fontWeight: 600,
                color: payment.status === 'VERIFIED' ? '#16a34a' : payment.status === 'SUBMITTED' ? '#2563eb' : '#64748b'
              }}>
                {payment.status}
              </span>
            </div>
          </div>

          {payment.additionalPaymentStatus !== 'NONE' && (
            <div style={{
              marginTop: '1rem',
              paddingTop: '0.75rem',
              borderTop: '1px dashed var(--color-border)',
              background: '#f8fafc',
              padding: '0.75rem',
              borderRadius: 'var(--radius-sm)'
            }}>
              <h5 style={{ fontWeight: 600, marginBottom: '0.25rem' }}>Price Adjustment Details:</h5>
              <div style={{ fontSize: '0.875rem' }}>
                <span>Extra Cost: ৳{parseFloat(payment.additionalAmount).toFixed(2)} + Fee: ৳{parseFloat(payment.additionalFee).toFixed(2)} = <strong>৳{parseFloat(payment.additionalTotal).toFixed(2)}</strong></span>
                {payment.additionalTrxId && (
                  <span style={{ marginLeft: '1rem' }}>Remaining TrxID: <code>{payment.additionalTrxId}</code></span>
                )}
                <span style={{ marginLeft: '1rem', fontWeight: 600 }}>Status: {payment.additionalPaymentStatus}</span>
              </div>
            </div>
          )}
        </Card>
      )}

      {/* Full Status Update Timeline */}
      <Card
        title="Delivery Status Timeline"
        subtitle="Tracking every milestone recorded by the delivery partner"
        className="detail-card"
      >
        {timeline.length === 0 ? (
          <p className="text-muted">
            No status updates have been recorded yet. The order is in REQUESTED state.
          </p>
        ) : (
          <div className="timeline-container">
            {timeline.map((update, idx) => (
              <div key={update.id || idx} className="timeline-step">
                <div className="timeline-marker" aria-hidden="true" />
                <div className="timeline-content">
                  <div className="timeline-header">
                    <StatusBadge status={update.status} size="sm" />
                    <span className="timeline-time">
                      {update.createdAt ? new Date(update.createdAt).toLocaleString() : ''}
                    </span>
                  </div>
                  {update.note && (
                    <p className="timeline-note">{update.note}</p>
                  )}
                </div>
              </div>
            ))}
          </div>
        )}
      </Card>

      {/* Rating Section - Shown Once Status reaches DELIVERED */}
      {request.status === 'DELIVERED' && (
        <Card
          title="Delivery Partner Rating"
          subtitle={
            existingRating
              ? 'Your feedback has been recorded'
              : 'Rate your delivery partner experience'
          }
          className="detail-card"
        >
          {existingRating ? (
            <div className="existing-rating">
              <div className="star-display">
                {[1, 2, 3, 4, 5].map((s) => (
                  <span
                    key={s}
                    className={`star-icon ${s <= existingRating.score ? 'star-icon--filled' : 'star-icon--empty'}`}
                  >
                    ★
                  </span>
                ))}
                <span className="rating-score-label">({existingRating.score} / 5)</span>
              </div>
              {existingRating.comment && (
                <p className="rating-comment">"{existingRating.comment}"</p>
              )}
              <span className="rating-date">
                Submitted on{' '}
                {new Date(existingRating.createdAt).toLocaleDateString()}
              </span>
            </div>
          ) : isCustomer ? (
            <form onSubmit={handleRatingSubmit} className="rating-form">
              {ratingError && (
                <div className="ui-alert ui-alert--error mb-2" role="alert">
                  {ratingError}
                </div>
              )}

              <div className="star-picker">
                {[1, 2, 3, 4, 5].map((s) => (
                  <button
                    key={s}
                    type="button"
                    className={`star-btn ${s <= ratingScore ? 'star-btn--active' : ''}`}
                    onClick={() => setRatingScore(s)}
                    aria-label={`Rate ${s} out of 5 stars`}
                  >
                    ★
                  </button>
                ))}
                <span className="rating-score-label">({ratingScore} / 5)</span>
              </div>

              <div className="ui-input-group mt-2">
                <label htmlFor="rating-comment" className="ui-input-label">
                  Feedback Comment (Optional)
                </label>
                <textarea
                  id="rating-comment"
                  value={ratingComment}
                  onChange={(e) => setRatingComment(e.target.value)}
                  placeholder="Share details about the delivery timeliness, packaging, or communication..."
                  className="ui-input ui-textarea"
                  rows="3"
                />
              </div>

              <Button
                type="submit"
                variant="primary"
                size="md"
                isLoading={isSubmittingRating}
                className="mt-2"
              >
                Submit Rating
              </Button>
            </form>
          ) : (
            <p className="text-muted">Awaiting customer rating feedback.</p>
          )}
        </Card>
      )}
    </div>
  );
}
