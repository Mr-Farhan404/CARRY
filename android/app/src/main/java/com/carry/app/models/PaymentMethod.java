package com.carry.app.models;

public enum PaymentMethod {
    BKASH("bKash"),
    NAGAD("Nagad"),
    SIMULATED("Simulated");

    private final String displayName;

    PaymentMethod(String displayName) {
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
