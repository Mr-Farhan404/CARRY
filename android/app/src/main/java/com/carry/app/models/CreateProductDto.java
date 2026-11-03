package com.carry.app.models;

import com.google.gson.annotations.SerializedName;
import java.math.BigDecimal;

public class CreateProductDto {

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
    private WeightClass weightClass = WeightClass.LIGHT;

    @SerializedName("sizeClass")
    private SizeClass sizeClass = SizeClass.SMALL;

    @SerializedName("isSensitive")
    private Boolean isSensitive = false;

    @SerializedName("isActive")
    private Boolean isActive = true;

    public CreateProductDto() {}

    public CreateProductDto(String name, ProductCategory category, String description,
                            String imageUrl, BigDecimal estimatedPrice) {
        this.name = name;
        this.category = category;
        this.description = description;
        this.imageUrl = imageUrl;
        this.estimatedPrice = estimatedPrice;
        this.weightClass = WeightClass.LIGHT;
        this.sizeClass = SizeClass.SMALL;
        this.isSensitive = false;
        this.isActive = true;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public ProductCategory getCategory() { return category; }
    public void setCategory(ProductCategory category) { this.category = category; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public BigDecimal getEstimatedPrice() { return estimatedPrice; }
    public void setEstimatedPrice(BigDecimal estimatedPrice) { this.estimatedPrice = estimatedPrice; }

    public WeightClass getWeightClass() { return weightClass; }
    public void setWeightClass(WeightClass weightClass) { this.weightClass = weightClass; }

    public SizeClass getSizeClass() { return sizeClass; }
    public void setSizeClass(SizeClass sizeClass) { this.sizeClass = sizeClass; }

    public Boolean getIsSensitive() { return isSensitive; }
    public void setIsSensitive(Boolean sensitive) { isSensitive = sensitive; }

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean active) { isActive = active; }
}
