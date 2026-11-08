package com.carry.app.models;

import com.google.firebase.database.IgnoreExtraProperties;

@IgnoreExtraProperties
public class LiveLocation {

    private Double lat;
    private Double lng;
    private Long updatedAt;
    private String uid;

    public LiveLocation() {
        // Required for Firebase Realtime Database
    }

    public LiveLocation(Double lat, Double lng, Long updatedAt, String uid) {
        this.lat = lat;
        this.lng = lng;
        this.updatedAt = updatedAt;
        this.uid = uid;
    }

    public Double getLat() {
        return lat;
    }

    public void setLat(Double lat) {
        this.lat = lat;
    }

    public Double getLng() {
        return lng;
    }

    public void setLng(Double lng) {
        this.lng = lng;
    }

    public Long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Long updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }
}
