package com.carry.dto;

import com.carry.entity.ProductCategory;
import com.carry.entity.SizeClass;
import com.carry.entity.WeightClass;
import jakarta.validation.constraints.DecimalMin;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateProductDto {

    private String name;
    private ProductCategory category;
    private String description;
    private String imageUrl;

    @DecimalMin(value = "0.0", message = "Estimated price must be non-negative")
    private BigDecimal estimatedPrice;

    private WeightClass weightClass;
    private SizeClass sizeClass;
    private Boolean isSensitive;
    private Boolean isActive;
}
