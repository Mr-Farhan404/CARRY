package com.carry.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "moderator_reports")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ModeratorReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "moderator_id", nullable = false)
    private User moderator;

    @NotNull
    @Column(name = "moderator_name", length = 100, nullable = false)
    private String moderatorName;

    @NotNull
    @Column(name = "action_type", length = 50, nullable = false)
    private String actionType;

    @NotNull
    @Column(columnDefinition = "TEXT", nullable = false)
    private String description;

    @Column(name = "target_id")
    private Long targetId;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
