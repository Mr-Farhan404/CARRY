package com.carry.app.utils;

import android.content.Context;
import android.content.SharedPreferences;

import java.nio.charset.StandardCharsets;
import com.carry.app.data.SessionStore;
import com.carry.app.models.User;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class SessionManager implements SessionStore {
    private static final String PREF_NAME = "carry_session";
    private static final String KEY_TOKEN = "jwt_token";
    private static final String KEY_USER = "user_json";
    private static final String KEY_BASE_URL = "base_url";
    private static final String KEY_ACTIVE_TRIP_ID = "active_trip_id";
    private static final String KEY_FIREBASE_UID = "firebase_uid";

    public static final String URL_LOCAL_EMULATOR = "http://10.0.2.2:8080/";
    public static final String URL_RENDER_CLOUD = "https://carry-backend-hctl.onrender.com/";

    private static SessionManager instance;
    private final SharedPreferences prefs;
    private final Gson gson;

    private SessionManager(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        gson = new Gson();
    }

    public static synchronized SessionManager getInstance(Context context) {
        if (instance == null) {
            instance = new SessionManager(context);
        }
        return instance;
    }

    public void saveToken(String token) {
        prefs.edit().putString(KEY_TOKEN, token).apply();
    }

    public String getToken() {
        return prefs.getString(KEY_TOKEN, null);
    }

    public boolean isTokenExpired() {
        String token = getToken();
        if (token == null || token.trim().isEmpty()) {
            return true;
        }
        try {
            String[] parts = token.split("\\.");
            if (parts.length < 2) {
                return true;
            }
            byte[] bytes;
            try {
                bytes = java.util.Base64.getUrlDecoder().decode(parts[1]);
            } catch (Throwable t) {
                bytes = android.util.Base64.decode(parts[1], android.util.Base64.URL_SAFE | android.util.Base64.NO_WRAP);
            }
            String json = new String(bytes, StandardCharsets.UTF_8);
            JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
            if (obj.has("exp")) {
                long expSeconds = obj.get("exp").getAsLong();
                long nowSeconds = System.currentTimeMillis() / 1000L;
                return nowSeconds >= expSeconds;
            }
        } catch (Exception ignored) {}
        return false;
    }

    public boolean isLoggedIn() {
        String token = getToken();
        if (token == null || token.trim().isEmpty()) {
            return false;
        }
        if (isTokenExpired()) {
            logout();
            return false;
        }
        return true;
    }

    public void saveUser(User user) {
        if (user == null) {
            prefs.edit().remove(KEY_USER).apply();
        } else {
            String json = gson.toJson(user);
            prefs.edit().putString(KEY_USER, json).apply();
        }
    }

    public User getUser() {
        String json = prefs.getString(KEY_USER, null);
        if (json == null) return null;
        try {
            return gson.fromJson(json, User.class);
        } catch (Exception e) {
            return null;
        }
    }

    public void setBaseUrl(String url) {
        if (url != null && !url.endsWith("/")) {
            url += "/";
        }
        prefs.edit().putString(KEY_BASE_URL, url).apply();
    }

    public String getBaseUrl() {
        return prefs.getString(KEY_BASE_URL, URL_RENDER_CLOUD);
    }

    public void setActiveTripId(Long tripId) {
        if (tripId == null) {
            prefs.edit().remove(KEY_ACTIVE_TRIP_ID).apply();
        } else {
            prefs.edit().putLong(KEY_ACTIVE_TRIP_ID, tripId).apply();
        }
    }

    public Long getActiveTripId() {
        long id = prefs.getLong(KEY_ACTIVE_TRIP_ID, -1L);
        return id != -1L ? id : null;
    }

    public void saveFirebaseUid(String uid) {
        if (uid == null) {
            prefs.edit().remove(KEY_FIREBASE_UID).apply();
        } else {
            prefs.edit().putString(KEY_FIREBASE_UID, uid).apply();
        }
    }

    public String getFirebaseUid() {
        return prefs.getString(KEY_FIREBASE_UID, null);
    }

    public void logout() {
        prefs.edit()
                .remove(KEY_TOKEN)
                .remove(KEY_USER)
                .remove(KEY_ACTIVE_TRIP_ID)
                .remove(KEY_FIREBASE_UID)
                .apply();
    }
}
