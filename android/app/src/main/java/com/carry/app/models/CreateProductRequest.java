package com.carry.app.models;

import com.google.gson.annotations.SerializedName;
import java.math.BigDecimal;

public class CreateProductRequest {
    @SerializedName("orderType")
    private OrderType orderType = OrderType.MANUAL;

    @SerializedName("productId")
    private Long productId;

    @SerializedName("productName")
    private String productName;

    @SerializedName("category")
    private String category;

    @SerializedName("quantity")
    private Integer quantity = 1;

    @SerializedName("preferredShop")
    private String preferredShop;

    @SerializedName("pickupArea")
    private LocationArea pickupArea;

    @SerializedName("budget")
    private BigDecimal budget;

    @SerializedName("instructions")
    private String instructions;

    @SerializedName("paymentMethod")
    private PaymentMethod paymentMethod;

    @SerializedName("senderPhone")
    private String senderPhone;

    @SerializedName("trxId")
    private String trxId;

    public CreateProductRequest() {}

    public OrderType getOrderType() { return orderType; }
    public void setOrderType(OrderType orderType) { this.orderType = orderType; }

    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }

    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }

    public String getPreferredShop() { return preferredShop; }
    public void setPreferredShop(String preferredShop) { this.preferredShop = preferredShop; }

    public LocationArea getPickupArea() { return pickupArea; }
    public void setPickupArea(LocationArea pickupArea) { this.pickupArea = pickupArea; }

    public BigDecimal getBudget() { return budget; }
    public void setBudget(BigDecimal budget) { this.budget = budget; }

    public String getInstructions() { return instructions; }
    public void setInstructions(String instructions) { this.instructions = instructions; }

    public PaymentMethod getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(PaymentMethod paymentMethod) { this.paymentMethod = paymentMethod; }

    public String getSenderPhone() { return senderPhone; }
    public void setSenderPhone(String senderPhone) { this.senderPhone = senderPhone; }

    public String getTrxId() { return trxId; }
    public void setTrxId(String trxId) { this.trxId = trxId; }
}
