package com.carry.app.models;

import com.google.gson.annotations.SerializedName;
import java.math.BigDecimal;
import java.io.Serializable;

public class Product implements Serializable {
    @SerializedName("id")
    private Long id;

    @SerializedName("name")
    private String name;

    @SerializedName("category")
    private ProductCategory category;

    @SerializedName("description")
    private String description;

    @SerializedName("imageUrl")
    private String imageUrl;

    @SerializedName("estimatedPrice")
    private BigDecimal estimatedPrice;

    @SerializedName("weightClass")
    private WeightClass weightClass;

    @SerializedName("sizeClass")
    private SizeClass sizeClass;

    @SerializedName("isSensitive")
    private Boolean isSensitive;

    @SerializedName("isActive")
    private Boolean isActive;

    @SerializedName("baseDeliveryCharge")
    private BigDecimal baseDeliveryCharge;

    public Product() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public ProductCategory getCategory() { return category; }
    public void setCategory(ProductCategory category) { this.category = category; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public BigDecimal getEstimatedPrice() {
        return estimatedPrice != null ? estimatedPrice : BigDecimal.ZERO;
    }
    public void setEstimatedPrice(BigDecimal estimatedPrice) { this.estimatedPrice = estimatedPrice; }

    public WeightClass getWeightClass() { return weightClass; }
    public void setWeightClass(WeightClass weightClass) { this.weightClass = weightClass; }

    public SizeClass getSizeClass() { return sizeClass; }
    public void setSizeClass(SizeClass sizeClass) { this.sizeClass = sizeClass; }

    public Boolean getIsSensitive() { return isSensitive != null && isSensitive; }
    public void setIsSensitive(Boolean sensitive) { isSensitive = sensitive; }

    public Boolean getIsActive() { return isActive != null && isActive; }
    public void setIsActive(Boolean active) { isActive = active; }

    public BigDecimal getBaseDeliveryCharge() { return baseDeliveryCharge; }
    public void setBaseDeliveryCharge(BigDecimal baseDeliveryCharge) { this.baseDeliveryCharge = baseDeliveryCharge; }
}
