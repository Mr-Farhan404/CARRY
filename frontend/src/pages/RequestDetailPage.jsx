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
          title="Partner Rating & Feedback"
          subtitle="Rate your delivery partner to help build university trust and recompute their public score"
          className="detail-card rating-card"
        >
          {existingRating ? (
            <div className="submitted-rating-box">
              <div className="rating-badge-row">
                <span className="rating-score-display">
                  {'★'.repeat(existingRating.score)}{'☆'.repeat(5 - existingRating.score)}
                </span>
                <span className="rating-score-num">({existingRating.score} / 5)</span>
              </div>
              {existingRating.comment ? (
                <p className="rating-comment-text">"{existingRating.comment}"</p>
              ) : (
                <p className="text-muted">No comment provided.</p>
              )}
              <span className="rating-timestamp">
                Rated on {new Date(existingRating.createdAt).toLocaleDateString()}
              </span>
            </div>
          ) : isCustomer ? (
            <form onSubmit={handleRatingSubmit} className="rating-form">
              {ratingError && (
                <div className="ui-alert ui-alert--error" role="alert">
                  {ratingError}
                </div>
              )}

              <div className="rating-selector-group">
                <label className="ui-input-label">Delivery Score (1 to 5 Stars):</label>
                <div className="star-rating-buttons">
                  {[1, 2, 3, 4, 5].map((star) => (
                    <button
                      key={star}
                      type="button"
                      onClick={() => setRatingScore(star)}
                      className={`star-btn ${ratingScore >= star ? 'star-btn--active' : ''}`}
                      aria-label={`${star} Star${star > 1 ? 's' : ''}`}
                    >
                      ★
                    </button>
                  ))}
                  <span className="star-rating-label">
                    {ratingScore} of 5 Stars
                  </span>
                </div>
              </div>

              <div className="ui-input-group">
                <label htmlFor="rating-comment" className="ui-input-label">
                  Feedback / Comment (optional)
                </label>
                <textarea
                  id="rating-comment"
                  rows="3"
                  value={ratingComment}
                  onChange={(e) => setRatingComment(e.target.value)}
                  placeholder="Share details about punctuality, item condition, or delivery communication..."
                  className="ui-input ui-textarea"
                  maxLength={500}
                />
              </div>

              <Button
                type="submit"
                variant="primary"
                size="md"
                isLoading={isSubmittingRating}
              >
                Submit Delivery Rating
              </Button>
            </form>
          ) : (
            <p className="text-muted">
              Only the customer who ordered this item can submit a rating.
            </p>
          )}
        </Card>
      )}
    </div>
  );
}
