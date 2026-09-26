import React, { createContext, useContext, useState, useMemo } from 'react';
import { calculateEstimatedDeliveryCharge } from '../constants/deliveryCharge';

const CartContext = createContext(null);

export function CartProvider({ children }) {
  // Items array: [{ product, quantity }] for standard cart
  const [items, setItems] = useState([]);
  const [isCartOpen, setIsCartOpen] = useState(false);
  const [cartStep, setCartStep] = useState('CART'); // 'CART' | 'CHECKOUT'

  // Dedicated single-item direct checkout (does NOT mix with cart items)
  const [directCheckoutItem, setDirectCheckoutItem] = useState(null);

  // Add product to cart (or increase quantity if already present)
  const addToCart = (product, quantity = 1) => {
    const qtyToAdd = Math.max(1, parseInt(quantity, 10) || 1);
    setItems((prevItems) => {
      const existingIndex = prevItems.findIndex(
        (item) => item.product.id === product.id
      );
      if (existingIndex > -1) {
        const updated = [...prevItems];
        updated[existingIndex] = {
          ...updated[existingIndex],
          quantity: updated[existingIndex].quantity + qtyToAdd,
        };
        return updated;
      }
      return [...prevItems, { product, quantity: qtyToAdd }];
    });
  };

  // Update specific item's quantity in cart
  const updateQuantity = (productId, newQuantity) => {
    const qty = parseInt(newQuantity, 10);
    if (isNaN(qty) || qty <= 0) {
      removeFromCart(productId);
      return;
    }
    setItems((prevItems) =>
      prevItems.map((item) =>
        item.product.id === productId ? { ...item, quantity: qty } : item
      )
    );
  };

  // Remove item completely from cart
  const removeFromCart = (productId) => {
    setItems((prevItems) =>
      prevItems.filter((item) => item.product.id !== productId)
    );
  };

  // Clear all items in cart
  const clearCart = () => {
    setItems([]);
    setDirectCheckoutItem(null);
    setCartStep('CART');
  };

  // Open standard cart drawer
  const openCart = () => {
    setDirectCheckoutItem(null);
    setCartStep('CART');
    setIsCartOpen(true);
  };

  // Open checkout for all items currently in cart
  const openCheckout = () => {
    setDirectCheckoutItem(null);
    setCartStep('CHECKOUT');
    setIsCartOpen(true);
  };

  // Open direct checkout for a specific product ONLY (does not touch or include cart items)
  const openDirectCheckout = (product, quantity = 1) => {
    const qty = Math.max(1, parseInt(quantity, 10) || 1);
    setDirectCheckoutItem({ product, quantity: qty });
    setCartStep('CHECKOUT');
    setIsCartOpen(true);
  };

  const closeCart = () => {
    setIsCartOpen(false);
    setDirectCheckoutItem(null);
    setCartStep('CART');
  };

  const toggleCart = () => setIsCartOpen((prev) => !prev);

  // Active items being checked out:
  // If direct checkout is active, ONLY that single item is checked out.
  // Otherwise, all items in the cart are checked out.
  const activeCheckoutItems = useMemo(() => {
    return directCheckoutItem ? [directCheckoutItem] : items;
  }, [directCheckoutItem, items]);

  // Calculations for cart badges and totals
  const { totalCount, estimatedProductTotal, estimatedDeliveryTotal, gatewayFee, grandTotalWithMfs } =
    useMemo(() => {
      let count = 0;
      let productTotal = 0;
      let deliveryTotal = 0;

      items.forEach(({ product, quantity }) => {
        count += quantity;
        const price = parseFloat(product.estimatedPrice) || 0;
        productTotal += price * quantity;
        deliveryTotal += calculateEstimatedDeliveryCharge(product, quantity);
      });

      const mfs = Math.round(productTotal * 0.0139 * 100) / 100;
      const grand = Math.round((productTotal + deliveryTotal + mfs) * 100) / 100;

      return {
        totalCount: count,
        estimatedProductTotal: productTotal,
        estimatedDeliveryTotal: deliveryTotal,
        gatewayFee: mfs,
        grandTotalWithMfs: grand,
      };
    }, [items]);

  // Calculations specifically for the active checkout screen (single item or cart items)
  const checkoutTotals = useMemo(() => {
    let count = 0;
    let productTotal = 0;
    let deliveryTotal = 0;

    activeCheckoutItems.forEach(({ product, quantity }) => {
      count += quantity;
      const price = parseFloat(product.estimatedPrice) || 0;
      productTotal += price * quantity;
      deliveryTotal += calculateEstimatedDeliveryCharge(product, quantity);
    });

    const mfs = Math.round(productTotal * 0.0139 * 100) / 100;
    const grand = Math.round((productTotal + deliveryTotal + mfs) * 100) / 100;

    return {
      count,
      productTotal,
      deliveryTotal,
      gatewayFee: mfs,
      grandTotal: grand,
    };
  }, [activeCheckoutItems]);

  const value = {
    items,
    activeCheckoutItems,
    directCheckoutItem,
    isDirectCheckout: Boolean(directCheckoutItem),
    addToCart,
    updateQuantity,
    removeFromCart,
    clearCart,
    isCartOpen,
    openCart,
    openCheckout,
    openDirectCheckout,
    closeCart,
    toggleCart,
    cartStep,
    setCartStep,
    totalCount,
    estimatedProductTotal,
    estimatedDeliveryTotal,
    gatewayFee,
    grandTotalWithMfs,
    estimatedGrandTotal: grandTotalWithMfs,
    checkoutTotals,
  };

  return <CartContext.Provider value={value}>{children}</CartContext.Provider>;
}

export function useCart() {
  const context = useContext(CartContext);
  if (!context) {
    throw new Error('useCart must be used within a CartProvider');
  }
  return context;
}
