package com.carry.service;

import com.carry.dto.ApplyRoleDto;
import com.carry.dto.RoleApplicationResponseDto;
import com.carry.entity.ApplicationStatus;
import com.carry.entity.RoleApplication;
import com.carry.entity.User;
import com.carry.entity.UserRole;
import com.carry.exception.ResourceNotFoundException;
import com.carry.repository.RoleApplicationRepository;
import com.carry.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RoleApplicationService {

    private final RoleApplicationRepository roleApplicationRepository;
    private final UserRepository userRepository;
    private final ModeratorReportService moderatorReportService;

    @Transactional
    public RoleApplicationResponseDto applyForRole(ApplyRoleDto dto, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        if (user.getRole() == UserRole.TEACHER) {
            throw new IllegalArgumentException("Teacher accounts are not permitted to apply for Moderator or Delivery Partner roles.");
        }

        UserRole requestedRole = dto.getRequestedRole();
        if (requestedRole != UserRole.DELIVERY_PARTNER && requestedRole != UserRole.MODERATOR) {
            throw new IllegalArgumentException("Invalid requested role: " + requestedRole + ". Only DELIVERY_PARTNER and MODERATOR are allowed.");
        }

        if (user.getRole() == requestedRole) {
            throw new IllegalArgumentException("You already hold the role: " + requestedRole);
        }

        // Check if there is already a pending application for this user and role
        roleApplicationRepository.findByUserIdAndRequestedRoleAndStatus(user.getId(), requestedRole, ApplicationStatus.PENDING)
                .ifPresent(app -> {
                    throw new IllegalArgumentException("You already have a pending application for " + requestedRole);
                });

        if (dto.getAgreedToRules() != null && !dto.getAgreedToRules()) {
            throw new IllegalArgumentException("You must agree to all the rules to submit an application.");
        }

        RoleApplication application = RoleApplication.builder()
                .user(user)
                .requestedRole(requestedRole)
                .status(ApplicationStatus.PENDING)
                .alternativePhone(dto.getAlternativePhone() != null ? dto.getAlternativePhone().trim() : null)
                .bkashNumber(dto.getBkashNumber() != null ? dto.getBkashNumber().trim() : null)
                .estimatedReturnTime(dto.getEstimatedReturnTime() != null ? dto.getEstimatedReturnTime().trim() : null)
                .activeHonestCommitment(Boolean.TRUE.equals(dto.getActiveHonestCommitment()))
                .agreedToRules(dto.getAgreedToRules() == null || Boolean.TRUE.equals(dto.getAgreedToRules()))
                .reason(dto.getReason() != null ? dto.getReason().trim() : null)
                .build();

        RoleApplication saved = roleApplicationRepository.save(application);
        log.info("User {} applied for role {}", user.getEmail(), requestedRole);
        return RoleApplicationResponseDto.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public List<RoleApplicationResponseDto> getMyApplications(Long userId) {
        return roleApplicationRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(RoleApplicationResponseDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<RoleApplicationResponseDto> getPendingDeliveryPartnerApplications() {
        return roleApplicationRepository.findByRequestedRoleAndStatusOrderByCreatedAtDesc(UserRole.DELIVERY_PARTNER, ApplicationStatus.PENDING)
                .stream()
                .map(RoleApplicationResponseDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<RoleApplicationResponseDto> getAllDeliveryPartnerApplications() {
        return roleApplicationRepository.findByRequestedRoleOrderByCreatedAtDesc(UserRole.DELIVERY_PARTNER)
                .stream()
                .map(RoleApplicationResponseDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<RoleApplicationResponseDto> getPendingModeratorApplications() {
        return roleApplicationRepository.findByRequestedRoleAndStatusOrderByCreatedAtDesc(UserRole.MODERATOR, ApplicationStatus.PENDING)
                .stream()
                .map(RoleApplicationResponseDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<RoleApplicationResponseDto> getAllModeratorApplications() {
        return roleApplicationRepository.findByRequestedRoleOrderByCreatedAtDesc(UserRole.MODERATOR)
                .stream()
                .map(RoleApplicationResponseDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional
    public RoleApplicationResponseDto reviewDeliveryPartnerApplication(Long applicationId, boolean approve, String notes, Long reviewerId) {
        RoleApplication application = roleApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Role application not found with id: " + applicationId));

        if (application.getRequestedRole() != UserRole.DELIVERY_PARTNER) {
            throw new IllegalArgumentException("This application is not for Delivery Partner.");
        }

        User reviewer = userRepository.findById(reviewerId)
                .orElseThrow(() -> new ResourceNotFoundException("Reviewer not found with id: " + reviewerId));

        application.setStatus(approve ? ApplicationStatus.APPROVED : ApplicationStatus.REJECTED);
        application.setReviewedBy(reviewer);
        application.setReviewerNotes(notes);

        if (approve) {
            User applicant = application.getUser();
            applicant.setRole(UserRole.DELIVERY_PARTNER);
            userRepository.save(applicant);
            log.info("Applicant {} upgraded to DELIVERY_PARTNER by {}", applicant.getEmail(), reviewer.getEmail());
        }

        RoleApplication saved = roleApplicationRepository.save(application);

        // If reviewer is a moderator, log this action in moderator_reports
        if (reviewer.getRole() == UserRole.MODERATOR) {
            String actionType = approve ? "APPROVE_PARTNER" : "REJECT_PARTNER";
            String description = String.format("%s delivery partner application #%d for %s (%s). Notes: %s",
                    approve ? "Approved" : "Rejected",
                    application.getId(),
                    application.getUser().getFullName(),
                    application.getUser().getEmail(),
                    notes != null ? notes : "No notes");
            moderatorReportService.logActivity(reviewer, actionType, description, application.getId());
        }

        return RoleApplicationResponseDto.fromEntity(saved);
    }

    @Transactional
    public RoleApplicationResponseDto reviewModeratorApplication(Long applicationId, boolean approve, String notes, Long adminId) {
        RoleApplication application = roleApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Role application not found with id: " + applicationId));

        if (application.getRequestedRole() != UserRole.MODERATOR) {
            throw new IllegalArgumentException("This application is not for Moderator.");
        }

        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new ResourceNotFoundException("Admin not found with id: " + adminId));

        if (admin.getRole() != UserRole.ADMIN && !Boolean.TRUE.equals(admin.getIsAdmin())) {
            throw new IllegalArgumentException("Only Admin can review Moderator applications.");
        }

        application.setStatus(approve ? ApplicationStatus.APPROVED : ApplicationStatus.REJECTED);
        application.setReviewedBy(admin);
        application.setReviewerNotes(notes);

        if (approve) {
            User applicant = application.getUser();
            applicant.setRole(UserRole.MODERATOR);
            userRepository.save(applicant);
            log.info("Applicant {} upgraded to MODERATOR by {}", applicant.getEmail(), admin.getEmail());
        }

        RoleApplication saved = roleApplicationRepository.save(application);
        return RoleApplicationResponseDto.fromEntity(saved);
    }
}
