package com.carry.app.models;

public enum WeightClass {
    LIGHT("Light (< 1kg)"),
    MEDIUM("Medium (1 - 3kg)"),
    HEAVY("Heavy (> 3kg)");

    private final String displayName;

    WeightClass(String displayName) {
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
