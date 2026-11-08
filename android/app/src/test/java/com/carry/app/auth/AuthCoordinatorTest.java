package com.carry.app.auth;

import com.carry.app.api.ApiService;
import com.carry.app.data.SessionStore;
import com.carry.app.data.firebase.FirebaseAuthGateway;
import com.carry.app.models.*;
import com.carry.app.ui.auth.AuthCoordinator;

import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.ResponseBody;
import okio.Timeout;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static org.junit.Assert.*;

public class AuthCoordinatorTest {

    private InMemorySessionStore sessionStore;
    private FakeFirebaseAuthGateway fakeFirebaseGateway;

    @Before
    public void setUp() {
        sessionStore = new InMemorySessionStore();
        fakeFirebaseGateway = new FakeFirebaseAuthGateway();
    }

    // --- TEST 1: backend ok + firebase ok ---
    @Test
    public void testBackendOk_FirebaseOk_Succeeds() {
        User expectedUser = new User();
        expectedUser.setId(101L);
        expectedUser.setEmail("test@kuet.ac.bd");
        expectedUser.setFullName("Test Student");

        ApiService fakeApi = createFakeApiService(true, "valid_jwt_token", expectedUser);
        fakeFirebaseGateway.mode = FakeFirebaseAuthGateway.Mode.SUCCESS;
        fakeFirebaseGateway.mockUid = "firebase_uid_101";

        AuthCoordinator coordinator = new AuthCoordinator(fakeApi, fakeFirebaseGateway, sessionStore);

        AtomicBoolean successCalled = new AtomicBoolean(false);
        AtomicReference<String> resultUid = new AtomicReference<>();

        coordinator.performLogin("test@kuet.ac.bd", "password123", new AuthCoordinator.AuthResultCallback() {
            @Override
            public void onSuccess(User user, String firebaseUid, String message) {
                successCalled.set(true);
                resultUid.set(firebaseUid);
            }

            @Override
            public void onFailure(String errorMessage) {
                fail("Expected success, but got failure: " + errorMessage);
            }
        });

        assertTrue("Coordinator onSuccess should be called", successCalled.get());
        assertEquals("firebase_uid_101", resultUid.get());
        assertEquals("valid_jwt_token", sessionStore.getToken());
        assertEquals("firebase_uid_101", sessionStore.getFirebaseUid());
        assertEquals(1, fakeFirebaseGateway.signInOrRegisterCalls);
    }

    // --- TEST 2: backend ok + firebase user missing (creates it) ---
    @Test
    public void testBackendOk_FirebaseUserMissing_CreatesAndSucceeds() {
        User expectedUser = new User();
        expectedUser.setId(102L);
        expectedUser.setEmail("newuser@kuet.ac.bd");

        ApiService fakeApi = createFakeApiService(true, "jwt_token_102", expectedUser);
        fakeFirebaseGateway.mode = FakeFirebaseAuthGateway.Mode.USER_MISSING_THEN_CREATED;
        fakeFirebaseGateway.mockUid = "self_healed_uid_102";

        AuthCoordinator coordinator = new AuthCoordinator(fakeApi, fakeFirebaseGateway, sessionStore);

        AtomicBoolean successCalled = new AtomicBoolean(false);
        AtomicReference<String> resultUid = new AtomicReference<>();

        coordinator.performLogin("newuser@kuet.ac.bd", "password123", new AuthCoordinator.AuthResultCallback() {
            @Override
            public void onSuccess(User user, String firebaseUid, String message) {
                successCalled.set(true);
                resultUid.set(firebaseUid);
            }

            @Override
            public void onFailure(String errorMessage) {
                fail("Expected self-healing success, but got failure: " + errorMessage);
            }
        });

        assertTrue("Coordinator onSuccess should be called", successCalled.get());
        assertEquals("self_healed_uid_102", resultUid.get());
        assertEquals("self_healed_uid_102", sessionStore.getFirebaseUid());
        assertTrue(fakeFirebaseGateway.selfHealed);
    }

