import React, { createContext, useContext, useState, useMemo } from 'react';
import { calculateEstimatedDeliveryCharge } from '../constants/deliveryCharge';

const CartContext = createContext(null);

export function CartProvider({ children }) {
  // Items array: [{ product, quantity }]
  const [items, setItems] = useState([]);
  const [isCartOpen, setIsCartOpen] = useState(false);
  const [cartStep, setCartStep] = useState('CART'); // 'CART' | 'CHECKOUT'

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

  // Update specific item's quantity
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

  // Remove item completely
  const removeFromCart = (productId) => {
    setItems((prevItems) =>
      prevItems.filter((item) => item.product.id !== productId)
    );
  };

  // Clear all items in cart
  const clearCart = () => {
    setItems([]);
    setCartStep('CART');
  };

  const openCart = () => {
    setCartStep('CART');
    setIsCartOpen(true);
  };

  const openCheckout = () => {
    setCartStep('CHECKOUT');
    setIsCartOpen(true);
  };

  const closeCart = () => {
    setIsCartOpen(false);
  };

  const toggleCart = () => setIsCartOpen((prev) => !prev);

  // Calculations: matching formula
  // need payment = productPrice + deliveryFee + gatewayFee (13.90 / 1000 = 1.39%)
  const {
    totalCount,
    estimatedProductTotal,
    estimatedDeliveryTotal,
    gatewayFee,
    grandTotalWithMfs,
  } = useMemo(() => {
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

  const value = {
    items,
    addToCart,
    updateQuantity,
    removeFromCart,
    clearCart,
    isCartOpen,
    openCart,
    openCheckout,
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
