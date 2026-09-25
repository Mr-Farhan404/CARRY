import React, { useEffect, useState, useCallback } from 'react';
import { Link, useLocation } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { Button, Card, StatusBadge } from '../components/common';
import { requestsApi, tripsApi } from '../services/api';

export default function AvailableRequestsPage() {
  const { token } = useAuth();
  const location = useLocation();

  const [availableRequests, setAvailableRequests] = useState([]);
  const [activeTrips, setActiveTrips] = useState([]);
  const [selectedTripByRequest, setSelectedTripByRequest] = useState({});
  const [isLoading, setIsLoading] = useState(true);
  const [acceptingId, setAcceptingId] = useState(null);

  const [successMessage, setSuccessMessage] = useState(location.state?.message || '');
  const [conflictMessage, setConflictMessage] = useState('');
  const [errorMessage, setErrorMessage] = useState('');

  const loadData = useCallback(async () => {
    setIsLoading(true);
    setErrorMessage('');
    try {
      const [requestsData, myTripsData] = await Promise.all([
        requestsApi.getAvailable(token),
        tripsApi.getMyTrips(token),
      ]);

      const active = (myTripsData || []).filter(
        (t) => t.status === 'PLANNED' || t.status === 'IN_CITY'
      );

      setAvailableRequests(requestsData || []);
      setActiveTrips(active);

      // Pre-map matching trips for each request
      const tripMap = {};
      (requestsData || []).forEach((req) => {
        const matchingTrip = active.find((t) => t.destinationArea === req.pickupArea);
        if (matchingTrip) {
          tripMap[req.id] = matchingTrip.id;
        }
      });
      setSelectedTripByRequest(tripMap);
    } catch (err) {
      setErrorMessage(err.message || 'Failed to load available delivery requests.');
    } finally {
      setIsLoading(false);
    }
  }, [token]);

  useEffect(() => {
    loadData();
  }, [loadData]);

  const handleAccept = async (req) => {
    const tripId =
      selectedTripByRequest[req.id] ||
      activeTrips.find((t) => t.destinationArea === req.pickupArea)?.id ||
      (activeTrips.length === 1 ? activeTrips[0].id : null);

    if (!tripId) {
      setErrorMessage(
        'Please select which of your active trips will fulfill this request.'
      );
      return;
    }

    setAcceptingId(req.id);
    setConflictMessage('');
    setErrorMessage('');
    setSuccessMessage('');

    try {
      await requestsApi.accept(
        req.id,
        {
          tripId: parseInt(tripId, 10),
          version: req.version,
        },
        token
      );

      setSuccessMessage(
        `Successfully accepted "${req.productName}"! It is now assigned to your deliveries.`
      );
      // Refresh list to reflect latest available orders
      await loadData();
    } catch (err) {
      if (err.status === 409) {
        // Requirement: "If accept returns 409, show a clear 'this was just accepted by someone else' message and refresh the list — never show a raw error or let the UI look stuck."
        setConflictMessage(
          `Notice: "${req.productName}" was just accepted by another delivery partner.`
        );
        // Refresh the list immediately so the UI does not look stuck
        await loadData();
      } else {
        setErrorMessage(err.message || 'Failed to accept this request.');
      }
    } finally {
      setAcceptingId(null);
    }
  };

  const hasNoActiveTrips = !isLoading && activeTrips.length === 0;

  return (
    <div className="available-requests-page">
      <div className="page-header-row">
        <div>
          <h1 className="page-title">Available Requests</h1>
          <p className="page-subtitle">
            Orders placed by students that match the destination zone of your active trips
          </p>
        </div>
        <div className="header-actions">
          <Link to="/trips/new">
            <Button variant="outline" size="md">
              + Post Another Trip
            </Button>
          </Link>
          <Link to="/deliveries">
            <Button variant="primary" size="md">
              My Deliveries →
            </Button>
          </Link>
        </div>
      </div>

      {successMessage && (
        <div className="ui-alert ui-alert--success" role="status">
          {successMessage}
        </div>
      )}

      {/* Clear 409 Conflict Message */}
      {conflictMessage && (
        <div className="ui-alert ui-alert--error" role="alert">
          <strong>Order Conflict:</strong> {conflictMessage} The available requests list has been updated.
        </div>
      )}

      {errorMessage && (
        <div className="ui-alert ui-alert--error" role="alert">
          {errorMessage}{' '}
          <button type="button" onClick={loadData} className="auth-link">
            Retry
          </button>
        </div>
      )}

      {hasNoActiveTrips && (
        <Card className="trip-prompt-card">
          <div className="trip-prompt-content">
            <span className="trip-prompt-icon">🚲</span>
            <div className="trip-prompt-text">
              <h3>No Active Trips Planned</h3>
              <p>
                To see matching student requests, post a planned trip to Khulna city with
                your departure time and destination area.
              </p>
            </div>
            <Link to="/trips/new">
              <Button variant="primary" size="md">
                Post a Trip Now
              </Button>
            </Link>
          </div>
        </Card>
      )}

      {isLoading ? (
        <div className="ui-loading-card">
          <span className="spinner" aria-hidden="true"></span>
          <span>Searching for matching requests...</span>
        </div>
      ) : availableRequests.length === 0 ? (
        <Card className="empty-state-card">
          <div className="empty-state">
            <span className="empty-state__icon">🔍</span>
            <h3>No requests currently available</h3>
            <p>
              There are currently no open student requests matching your active destination
              zones.
            </p>
            {activeTrips.length > 0 && (
              <p className="text-muted mt-2">
                Active trip destinations:{' '}
                {activeTrips.map((t) => t.destinationArea.replace(/_/g, ' ')).join(', ')}
              </p>
            )}
            <Button
              variant="outline"
              size="sm"
              onClick={loadData}
              className="mt-2"
            >
              Refresh Available Requests
            </Button>
          </div>
        </Card>
      ) : (
        <div className="requests-list">
          {availableRequests.map((req) => {
            const matchingTrips = activeTrips.filter(
              (t) => t.destinationArea === req.pickupArea
            );
            const isAccepting = acceptingId === req.id;

            return (
              <Card key={req.id} className="request-card">
                <div className="request-card__header">
                  <div className="request-card__title-box">
                    <h3 className="request-card__title">{req.productName}</h3>
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
                    <span className="meta-label">Posted:</span>
                    <span className="meta-val">
                      {req.createdAt ? new Date(req.createdAt).toLocaleDateString() : 'N/A'}
                    </span>
                  </div>
                </div>

                {req.preferredShop && (
                  <div className="request-card__sub-detail">
                    <strong>Preferred Store:</strong> {req.preferredShop}
                  </div>
                )}

                {req.instructions && (
                  <div className="request-card__sub-detail">
                    <strong>Instructions:</strong> {req.instructions}
                  </div>
                )}

                <div className="available-card-footer">
                  {matchingTrips.length > 1 ? (
                    <div className="trip-select-inline">
                      <label htmlFor={`trip-select-${req.id}`} className="text-muted">
                        Assign to Trip:
                      </label>
                      <select
                        id={`trip-select-${req.id}`}
                        value={selectedTripByRequest[req.id] || ''}
                        onChange={(e) =>
                          setSelectedTripByRequest((prev) => ({
                            ...prev,
                            [req.id]: e.target.value,
                          }))
                        }
                        className="ui-input ui-select ui-select--compact"
                      >
                        {matchingTrips.map((t) => (
                          <option key={t.id} value={t.id}>
                            Trip #{t.id} ({t.destinationArea.replace(/_/g, ' ')})
                          </option>
                        ))}
                      </select>
                    </div>
                  ) : (
                    <span className="text-muted">
                      Matches Trip #{matchingTrips[0]?.id || '1'} ({req.pickupArea?.replace(/_/g, ' ')})
                    </span>
                  )}

                  {/* Accept Button with Graceful 409 Handling */}
                  <Button
                    variant="primary"
                    size="md"
                    isLoading={isAccepting}
                    disabled={isAccepting || matchingTrips.length === 0}
                    onClick={() => handleAccept(req)}
                  >
                    Accept Order
                  </Button>
                </div>
              </Card>
            );
          })}
        </div>
      )}
    </div>
  );
}
