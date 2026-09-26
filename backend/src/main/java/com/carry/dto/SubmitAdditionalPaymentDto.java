package com.carry.dto;

import com.carry.entity.PaymentMethod;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SubmitAdditionalPaymentDto {
    @NotNull(message = "Payment method is required")
    private PaymentMethod paymentMethod;

    @NotBlank(message = "Sender phone is required")
    private String senderPhone;

    @NotBlank(message = "Transaction ID (TrxID) is required")
    private String trxId;
}
