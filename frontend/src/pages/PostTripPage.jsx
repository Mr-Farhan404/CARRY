import React, { useEffect, useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { Button, Input, Select, Card } from '../components/common';
import { metaApi, tripsApi } from '../services/api';

export default function PostTripPage() {
  const { token } = useAuth();
  const navigate = useNavigate();

  const [zones, setZones] = useState([]);
  const [isLoadingZones, setIsLoadingZones] = useState(true);
  const [zonesError, setZonesError] = useState('');

  // Default departure: next hour rounded
  const now = new Date();
  now.setHours(now.getHours() + 1, 0, 0, 0);
  const defaultDeparture = now.toISOString().slice(0, 16);

  const later = new Date(now.getTime() + 3 * 60 * 60 * 1000);
  const defaultReturn = later.toISOString().slice(0, 16);

  const [formData, setFormData] = useState({
    departureTime: defaultDeparture,
    expectedReturnTime: defaultReturn,
    destinationArea: '',
    capacityNotes: '',
  });

  const [errors, setErrors] = useState({});
  const [globalError, setGlobalError] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);

  // Load destination zones from GET /api/meta/zones
  const loadZones = async () => {
    setIsLoadingZones(true);
    setZonesError('');
    try {
      const data = await metaApi.getZones();
      setZones(data || []);
    } catch (err) {
      setZonesError(err.message || 'Failed to load destination zones.');
    } finally {
      setIsLoadingZones(false);
    }
  };

  useEffect(() => {
    loadZones();
  }, []);

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData((prev) => ({ ...prev, [name]: value }));
    if (errors[name]) {
      setErrors((prev) => ({ ...prev, [name]: '' }));
    }
    if (globalError) setGlobalError('');
  };

  const validate = () => {
    const newErrors = {};

    if (!formData.departureTime) {
      newErrors.departureTime = 'Departure time is required';
    }

    if (!formData.expectedReturnTime) {
      newErrors.expectedReturnTime = 'Expected return time is required';
    }

    if (formData.departureTime && formData.expectedReturnTime) {
      const dep = new Date(formData.departureTime);
      const ret = new Date(formData.expectedReturnTime);
      if (ret <= dep) {
        newErrors.expectedReturnTime = 'Expected return time must be strictly after departure time';
      }
    }

    if (!formData.destinationArea) {
      newErrors.destinationArea = 'Please select a destination zone in Khulna';
    }

    return newErrors;
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setGlobalError('');
    setErrors({});

    const validationErrors = validate();
    if (Object.keys(validationErrors).length > 0) {
      setErrors(validationErrors);
      return;
    }

    setIsSubmitting(true);
    try {
      const payload = {
        departureTime: formData.departureTime,
        expectedReturnTime: formData.expectedReturnTime,
        destinationArea: formData.destinationArea,
        capacityNotes: formData.capacityNotes.trim() || null,
      };

      await tripsApi.create(payload, token);

      // Once trip is posted, partner can immediately browse available matching requests!
      navigate('/requests/available', {
        state: {
          message: 'Trip posted successfully! Here are the available requests matching your trip destination.',
        },
      });
    } catch (err) {
      if (err.fields) {
        setErrors(err.fields);
      } else {
        setGlobalError(err.message || 'Failed to post trip. Please try again.');
      }
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="post-trip-page">
      <div className="page-header">
        <Link to="/deliveries" className="back-link">
          ← Back to My Deliveries
        </Link>
      </div>

      <Card
        title="Post a Planned Trip"
        subtitle="Let campus peers know you are travelling to Khulna city and can bring items back"
        className="trip-form-card"
      >
        {globalError && (
          <div className="ui-alert ui-alert--error" role="alert">
            {globalError}
          </div>
        )}

        {zonesError && (
          <div className="ui-alert ui-alert--error" role="alert">
            {zonesError}{' '}
            <button type="button" onClick={loadZones} className="auth-link">
              Retry
            </button>
          </div>
        )}

        <form onSubmit={handleSubmit} className="trip-form" noValidate>
          <Select
            label="Destination Zone (Khulna City)"
            id="trip-destinationArea"
            name="destinationArea"
            value={formData.destinationArea}
            onChange={handleChange}
            placeholder={isLoadingZones ? 'Loading zones...' : 'Select destination zone'}
            options={zones}
            error={errors.destinationArea}
            disabled={isLoadingZones}
            helperText="Customer requests matching this zone will appear in your Available Requests"
            required
          />

          <div className="form-grid">
            <Input
              label="Departure Time"
              id="trip-departureTime"
              name="departureTime"
              type="datetime-local"
              value={formData.departureTime}
              onChange={handleChange}
              error={errors.departureTime}
              required
            />

            <Input
              label="Expected Return Time"
              id="trip-expectedReturnTime"
              name="expectedReturnTime"
              type="datetime-local"
              value={formData.expectedReturnTime}
              onChange={handleChange}
              error={errors.expectedReturnTime}
              required
              helperText="Must be after departure time"
            />
          </div>

          <div className="ui-input-group">
            <label htmlFor="trip-capacityNotes" className="ui-input-label">
              Capacity & Vehicle Notes (optional)
            </label>
            <textarea
              id="trip-capacityNotes"
              name="capacityNotes"
              rows="3"
              value={formData.capacityNotes}
              onChange={handleChange}
              placeholder="e.g. Travelling by bus, can carry light items or books; no fragile electronics..."
              className="ui-input ui-textarea"
            />
          </div>

          <div className="form-actions">
            <Link to="/deliveries">
              <Button variant="outline" size="md">
                Cancel
              </Button>
            </Link>
            <Button
              type="submit"
              variant="primary"
              size="md"
              isLoading={isSubmitting}
            >
              Post Trip & Match Orders
            </Button>
          </div>
        </form>
      </Card>
    </div>
  );
}
