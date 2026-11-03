package com.carry.app.models;

import com.google.gson.annotations.SerializedName;

public class User {
    @SerializedName("id")
    private Long id;

    @SerializedName("studentId")
    private String studentId;

    @SerializedName("fullName")
    private String fullName;

    @SerializedName("email")
    private String email;

    @SerializedName("phone")
    private String phone;

    @SerializedName("department")
    private String department;

    @SerializedName("isAdmin")
    private Boolean isAdmin;

    @SerializedName("role")
    private UserRole role;

    public User() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public Boolean getIsAdmin() { return isAdmin != null && isAdmin; }
    public void setIsAdmin(Boolean admin) { isAdmin = admin; }

    public UserRole getRole() {
        if (role != null) return role;
        if (Boolean.TRUE.equals(isAdmin)) return UserRole.ADMIN;
        return UserRole.STUDENT;
    }
    public void setRole(UserRole role) { this.role = role; }
}
