import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useCart } from '../../context/CartContext';
import { useAuth } from '../../context/AuthContext';
import { requestsApi, metaApi } from '../../services/api';
import { calculateEstimatedDeliveryCharge } from '../../constants/deliveryCharge';
import Button from '../common/Button';
import Input from '../common/Input';
import Select from '../common/Select';

export default function CartDrawer() {
  const {
    items,
    activeCheckoutItems,
    isDirectCheckout,
    directCheckoutItem,
    isCartOpen,
    closeCart,
    updateQuantity,
    removeFromCart,
    clearCart,
    cartStep,
    setCartStep,
    totalCount,
    estimatedProductTotal,
    estimatedDeliveryTotal,
    gatewayFee,
    grandTotalWithMfs,
    checkoutTotals,
  } = useCart();

  const { isAuthenticated, token } = useAuth();
  const navigate = useNavigate();

  // Zones for pickup area
  const [zones, setZones] = useState([]);
  const [isLoadingZones, setIsLoadingZones] = useState(false);

  // Form state for checkout
  const [checkoutForm, setCheckoutForm] = useState({
    pickupArea: '',
    preferredShop: '',
    instructions: '',
    paymentMethod: 'BKASH',
    senderPhone: '',
    trxId: '',
  });

  const [formErrors, setFormErrors] = useState({});
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [checkoutError, setCheckoutError] = useState(null);
  const [createdOrders, setCreatedOrders] = useState(null);

  // Load pickup zones from API
  useEffect(() => {
    if (isCartOpen) {
      setIsLoadingZones(true);
      metaApi
        .getZones()
        .then((data) => {
          if (data && data.length > 0) {
            setZones(data);
            setCheckoutForm((prev) => ({
              ...prev,
              pickupArea: prev.pickupArea || data[0] || 'NEW_MARKET',
            }));
          }
        })
        .catch(() => {
          const fallback = [
            'NEW_MARKET',
            'SONADANGA',
            'GOLLAMARI',
            'DAULATPUR',
            'KUET_AREA',
            'ELECTRONICS_MARKET',
            'SHIB_BARI',
            'OTHER',
          ];
          setZones(fallback);
          setCheckoutForm((prev) => ({
            ...prev,
            pickupArea: prev.pickupArea || 'NEW_MARKET',
          }));
        })
        .finally(() => setIsLoadingZones(false));
    }
  }, [isCartOpen]);

  if (!isCartOpen) return null;

  const handleFormChange = (e) => {
    const { name, value } = e.target;
    setCheckoutForm((prev) => ({ ...prev, [name]: value }));
    if (formErrors[name]) {
      setFormErrors((prev) => ({ ...prev, [name]: '' }));
    }
    if (checkoutError) setCheckoutError(null);
  };

  const handleProceedToCheckout = () => {
    if (!isAuthenticated) {
      closeCart();
      navigate('/login');
      return;
    }
    if (items.length === 0) return;
    setCartStep('CHECKOUT');
  };

  const validateCheckoutForm = () => {
    const errs = {};
    if (!checkoutForm.pickupArea) {
      errs.pickupArea = 'Please select a Khulna pickup zone';
    }
    if (!checkoutForm.paymentMethod) {
      errs.paymentMethod = 'Please select bKash or Nagad';
    }
    if (!checkoutForm.senderPhone?.trim()) {
      errs.senderPhone = 'Sender phone number is required';
    } else if (!/^01[3-9]\d{8}$/.test(checkoutForm.senderPhone.trim())) {
      errs.senderPhone = 'Enter a valid 11-digit Bangladeshi mobile number (e.g. 01712345678)';
    }
    if (!checkoutForm.trxId?.trim()) {
      errs.trxId = 'Transaction ID (TrxID) is required';
    } else if (checkoutForm.trxId.trim().length < 5) {
      errs.trxId = 'Please enter a valid TrxID (min 5 characters)';
    }
    return errs;
  };

  const handleSubmitOrder = async (e) => {
    if (e) e.preventDefault();
    if (!isAuthenticated) {
      closeCart();
      navigate('/login');
      return;
    }
    if (activeCheckoutItems.length === 0) return;

    const validationErrors = validateCheckoutForm();
    if (Object.keys(validationErrors).length > 0) {
      setFormErrors(validationErrors);
      return;
    }

    setIsSubmitting(true);
    setCheckoutError(null);

    try {
      const payload = activeCheckoutItems.map((item) => ({
        productId: item.product.id,
        quantity: item.quantity,
        pickupArea: checkoutForm.pickupArea,
        preferredShop: checkoutForm.preferredShop.trim() || null,
        instructions: checkoutForm.instructions.trim() || null,
        paymentMethod: checkoutForm.paymentMethod,
        senderPhone: checkoutForm.senderPhone.trim(),
        trxId: checkoutForm.trxId.trim().toUpperCase(),
      }));

      const response = await requestsApi.checkout(payload, token);
      setCreatedOrders({
        requests: response,
        paymentInfo: { ...checkoutForm, totalPaid: checkoutTotals.grandTotal },
        isDirect: isDirectCheckout,
      });

      if (!isDirectCheckout) {
        clearCart();
      }
    } catch (err) {
      console.error('Checkout failed:', err);
      setCheckoutError(err.message || 'Failed to complete checkout. Please try again.');
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleCloseConfirmation = () => {
    setCreatedOrders(null);
    closeCart();
    navigate('/requests');
  };

  const handleBack = () => {
    if (isDirectCheckout) {
      closeCart();
    } else {
      setCartStep('CART');
    }
  };

  return (
    <div className="cart-backdrop" onClick={closeCart}>
      <div className="cart-drawer" onClick={(e) => e.stopPropagation()}>
        {/* Header */}
        <div className="cart-drawer__header">
          <div className="cart-drawer__title-row">
            {cartStep === 'CHECKOUT' && !createdOrders && (
              <button
                type="button"
                className="cart-back-btn"
                onClick={handleBack}
                title={isDirectCheckout ? 'Close direct order' : 'Back to Cart Items'}
                aria-label="Back"
              >
                ←
              </button>
            )}
            <span className="cart-drawer__icon">
              {isDirectCheckout ? '⚡' : '🛒'}
            </span>
            <h2>
              {createdOrders
                ? 'Order Placed!'
                : cartStep === 'CHECKOUT'
                ? isDirectCheckout
                  ? 'Instant Order & Payment'
                  : 'Delivery & Payment'
                : `Your Cart ${totalCount > 0 ? `(${totalCount})` : ''}`}
            </h2>
          </div>
          <button
            type="button"
            className="cart-drawer__close-btn"
            onClick={closeCart}
            aria-label="Close cart"
          >
            ✕
          </button>
        </div>

        {/* Confirmation Screen */}
        {createdOrders ? (
          <div className="cart-drawer__confirmation">
            <div className="cart-confirmation-card">
              <span className="cart-confirmation-icon">🎉</span>
              <h3>Order & Payment Submitted!</h3>
              <p>
                Your request has been placed in <strong>REQUESTED</strong> status with payment verification pending. Once an admin verifies your transaction, campus delivery partners can accept and fulfill it.
              </p>

              {/* Payment Summary Box */}
              <div className="cart-confirmation-payment-box">
                <div className="confirm-row">
                  <span>Pickup Zone:</span>
                  <strong>{createdOrders.paymentInfo.pickupArea?.replace(/_/g, ' ')}</strong>
                </div>
                <div className="confirm-row">
                  <span>Payment Method:</span>
                  <strong>{createdOrders.paymentInfo.paymentMethod}</strong>
                </div>
                <div className="confirm-row">
                  <span>TrxID:</span>
                  <code className="trx-code">{createdOrders.paymentInfo.trxId?.toUpperCase()}</code>
                </div>
                <div className="confirm-row">
                  <span>Sender Phone:</span>
                  <span>{createdOrders.paymentInfo.senderPhone}</span>
                </div>
                <div className="confirm-row confirm-row--total">
                  <span>Total Paid:</span>
                  <strong>৳{createdOrders.paymentInfo.totalPaid?.toFixed(2)}</strong>
                </div>
              </div>

              {/* Created Requests list */}
              <div className="cart-confirmation-list">
                {createdOrders.requests.map((order) => (
                  <div key={order.id} className="cart-confirmation-item">
                    <div className="cart-confirmation-item__info">
                      <span className="cart-confirmation-item__name">{order.productName}</span>
                      <span className="cart-confirmation-item__sub">
                        Req #{order.id} &bull; Qty: {order.quantity} &bull; Unit: ৳{order.unitPriceSnapshot}
                      </span>
                    </div>
                    <div className="cart-confirmation-item__pricing">
                      <div className="cart-confirmation-item__delivery">
                        +৳{order.deliveryCharge} delivery
                      </div>
                      <span className="cart-confirmation-item__status">{order.status}</span>
                    </div>
                  </div>
                ))}
              </div>

              <div className="cart-confirmation-actions">
                <Button variant="primary" onClick={handleCloseConfirmation}>
                  View My Requests
                </Button>
                <Button
                  variant="outline"
                  onClick={() => {
                    setCreatedOrders(null);
                    closeCart();
                  }}
                >
                  Continue Shopping
                </Button>
              </div>
            </div>
          </div>
        ) : cartStep === 'CHECKOUT' ? (
          /* ========================================================================= */
          /* Step 2: Pickup Information & bKash / Nagad Payment Portal                 */
          /* ========================================================================= */
          <div className="cart-drawer__body cart-checkout-scroll">
            {checkoutError && (
              <div className="cart-error-banner" role="alert">
                <strong>Checkout Error:</strong> {checkoutError}
              </div>
            )}

            <form onSubmit={handleSubmitOrder} className="cart-checkout-form" noValidate>
              {/* Product Details (Short Summary) */}
              <div className="cart-form-section cart-product-preview-section">
                <div className="preview-header-row">
                  <h3 className="cart-section-title">📦 Product Details</h3>
                  {isDirectCheckout && (
                    <span className="direct-order-badge">⚡ Direct Order</span>
                  )}
                </div>

                <div className="cart-preview-list">
                  {activeCheckoutItems.map(({ product, quantity }) => {
                    const price = parseFloat(product.estimatedPrice) || 0;
                    const delivery = calculateEstimatedDeliveryCharge(product, quantity);
                    return (
                      <div key={product.id} className="cart-preview-item">
                        <img
                          src={product.imageUrl || 'https://picsum.photos/seed/carry/60/60'}
                          alt={product.name}
                          className="cart-preview-thumb"
                          onError={(e) => {
                            e.target.onerror = null;
                            e.target.src = 'https://picsum.photos/seed/carry/60/60';
                          }}
                        />
                        <div className="cart-preview-info">
                          <h4 className="cart-preview-name">{product.name}</h4>
                          <div className="cart-preview-tags">
                            <span className="preview-tag preview-tag--cat">{product.category}</span>
                            {product.isSensitive && (
                              <span className="preview-tag preview-tag--fragile">Fragile</span>
                            )}
                          </div>
                          <div className="cart-preview-math">
                            <span>Qty: <strong>{quantity}</strong> × ৳{price.toFixed(2)}</span>
                            <span className="preview-deliv">+৳{delivery.toFixed(2)} delivery</span>
                          </div>
                        </div>
                      </div>
                    );
                  })}
                </div>
              </div>

              {/* Pickup Information */}
              <div className="cart-form-section">
                <h3 className="cart-section-title">📍 Pickup Location (Khulna)</h3>
                <p className="cart-section-sub">
                  Choose where the delivery partner should buy and collect your items in Khulna city.
                </p>

                <Select
                  label="Pickup Zone"
                  id="checkout-pickupArea"
                  name="pickupArea"
                  value={checkoutForm.pickupArea}
                  onChange={handleFormChange}
                  options={zones}
                  placeholder={isLoadingZones ? 'Loading zones...' : 'Select pickup zone'}
                  error={formErrors.pickupArea}
                  disabled={isLoadingZones}
                  helperText="Traveling student partners in this zone will see your order"
                  required
                />

                <Input
                  label="Preferred Shop / Market (Optional)"
                  id="checkout-preferredShop"
                  name="preferredShop"
                  value={checkoutForm.preferredShop}
                  onChange={handleFormChange}
                  placeholder="e.g. Shib Bari Store, New Market Plaza"
                />

                <div className="ui-input-group">
                  <label htmlFor="checkout-instructions" className="ui-input-label">
                    Special Delivery Instructions (Optional)
                  </label>
                  <textarea
                    id="checkout-instructions"
                    name="instructions"
                    rows="2"
                    value={checkoutForm.instructions}
                    onChange={handleFormChange}
                    placeholder="Specific hall delivery note or item brand request..."
                    className="ui-input ui-textarea"
                  />
                </div>
              </div>

              {/* Payment Details Section */}
              <div className="cart-form-section cart-payment-section">
                <h3 className="cart-section-title">💳 Upfront Payment (bKash / Nagad)</h3>
                <p className="cart-section-sub">
                  Formula: <code>Total = Products + Delivery + MFS Fee (1.39%)</code>
                </p>

                {/* Price Breakdown Summary */}
                <div className="cart-cost-summary-box">
                  <div className="cost-row">
                    <span>Products Subtotal ({checkoutTotals.count} items):</span>
                    <strong>৳{checkoutTotals.productTotal.toFixed(2)}</strong>
                  </div>
                  <div className="cost-row">
                    <span>Campus Delivery Charge:</span>
                    <strong>৳{checkoutTotals.deliveryTotal.toFixed(2)}</strong>
                  </div>
                  <div className="cost-row">
                    <span>MFS Cash-out Fee (13.90/1000 = 1.39%):</span>
                    <strong>৳{checkoutTotals.gatewayFee.toFixed(2)}</strong>
                  </div>
                  <div className="cost-row cost-row--total">
                    <span>Total Upfront Payment:</span>
                    <span className="total-highlight">৳{checkoutTotals.grandTotal.toFixed(2)}</span>
                  </div>
                </div>

                {/* MFS Send Money Instruction Notice */}
                <div className="cart-mfs-instruction-box">
                  <p className="mfs-instruction-head">
                    📢 Send Money / Cash In <strong>৳{checkoutTotals.grandTotal.toFixed(2)}</strong> to Carry Campus Account:
                  </p>
                  <div className="mfs-account-pill">
                    📱 <strong>01700000000</strong> <span>(bKash / Nagad Personal)</span>
                  </div>
                  <p className="mfs-instruction-note">
                    After sending money from your app, select your method, sender mobile number, and enter the Transaction ID (TrxID) below.
                  </p>
                </div>

                {/* Payment Method Toggle */}
                <div className="cart-method-toggle-wrap">
                  <label className="ui-input-label">Select Payment Method:</label>
                  <div className="cart-method-radios">
                    <label
                      className={`cart-method-radio ${
                        checkoutForm.paymentMethod === 'BKASH' ? 'cart-method-radio--bkash' : ''
                      }`}
                    >
                      <input
                        type="radio"
                        name="paymentMethod"
                        value="BKASH"
                        checked={checkoutForm.paymentMethod === 'BKASH'}
                        onChange={handleFormChange}
                      />
                      <span>bKash</span>
                    </label>

                    <label
                      className={`cart-method-radio ${
                        checkoutForm.paymentMethod === 'NAGAD' ? 'cart-method-radio--nagad' : ''
                      }`}
                    >
                      <input
                        type="radio"
                        name="paymentMethod"
                        value="NAGAD"
                        checked={checkoutForm.paymentMethod === 'NAGAD'}
                        onChange={handleFormChange}
                      />
                      <span>Nagad</span>
                    </label>
                  </div>
                  {formErrors.paymentMethod && (
                    <span className="ui-input-error">{formErrors.paymentMethod}</span>
                  )}
                </div>

                {/* Sender Phone and TrxID */}
                <Input
                  label="Sender Mobile Number"
                  id="checkout-senderPhone"
                  name="senderPhone"
                  type="text"
                  value={checkoutForm.senderPhone}
                  onChange={handleFormChange}
                  placeholder="017xxxxxxxx"
                  error={formErrors.senderPhone}
                  helperText="The bKash/Nagad number you sent money from"
                  required
                />

                <Input
                  label="Transaction ID (TrxID)"
                  id="checkout-trxId"
                  name="trxId"
                  type="text"
                  value={checkoutForm.trxId}
                  onChange={handleFormChange}
                  placeholder="e.g. 9J8K2LA19"
                  error={formErrors.trxId}
                  helperText="SMS confirmation TrxID from bKash or Nagad"
                  required
                />
              </div>

              {/* Submit Buttons */}
              <div className="cart-checkout-actions">
                <Button
                  type="button"
                  variant="outline"
                  onClick={handleBack}
                  disabled={isSubmitting}
                >
                  {isDirectCheckout ? 'Cancel' : '← Back to Items'}
                </Button>
                <Button
                  type="submit"
                  variant="primary"
                  isLoading={isSubmitting}
                  disabled={isSubmitting}
                >
                  Confirm & Pay ৳{checkoutTotals.grandTotal.toFixed(2)}
                </Button>
              </div>
            </form>
          </div>
        ) : (
          /* ========================================================================= */
          /* Step 1: Cart Items Review                                                 */
          /* ========================================================================= */
          <>
            {checkoutError && (
              <div className="cart-error-banner" role="alert">
                <strong>Checkout Error:</strong> {checkoutError}
              </div>
            )}

            <div className="cart-drawer__body">
              {items.length === 0 ? (
                <div className="cart-empty-state">
                  <span className="cart-empty-icon">🛍️</span>
                  <h3>Your cart is empty</h3>
                  <p>Explore campus products in our shop and add your favorite items to order!</p>
                  <Button variant="primary" size="sm" onClick={closeCart}>
                    Browse Shop
                  </Button>
                </div>
              ) : (
                <div className="cart-items-list">
                  {items.map(({ product, quantity }) => {
                    const lineDeliveryFee = calculateEstimatedDeliveryCharge(product, quantity);
                    const lineProductCost = (parseFloat(product.estimatedPrice) || 0) * quantity;

                    return (
                      <div key={product.id} className="cart-item-card">
                        <img
                          src={product.imageUrl || 'https://picsum.photos/seed/carry/100/100'}
                          alt={product.name}
                          className="cart-item-img"
                          onError={(e) => {
                            e.target.onerror = null;
                            e.target.src = 'https://picsum.photos/seed/carry/100/100';
                          }}
                        />

                        <div className="cart-item-details">
                          <div className="cart-item-title-row">
                            <h4 className="cart-item-name">{product.name}</h4>
                            <button
                              type="button"
                              className="cart-item-remove-btn"
                              onClick={() => removeFromCart(product.id)}
                              title="Remove item"
                            >
                              🗑️
                            </button>
                          </div>

                          <div className="cart-item-badges">
                            <span className="cart-badge cart-badge--category">{product.category}</span>
                            {product.isSensitive && (
                              <span className="cart-badge cart-badge--sensitive">Fragile</span>
                            )}
                          </div>

                          <div className="cart-item-pricing">
                            <span className="cart-item-price">
                              ৳{parseFloat(product.estimatedPrice).toFixed(2)} each
                            </span>
                            <span className="cart-item-delivery">
                              Est. delivery: ৳{lineDeliveryFee.toFixed(2)}
                            </span>
                          </div>

                          <div className="cart-item-bottom-row">
                            <div className="cart-quantity-selector">
                              <button
                                type="button"
                                className="qty-btn"
                                onClick={() => updateQuantity(product.id, quantity - 1)}
                                aria-label="Decrease quantity"
                              >
                                −
                              </button>
                              <span className="qty-value">{quantity}</span>
                              <button
                                type="button"
                                className="qty-btn"
                                onClick={() => updateQuantity(product.id, quantity + 1)}
                                aria-label="Increase quantity"
                              >
                                +
                              </button>
                            </div>

                            <div className="cart-item-subtotal">
                              ৳{(lineProductCost + lineDeliveryFee).toFixed(2)}
                            </div>
                          </div>
                        </div>
                      </div>
                    );
                  })}
                </div>
              )}
            </div>

            {/* Footer Summary & Proceed */}
            {items.length > 0 && (
              <div className="cart-drawer__footer">
                <div className="cart-summary-row">
                  <span>Items Subtotal:</span>
                  <span>৳{estimatedProductTotal.toFixed(2)}</span>
                </div>
                <div className="cart-summary-row">
                  <span>Campus Delivery:</span>
                  <span>৳{estimatedDeliveryTotal.toFixed(2)}</span>
                </div>
                <div className="cart-summary-row">
                  <span>MFS Cash-out Fee (1.39%):</span>
                  <span>৳{gatewayFee.toFixed(2)}</span>
                </div>
                <div className="cart-summary-total">
                  <span>Total Upfront Payment:</span>
                  <span className="total-amount">৳{grandTotalWithMfs.toFixed(2)}</span>
                </div>

                <Button
                  variant="primary"
                  className="cart-checkout-btn"
                  onClick={handleProceedToCheckout}
                >
                  {isAuthenticated
                    ? `Proceed to Payment & Delivery (৳${grandTotalWithMfs.toFixed(2)})`
                    : 'Login to Checkout'}
                </Button>
              </div>
            )}
          </>
        )}
      </div>
    </div>
  );
}
