package com.carry.app.models;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;

public class Trip implements Serializable {
    @SerializedName("id")
    private Long id;

    @SerializedName("partnerId")
    private Long partnerId;

    @SerializedName("partnerName")
    private String partnerName;

    @SerializedName("departureTime")
    private String departureTime;

    @SerializedName("expectedReturnTime")
    private String expectedReturnTime;

    @SerializedName("actualReturnTime")
    private String actualReturnTime;

    @SerializedName("destinationArea")
    private LocationArea destinationArea;

    @SerializedName("capacityNotes")
    private String capacityNotes;

    @SerializedName("status")
    private TripStatus status;

    @SerializedName("createdAt")
    private String createdAt;

    public Trip() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getPartnerId() { return partnerId; }
    public void setPartnerId(Long partnerId) { this.partnerId = partnerId; }

    public String getPartnerName() { return partnerName; }
    public void setPartnerName(String partnerName) { this.partnerName = partnerName; }

    public String getDepartureTime() { return departureTime; }
    public void setDepartureTime(String departureTime) { this.departureTime = departureTime; }

    public String getExpectedReturnTime() { return expectedReturnTime; }
    public void setExpectedReturnTime(String expectedReturnTime) { this.expectedReturnTime = expectedReturnTime; }

    public String getActualReturnTime() { return actualReturnTime; }
    public void setActualReturnTime(String actualReturnTime) { this.actualReturnTime = actualReturnTime; }

    public LocationArea getDestinationArea() { return destinationArea; }
    public void setDestinationArea(LocationArea destinationArea) { this.destinationArea = destinationArea; }

    public String getCapacityNotes() { return capacityNotes; }
    public void setCapacityNotes(String capacityNotes) { this.capacityNotes = capacityNotes; }

    public TripStatus getStatus() { return status; }
    public void setStatus(TripStatus status) { this.status = status; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
}
