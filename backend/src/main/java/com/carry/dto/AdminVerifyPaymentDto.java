package com.carry.dto;

import com.carry.entity.AdditionalPaymentStatus;
import com.carry.entity.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminVerifyPaymentDto {
    private PaymentStatus status;
    private AdditionalPaymentStatus additionalPaymentStatus;
    private String adminNotes;
}
