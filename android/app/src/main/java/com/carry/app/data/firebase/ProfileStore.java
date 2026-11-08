package com.carry.app.data.firebase;

import com.carry.app.models.UserProfile;

public interface ProfileStore {

    interface ProfileCallback {
        void onSuccess(UserProfile profile);
        void onFailure(String errorMessage);
    }

    interface UpdateCallback {
        void onSuccess();
        void onFailure(String errorMessage);
    }

    void getProfile(String firebaseUid, ProfileCallback callback);

    void saveOrUpdateProfile(UserProfile profile, UpdateCallback callback);

    void updateDisplayName(String firebaseUid, String newDisplayName, UpdateCallback callback);

    void updatePhotoBase64(String firebaseUid, String photoBase64, UpdateCallback callback);
}
