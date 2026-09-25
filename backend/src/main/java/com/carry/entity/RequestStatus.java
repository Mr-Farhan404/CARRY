package com.carry.entity;

public enum RequestStatus {
    REQUESTED,
    ACCEPTED,
    COLLECTED,
    RETURNING,
    READY_FOR_DELIVERY,
    DELIVERED,
    CANCELLED;

    public boolean canTransitionTo(RequestStatus next) {
        return switch (this) {
            case ACCEPTED -> next == COLLECTED;
            case COLLECTED -> next == RETURNING;
            case RETURNING -> next == READY_FOR_DELIVERY;
            case READY_FOR_DELIVERY -> next == DELIVERED;
            default -> false;
        };
    }
}
