package com.carry.app.data.firebase;

import com.carry.app.models.UserProfile;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

public class ProfileStoreImpl implements ProfileStore {

    private static final String COLLECTION_USERS = "users";
    private final FirebaseFirestore firestore;

    public ProfileStoreImpl() {
        this.firestore = FirebaseFirestore.getInstance();
    }

    public ProfileStoreImpl(FirebaseFirestore firestore) {
        this.firestore = firestore;
    }

    @Override
    public void getProfile(String firebaseUid, ProfileCallback callback) {
        if (firebaseUid == null || firebaseUid.trim().isEmpty()) {
            if (callback != null) callback.onFailure("Invalid user identifier.");
            return;
        }

        firestore.collection(COLLECTION_USERS)
                .document(firebaseUid)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        UserProfile profile = documentSnapshot.toObject(UserProfile.class);
                        if (profile != null) {
                            profile.setFirebaseUid(firebaseUid);
                        }
                        if (callback != null) callback.onSuccess(profile);
                    } else {
                        if (callback != null) callback.onSuccess(null);
                    }
                })
                .addOnFailureListener(e -> {
                    if (callback != null) callback.onFailure("Failed to load user profile from cloud.");
                });
    }

    @Override
    public void saveOrUpdateProfile(UserProfile profile, UpdateCallback callback) {
        if (profile == null || profile.getFirebaseUid() == null) {
            if (callback != null) callback.onFailure("User profile data is missing.");
            return;
        }

        Map<String, Object> data = new HashMap<>();
        data.put("firebaseUid", profile.getFirebaseUid());
        data.put("displayName", profile.getDisplayName());
        data.put("backendUserId", profile.getBackendUserId());
        data.put("department", profile.getDepartment());
        if (profile.getPhotoUrl() != null) {
            data.put("photoUrl", profile.getPhotoUrl());
        }
        if (profile.getPhotoBase64() != null) {
            data.put("photoBase64", profile.getPhotoBase64());
        }
        data.put("updatedAt", FieldValue.serverTimestamp());

        firestore.collection(COLLECTION_USERS)
                .document(profile.getFirebaseUid())
                .set(data, SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    if (callback != null) callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    if (callback != null) callback.onFailure("Could not sync profile to cloud.");
                });
    }

    @Override
    public void updateDisplayName(String firebaseUid, String newDisplayName, UpdateCallback callback) {
        if (firebaseUid == null || newDisplayName == null || newDisplayName.trim().isEmpty()) {
            if (callback != null) callback.onFailure("Display name cannot be empty.");
            return;
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put("displayName", newDisplayName.trim());
        updates.put("updatedAt", FieldValue.serverTimestamp());

        firestore.collection(COLLECTION_USERS)
                .document(firebaseUid)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    if (callback != null) callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    if (callback != null) callback.onFailure("Failed to update display name.");
                });
    }

    @Override
    public void updatePhotoBase64(String firebaseUid, String photoBase64, UpdateCallback callback) {
        if (firebaseUid == null) {
            if (callback != null) callback.onFailure("User identifier missing.");
            return;
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put("photoBase64", photoBase64);
        updates.put("updatedAt", FieldValue.serverTimestamp());

        firestore.collection(COLLECTION_USERS)
                .document(firebaseUid)
                .set(updates, SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    if (callback != null) callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    if (callback != null) callback.onFailure("Failed to save profile photo.");
                });
    }
}
