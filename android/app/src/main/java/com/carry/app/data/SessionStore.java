package com.carry.app.data;

import com.carry.app.models.User;

public interface SessionStore {
    void saveToken(String token);
    String getToken();
    void saveUser(User user);
    User getUser();
    void saveFirebaseUid(String uid);
    String getFirebaseUid();
    void logout();
}
