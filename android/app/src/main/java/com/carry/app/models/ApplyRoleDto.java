package com.carry.app.models;

import com.google.gson.annotations.SerializedName;

public class ApplyRoleDto {
    @SerializedName("requestedRole")
    private UserRole requestedRole;

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

    @SerializedName("reason")
    private String reason;

    public ApplyRoleDto() {}

    public ApplyRoleDto(UserRole requestedRole, String alternativePhone, String bkashNumber,
                        String estimatedReturnTime, Boolean activeHonestCommitment,
                        Boolean agreedToRules, String reason) {
        this.requestedRole = requestedRole;
        this.alternativePhone = alternativePhone;
        this.bkashNumber = bkashNumber;
        this.estimatedReturnTime = estimatedReturnTime;
        this.activeHonestCommitment = activeHonestCommitment;
        this.agreedToRules = agreedToRules;
        this.reason = reason;
    }

    public UserRole getRequestedRole() { return requestedRole; }
    public void setRequestedRole(UserRole requestedRole) { this.requestedRole = requestedRole; }

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

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
