package com.carry.dto;

import com.carry.entity.ProductCategory;
import com.carry.entity.SizeClass;
import com.carry.entity.WeightClass;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateProductDto {

    @NotBlank(message = "Product name is required")
    private String name;

    @NotNull(message = "Category is required")
    private ProductCategory category;

    private String description;

    private String imageUrl;

    @NotNull(message = "Estimated price is required")
    @DecimalMin(value = "0.0", message = "Estimated price must be non-negative")
    private BigDecimal estimatedPrice;

    @NotNull(message = "Weight class is required")
    private WeightClass weightClass;

    @NotNull(message = "Size class is required")
    private SizeClass sizeClass;

    @Builder.Default
    private Boolean isSensitive = false;

    @Builder.Default
    private Boolean isActive = true;
}
