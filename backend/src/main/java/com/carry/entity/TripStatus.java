package com.carry.entity;

public enum TripStatus {
    PLANNED,
    IN_CITY,
    RETURNING,
    COMPLETED,
    CANCELLED;

    public boolean isActive() {
        return this == PLANNED || this == IN_CITY;
    }
}
