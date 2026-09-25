import React, { useEffect, useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { Button, Input, Select, Card } from '../components/common';
import { metaApi, requestsApi } from '../services/api';

export default function NewRequestPage() {
  const { token } = useAuth();
  const navigate = useNavigate();

  const [zones, setZones] = useState([]);
  const [isLoadingZones, setIsLoadingZones] = useState(true);
  const [zonesError, setZonesError] = useState('');

  const [formData, setFormData] = useState({
    productName: '',
    category: '',
    quantity: 1,
    preferredShop: '',
    pickupArea: '',
    budget: '',
    instructions: '',
  });

  const [errors, setErrors] = useState({});
  const [globalError, setGlobalError] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);

  // Requirement: Populate dropdown from GET /api/meta/zones call, do NOT hardcode!
  const loadZones = async () => {
    setIsLoadingZones(true);
    setZonesError('');
    try {
      const data = await metaApi.getZones();
      setZones(data || []);
      if (data && data.length > 0 && !formData.pickupArea) {
        // Pre-select first option or leave blank for placeholder
      }
    } catch (err) {
      setZonesError(err.message || 'Failed to load location zones from server.');
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

    if (!formData.productName.trim()) {
      newErrors.productName = 'Product name is required';
    }

    if (!formData.pickupArea) {
      newErrors.pickupArea = 'Please select a pickup zone in Khulna';
    }

    const qty = parseInt(formData.quantity, 10);
    if (isNaN(qty) || qty < 1) {
      newErrors.quantity = 'Quantity must be at least 1';
    }

    if (formData.budget !== '' && formData.budget !== null) {
      const b = parseFloat(formData.budget);
      if (isNaN(b) || b < 0) {
        newErrors.budget = 'Budget must be a non-negative number';
      }
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
        productName: formData.productName.trim(),
        category: formData.category.trim() || null,
        quantity: parseInt(formData.quantity, 10) || 1,
        preferredShop: formData.preferredShop.trim() || null,
        pickupArea: formData.pickupArea,
        budget: formData.budget !== '' ? parseFloat(formData.budget) : null,
        instructions: formData.instructions.trim() || null,
      };

      const newRequest = await requestsApi.create(payload, token);
      // Navigate to the created request's detail page or to list
      navigate(`/requests/${newRequest.id}`, {
        state: { message: 'Product request placed successfully!' },
      });
    } catch (err) {
      if (err.fields) {
        setErrors(err.fields);
      } else {
        setGlobalError(err.message || 'Failed to submit request. Please try again.');
      }
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="new-request-page">
      <div className="page-header">
        <Link to="/requests" className="back-link">
          ← Back to My Requests
        </Link>
      </div>

      <Card
        title="Create New Delivery Request"
        subtitle="Specify items you need bought in Khulna city and brought back to campus"
        className="request-form-card"
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

        <form onSubmit={handleSubmit} className="request-form" noValidate>
          <Input
            label="Product Name"
            id="req-productName"
            name="productName"
            type="text"
            value={formData.productName}
            onChange={handleChange}
            placeholder="e.g. Arduino Uno R3, Calculus 10th Ed."
            error={errors.productName}
            required
            autoFocus
          />

          <div className="form-grid">
            <Input
              label="Category"
              id="req-category"
              name="category"
              type="text"
              value={formData.category}
              onChange={handleChange}
              placeholder="e.g. Electronics, Stationery, Medicine"
              error={errors.category}
            />

            <Input
              label="Quantity"
              id="req-quantity"
              name="quantity"
              type="number"
              min="1"
              value={formData.quantity}
              onChange={handleChange}
              error={errors.quantity}
              required
            />
          </div>

          <div className="form-grid">
            <Select
              label="Pickup Area (Khulna Zone)"
              id="req-pickupArea"
              name="pickupArea"
              value={formData.pickupArea}
              onChange={handleChange}
              placeholder={isLoadingZones ? 'Loading zones...' : 'Select pickup zone'}
              options={zones}
              error={errors.pickupArea}
              disabled={isLoadingZones}
              helperText="Delivery partners travelling to this zone will be able to accept your order"
              required
            />

            <Input
              label="Estimated Budget (BDT ৳)"
              id="req-budget"
              name="budget"
              type="number"
              step="0.01"
              min="0"
              value={formData.budget}
              onChange={handleChange}
              placeholder="e.g. 450.00"
              error={errors.budget}
              helperText="Approximate cost of the item"
            />
          </div>

          <Input
            label="Preferred Shop / Store"
            id="req-preferredShop"
            name="preferredShop"
            type="text"
            value={formData.preferredShop}
            onChange={handleChange}
            placeholder="e.g. Khulna Book House, Shib Bari Supermarket (optional)"
            error={errors.preferredShop}
          />

          <div className="ui-input-group">
            <label htmlFor="req-instructions" className="ui-input-label">
              Special Instructions
            </label>
            <textarea
              id="req-instructions"
              name="instructions"
              rows="3"
              value={formData.instructions}
              onChange={handleChange}
              placeholder="Specific brand, model, or instructions for the delivery partner..."
              className="ui-input ui-textarea"
            />
          </div>

          <div className="form-actions">
            <Link to="/requests">
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
              Submit Request
            </Button>
          </div>
        </form>
      </Card>
    </div>
  );
}
