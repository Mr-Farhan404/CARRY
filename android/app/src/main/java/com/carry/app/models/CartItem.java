package com.carry.app.models;

import java.io.Serializable;
import java.math.BigDecimal;

public class CartItem implements Serializable {
    private Product product;
    private int quantity;

    public CartItem(Product product, int quantity) {
        this.product = product;
        this.quantity = Math.max(1, quantity);
    }

    public Product getProduct() { return product; }
    public void setProduct(Product product) { this.product = product; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = Math.max(1, quantity); }

    public BigDecimal getSubtotal() {
        if (product == null || product.getEstimatedPrice() == null) {
            return BigDecimal.ZERO;
        }
        return product.getEstimatedPrice().multiply(BigDecimal.valueOf(quantity));
    }
}
