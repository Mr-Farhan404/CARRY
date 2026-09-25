import React, { useEffect, useState, useCallback } from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { Button, Card, StatusBadge } from '../components/common';
import { tripsApi } from '../services/api';

const NEXT_TRIP_TRANSITION_MAP = {
  PLANNED: {
    nextStatus: 'IN_CITY',
    buttonLabel: 'Mark as In City',
    buttonVariant: 'primary'
  },
  IN_CITY: {
    nextStatus: 'RETURNING',
    buttonLabel: 'Mark as Returning',
    buttonVariant: 'primary'
  },
  RETURNING: {
    nextStatus: 'COMPLETED',
    buttonLabel: 'Mark as Completed',
    buttonVariant: 'primary'
  }
};

export default function MyTripsPage() {
  const { token } = useAuth();
  const [trips, setTrips] = useState([]);
  const [isLoading, setIsLoading] = useState(true);
  const [updatingId, setUpdatingId] = useState(null);
  const [errorMessage, setErrorMessage] = useState('');
  const [successMessage, setSuccessMessage] = useState('');

  const loadTrips = useCallback(async () => {
    setIsLoading(true);
    setErrorMessage('');
    try {
      const data = await tripsApi.getMyTrips(token);
      setTrips(data || []);
    } catch (err) {
      setErrorMessage(err.message || 'Failed to load your trips.');
    } finally {
      setIsLoading(false);
    }
  }, [token]);

  useEffect(() => {
    loadTrips();
  }, [loadTrips]);

  const handleAdvanceStatus = async (trip) => {
    const transition = NEXT_TRIP_TRANSITION_MAP[trip.status];
    if (!transition) return;
    setUpdatingId(trip.id);
    setErrorMessage('');
    setSuccessMessage('');
    try {
      await tripsApi.updateStatus(trip.id, transition.nextStatus, token);
      setSuccessMessage(`Trip #${trip.id} updated to ${transition.nextStatus}!`);
      await loadTrips();
    } catch (err) {
      setErrorMessage(err.message || 'Failed to update trip status.');
    } finally {
      setUpdatingId(null);
    }
  };

  return (
    <div className="my-trips-page">
      <div className="page-header-row">
        <div>
          <h1 className="page-title">My Trips</h1>
          <p className="page-subtitle">Manage your posted trips</p>
        </div>
        <Link to="/trips/new">
          <Button variant="primary" size="md">+ Post New Trip</Button>
        </Link>
      </div>

      {successMessage && <div className="ui-alert ui-alert--success">{successMessage}</div>}
      {errorMessage && <div className="ui-alert ui-alert--error">{errorMessage}</div>}

      {isLoading ? (
        <div className="ui-loading-card">Loading...</div>
      ) : trips.length === 0 ? (
        <Card className="empty-state-card">
          <div className="empty-state">
            <h3>No Trips Found</h3>
            <p>You haven't posted any trips yet.</p>
          </div>
        </Card>
      ) : (
        <div className="requests-list">
          {trips.map((trip) => {
            const transition = NEXT_TRIP_TRANSITION_MAP[trip.status];
            const isUpdating = updatingId === trip.id;
            return (
              <Card key={trip.id} className="delivery-card">
                <div className="request-card__header">
                  <h3 className="request-card__title">Trip #{trip.id} to {trip.destinationArea}</h3>
                  <StatusBadge status={trip.status} />
                </div>
                <div className="delivery-action-panel mt-2">
                  {transition ? (
                    <Button
                      variant={transition.buttonVariant}
                      size="sm"
                      isLoading={isUpdating}
                      disabled={isUpdating}
                      onClick={() => handleAdvanceStatus(trip)}
                    >
                      → {transition.buttonLabel}
                    </Button>
                  ) : (
                    <span className="text-muted">No further transitions</span>
                  )}
                </div>
              </Card>
            );
          })}
        </div>
      )}
    </div>
  );
}
