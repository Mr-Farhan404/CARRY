package com.carry.app.models;

import com.google.gson.annotations.SerializedName;

public class SubmitAdditionalPaymentDto {
    @SerializedName("paymentMethod")
    private PaymentMethod paymentMethod;

    @SerializedName("senderPhone")
    private String senderPhone;

    @SerializedName("trxId")
    private String trxId;

    public SubmitAdditionalPaymentDto() {}

    public SubmitAdditionalPaymentDto(PaymentMethod paymentMethod, String senderPhone, String trxId) {
        this.paymentMethod = paymentMethod;
        this.senderPhone = senderPhone;
        this.trxId = trxId;
    }

    public PaymentMethod getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(PaymentMethod paymentMethod) { this.paymentMethod = paymentMethod; }

    public String getSenderPhone() { return senderPhone; }
    public void setSenderPhone(String senderPhone) { this.senderPhone = senderPhone; }

    public String getTrxId() { return trxId; }
    public void setTrxId(String trxId) { this.trxId = trxId; }
}
