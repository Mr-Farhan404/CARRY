package com.carry.app.models;

import com.google.gson.annotations.SerializedName;

public class AcceptRequestDto {
    @SerializedName("tripId")
    private Long tripId;

    @SerializedName("version")
    private Integer version;

    public AcceptRequestDto() {}

    public AcceptRequestDto(Long tripId, Integer version) {
        this.tripId = tripId;
        this.version = version;
    }

    public Long getTripId() { return tripId; }
    public void setTripId(Long tripId) { this.tripId = tripId; }

    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }
}
