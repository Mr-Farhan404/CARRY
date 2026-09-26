package com.carry.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "products", indexes = {
    @Index(name = "idx_products_category", columnList = "category"),
    @Index(name = "idx_products_is_active", columnList = "is_active")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(length = 150, nullable = false)
    private String name;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(length = 50, nullable = false)
    private ProductCategory category;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @NotNull
    @DecimalMin(value = "0.0")
    @Column(name = "estimated_price", precision = 10, scale = 2, nullable = false)
    private BigDecimal estimatedPrice;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "weight_class", length = 30, nullable = false)
    private WeightClass weightClass;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "size_class", length = 30, nullable = false)
    private SizeClass sizeClass;

    @NotNull
    @Column(name = "is_sensitive", nullable = false)
    @Builder.Default
    private Boolean isSensitive = false;

    @NotNull
    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
