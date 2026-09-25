package com.carry.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Column(length = 100, nullable = false)
    private String fullName;

    @NotNull
    @Column(length = 30, unique = true, nullable = false)
    private String studentId;

    @NotNull
    @Column(length = 120, unique = true, nullable = false)
    private String email;

    @Column(length = 20)
    private String phone;

    @Column(length = 50)
    private String department;

    @NotNull
    @Column(length = 255, nullable = false)
    private String passwordHash;

    @Column(nullable = false)
    @Builder.Default
    private Boolean isAdmin = false;

    @Column(precision = 3, scale = 2)
    private BigDecimal avgRating;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;
}
