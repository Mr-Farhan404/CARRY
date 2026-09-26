package com.carry.dto;

import com.carry.entity.AdditionalPaymentStatus;
import com.carry.entity.Payment;
import com.carry.entity.PaymentMethod;
import com.carry.entity.PaymentStatus;
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
public class PaymentResponseDto {
    private Long id;
    private Long requestId;
    private String productName;
    private String customerName;
    private String customerEmail;
    private String partnerName;
    private BigDecimal productCost;
    private BigDecimal deliveryFee;
    private BigDecimal gatewayFee;
    private BigDecimal total;
    private PaymentStatus status;
    private PaymentMethod paymentMethod;
    private String senderPhone;
    private String trxId;

    // Need more / price adjustment details
    private BigDecimal additionalAmount;
    private BigDecimal additionalFee;
    private BigDecimal additionalTotal;
    private PaymentMethod additionalPaymentMethod;
    private String additionalSenderPhone;
    private String additionalTrxId;
    private AdditionalPaymentStatus additionalPaymentStatus;
    private String needMoreReason;
    private String adminNotes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static PaymentResponseDto fromEntity(Payment p) {
        if (p == null) return null;

        String productName = null;
        String customerName = null;
        String customerEmail = null;
        String partnerName = null;

        if (p.getRequest() != null) {
            productName = p.getRequest().getProductName();
            if (p.getRequest().getCustomer() != null) {
                customerName = p.getRequest().getCustomer().getFullName();
                customerEmail = p.getRequest().getCustomer().getEmail();
            }
            if (p.getRequest().getMatchedTrip() != null && p.getRequest().getMatchedTrip().getPartner() != null) {
                partnerName = p.getRequest().getMatchedTrip().getPartner().getFullName();
            }
        }

        return PaymentResponseDto.builder()
                .id(p.getId())
                .requestId(p.getRequest() != null ? p.getRequest().getId() : null)
                .productName(productName)
                .customerName(customerName)
                .customerEmail(customerEmail)
                .partnerName(partnerName)
                .productCost(p.getProductCost())
                .deliveryFee(p.getDeliveryFee())
                .gatewayFee(p.getGatewayFee())
                .total(p.getTotal())
                .status(p.getStatus())
                .paymentMethod(p.getPaymentMethod())
                .senderPhone(p.getSenderPhone())
                .trxId(p.getTrxId())
                .additionalAmount(p.getAdditionalAmount())
                .additionalFee(p.getAdditionalFee())
                .additionalTotal(p.getAdditionalTotal())
                .additionalPaymentMethod(p.getAdditionalPaymentMethod())
                .additionalSenderPhone(p.getAdditionalSenderPhone())
                .additionalTrxId(p.getAdditionalTrxId())
                .additionalPaymentStatus(p.getAdditionalPaymentStatus())
                .needMoreReason(p.getNeedMoreReason())
                .adminNotes(p.getAdminNotes())
                .createdAt(p.getCreatedAt())
                .updatedAt(p.getUpdatedAt())
                .build();
    }
}
