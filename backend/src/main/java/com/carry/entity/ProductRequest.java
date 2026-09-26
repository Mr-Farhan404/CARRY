package com.carry.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "requests", indexes = {
    @Index(name = "idx_requests_status", columnList = "status"),
    @Index(name = "idx_requests_customer_id", columnList = "customer_id"),
    @Index(name = "idx_requests_matched_trip_id", columnList = "matched_trip_id"),
    @Index(name = "idx_requests_order_type", columnList = "order_type"),
    @Index(name = "idx_requests_product_id", columnList = "product_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "order_type", length = 30, nullable = false)
    @Builder.Default
    private OrderType orderType = OrderType.MANUAL;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;

    @Column(name = "unit_price_snapshot", precision = 10, scale = 2)
    private BigDecimal unitPriceSnapshot;

    @Column(name = "delivery_charge", precision = 10, scale = 2)
    private BigDecimal deliveryCharge;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private User customer;

    @NotNull
    @Column(length = 150, nullable = false)
    private String productName;

    @Column(length = 50)
    private String category;

    @Min(1)
    @Column(nullable = false)
    @Builder.Default
    private Integer quantity = 1;

    @Column(length = 150)
    private String preferredShop;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(length = 50, nullable = false)
    private LocationArea pickupArea;

    @Min(0)
    @Column(precision = 10, scale = 2)
    private BigDecimal budget;

    @Column(columnDefinition = "TEXT")
    private String instructions;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RequestStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "matched_trip_id")
    private Trip matchedTrip;

    @OneToOne(mappedBy = "request", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Payment payment;

    @Version
    @Builder.Default
    private Integer version = 0;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
