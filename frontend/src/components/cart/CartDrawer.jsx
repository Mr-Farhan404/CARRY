import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useCart } from '../../context/CartContext';
import { useAuth } from '../../context/AuthContext';
import { requestsApi } from '../../services/api';
import { calculateEstimatedDeliveryCharge } from '../../constants/deliveryCharge';
import Button from '../common/Button';

export default function CartDrawer() {
  const {
    items,
    isCartOpen,
    closeCart,
    updateQuantity,
    removeFromCart,
    clearCart,
    totalCount,
    estimatedProductTotal,
    estimatedDeliveryTotal,
    estimatedGrandTotal,
  } = useCart();

  const { isAuthenticated, token } = useAuth();
  const navigate = useNavigate();

  const [isSubmitting, setIsSubmitting] = useState(false);
  const [checkoutError, setCheckoutError] = useState(null);
  const [createdOrders, setCreatedOrders] = useState(null);

  if (!isCartOpen) return null;

  const handleCheckout = async () => {
    if (!isAuthenticated) {
      closeCart();
      navigate('/login');
      return;
    }

    if (items.length === 0) return;

    setIsSubmitting(true);
    setCheckoutError(null);

    try {
      const payload = items.map((item) => ({
        productId: item.product.id,
        quantity: item.quantity,
      }));

      const response = await requestsApi.checkout(payload, token);
      setCreatedOrders(response);
      clearCart();
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

  return (
    <div className="cart-backdrop" onClick={closeCart}>
      <div className="cart-drawer" onClick={(e) => e.stopPropagation()}>
        {/* Header */}
        <div className="cart-drawer__header">
          <div className="cart-drawer__title-row">
            <span className="cart-drawer__icon">🛒</span>
            <h2>Your Cart {totalCount > 0 && <span className="cart-drawer__badge">({totalCount})</span>}</h2>
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
              <h3>Order Placed Successfully!</h3>
              <p>Your campus requests have been created in <strong>REQUESTED</strong> status. Delivery partners traveling to these areas can now accept and fulfill your order.</p>

              <div className="cart-confirmation-list">
                {createdOrders.map((order) => (
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
        ) : (
          <>
            {/* Error Banner */}
            {checkoutError && (
              <div className="cart-error-banner" role="alert">
                <strong>Checkout Error:</strong> {checkoutError}
              </div>
            )}

            {/* Cart Body */}
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

            {/* Footer Summary & Checkout */}
            {items.length > 0 && (
              <div className="cart-drawer__footer">
                <div className="cart-summary-row">
                  <span>Items Subtotal:</span>
                  <span>৳{estimatedProductTotal.toFixed(2)}</span>
                </div>
                <div className="cart-summary-row">
                  <span>Est. Campus Delivery:</span>
                  <span>৳{estimatedDeliveryTotal.toFixed(2)}</span>
                </div>
                <div className="cart-summary-total">
                  <span>Est. Grand Total:</span>
                  <span className="total-amount">৳{estimatedGrandTotal.toFixed(2)}</span>
                </div>
                <p className="cart-summary-footnote">
                  * Binding delivery charge is finalized by the server upon checkout.
                </p>

                <Button
                  variant="primary"
                  className="cart-checkout-btn"
                  onClick={handleCheckout}
                  isLoading={isSubmitting}
                  disabled={items.length === 0 || isSubmitting}
                >
                  {isAuthenticated ? `Checkout (৳${estimatedGrandTotal.toFixed(2)})` : 'Login to Checkout'}
                </Button>
              </div>
            )}
          </>
        )}
      </div>
    </div>
  );
}
