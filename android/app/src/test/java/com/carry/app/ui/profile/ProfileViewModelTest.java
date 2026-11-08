package com.carry.app.ui.profile;

import com.carry.app.data.firebase.PhotoStore;
import com.carry.app.data.firebase.ProfileStore;
import com.carry.app.models.User;
import com.carry.app.models.UserProfile;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class ProfileViewModelTest {

    private FakeProfileStore fakeProfileStore;
    private FakePhotoStore fakePhotoStore;
    private ProfileViewModel viewModel;

    @Before
    public void setUp() {
        fakeProfileStore = new FakeProfileStore();
        fakePhotoStore = new FakePhotoStore();
        viewModel = new ProfileViewModel(fakeProfileStore, fakePhotoStore);
    }

    @Test
    public void testLoadProfile_existingProfile_loadsSuccessfully() {
        UserProfile existing = new UserProfile("uid_test_1", "Existing Name", 42L, "CSE", null);
        fakeProfileStore.storedProfile = existing;

        List<ProfileViewModel.ProfileState> recordedStates = new ArrayList<>();
        viewModel.setStateListener(recordedStates::add);

        User backendUser = new User();
        backendUser.setId(42L);
        viewModel.loadOrSyncProfile("uid_test_1", backendUser);

        ProfileViewModel.ProfileState finalState = viewModel.getCurrentState();
        assertNotNull(finalState.profile);
        assertEquals("Existing Name", finalState.profile.getDisplayName());
        assertEquals("uid_test_1", finalState.profile.getFirebaseUid());
        assertFalse(finalState.isLoading);
        assertNull(finalState.errorMessage);
    }

    @Test
    public void testLoadProfile_profileMissing_createsNewProfile() {
        fakeProfileStore.storedProfile = null; // Profile does not exist yet

        User backendUser = new User();
        backendUser.setId(77L);
        backendUser.setFullName("Rahim Ahmed");
        backendUser.setDepartment("EEE");

        viewModel.loadOrSyncProfile("uid_test_new", backendUser);

        ProfileViewModel.ProfileState finalState = viewModel.getCurrentState();
        assertNotNull(finalState.profile);
        assertEquals("Rahim Ahmed", finalState.profile.getDisplayName());
        assertEquals((Long) 77L, finalState.profile.getBackendUserId());
        assertEquals("EEE", finalState.profile.getDepartment());
        assertEquals(1, fakeProfileStore.saveCalls);
    }

    @Test
    public void testLoadProfile_storeFails_setsErrorMessage() {
        fakeProfileStore.shouldFail = true;

        User backendUser = new User();
        viewModel.loadOrSyncProfile("uid_test_fail", backendUser);

        ProfileViewModel.ProfileState finalState = viewModel.getCurrentState();
        assertFalse(finalState.isLoading);
        assertNotNull(finalState.errorMessage);
        assertEquals("Simulated profile load failure", finalState.errorMessage);
    }

    @Test
    public void testUpdateDisplayName_success() {
        UserProfile existing = new UserProfile("uid_test_1", "Old Name", 1L, "CSE", null);
        fakeProfileStore.storedProfile = existing;
        viewModel.loadOrSyncProfile("uid_test_1", null);

        viewModel.updateDisplayName("Brand New Name");

        ProfileViewModel.ProfileState state = viewModel.getCurrentState();
        assertEquals("Brand New Name", state.profile.getDisplayName());
        assertNull(state.errorMessage);
        assertNotNull(state.statusMessage);
        assertTrue(state.statusMessage.contains("updated successfully"));
        assertEquals(1, fakeProfileStore.updateDisplayNameCalls);
    }

    @Test
    public void testUpdateDisplayName_empty_setsErrorMessage() {
        UserProfile existing = new UserProfile("uid_test_1", "Old Name", 1L, "CSE", null);
        fakeProfileStore.storedProfile = existing;
        viewModel.loadOrSyncProfile("uid_test_1", null);

        viewModel.updateDisplayName("   ");

        ProfileViewModel.ProfileState state = viewModel.getCurrentState();
        assertNotNull(state.errorMessage);
        assertEquals("Display name cannot be empty.", state.errorMessage);
        assertEquals(0, fakeProfileStore.updateDisplayNameCalls);
    }

    @Test
    public void testUploadProfilePhoto_success() {
        UserProfile existing = new UserProfile("uid_test_1", "Old Name", 1L, "CSE", null);
        fakeProfileStore.storedProfile = existing;
        viewModel.loadOrSyncProfile("uid_test_1", null);

        byte[] sampleJpegBytes = new byte[]{1, 2, 3, 4, 5};
        fakePhotoStore.mockPhotoResult = "base64_encoded_photo_data";

        viewModel.uploadProfilePhoto(sampleJpegBytes);

        ProfileViewModel.ProfileState state = viewModel.getCurrentState();
        assertFalse(state.isUploadingPhoto);
        assertNotNull(state.profile.getPhotoBase64());
        assertEquals("base64_encoded_photo_data", state.profile.getPhotoBase64());
        assertNotNull(state.statusMessage);
        assertEquals(1, fakePhotoStore.uploadCalls);
    }

    @Test
    public void testUploadProfilePhoto_empty_rejected() {
        UserProfile existing = new UserProfile("uid_test_1", "Old Name", 1L, "CSE", null);
        fakeProfileStore.storedProfile = existing;
        viewModel.loadOrSyncProfile("uid_test_1", null);

        viewModel.uploadProfilePhoto(new byte[0]);

        ProfileViewModel.ProfileState state = viewModel.getCurrentState();
        assertNotNull(state.errorMessage);
        assertEquals(0, fakePhotoStore.uploadCalls);
    }

    @Test
    public void testUploadProfilePhoto_storeFails_setsError() {
        UserProfile existing = new UserProfile("uid_test_1", "Old Name", 1L, "CSE", null);
        fakeProfileStore.storedProfile = existing;
        viewModel.loadOrSyncProfile("uid_test_1", null);

        fakePhotoStore.shouldFail = true;
        viewModel.uploadProfilePhoto(new byte[]{1, 2, 3});

        ProfileViewModel.ProfileState state = viewModel.getCurrentState();
        assertFalse(state.isUploadingPhoto);
        assertNotNull(state.errorMessage);
        assertEquals("Simulated photo upload failure", state.errorMessage);
    }

    @Test
    public void testRemoveStateListener_cleansUp() {
        final int[] notifications = {0};
        viewModel.setStateListener(state -> notifications[0]++);

        viewModel.removeStateListener();
        viewModel.updateDisplayName("Any Name");

        // After removal, listener count should not increment further
        assertEquals(1, notifications[0]); // Only the initial registration notification
    }

    // --- Fake Stores ---

    private static class FakeProfileStore implements ProfileStore {
        UserProfile storedProfile;
        boolean shouldFail = false;
        int saveCalls = 0;
        int updateDisplayNameCalls = 0;

        @Override
        public void getProfile(String firebaseUid, ProfileCallback callback) {
            if (shouldFail) {
                callback.onFailure("Simulated profile load failure");
            } else {
                callback.onSuccess(storedProfile);
            }
        }

        @Override
        public void saveOrUpdateProfile(UserProfile profile, UpdateCallback callback) {
            saveCalls++;
            if (shouldFail) {
                callback.onFailure("Simulated profile save failure");
            } else {
                this.storedProfile = profile;
                callback.onSuccess();
            }
        }

        @Override
        public void updateDisplayName(String firebaseUid, String newDisplayName, UpdateCallback callback) {
            updateDisplayNameCalls++;
            if (shouldFail) {
                callback.onFailure("Simulated display name update failure");
            } else {
                if (storedProfile != null) storedProfile.setDisplayName(newDisplayName);
                callback.onSuccess();
            }
        }

        @Override
        public void updatePhotoBase64(String firebaseUid, String photoBase64, UpdateCallback callback) {
            if (shouldFail) {
                callback.onFailure("Simulated photo update failure");
            } else {
                if (storedProfile != null) storedProfile.setPhotoBase64(photoBase64);
                callback.onSuccess();
            }
        }
    }

    private static class FakePhotoStore implements PhotoStore {
        boolean shouldFail = false;
        String mockPhotoResult = "mock_photo_string";
        int uploadCalls = 0;

        @Override
        public void uploadProfilePhoto(String firebaseUid, byte[] jpegBytes, UploadCallback callback) {
            uploadCalls++;
            if (shouldFail) {
                callback.onFailure("Simulated photo upload failure");
            } else {
                callback.onSuccess(mockPhotoResult);
            }
        }
    }
}
