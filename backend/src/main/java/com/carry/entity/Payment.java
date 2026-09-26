package com.carry.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "request_id", nullable = false, unique = true)
    private ProductRequest request;

    @NotNull
    @Column(precision = 10, scale = 2, nullable = false)
    private BigDecimal productCost;

    @NotNull
    @Column(precision = 10, scale = 2, nullable = false)
    private BigDecimal deliveryFee;

    @Column(precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal gatewayFee = BigDecimal.ZERO;

    @NotNull
    @Column(precision = 10, scale = 2, nullable = false)
    private BigDecimal total;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(length = 50, nullable = false)
    private PaymentStatus status;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private PaymentMethod paymentMethod;

    @Column(length = 30)
    private String senderPhone;

    @Column(length = 100)
    private String trxId;

    // "Need More" / Price Adjustment Fields
    @Column(precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal additionalAmount = BigDecimal.ZERO;

    @Column(precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal additionalFee = BigDecimal.ZERO;

    @Column(precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal additionalTotal = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private PaymentMethod additionalPaymentMethod;

    @Column(length = 30)
    private String additionalSenderPhone;

    @Column(length = 100)
    private String additionalTrxId;

    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    @Builder.Default
    private AdditionalPaymentStatus additionalPaymentStatus = AdditionalPaymentStatus.NONE;

    @Column(columnDefinition = "TEXT")
    private String needMoreReason;

    @Column(columnDefinition = "TEXT")
    private String adminNotes;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
