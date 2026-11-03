package com.carry.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "role_applications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoleApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "requested_role", length = 30, nullable = false)
    private UserRole requestedRole;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(length = 30, nullable = false)
    @Builder.Default
    private ApplicationStatus status = ApplicationStatus.PENDING;

    @Column(name = "alternative_phone", length = 30)
    private String alternativePhone;

    @Column(name = "bkash_number", length = 30)
    private String bkashNumber;

    @Column(name = "estimated_return_time", length = 100)
    private String estimatedReturnTime;

    @Column(name = "active_honest_commitment")
    @Builder.Default
    private Boolean activeHonestCommitment = false;

    @Column(name = "agreed_to_rules", nullable = false)
    @Builder.Default
    private Boolean agreedToRules = true;

    @Column(columnDefinition = "TEXT")
    private String reason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by")
    private User reviewedBy;

    @Column(name = "admin_notes", columnDefinition = "TEXT")
    private String reviewerNotes;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
