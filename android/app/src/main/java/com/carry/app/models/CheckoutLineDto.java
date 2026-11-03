package com.carry.app.models;

import com.google.gson.annotations.SerializedName;

public class CheckoutLineDto {
    @SerializedName("productId")
    private Long productId;

    @SerializedName("quantity")
    private Integer quantity;

    @SerializedName("pickupArea")
    private LocationArea pickupArea;

    @SerializedName("preferredShop")
    private String preferredShop;

    @SerializedName("instructions")
    private String instructions;

    @SerializedName("paymentMethod")
    private PaymentMethod paymentMethod;

    @SerializedName("senderPhone")
    private String senderPhone;

    @SerializedName("trxId")
    private String trxId;

    public CheckoutLineDto() {}

    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }

    public LocationArea getPickupArea() { return pickupArea; }
    public void setPickupArea(LocationArea pickupArea) { this.pickupArea = pickupArea; }

    public String getPreferredShop() { return preferredShop; }
    public void setPreferredShop(String preferredShop) { this.preferredShop = preferredShop; }

    public String getInstructions() { return instructions; }
    public void setInstructions(String instructions) { this.instructions = instructions; }

    public PaymentMethod getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(PaymentMethod paymentMethod) { this.paymentMethod = paymentMethod; }

    public String getSenderPhone() { return senderPhone; }
    public void setSenderPhone(String senderPhone) { this.senderPhone = senderPhone; }

    public String getTrxId() { return trxId; }
    public void setTrxId(String trxId) { this.trxId = trxId; }
}
