import React, { useEffect, useState, useMemo } from 'react';
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
    paymentMethod: 'BKASH',
    senderPhone: '',
    trxId: '',
  });

  const [errors, setErrors] = useState({});
  const [globalError, setGlobalError] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);

  // Price calculations based on formula:
  // need payment = (product_price + product_price * (13.90 / 1000) + 30) taka
  const costBreakdown = useMemo(() => {
    const rawBudget = parseFloat(formData.budget);
    const productPrice = !isNaN(rawBudget) && rawBudget > 0 ? rawBudget : 0;
    const deliveryFee = 30.0;
    // 13.90 / 1000 = 0.0139
    const gatewayFee = Math.round(productPrice * 0.0139 * 100) / 100;
    const total = Math.round((productPrice + deliveryFee + gatewayFee) * 100) / 100;

    return {
      productPrice,
      deliveryFee,
      gatewayFee,
      total,
    };
  }, [formData.budget]);

  const loadZones = async () => {
    setIsLoadingZones(true);
    setZonesError('');
    try {
      const data = await metaApi.getZones();
      setZones(data || []);
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

    if (formData.budget === '' || formData.budget === null) {
      newErrors.budget = 'Estimated product budget is required for purchase calculation';
    } else {
      const b = parseFloat(formData.budget);
      if (isNaN(b) || b <= 0) {
        newErrors.budget = 'Budget must be greater than 0 taka';
      }
    }

    // MFS Payment validation
    if (!formData.paymentMethod) {
      newErrors.paymentMethod = 'Please select bKash or Nagad';
    }
    if (!formData.senderPhone.trim()) {
      newErrors.senderPhone = 'Sender phone number is required';
    } else if (!/^01[3-9]\d{8}$/.test(formData.senderPhone.trim())) {
      newErrors.senderPhone = 'Enter a valid 11-digit Bangladeshi mobile number (e.g. 01712345678)';
    }

    if (!formData.trxId.trim()) {
      newErrors.trxId = 'Transaction ID (TrxID) is required';
    } else if (formData.trxId.trim().length < 5) {
      newErrors.trxId = 'Please enter a valid TrxID';
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
        budget: parseFloat(formData.budget),
        instructions: formData.instructions.trim() || null,
        paymentMethod: formData.paymentMethod,
        senderPhone: formData.senderPhone.trim(),
        trxId: formData.trxId.trim().toUpperCase(),
      };

      const newRequest = await requestsApi.create(payload, token);
      navigate(`/requests/${newRequest.id}`, {
        state: { message: 'Product request and payment submitted successfully!' },
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
        subtitle="Specify the product you need from Khulna city and pay upfront via bKash / Nagad"
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
              label="Estimated Product Price (BDT ৳)"
              id="req-budget"
              name="budget"
              type="number"
              step="0.01"
              min="1"
              value={formData.budget}
              onChange={handleChange}
              placeholder="e.g. 350.00"
              error={errors.budget}
              helperText="Price of the product alone at the shop"
              required
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
              rows="2"
              value={formData.instructions}
              onChange={handleChange}
              placeholder="Specific brand, model, or instructions for the delivery partner..."
              className="ui-input ui-textarea"
            />
          </div>

          {/* Upfront MFS Payment Section */}
          <div className="payment-upfront-section" style={{
            marginTop: '1.5rem',
            padding: '1.25rem',
            background: 'var(--color-bg-app)',
            border: '1px solid var(--color-border)',
            borderRadius: 'var(--radius-md)'
          }}>
            <h3 style={{ fontSize: '1.1rem', fontWeight: 600, color: 'var(--color-text-main)', marginBottom: '0.5rem' }}>
              💳 Payment Details (bKash / Nagad)
            </h3>
            <p style={{ fontSize: '0.875rem', color: 'var(--color-text-muted)', marginBottom: '1rem' }}>
              Formula: <code>Total = Product Price + Campus Delivery (৳30) + MFS Fee (1.39%)</code>
            </p>

            {/* Price Breakdown Summary */}
            <div style={{
              background: '#fff',
              padding: '1rem',
              borderRadius: 'var(--radius-sm)',
              border: '1px solid var(--color-border)',
              marginBottom: '1.25rem'
            }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '0.4rem', fontSize: '0.9rem' }}>
                <span style={{ color: 'var(--color-text-muted)' }}>Product Price (Estimated):</span>
                <strong>৳{costBreakdown.productPrice.toFixed(2)}</strong>
              </div>
              <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '0.4rem', fontSize: '0.9rem' }}>
                <span style={{ color: 'var(--color-text-muted)' }}>Campus Delivery Charge:</span>
                <strong>৳{costBreakdown.deliveryFee.toFixed(2)}</strong>
              </div>
              <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '0.4rem', fontSize: '0.9rem' }}>
                <span style={{ color: 'var(--color-text-muted)' }}>MFS Cash-out Fee (13.90/1000 = 1.39%):</span>
                <strong>৳{costBreakdown.gatewayFee.toFixed(2)}</strong>
              </div>
              <div style={{
                display: 'flex',
                justifyContent: 'space-between',
                paddingTop: '0.5rem',
                borderTop: '1px dashed var(--color-border)',
                fontSize: '1.1rem',
                fontWeight: 700,
                color: 'var(--color-primary)'
              }}>
                <span>Total Payment Required:</span>
                <span>৳{costBreakdown.total.toFixed(2)}</span>
              </div>
            </div>

            {/* Instruction banner */}
            <div style={{
              background: '#eff6ff',
              border: '1px solid #bfdbfe',
              padding: '0.85rem',
              borderRadius: 'var(--radius-sm)',
              marginBottom: '1.25rem',
              fontSize: '0.875rem',
              color: '#1e40af'
            }}>
              <p style={{ fontWeight: 600, marginBottom: '0.25rem' }}>
                📢 Send Money / Cash In <strong>৳{costBreakdown.total.toFixed(2)}</strong> to Carry Campus Account:
              </p>
              <div style={{ fontSize: '1.05rem', fontWeight: 700, color: '#1d4ed8' }}>
                📱 01700000000 (bKash / Nagad Personal)
              </div>
              <p style={{ fontSize: '0.8rem', marginTop: '0.35rem', color: '#3b82f6' }}>
                After sending money, enter your sender phone number and Transaction ID (TrxID) below.
              </p>
            </div>

            {/* Select Method */}
            <div style={{ marginBottom: '1rem' }}>
              <label style={{ display: 'block', fontWeight: 600, fontSize: '0.9rem', marginBottom: '0.5rem' }}>
                Select Payment Method:
              </label>
              <div style={{ display: 'flex', gap: '1rem' }}>
                <label style={{
                  display: 'flex',
                  alignItems: 'center',
                  gap: '0.5rem',
                  padding: '0.5rem 1rem',
                  border: formData.paymentMethod === 'BKASH' ? '2px solid #e2136e' : '1px solid var(--color-border)',
                  borderRadius: 'var(--radius-sm)',
                  cursor: 'pointer',
                  backgroundColor: formData.paymentMethod === 'BKASH' ? '#fdf2f8' : '#fff',
                  fontWeight: 600,
                  color: formData.paymentMethod === 'BKASH' ? '#be185d' : 'var(--color-text-main)'
                }}>
                  <input
                    type="radio"
                    name="paymentMethod"
                    value="BKASH"
                    checked={formData.paymentMethod === 'BKASH'}
                    onChange={handleChange}
                  />
                  <span>bKash</span>
                </label>

                <label style={{
                  display: 'flex',
                  alignItems: 'center',
                  gap: '0.5rem',
                  padding: '0.5rem 1rem',
                  border: formData.paymentMethod === 'NAGAD' ? '2px solid #f97316' : '1px solid var(--color-border)',
                  borderRadius: 'var(--radius-sm)',
                  cursor: 'pointer',
                  backgroundColor: formData.paymentMethod === 'NAGAD' ? '#fff7ed' : '#fff',
                  fontWeight: 600,
                  color: formData.paymentMethod === 'NAGAD' ? '#c2410c' : 'var(--color-text-main)'
                }}>
                  <input
                    type="radio"
                    name="paymentMethod"
                    value="NAGAD"
                    checked={formData.paymentMethod === 'NAGAD'}
                    onChange={handleChange}
                  />
                  <span>Nagad</span>
                </label>
              </div>
              {errors.paymentMethod && (
                <span className="ui-input-error">{errors.paymentMethod}</span>
              )}
            </div>

            <div className="form-grid">
              <Input
                label="Sender Phone Number"
                id="req-senderPhone"
                name="senderPhone"
                type="text"
                value={formData.senderPhone}
                onChange={handleChange}
                placeholder="017xxxxxxxx"
                error={errors.senderPhone}
                helperText="The mobile number you sent the money from"
                required
              />

              <Input
                label="Transaction ID (TrxID)"
                id="req-trxId"
                name="trxId"
                type="text"
                value={formData.trxId}
                onChange={handleChange}
                placeholder="e.g. 9J8K2LA19"
                error={errors.trxId}
                helperText="SMS confirmation TrxID from bKash/Nagad"
                required
              />
            </div>
          </div>

          <div className="form-actions" style={{ marginTop: '1.5rem' }}>
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
              Submit Order & Payment (৳{costBreakdown.total.toFixed(2)})
            </Button>
          </div>
        </form>
      </Card>
    </div>
  );
}
