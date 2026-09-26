package com.carry.dto;

import com.carry.entity.LocationArea;
import com.carry.entity.PaymentMethod;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CheckoutLineDto {

    @NotNull(message = "Product ID is required")
    private Long productId;

    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be at least 1")
    @Builder.Default
    private Integer quantity = 1;

    private LocationArea pickupArea;
    private String preferredShop;
    private String instructions;
    private PaymentMethod paymentMethod;
    private String senderPhone;
    private String trxId;
}
