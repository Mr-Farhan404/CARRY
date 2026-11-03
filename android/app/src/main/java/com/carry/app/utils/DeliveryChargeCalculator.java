package com.carry.app.utils;

import com.carry.app.models.Product;
import com.carry.app.models.SizeClass;
import com.carry.app.models.WeightClass;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class DeliveryChargeCalculator {

    public static final BigDecimal BASE_CHARGE = new BigDecimal("30.00");
    public static final BigDecimal SENSITIVE_SURCHARGE = new BigDecimal("20.00");
    public static final BigDecimal PER_EXTRA_UNIT = new BigDecimal("10.00");
    public static final BigDecimal MFS_FEE_RATE = new BigDecimal("0.0139"); // 1.39%

    public static BigDecimal calculateDeliveryCharge(Product product, int quantity) {
        if (product == null) {
            return BASE_CHARGE;
        }
        return calculateDeliveryCharge(
                product.getWeightClass(),
                product.getSizeClass(),
                product.getIsSensitive(),
                quantity
        );
    }

    public static BigDecimal calculateDeliveryCharge(
            WeightClass weightClass,
            SizeClass sizeClass,
            Boolean isSensitive,
            int quantity) {

        BigDecimal charge = BASE_CHARGE;

        if (weightClass != null) {
            switch (weightClass) {
                case MEDIUM:
                    charge = charge.add(new BigDecimal("15.00"));
                    break;
                case HEAVY:
                    charge = charge.add(new BigDecimal("30.00"));
                    break;
                default:
                    break;
            }
        }

        if (sizeClass != null) {
            switch (sizeClass) {
                case MEDIUM:
                    charge = charge.add(new BigDecimal("10.00"));
                    break;
                case LARGE:
                    charge = charge.add(new BigDecimal("25.00"));
                    break;
                default:
                    break;
            }
        }

        if (Boolean.TRUE.equals(isSensitive)) {
            charge = charge.add(SENSITIVE_SURCHARGE);
        }

        int qty = Math.max(1, quantity);
        if (qty > 1) {
            BigDecimal extraUnits = BigDecimal.valueOf(qty - 1);
            charge = charge.add(PER_EXTRA_UNIT.multiply(extraUnits));
        }

        return charge.setScale(2, RoundingMode.HALF_UP);
    }

    public static BigDecimal calculateGatewayFee(BigDecimal subtotal) {
        if (subtotal == null || subtotal.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        return subtotal.multiply(MFS_FEE_RATE).setScale(2, RoundingMode.CEILING);
    }

    public static BigDecimal calculateTotal(BigDecimal productCost, BigDecimal deliveryCharge) {
        BigDecimal base = (productCost != null ? productCost : BigDecimal.ZERO)
                .add(deliveryCharge != null ? deliveryCharge : BigDecimal.ZERO);
        BigDecimal fee = calculateGatewayFee(base);
        return base.add(fee).setScale(2, RoundingMode.HALF_UP);
    }
}
