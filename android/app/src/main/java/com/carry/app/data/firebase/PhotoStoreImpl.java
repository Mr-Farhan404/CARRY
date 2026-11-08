package com.carry.app.data.firebase;

import android.util.Base64;

public class PhotoStoreImpl implements PhotoStore {

    private final ProfileStore profileStore;

    public PhotoStoreImpl(ProfileStore profileStore) {
        this.profileStore = profileStore;
    }

    @Override
    public void uploadProfilePhoto(String firebaseUid, byte[] jpegBytes, UploadCallback callback) {
        if (firebaseUid == null || firebaseUid.trim().isEmpty()) {
            if (callback != null) callback.onFailure("Invalid user ID.");
            return;
        }

        if (jpegBytes == null || jpegBytes.length == 0) {
            if (callback != null) callback.onFailure("Empty image data.");
            return;
        }

        // Validate 100KB constraint
        if (jpegBytes.length > 100 * 1024) {
            if (callback != null) callback.onFailure("Image exceeds maximum allowed size (100 KB).");
            return;
        }

        try {
            String base64Data = Base64.encodeToString(jpegBytes, Base64.NO_WRAP);
            profileStore.updatePhotoBase64(firebaseUid, base64Data, new ProfileStore.UpdateCallback() {
                @Override
                public void onSuccess() {
                    if (callback != null) {
                        callback.onSuccess(base64Data);
                    }
                }

                @Override
                public void onFailure(String errorMessage) {
                    if (callback != null) {
                        callback.onFailure(errorMessage);
                    }
                }
            });
        } catch (Exception e) {
            if (callback != null) {
                callback.onFailure("Failed to encode profile image.");
            }
        }
    }
}
