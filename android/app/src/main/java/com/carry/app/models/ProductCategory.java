package com.carry.app.models;

public enum ProductCategory {
    FOOD("Food & Snacks"),
    ELECTRONICS("Electronics & Gadgets"),
    OTHERS("Stationery & Others");

    private final String displayName;

    ProductCategory(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
