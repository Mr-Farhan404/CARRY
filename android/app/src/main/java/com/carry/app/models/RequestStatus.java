package com.carry.app.models;

public enum RequestStatus {
    REQUESTED("Requested", "Waiting for a carrier"),
    ACCEPTED("Accepted", "Carrier assigned"),
    COLLECTED("Collected", "Items purchased in city"),
    RETURNING("Returning", "Carrier returning to campus"),
    READY_FOR_DELIVERY("Ready for Delivery", "Ready at campus pickup spot"),
    DELIVERED("Delivered", "Delivered successfully"),
    CANCELLED("Cancelled", "Request cancelled");

    private final String label;
    private final String description;

    RequestStatus(String label, String description) {
        this.label = label;
        this.description = description;
    }

    public String getLabel() {
        return label;
    }

    public String getDescription() {
        return description;
    }

    public boolean canTransitionTo(RequestStatus next) {
        switch (this) {
            case ACCEPTED:
                return next == COLLECTED;
            case COLLECTED:
                return next == RETURNING;
            case RETURNING:
                return next == READY_FOR_DELIVERY;
            case READY_FOR_DELIVERY:
                return next == DELIVERED;
            default:
                return false;
        }
    }
}
