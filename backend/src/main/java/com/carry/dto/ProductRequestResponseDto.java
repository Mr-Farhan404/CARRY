package com.carry.dto;

import com.carry.entity.LocationArea;
import com.carry.entity.Payment;
import com.carry.entity.ProductRequest;
import com.carry.entity.RequestStatus;
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
public class ProductRequestResponseDto {

    private Long id;
    private Long customerId;
    private String customerName;
    private String productName;
    private String category;
    private Integer quantity;
    private String preferredShop;
    private LocationArea pickupArea;
    private BigDecimal budget;
    private String instructions;
    private RequestStatus status;
    private Long matchedTripId;
    private Integer version;
    private PaymentResponseDto payment;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static ProductRequestResponseDto fromEntity(ProductRequest request) {
        return fromEntity(request, request != null ? request.getPayment() : null);
    }

    public static ProductRequestResponseDto fromEntity(ProductRequest request, Payment payment) {
        return ProductRequestResponseDto.builder()
                .id(request.getId())
                .customerId(request.getCustomer() != null ? request.getCustomer().getId() : null)
                .customerName(request.getCustomer() != null ? request.getCustomer().getFullName() : null)
                .productName(request.getProductName())
                .category(request.getCategory())
                .quantity(request.getQuantity())
                .preferredShop(request.getPreferredShop())
                .pickupArea(request.getPickupArea())
                .budget(request.getBudget())
                .instructions(request.getInstructions())
                .status(request.getStatus())
                .matchedTripId(request.getMatchedTrip() != null ? request.getMatchedTrip().getId() : null)
                .version(request.getVersion())
                .payment(payment != null ? PaymentResponseDto.fromEntity(payment) : null)
                .createdAt(request.getCreatedAt())
                .updatedAt(request.getUpdatedAt())
                .build();
    }
}
