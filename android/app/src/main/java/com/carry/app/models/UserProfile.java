package com.carry.app.models;

import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.IgnoreExtraProperties;
import com.google.firebase.firestore.ServerTimestamp;

import java.util.Date;

@IgnoreExtraProperties
public class UserProfile {

    private String firebaseUid;
    private String displayName;
    private Long backendUserId;
    private String department;
    private String photoUrl;
    private String photoBase64;

    @ServerTimestamp
    private Date updatedAt;

    public UserProfile() {
        // Required for Firestore deserialization
    }

    public UserProfile(String firebaseUid, String displayName, Long backendUserId, String department, String photoUrl) {
        this.firebaseUid = firebaseUid;
        this.displayName = displayName;
        this.backendUserId = backendUserId;
        this.department = department;
        this.photoUrl = photoUrl;
    }

    public String getFirebaseUid() {
        return firebaseUid;
    }

    public void setFirebaseUid(String firebaseUid) {
        this.firebaseUid = firebaseUid;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public Long getBackendUserId() {
        return backendUserId;
    }

    public void setBackendUserId(Long backendUserId) {
        this.backendUserId = backendUserId;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public String getPhotoUrl() {
        return photoUrl;
    }

    public void setPhotoUrl(String photoUrl) {
        this.photoUrl = photoUrl;
    }

    public String getPhotoBase64() {
        return photoBase64;
    }

    public void setPhotoBase64(String photoBase64) {
        this.photoBase64 = photoBase64;
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
    }
}
