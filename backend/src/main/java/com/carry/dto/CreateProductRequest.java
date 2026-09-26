package com.carry.dto;

import com.carry.entity.LocationArea;
import com.carry.entity.OrderType;
import com.carry.entity.PaymentMethod;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
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
public class CreateProductRequest {

    private OrderType orderType;
    private Long productId;

    private String productName;

    private String category;

    @Min(value = 1, message = "Quantity must be at least 1")
    @Builder.Default
    private Integer quantity = 1;

    private String preferredShop;

    @NotNull(message = "Pickup area is required")
    private LocationArea pickupArea;

    @DecimalMin(value = "0.0", message = "Budget must be greater than or equal to 0")
    private BigDecimal budget;

    private String instructions;

    // Upfront MFS Payment details (Optional for backward compatibility, but used for bKash/Nagad)
    private PaymentMethod paymentMethod;
    private String senderPhone;
    private String trxId;
}
