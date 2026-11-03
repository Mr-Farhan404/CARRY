package com.carry.dto;

import com.carry.entity.ApplicationStatus;
import com.carry.entity.RoleApplication;
import com.carry.entity.UserRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoleApplicationResponseDto {

    private Long id;
    private Long userId;
    private String userName;
    private String userEmail;
    private String studentId;
    private String department;
    private String phone;
    private String alternativePhone;
    private String bkashNumber;
    private String estimatedReturnTime;
    private Boolean activeHonestCommitment;
    private Boolean agreedToRules;
    private UserRole requestedRole;
    private ApplicationStatus status;
    private String reason;
    private String reviewedByName;
    private String reviewerNotes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static RoleApplicationResponseDto fromEntity(RoleApplication app) {
        if (app == null) return null;
        return RoleApplicationResponseDto.builder()
                .id(app.getId())
                .userId(app.getUser() != null ? app.getUser().getId() : null)
                .userName(app.getUser() != null ? app.getUser().getFullName() : null)
                .userEmail(app.getUser() != null ? app.getUser().getEmail() : null)
                .studentId(app.getUser() != null ? app.getUser().getStudentId() : null)
                .department(app.getUser() != null ? app.getUser().getDepartment() : null)
                .phone(app.getUser() != null ? app.getUser().getPhone() : null)
                .alternativePhone(app.getAlternativePhone())
                .bkashNumber(app.getBkashNumber())
                .estimatedReturnTime(app.getEstimatedReturnTime())
                .activeHonestCommitment(app.getActiveHonestCommitment())
                .agreedToRules(app.getAgreedToRules())
                .requestedRole(app.getRequestedRole())
                .status(app.getStatus())
                .reason(app.getReason())
                .reviewedByName(app.getReviewedBy() != null ? app.getReviewedBy().getFullName() : null)
                .reviewerNotes(app.getReviewerNotes())
                .createdAt(app.getCreatedAt())
                .updatedAt(app.getUpdatedAt())
                .build();
    }
}
