package com.carry.entity;

public enum UserRole {
    ADMIN,
    MODERATOR,
    STUDENT,
    DELIVERY_PARTNER,
    TEACHER;

    public boolean isAdmin() {
        return this == ADMIN;
    }

    public boolean isModerator() {
        return this == MODERATOR;
    }

    public boolean isDeliveryPartner() {
        return this == DELIVERY_PARTNER;
    }

    public boolean isStudent() {
        return this == STUDENT;
    }

    public boolean isTeacher() {
        return this == TEACHER;
    }
}
