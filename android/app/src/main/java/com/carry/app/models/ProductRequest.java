package com.carry.app.models;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;
import java.math.BigDecimal;

public class ProductRequest implements Serializable {
    @SerializedName("id")
    private Long id;

    @SerializedName("orderType")
    private OrderType orderType;

    @SerializedName("productId")
    private Long productId;

    @SerializedName("unitPriceSnapshot")
    private BigDecimal unitPriceSnapshot;

    @SerializedName("deliveryCharge")
    private BigDecimal deliveryCharge;

    @SerializedName("customerId")
    private Long customerId;

    @SerializedName("customerName")
    private String customerName;

    @SerializedName("productName")
    private String productName;

    @SerializedName("category")
    private String category;

    @SerializedName("quantity")
    private Integer quantity;

    @SerializedName("preferredShop")
    private String preferredShop;

    @SerializedName("pickupArea")
    private LocationArea pickupArea;

    @SerializedName("budget")
    private BigDecimal budget;

    @SerializedName("instructions")
    private String instructions;

    @SerializedName("status")
    private RequestStatus status;

    @SerializedName("matchedTripId")
    private Long matchedTripId;

    @SerializedName("version")
    private Integer version;

    @SerializedName("payment")
    private Payment payment;

    @SerializedName("createdAt")
    private String createdAt;

    @SerializedName("updatedAt")
    private String updatedAt;

    public ProductRequest() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public OrderType getOrderType() { return orderType != null ? orderType : OrderType.MANUAL; }
    public void setOrderType(OrderType orderType) { this.orderType = orderType; }

    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }

    public BigDecimal getUnitPriceSnapshot() { return unitPriceSnapshot; }
    public void setUnitPriceSnapshot(BigDecimal unitPriceSnapshot) { this.unitPriceSnapshot = unitPriceSnapshot; }

    public BigDecimal getDeliveryCharge() { return deliveryCharge != null ? deliveryCharge : BigDecimal.ZERO; }
    public void setDeliveryCharge(BigDecimal deliveryCharge) { this.deliveryCharge = deliveryCharge; }

    public Long getCustomerId() { return customerId; }
    public void setCustomerId(Long customerId) { this.customerId = customerId; }

    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }

    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public Integer getQuantity() { return quantity != null ? quantity : 1; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }

    public String getPreferredShop() { return preferredShop; }
    public void setPreferredShop(String preferredShop) { this.preferredShop = preferredShop; }

    public LocationArea getPickupArea() { return pickupArea; }
    public void setPickupArea(LocationArea pickupArea) { this.pickupArea = pickupArea; }

    public BigDecimal getBudget() { return budget != null ? budget : BigDecimal.ZERO; }
    public void setBudget(BigDecimal budget) { this.budget = budget; }

    public String getInstructions() { return instructions; }
    public void setInstructions(String instructions) { this.instructions = instructions; }

    public RequestStatus getStatus() { return status; }
    public void setStatus(RequestStatus status) { this.status = status; }

    public Long getMatchedTripId() { return matchedTripId; }
    public void setMatchedTripId(Long matchedTripId) { this.matchedTripId = matchedTripId; }

    public Integer getVersion() { return version != null ? version : 0; }
    public void setVersion(Integer version) { this.version = version; }

    public Payment getPayment() { return payment; }
    public void setPayment(Payment payment) { this.payment = payment; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }
}
