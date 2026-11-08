package com.carry.app.data.firebase;

import com.google.firebase.FirebaseNetworkException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseAuthWeakPasswordException;
import com.google.firebase.auth.FirebaseUser;

public class FirebaseAuthGatewayImpl implements FirebaseAuthGateway {

    private final FirebaseAuth auth;

    public FirebaseAuthGatewayImpl() {
        this.auth = FirebaseAuth.getInstance();
    }

    public FirebaseAuthGatewayImpl(FirebaseAuth auth) {
        this.auth = auth;
    }

    @Override
    public boolean isUserSignedIn() {
        return auth.getCurrentUser() != null;
    }

    @Override
    public String getCurrentFirebaseUid() {
        FirebaseUser user = auth.getCurrentUser();
        return user != null ? user.getUid() : null;
    }

    @Override
    public void register(String email, String password, AuthCallback callback) {
        auth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(result -> {
                    FirebaseUser user = result.getUser();
                    if (user != null && callback != null) {
                        callback.onSuccess(user.getUid());
                    } else if (callback != null) {
                        callback.onFailure("Failed to retrieve user identity.");
                    }
                })
                .addOnFailureListener(e -> {
                    if (callback != null) {
                        callback.onFailure(mapError(e));
                    }
                });
    }

    @Override
    public void signIn(String email, String password, AuthCallback callback) {
        auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(result -> {
                    FirebaseUser user = result.getUser();
                    if (user != null && callback != null) {
                        callback.onSuccess(user.getUid());
                    } else if (callback != null) {
                        callback.onFailure("Failed to retrieve user identity.");
                    }
                })
                .addOnFailureListener(e -> {
                    if (callback != null) {
                        callback.onFailure(mapError(e));
                    }
                });
    }

    @Override
    public void signInOrRegister(String email, String password, AuthCallback callback) {
        auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(result -> {
                    FirebaseUser user = result.getUser();
                    if (user != null && callback != null) {
                        callback.onSuccess(user.getUid());
                    } else if (callback != null) {
                        callback.onFailure("Failed to retrieve user identity.");
                    }
                })
                .addOnFailureListener(e -> {
                    if (e instanceof FirebaseAuthInvalidUserException || e instanceof FirebaseAuthInvalidCredentialsException) {
                        // User does not exist in Firebase (or Email Enumeration Protection returned invalid credential) -> self-heal by creating user
                        auth.createUserWithEmailAndPassword(email, password)
                                .addOnSuccessListener(createResult -> {
                                    FirebaseUser user = createResult.getUser();
                                    if (user != null && callback != null) {
                                        callback.onSuccess(user.getUid());
                                    } else if (callback != null) {
                                        callback.onFailure("User created but identity missing.");
                                    }
                                })
                                .addOnFailureListener(createError -> {
                                    if (createError instanceof FirebaseAuthUserCollisionException) {
                                        // User already exists in Firebase, so original failure was truly invalid password
                                        if (callback != null) {
                                            callback.onFailure(mapError(e));
                                        }
                                    } else if (callback != null) {
                                        callback.onFailure(mapError(createError));
                                    }
                                });
                    } else {
                        if (callback != null) {
                            callback.onFailure(mapError(e));
                        }
                    }
                });
    }

    @Override
    public void signOut() {
        auth.signOut();
    }

    private String mapError(Exception e) {
        if (e == null) return "An unknown error occurred.";
        if (e instanceof FirebaseAuthWeakPasswordException) {
            return "The password does not meet security requirements.";
        } else if (e instanceof FirebaseAuthInvalidCredentialsException) {
            return "Invalid credentials provided.";
        } else if (e instanceof FirebaseAuthUserCollisionException) {
            return "An account with this email address already exists.";
        } else if (e instanceof FirebaseNetworkException) {
            return "Network connection issue while reaching Firebase services.";
        }
        return "Live services are temporarily unavailable.";
    }
}