    // --- TEST 3: backend ok + firebase down (still succeeds) ---
    @Test
    public void testBackendOk_FirebaseDown_StillSucceedsThroughBackend() {
        User expectedUser = new User();
        expectedUser.setId(103L);
        expectedUser.setEmail("offline@kuet.ac.bd");

        ApiService fakeApi = createFakeApiService(true, "jwt_token_103", expectedUser);
        fakeFirebaseGateway.mode = FakeFirebaseAuthGateway.Mode.DOWN;

        AuthCoordinator coordinator = new AuthCoordinator(fakeApi, fakeFirebaseGateway, sessionStore);

        AtomicBoolean successCalled = new AtomicBoolean(false);
        AtomicReference<String> resultMessage = new AtomicReference<>();

        coordinator.performLogin("offline@kuet.ac.bd", "password123", new AuthCoordinator.AuthResultCallback() {
            @Override
            public void onSuccess(User user, String firebaseUid, String message) {
                successCalled.set(true);
                resultMessage.set(message);
                assertNull("Firebase UID should be null when Firebase is down", firebaseUid);
            }

            @Override
            public void onFailure(String errorMessage) {
                fail("App must still work through backend when Firebase is down, but got failure: " + errorMessage);
            }
        });

        assertTrue("Login must succeed via backend even if Firebase is down", successCalled.get());
        assertEquals("jwt_token_103", sessionStore.getToken());
        assertNotNull(resultMessage.get());
        assertTrue(resultMessage.get().contains("offline"));
    }

    // --- TEST 4: backend fails (firebase never called) ---
    @Test
    public void testBackendFails_FirebaseNeverCalled() {
        ApiService fakeApi = createFakeApiService(false, null, null);
        fakeFirebaseGateway.mode = FakeFirebaseAuthGateway.Mode.SUCCESS;

        AuthCoordinator coordinator = new AuthCoordinator(fakeApi, fakeFirebaseGateway, sessionStore);

        AtomicBoolean failureCalled = new AtomicBoolean(false);

        coordinator.performLogin("wrong@kuet.ac.bd", "badpassword", new AuthCoordinator.AuthResultCallback() {
            @Override
            public void onSuccess(User user, String firebaseUid, String message) {
                fail("Expected failure for invalid credentials, but got success");
            }

            @Override
            public void onFailure(String errorMessage) {
                failureCalled.set(true);
                assertEquals("Invalid credentials. Please try again.", errorMessage);
            }
        });

        assertTrue("Failure callback must be invoked", failureCalled.get());
        assertEquals("Firebase must NEVER be called when backend fails", 0, fakeFirebaseGateway.totalCalls());
        assertNull("No token should be saved", sessionStore.getToken());
        assertNull("No Firebase UID should be saved", sessionStore.getFirebaseUid());
    }

    // --- TEST 5: Registration backend fails (firebase never called) ---
    @Test
    public void testRegisterBackendFails_FirebaseNeverCalled() {
        ApiService fakeApi = createFakeApiService(false, null, null);
        fakeFirebaseGateway.mode = FakeFirebaseAuthGateway.Mode.SUCCESS;

        AuthCoordinator coordinator = new AuthCoordinator(fakeApi, fakeFirebaseGateway, sessionStore);

        RegisterRequest request = new RegisterRequest("Name", "1907001", "exists@kuet.ac.bd", "01700000000", "CSE", "password123", UserRole.STUDENT);
        AtomicBoolean failureCalled = new AtomicBoolean(false);

        coordinator.performRegister(request, new AuthCoordinator.AuthResultCallback() {
            @Override
            public void onSuccess(User user, String firebaseUid, String message) {
                fail("Expected registration failure");
            }

            @Override
            public void onFailure(String errorMessage) {
                failureCalled.set(true);
            }
        });

        assertTrue(failureCalled.get());
        assertEquals("Firebase must NEVER be called when backend registration fails", 0, fakeFirebaseGateway.totalCalls());
    }

    // --- TEST 6: Registration backend ok + firebase fails (keeps backend account) ---
    @Test
    public void testRegisterBackendOk_FirebaseFails_KeepsBackendAccount() {
        User user = new User();
        user.setId(201L);
        user.setEmail("fresh@kuet.ac.bd");

        ApiService fakeApi = createFakeApiService(true, "new_reg_jwt", user);
        fakeFirebaseGateway.mode = FakeFirebaseAuthGateway.Mode.DOWN;

        AuthCoordinator coordinator = new AuthCoordinator(fakeApi, fakeFirebaseGateway, sessionStore);

        RegisterRequest request = new RegisterRequest("Fresh", "1907002", "fresh@kuet.ac.bd", "01700000001", "CSE", "password123", UserRole.STUDENT);
        AtomicBoolean successCalled = new AtomicBoolean(false);
        AtomicReference<String> resultMessage = new AtomicReference<>();

        coordinator.performRegister(request, new AuthCoordinator.AuthResultCallback() {
            @Override
            public void onSuccess(User u, String firebaseUid, String message) {
                successCalled.set(true);
                resultMessage.set(message);
            }

            @Override
            public void onFailure(String errorMessage) {
                fail("Expected backend account to be kept, but got failure: " + errorMessage);
            }
        });

        assertTrue(successCalled.get());
        assertEquals("new_reg_jwt", sessionStore.getToken());
        assertTrue("Should inform user live features will activate on next login",
                resultMessage.get().contains("Account created. Live features will activate on your next login"));
    }

