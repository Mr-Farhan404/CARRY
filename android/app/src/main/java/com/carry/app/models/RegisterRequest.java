package com.carry.app.models;

import com.google.gson.annotations.SerializedName;

public class RegisterRequest {
    @SerializedName("fullName")
    private String fullName;

    @SerializedName("studentId")
    private String studentId;

    @SerializedName("email")
    private String email;

    @SerializedName("phone")
    private String phone;

    @SerializedName("department")
    private String department;

    @SerializedName("password")
    private String password;

    @SerializedName("role")
    private UserRole role = UserRole.STUDENT;

    public RegisterRequest(String fullName, String studentId, String email, String phone, String department, String password) {
        this(fullName, studentId, email, phone, department, password, UserRole.STUDENT);
    }

    public RegisterRequest(String fullName, String studentId, String email, String phone, String department, String password, UserRole role) {
        this.fullName = fullName;
        this.studentId = studentId;
        this.email = email;
        this.phone = phone;
        this.department = department;
        this.password = password;
        this.role = role != null ? role : UserRole.STUDENT;
    }

    public UserRole getRole() { return role; }
    public void setRole(UserRole role) { this.role = role; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}
