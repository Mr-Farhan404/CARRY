package com.carry.app.models;

public enum TripStatus {
    PLANNED("Planned"),
    IN_CITY("In City"),
    RETURNING("Returning"),
    COMPLETED("Completed"),
    CANCELLED("Cancelled");

    private final String label;

    TripStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public boolean isActive() {
        return this == PLANNED || this == IN_CITY;
    }
}
