package com.carry.dto;

import com.carry.entity.Product;
import com.carry.entity.ProductCategory;
import com.carry.entity.SizeClass;
import com.carry.entity.WeightClass;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductResponseDto {

    private Long id;
    private String name;
    private ProductCategory category;
    private String description;
    private String imageUrl;
    private BigDecimal estimatedPrice;
    private WeightClass weightClass;
    private SizeClass sizeClass;
    private Boolean isSensitive;
    private Boolean isActive;
    private BigDecimal baseDeliveryCharge;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static ProductResponseDto fromEntity(Product p) {
        return fromEntity(p, null);
    }

    public static ProductResponseDto fromEntity(Product p, BigDecimal baseDeliveryCharge) {
        if (p == null) return null;
        return ProductResponseDto.builder()
                .id(p.getId())
                .name(p.getName())
                .category(p.getCategory())
                .description(p.getDescription())
                .imageUrl(p.getImageUrl())
                .estimatedPrice(p.getEstimatedPrice())
                .weightClass(p.getWeightClass())
                .sizeClass(p.getSizeClass())
                .isSensitive(p.getIsSensitive())
                .isActive(p.getIsActive())
                .baseDeliveryCharge(baseDeliveryCharge)
                .createdAt(p.getCreatedAt())
                .updatedAt(p.getUpdatedAt())
                .build();
    }
}
