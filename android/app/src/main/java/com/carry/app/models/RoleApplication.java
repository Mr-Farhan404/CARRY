package com.carry.app.models;

import com.google.gson.annotations.SerializedName;

public class RoleApplication {
    @SerializedName("id")
    private Long id;

    @SerializedName("userId")
    private Long userId;

    @SerializedName("userName")
    private String userName;

    @SerializedName("userEmail")
    private String userEmail;

    @SerializedName("studentId")
    private String studentId;

    @SerializedName("department")
    private String department;

    @SerializedName("phone")
    private String phone;

    @SerializedName("alternativePhone")
    private String alternativePhone;

    @SerializedName("bkashNumber")
    private String bkashNumber;

    @SerializedName("estimatedReturnTime")
    private String estimatedReturnTime;

    @SerializedName("activeHonestCommitment")
    private Boolean activeHonestCommitment;

    @SerializedName("agreedToRules")
    private Boolean agreedToRules;

    @SerializedName("requestedRole")
    private UserRole requestedRole;

    @SerializedName("status")
    private ApplicationStatus status;

    @SerializedName("reason")
    private String reason;

    @SerializedName("reviewedByName")
    private String reviewedByName;

    @SerializedName("reviewerNotes")
    private String reviewerNotes;

    @SerializedName("createdAt")
    private String createdAt;

    public RoleApplication() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    public String getUserEmail() { return userEmail; }
    public void setUserEmail(String userEmail) { this.userEmail = userEmail; }

    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getAlternativePhone() { return alternativePhone; }
    public void setAlternativePhone(String alternativePhone) { this.alternativePhone = alternativePhone; }

    public String getBkashNumber() { return bkashNumber; }
    public void setBkashNumber(String bkashNumber) { this.bkashNumber = bkashNumber; }

    public String getEstimatedReturnTime() { return estimatedReturnTime; }
    public void setEstimatedReturnTime(String estimatedReturnTime) { this.estimatedReturnTime = estimatedReturnTime; }

    public Boolean getActiveHonestCommitment() { return activeHonestCommitment; }
    public void setActiveHonestCommitment(Boolean activeHonestCommitment) { this.activeHonestCommitment = activeHonestCommitment; }

    public Boolean getAgreedToRules() { return agreedToRules; }
    public void setAgreedToRules(Boolean agreedToRules) { this.agreedToRules = agreedToRules; }

    public UserRole getRequestedRole() { return requestedRole; }
    public void setRequestedRole(UserRole requestedRole) { this.requestedRole = requestedRole; }

    public ApplicationStatus getStatus() { return status; }
    public void setStatus(ApplicationStatus status) { this.status = status; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public String getReviewedByName() { return reviewedByName; }
    public void setReviewedByName(String reviewedByName) { this.reviewedByName = reviewedByName; }

    public String getReviewerNotes() { return reviewerNotes; }
    public void setReviewerNotes(String reviewerNotes) { this.reviewerNotes = reviewerNotes; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
}
