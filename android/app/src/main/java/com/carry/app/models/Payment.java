package com.carry.app.models;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;
import java.math.BigDecimal;

public class Payment implements Serializable {
    @SerializedName("id")
    private Long id;

    @SerializedName("requestId")
    private Long requestId;

    @SerializedName("productName")
    private String productName;

    @SerializedName("customerName")
    private String customerName;

    @SerializedName("customerEmail")
    private String customerEmail;

    @SerializedName("partnerName")
    private String partnerName;

    @SerializedName("productCost")
    private BigDecimal productCost;

    @SerializedName("deliveryFee")
    private BigDecimal deliveryFee;

    @SerializedName("gatewayFee")
    private BigDecimal gatewayFee;

    @SerializedName("total")
    private BigDecimal total;

    @SerializedName("status")
    private PaymentStatus status;

    @SerializedName("paymentMethod")
    private PaymentMethod paymentMethod;

    @SerializedName("senderPhone")
    private String senderPhone;

    @SerializedName("trxId")
    private String trxId;

    // Need more / price adjustment details
    @SerializedName("additionalAmount")
    private BigDecimal additionalAmount;

    @SerializedName("additionalFee")
    private BigDecimal additionalFee;

    @SerializedName("additionalTotal")
    private BigDecimal additionalTotal;

    @SerializedName("additionalPaymentMethod")
    private PaymentMethod additionalPaymentMethod;

    @SerializedName("additionalSenderPhone")
    private String additionalSenderPhone;

    @SerializedName("additionalTrxId")
    private String additionalTrxId;

    @SerializedName("additionalPaymentStatus")
    private AdditionalPaymentStatus additionalPaymentStatus;

    @SerializedName("needMoreReason")
    private String needMoreReason;

    @SerializedName("adminNotes")
    private String adminNotes;

    @SerializedName("createdAt")
    private String createdAt;

    public Payment() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getRequestId() { return requestId; }
    public void setRequestId(Long requestId) { this.requestId = requestId; }

    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }

    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }

    public String getCustomerEmail() { return customerEmail; }
    public void setCustomerEmail(String customerEmail) { this.customerEmail = customerEmail; }

    public String getPartnerName() { return partnerName; }
    public void setPartnerName(String partnerName) { this.partnerName = partnerName; }

    public BigDecimal getProductCost() { return productCost != null ? productCost : BigDecimal.ZERO; }
    public void setProductCost(BigDecimal productCost) { this.productCost = productCost; }

    public BigDecimal getDeliveryFee() { return deliveryFee != null ? deliveryFee : BigDecimal.ZERO; }
    public void setDeliveryFee(BigDecimal deliveryFee) { this.deliveryFee = deliveryFee; }

    public BigDecimal getGatewayFee() { return gatewayFee != null ? gatewayFee : BigDecimal.ZERO; }
    public void setGatewayFee(BigDecimal gatewayFee) { this.gatewayFee = gatewayFee; }

    public BigDecimal getTotal() { return total != null ? total : BigDecimal.ZERO; }
    public void setTotal(BigDecimal total) { this.total = total; }

    public PaymentStatus getStatus() { return status; }
    public void setStatus(PaymentStatus status) { this.status = status; }

    public PaymentMethod getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(PaymentMethod paymentMethod) { this.paymentMethod = paymentMethod; }

    public String getSenderPhone() { return senderPhone; }
    public void setSenderPhone(String senderPhone) { this.senderPhone = senderPhone; }

    public String getTrxId() { return trxId; }
    public void setTrxId(String trxId) { this.trxId = trxId; }

    public BigDecimal getAdditionalAmount() { return additionalAmount; }
    public void setAdditionalAmount(BigDecimal additionalAmount) { this.additionalAmount = additionalAmount; }

    public BigDecimal getAdditionalFee() { return additionalFee; }
    public void setAdditionalFee(BigDecimal additionalFee) { this.additionalFee = additionalFee; }

    public BigDecimal getAdditionalTotal() { return additionalTotal; }
    public void setAdditionalTotal(BigDecimal additionalTotal) { this.additionalTotal = additionalTotal; }

    public PaymentMethod getAdditionalPaymentMethod() { return additionalPaymentMethod; }
    public void setAdditionalPaymentMethod(PaymentMethod additionalPaymentMethod) { this.additionalPaymentMethod = additionalPaymentMethod; }

    public String getAdditionalSenderPhone() { return additionalSenderPhone; }
    public void setAdditionalSenderPhone(String additionalSenderPhone) { this.additionalSenderPhone = additionalSenderPhone; }

    public String getAdditionalTrxId() { return additionalTrxId; }
    public void setAdditionalTrxId(String additionalTrxId) { this.additionalTrxId = additionalTrxId; }

    public AdditionalPaymentStatus getAdditionalPaymentStatus() { return additionalPaymentStatus; }
    public void setAdditionalPaymentStatus(AdditionalPaymentStatus additionalPaymentStatus) { this.additionalPaymentStatus = additionalPaymentStatus; }

    public String getNeedMoreReason() { return needMoreReason; }
    public void setNeedMoreReason(String needMoreReason) { this.needMoreReason = needMoreReason; }

    public String getAdminNotes() { return adminNotes; }
    public void setAdminNotes(String adminNotes) { this.adminNotes = adminNotes; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
}
