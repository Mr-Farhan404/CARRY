package com.carry.app.models;

import com.google.gson.annotations.SerializedName;
import java.math.BigDecimal;

public class NeedMorePaymentDto {
    @SerializedName("additionalAmount")
    private BigDecimal additionalAmount;

    @SerializedName("reason")
    private String reason;

    public NeedMorePaymentDto() {}

    public NeedMorePaymentDto(BigDecimal additionalAmount, String reason) {
        this.additionalAmount = additionalAmount;
        this.reason = reason;
    }

    public BigDecimal getAdditionalAmount() { return additionalAmount; }
    public void setAdditionalAmount(BigDecimal additionalAmount) { this.additionalAmount = additionalAmount; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
