package com.carry.app.ui.profile;

import com.carry.app.data.firebase.PhotoStore;
import com.carry.app.data.firebase.ProfileStore;
import com.carry.app.models.User;
import com.carry.app.models.UserProfile;

public class ProfileViewModel {

    public static class ProfileState {
        public UserProfile profile;
        public boolean isLoading;
        public boolean isUploadingPhoto;
        public String statusMessage;
        public String errorMessage;

        public ProfileState copy() {
            ProfileState state = new ProfileState();
            state.profile = this.profile;
            state.isLoading = this.isLoading;
            state.isUploadingPhoto = this.isUploadingPhoto;
            state.statusMessage = this.statusMessage;
            state.errorMessage = this.errorMessage;
            return state;
        }
    }

    public interface StateListener {
        void onStateChanged(ProfileState state);
    }

    private final ProfileStore profileStore;
    private final PhotoStore photoStore;
    private String currentFirebaseUid;
    private ProfileState currentState = new ProfileState();
    private StateListener stateListener;

    public ProfileViewModel(ProfileStore profileStore, PhotoStore photoStore) {
        this.profileStore = profileStore;
        this.photoStore = photoStore;
    }

    public void setStateListener(StateListener listener) {
        this.stateListener = listener;
        if (listener != null) {
            listener.onStateChanged(currentState.copy());
        }
    }

    public void removeStateListener() {
        this.stateListener = null;
    }

    public ProfileState getCurrentState() {
        return currentState.copy();
    }

    public void loadOrSyncProfile(String firebaseUid, User backendUser) {
        if (firebaseUid == null || firebaseUid.trim().isEmpty()) {
            return;
        }
        this.currentFirebaseUid = firebaseUid;

        currentState.isLoading = true;
        currentState.errorMessage = null;
        notifyState();

        profileStore.getProfile(firebaseUid, new ProfileStore.ProfileCallback() {
            @Override
            public void onSuccess(UserProfile existingProfile) {
                if (existingProfile != null) {
                    currentState.profile = existingProfile;
                    currentState.isLoading = false;
                    notifyState();
                } else {
                    // Create initial profile in Firestore
                    String name = backendUser != null && backendUser.getFullName() != null
                            ? backendUser.getFullName() : "KUET Member";
                    Long backendId = backendUser != null ? backendUser.getId() : null;
                    String dept = backendUser != null ? backendUser.getDepartment() : "General";

                    UserProfile newProfile = new UserProfile(firebaseUid, name, backendId, dept, null);
                    profileStore.saveOrUpdateProfile(newProfile, new ProfileStore.UpdateCallback() {
                        @Override
                        public void onSuccess() {
                            currentState.profile = newProfile;
                            currentState.isLoading = false;
                            notifyState();
                        }

                        @Override
                        public void onFailure(String error) {
                            currentState.isLoading = false;
                            currentState.errorMessage = error;
                            notifyState();
                        }
                    });
                }
            }

            @Override
            public void onFailure(String errorMessage) {
                currentState.isLoading = false;
                currentState.errorMessage = errorMessage;
                notifyState();
            }
        });
    }

    public void updateDisplayName(String newName) {
        if (currentFirebaseUid == null) return;
        if (newName == null || newName.trim().isEmpty()) {
            currentState.errorMessage = "Display name cannot be empty.";
            notifyState();
            return;
        }

        currentState.isLoading = true;
        currentState.errorMessage = null;
        currentState.statusMessage = null;
        notifyState();

        profileStore.updateDisplayName(currentFirebaseUid, newName, new ProfileStore.UpdateCallback() {
            @Override
            public void onSuccess() {
                if (currentState.profile != null) {
                    currentState.profile.setDisplayName(newName.trim());
                }
                currentState.isLoading = false;
                currentState.statusMessage = "Display name updated successfully.";
                notifyState();
            }

            @Override
            public void onFailure(String errorMessage) {
                currentState.isLoading = false;
                currentState.errorMessage = errorMessage;
                notifyState();
            }
        });
    }

    public void uploadProfilePhoto(byte[] jpegBytes) {
        if (currentFirebaseUid == null) return;
        if (jpegBytes == null || jpegBytes.length == 0) {
            currentState.errorMessage = "Photo selection cancelled or empty.";
            notifyState();
            return;
        }

        currentState.isUploadingPhoto = true;
        currentState.errorMessage = null;
        currentState.statusMessage = null;
        notifyState();

        photoStore.uploadProfilePhoto(currentFirebaseUid, jpegBytes, new PhotoStore.UploadCallback() {
            @Override
            public void onSuccess(String photoDataOrUrl) {
                if (currentState.profile != null) {
                    currentState.profile.setPhotoBase64(photoDataOrUrl);
                }
                currentState.isUploadingPhoto = false;
                currentState.statusMessage = "Profile picture updated successfully.";
                notifyState();
            }

            @Override
            public void onFailure(String errorMessage) {
                currentState.isUploadingPhoto = false;
                currentState.errorMessage = errorMessage;
                notifyState();
            }
        });
    }

    private void notifyState() {
        if (stateListener != null) {
            stateListener.onStateChanged(currentState.copy());
        }
    }
}
