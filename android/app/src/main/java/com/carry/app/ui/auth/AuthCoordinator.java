package com.carry.app.ui.auth;

import com.carry.app.api.ApiService;
import com.carry.app.data.SessionStore;
import com.carry.app.data.firebase.FirebaseAuthGateway;
import com.carry.app.models.AuthResponse;
import com.carry.app.models.LoginRequest;
import com.carry.app.models.RegisterRequest;
import com.carry.app.models.User;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AuthCoordinator {

    public interface AuthResultCallback {
        void onSuccess(User user, String firebaseUid, String message);
        void onFailure(String errorMessage);
    }

    private final ApiService apiService;
    private final FirebaseAuthGateway firebaseAuthGateway;
    private final SessionStore sessionStore;

    public AuthCoordinator(ApiService apiService, FirebaseAuthGateway firebaseAuthGateway, SessionStore sessionStore) {
        this.apiService = apiService;
        this.firebaseAuthGateway = firebaseAuthGateway;
        this.sessionStore = sessionStore;
    }

    public void performLogin(String email, String password, AuthResultCallback callback) {
        LoginRequest request = new LoginRequest(email, password);
        apiService.login(request).enqueue(new Callback<AuthResponse>() {
            @Override
            public void onResponse(Call<AuthResponse> call, Response<AuthResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getToken() != null) {
                    String token = response.body().getToken();
                    if (sessionStore != null) {
                        sessionStore.saveToken(token);
                    }

                    // Fetch authoritative user details
                    apiService.getMe().enqueue(new Callback<User>() {
                        @Override
                        public void onResponse(Call<User> meCall, Response<User> meResponse) {
                            User user = (meResponse.isSuccessful() && meResponse.body() != null) ? meResponse.body() : null;
                            if (user != null && sessionStore != null) {
                                sessionStore.saveUser(user);
                            }

                            // Dual sign-in with Firebase (self-healing)
                            firebaseAuthGateway.signInOrRegister(email, password, new FirebaseAuthGateway.AuthCallback() {
                                @Override
                                public void onSuccess(String firebaseUid) {
                                    if (sessionStore != null) {
                                        sessionStore.saveFirebaseUid(firebaseUid);
                                    }
                                    if (callback != null) {
                                        callback.onSuccess(user, firebaseUid, "Welcome to CARRY!");
                                    }
                                }

                                @Override
                                public void onFailure(String errorMessage) {
                                    // If Firebase is down or unreachable, app still continues through backend
                                    if (callback != null) {
                                        callback.onSuccess(user, null, "Welcome to CARRY! (Live features offline)");
                                    }
                                }
                            });
                        }

                        @Override
                        public void onFailure(Call<User> meCall, Throwable t) {
                            // Backend user fetch failed after login
                            firebaseAuthGateway.signInOrRegister(email, password, new FirebaseAuthGateway.AuthCallback() {
                                @Override
                                public void onSuccess(String firebaseUid) {
                                    if (sessionStore != null) {
                                        sessionStore.saveFirebaseUid(firebaseUid);
                                    }
                                    if (callback != null) {
                                        callback.onSuccess(null, firebaseUid, "Welcome to CARRY!");
                                    }
                                }

                                @Override
                                public void onFailure(String errorMessage) {
                                    if (callback != null) {
                                        callback.onSuccess(null, null, "Welcome to CARRY! (Live features offline)");
                                    }
                                }
                            });
                        }
                    });
                } else {
                    if (callback != null) {
                        callback.onFailure("Invalid credentials. Please try again.");
                    }
                }
            }

            @Override
            public void onFailure(Call<AuthResponse> call, Throwable t) {
                if (callback != null) {
                    callback.onFailure("Network error: " + (t != null ? t.getMessage() : "Unable to reach backend."));
                }
            }
        });
    }

    public void performRegister(RegisterRequest request, AuthResultCallback callback) {
        apiService.register(request).enqueue(new Callback<AuthResponse>() {
            @Override
            public void onResponse(Call<AuthResponse> call, Response<AuthResponse> response) {
                if (response.isSuccessful()) {
                    if (response.body() != null && response.body().getToken() != null) {
                        String token = response.body().getToken();
                        if (sessionStore != null) {
                            sessionStore.saveToken(token);
                        }

                        apiService.getMe().enqueue(new Callback<User>() {
                            @Override
                            public void onResponse(Call<User> meCall, Response<User> meResponse) {
                                User user = (meResponse.isSuccessful() && meResponse.body() != null) ? meResponse.body() : null;
                                if (user != null && sessionStore != null) {
                                    sessionStore.saveUser(user);
                                }

                                // Create or link Firebase user with the same email and password
                                firebaseAuthGateway.signInOrRegister(request.getEmail(), request.getPassword(), new FirebaseAuthGateway.AuthCallback() {
                                    @Override
                                    public void onSuccess(String firebaseUid) {
                                        if (sessionStore != null) {
                                            sessionStore.saveFirebaseUid(firebaseUid);
                                        }
                                        if (callback != null) {
                                            callback.onSuccess(user, firebaseUid, "Account created successfully!");
                                        }
                                    }

                                    @Override
                                    public void onFailure(String errorMessage) {
                                        // If Firebase creation fails, keep backend account and notify user
                                        if (callback != null) {
                                            callback.onSuccess(user, null, "Account created. Live features will activate on your next login");
                                        }
                                    }
                                });
                            }

                            @Override
                            public void onFailure(Call<User> meCall, Throwable t) {
                                firebaseAuthGateway.signInOrRegister(request.getEmail(), request.getPassword(), new FirebaseAuthGateway.AuthCallback() {
                                    @Override
                                    public void onSuccess(String firebaseUid) {
                                        if (sessionStore != null) {
                                            sessionStore.saveFirebaseUid(firebaseUid);
                                        }
                                        if (callback != null) {
                                            callback.onSuccess(null, firebaseUid, "Account created successfully!");
                                        }
                                    }

                                    @Override
                                    public void onFailure(String errorMessage) {
                                        if (callback != null) {
                                            callback.onSuccess(null, null, "Account created. Live features will activate on your next login");
                                        }
                                    }
                                });
                            }
                        });
                    } else {
                        // Backend returned 200 OK without body token -> log in seamlessly to obtain token & sync Firebase
                        performLogin(request.getEmail(), request.getPassword(), new AuthResultCallback() {
                            @Override
                            public void onSuccess(User user, String firebaseUid, String message) {
                                if (callback != null) {
                                    callback.onSuccess(user, firebaseUid, "Account created successfully!");
                                }
                            }

                            @Override
                            public void onFailure(String errorMessage) {
                                if (callback != null) {
                                    callback.onSuccess(null, null, "Account created successfully!");
                                }
                            }
                        });
                    }
                } else {
                    if (callback != null) {
                        callback.onFailure("Registration failed. Email or Student ID may already exist.");
                    }
                }
            }

            @Override
            public void onFailure(Call<AuthResponse> call, Throwable t) {
                if (callback != null) {
                    callback.onFailure("Network error: " + (t != null ? t.getMessage() : "Unable to reach backend."));
                }
            }
        });
    }
}
