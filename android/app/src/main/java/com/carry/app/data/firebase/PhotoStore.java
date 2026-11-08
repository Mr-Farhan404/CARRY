package com.carry.app.data.firebase;

public interface PhotoStore {

    interface UploadCallback {
        void onSuccess(String photoDataOrUrl);
        void onFailure(String errorMessage);
    }

    void uploadProfilePhoto(String firebaseUid, byte[] jpegBytes, UploadCallback callback);
}
