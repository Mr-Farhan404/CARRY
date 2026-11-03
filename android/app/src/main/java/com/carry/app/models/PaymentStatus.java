package com.carry.app.models;

public enum PaymentStatus {
    PENDING("Payment Pending"),
    SUBMITTED("Payment Submitted (Verifying)"),
    VERIFIED("Payment Verified"),
    SIMULATED_PAID("Paid (Test Mode)"),
    REJECTED("Payment Rejected");

    private final String label;

    PaymentStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
