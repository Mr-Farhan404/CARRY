package com.carry.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "trips", indexes = {
    @Index(name = "idx_trips_partner_id", columnList = "partner_id"),
    @Index(name = "idx_trips_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Trip {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "partner_id", nullable = false)
    private User partner;

    @NotNull
    @Column(nullable = false)
    private LocalDateTime departureTime;

    @NotNull
    @Column(nullable = false)
    private LocalDateTime expectedReturnTime;

    private LocalDateTime actualReturnTime;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(length = 50, nullable = false)
    private LocationArea destinationArea;

    @Column(length = 255)
    private String capacityNotes;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TripStatus status;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;
}
