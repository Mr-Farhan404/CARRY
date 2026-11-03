package com.carry.app.models;

import com.google.gson.annotations.SerializedName;

public class UpdateTripStatusRequest {
    @SerializedName("status")
    private TripStatus status;

    public UpdateTripStatusRequest() {}

    public UpdateTripStatusRequest(TripStatus status) {
        this.status = status;
    }

    public TripStatus getStatus() { return status; }
    public void setStatus(TripStatus status) { this.status = status; }
}