    // --- Helpers ---

    private ApiService createFakeApiService(boolean isSuccess, String token, User user) {
        return (ApiService) Proxy.newProxyInstance(
                ApiService.class.getClassLoader(),
                new Class<?>[]{ApiService.class},
                new InvocationHandler() {
                    @Override
                    public Object invoke(Object proxy, Method method, Object[] args) {
                        String name = method.getName();
                        if ("login".equals(name) || "register".equals(name)) {
                            if (isSuccess) {
                                AuthResponse authResponse = new AuthResponse();
                                authResponse.setToken(token);
                                return new ImmediateCall<>(Response.success(authResponse));
                            } else {
                                ResponseBody errorBody = ResponseBody.create(
                                        MediaType.parse("application/json"),
                                        "{\"status\":401,\"message\":\"Invalid credentials\"}"
                                );
                                return new ImmediateCall<>(Response.<AuthResponse>error(401, errorBody));
                            }
                        } else if ("getMe".equals(name)) {
                            if (isSuccess && user != null) {
                                return new ImmediateCall<>(Response.success(user));
                            } else {
                                return new ImmediateCall<>(Response.success(new User()));
                            }
                        }
                        return null;
                    }
                }
        );
    }

    private static class ImmediateCall<T> implements Call<T> {
        private final Response<T> response;

        public ImmediateCall(Response<T> response) {
            this.response = response;
        }

        @Override
        public void enqueue(Callback<T> callback) {
            callback.onResponse(this, response);
        }

        @Override
        public Response<T> execute() {
            return response;
        }

        @Override public boolean isExecuted() { return true; }
        @Override public void cancel() {}
        @Override public boolean isCanceled() { return false; }
        @Override public Call<T> clone() { return this; }
        @Override public Request request() { return new Request.Builder().url("http://localhost/").build(); }
        @Override public Timeout timeout() { return Timeout.NONE; }
    }

    private static class FakeFirebaseAuthGateway implements FirebaseAuthGateway {
        enum Mode { SUCCESS, USER_MISSING_THEN_CREATED, DOWN }

        Mode mode = Mode.SUCCESS;
        String mockUid = "mock_uid";
        int registerCalls = 0;
        int signInCalls = 0;
        int signInOrRegisterCalls = 0;
        int signOutCalls = 0;
        boolean selfHealed = false;

        int totalCalls() {
            return registerCalls + signInCalls + signInOrRegisterCalls;
        }

        @Override
        public boolean isUserSignedIn() {
            return mockUid != null;
        }

        @Override
        public String getCurrentFirebaseUid() {
            return mockUid;
        }

        @Override
        public void register(String email, String password, AuthCallback callback) {
            registerCalls++;
            if (mode == Mode.SUCCESS) {
                callback.onSuccess(mockUid);
            } else {
                callback.onFailure("Firebase unavailable");
            }
        }

        @Override
        public void signIn(String email, String password, AuthCallback callback) {
            signInCalls++;
            if (mode == Mode.SUCCESS) {
                callback.onSuccess(mockUid);
            } else {
                callback.onFailure("Firebase unavailable");
            }
        }

        @Override
        public void signInOrRegister(String email, String password, AuthCallback callback) {
            signInOrRegisterCalls++;
            if (mode == Mode.SUCCESS) {
                callback.onSuccess(mockUid);
            } else if (mode == Mode.USER_MISSING_THEN_CREATED) {
                selfHealed = true;
                callback.onSuccess(mockUid);
            } else {
                callback.onFailure("Network connection issue while reaching Firebase services.");
            }
        }

        @Override
        public void signOut() {
            signOutCalls++;
        }
    }

    private static class InMemorySessionStore implements SessionStore {
        private String token;
        private User user;
        private String firebaseUid;

        @Override public void saveToken(String token) { this.token = token; }
        @Override public String getToken() { return token; }
        @Override public void saveUser(User user) { this.user = user; }
        @Override public User getUser() { return user; }
        @Override public void saveFirebaseUid(String uid) { this.firebaseUid = uid; }
        @Override public String getFirebaseUid() { return firebaseUid; }
        @Override
        public void logout() {
            this.token = null;
            this.user = null;
            this.firebaseUid = null;
        }
    }
}
