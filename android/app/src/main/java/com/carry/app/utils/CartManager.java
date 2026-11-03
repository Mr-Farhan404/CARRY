package com.carry.app.utils;

import com.carry.app.models.CartItem;
import com.carry.app.models.Product;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class CartManager {
    public interface CartChangeListener {
        void onCartChanged();
    }

    private static CartManager instance;
    private final List<CartItem> items = new ArrayList<>();
    private final List<CartChangeListener> listeners = new ArrayList<>();

    private CartManager() {}

    public static synchronized CartManager getInstance() {
        if (instance == null) {
            instance = new CartManager();
        }
        return instance;
    }

    public void addListener(CartChangeListener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public void removeListener(CartChangeListener listener) {
        listeners.remove(listener);
    }

    private void notifyListeners() {
        for (CartChangeListener listener : listeners) {
            listener.onCartChanged();
        }
    }

    public synchronized void addItem(Product product, int quantity) {
        if (product == null || product.getId() == null) return;
        int qty = Math.max(1, quantity);

        for (CartItem item : items) {
            if (item.getProduct() != null && product.getId().equals(item.getProduct().getId())) {
                item.setQuantity(item.getQuantity() + qty);
                notifyListeners();
                return;
            }
        }

        items.add(new CartItem(product, qty));
        notifyListeners();
    }

    public synchronized void updateQuantity(Long productId, int quantity) {
        if (productId == null) return;

        Iterator<CartItem> iterator = items.iterator();
        while (iterator.hasNext()) {
            CartItem item = iterator.next();
            if (item.getProduct() != null && productId.equals(item.getProduct().getId())) {
                if (quantity <= 0) {
                    iterator.remove();
                } else {
                    item.setQuantity(quantity);
                }
                notifyListeners();
                return;
            }
        }
    }

    public synchronized void removeItem(Long productId) {
        if (productId == null) return;
        boolean removed = items.removeIf(item -> item.getProduct() != null && productId.equals(item.getProduct().getId()));
        if (removed) {
            notifyListeners();
        }
    }

    public synchronized void clear() {
        items.clear();
        notifyListeners();
    }

    public synchronized List<CartItem> getItems() {
        return Collections.unmodifiableList(new ArrayList<>(items));
    }

    public synchronized int getTotalItemCount() {
        int count = 0;
        for (CartItem item : items) {
            count += item.getQuantity();
        }
        return count;
    }

    public synchronized BigDecimal getTotalProductCost() {
        BigDecimal sum = BigDecimal.ZERO;
        for (CartItem item : items) {
            sum = sum.add(item.getSubtotal());
        }
        return sum;
    }

    public synchronized BigDecimal getTotalDeliveryCharge() {
        BigDecimal sum = BigDecimal.ZERO;
        for (CartItem item : items) {
            sum = sum.add(DeliveryChargeCalculator.calculateDeliveryCharge(item.getProduct(), item.getQuantity()));
        }
        return sum;
    }
}
