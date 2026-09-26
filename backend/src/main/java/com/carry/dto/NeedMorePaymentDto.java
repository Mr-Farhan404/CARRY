package com.carry.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class NeedMorePaymentDto {
    @NotNull(message = "Additional product cost is required")
    @DecimalMin(value = "1.0", message = "Additional amount must be at least 1 taka")
    private BigDecimal additionalAmount;

    @NotBlank(message = "Reason for price increase is required")
    private String reason;
}
