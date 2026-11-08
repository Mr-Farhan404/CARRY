package com.carry.app.data.firebase;

public interface FirebaseAuthGateway {

    interface AuthCallback {
        void onSuccess(String firebaseUid);
        void onFailure(String errorMessage);
    }

    boolean isUserSignedIn();

    String getCurrentFirebaseUid();

    void register(String email, String password, AuthCallback callback);

    void signIn(String email, String password, AuthCallback callback);

    void signInOrRegister(String email, String password, AuthCallback callback);

    void signOut();
}
