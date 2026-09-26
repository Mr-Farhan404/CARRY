package com.carry.service;

import com.carry.config.DeliveryChargeConfig;
import com.carry.entity.Product;
import com.carry.entity.SizeClass;
import com.carry.entity.WeightClass;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class DeliveryChargeService {

    public BigDecimal getManualBaseCharge() {
        return DeliveryChargeConfig.BASE_CHARGE.setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal calculateCatalogCharge(Product product, Integer quantity) {
        if (product == null) {
            return getManualBaseCharge();
        }
        return calculate(product.getWeightClass(), product.getSizeClass(), product.getIsSensitive(), quantity);
    }

    public BigDecimal calculate(WeightClass weightClass, SizeClass sizeClass, Boolean isSensitive, Integer quantity) {
        BigDecimal charge = DeliveryChargeConfig.BASE_CHARGE;

        if (weightClass != null) {
            BigDecimal weightSurcharge = DeliveryChargeConfig.WEIGHT_SURCHARGES.getOrDefault(weightClass, BigDecimal.ZERO);
            charge = charge.add(weightSurcharge);
        }

        if (sizeClass != null) {
            BigDecimal sizeSurcharge = DeliveryChargeConfig.SIZE_SURCHARGES.getOrDefault(sizeClass, BigDecimal.ZERO);
            charge = charge.add(sizeSurcharge);
        }

        if (Boolean.TRUE.equals(isSensitive)) {
            charge = charge.add(DeliveryChargeConfig.SENSITIVE_SURCHARGE);
        }

        int qty = (quantity != null && quantity > 1) ? quantity : 1;
        if (qty > 1) {
            BigDecimal extraCharge = DeliveryChargeConfig.PER_EXTRA_UNIT.multiply(BigDecimal.valueOf(qty - 1));
            charge = charge.add(extraCharge);
        }

        return charge.setScale(2, RoundingMode.HALF_UP);
    }
}
