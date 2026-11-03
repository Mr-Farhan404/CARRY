package com.carry.app.models;

import com.google.gson.annotations.SerializedName;

public class CreateTripRequest {
    @SerializedName("departureTime")
    private String departureTime;

    @SerializedName("expectedReturnTime")
    private String expectedReturnTime;

    @SerializedName("destinationArea")
    private LocationArea destinationArea;

    @SerializedName("capacityNotes")
    private String capacityNotes;

    public CreateTripRequest() {}

    public CreateTripRequest(String departureTime, String expectedReturnTime, LocationArea destinationArea, String capacityNotes) {
        this.departureTime = departureTime;
        this.expectedReturnTime = expectedReturnTime;
        this.destinationArea = destinationArea;
        this.capacityNotes = capacityNotes;
    }

    public String getDepartureTime() { return departureTime; }
    public void setDepartureTime(String departureTime) { this.departureTime = departureTime; }

    public String getExpectedReturnTime() { return expectedReturnTime; }
    public void setExpectedReturnTime(String expectedReturnTime) { this.expectedReturnTime = expectedReturnTime; }

    public LocationArea getDestinationArea() { return destinationArea; }
    public void setDestinationArea(LocationArea destinationArea) { this.destinationArea = destinationArea; }

    public String getCapacityNotes() { return capacityNotes; }
    public void setCapacityNotes(String capacityNotes) { this.capacityNotes = capacityNotes; }
}
