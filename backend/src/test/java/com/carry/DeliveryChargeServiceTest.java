package com.carry;

import com.carry.entity.Product;
import com.carry.entity.ProductCategory;
import com.carry.entity.SizeClass;
import com.carry.entity.WeightClass;
import com.carry.service.DeliveryChargeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DeliveryChargeServiceTest {

    private DeliveryChargeService deliveryChargeService;

    @BeforeEach
    void setUp() {
        deliveryChargeService = new DeliveryChargeService();
    }

    @Test
    void testManualBaseChargeIs30() {
        assertEquals(new BigDecimal("30.00"), deliveryChargeService.getManualBaseCharge());
    }

    @Test
    void testLightSmallNonSensitiveSingleQuantity() {
        BigDecimal charge = deliveryChargeService.calculate(WeightClass.LIGHT, SizeClass.SMALL, false, 1);
        // Base 30 + 0 + 0 + 0 + 0 = 30.00
        assertEquals(new BigDecimal("30.00"), charge);
    }

    @Test
    void testMediumWeightAndSizeNonSensitive() {
        BigDecimal charge = deliveryChargeService.calculate(WeightClass.MEDIUM, SizeClass.MEDIUM, false, 1);
        // Base 30 + 15 (weight) + 10 (size) + 0 + 0 = 55.00
        assertEquals(new BigDecimal("55.00"), charge);
    }

    @Test
    void testSensitiveItemAddsSurcharge() {
        BigDecimal charge = deliveryChargeService.calculate(WeightClass.LIGHT, SizeClass.SMALL, true, 1);
        // Base 30 + 0 + 0 + 20 (sensitive) + 0 = 50.00
        assertEquals(new BigDecimal("50.00"), charge);
    }

    @Test
    void testHeavyMediumSensitiveItem() {
        BigDecimal charge = deliveryChargeService.calculate(WeightClass.HEAVY, SizeClass.MEDIUM, true, 1);
        // Base 30 + 30 (heavy) + 10 (medium size) + 20 (sensitive) + 0 = 90.00
        assertEquals(new BigDecimal("90.00"), charge);
    }

    @Test
    void testHeavyLargeNonSensitiveItem() {
        BigDecimal charge = deliveryChargeService.calculate(WeightClass.HEAVY, SizeClass.LARGE, false, 1);
        // Base 30 + 30 (heavy) + 25 (large size) + 0 + 0 = 85.00
        assertEquals(new BigDecimal("85.00"), charge);
    }

    @Test
    void testHeavyLargeSensitiveWithMultipleQuantity() {
        // Base 30 + 30 (heavy) + 25 (large) + 20 (sensitive) = 105.00
        // qty=3 -> (3-1)*10 = 20.00
        // Total = 125.00
        BigDecimal charge = deliveryChargeService.calculate(WeightClass.HEAVY, SizeClass.LARGE, true, 3);
        assertEquals(new BigDecimal("125.00"), charge);
    }

    @Test
    void testQuantityEdgeCases() {
        // null quantity defaults to 1
        BigDecimal chargeNull = deliveryChargeService.calculate(WeightClass.LIGHT, SizeClass.SMALL, false, null);
        assertEquals(new BigDecimal("30.00"), chargeNull);

        // 0 quantity defaults to 1
        BigDecimal chargeZero = deliveryChargeService.calculate(WeightClass.LIGHT, SizeClass.SMALL, false, 0);
        assertEquals(new BigDecimal("30.00"), chargeZero);

        // negative quantity defaults to 1
        BigDecimal chargeNegative = deliveryChargeService.calculate(WeightClass.LIGHT, SizeClass.SMALL, false, -5);
        assertEquals(new BigDecimal("30.00"), chargeNegative);
    }

    @Test
    void testCalculateCatalogChargeWithProductEntity() {
        Product product = Product.builder()
                .name("RGB Gaming Keyboard")
                .category(ProductCategory.ELECTRONICS)
                .estimatedPrice(new BigDecimal("1950.00"))
                .weightClass(WeightClass.MEDIUM)
                .sizeClass(SizeClass.MEDIUM)
                .isSensitive(true)
                .isActive(true)
                .build();

        // 30 + 15 + 10 + 20 = 75.00
        BigDecimal singleQtyCharge = deliveryChargeService.calculateCatalogCharge(product, 1);
        assertEquals(new BigDecimal("75.00"), singleQtyCharge);

        // Qty 4 -> 75.00 + (3 * 10) = 105.00
        BigDecimal fourQtyCharge = deliveryChargeService.calculateCatalogCharge(product, 4);
        assertEquals(new BigDecimal("105.00"), fourQtyCharge);
    }

    @Test
    void testCalculateCatalogChargeWithNullProductFallsBackToBase() {
        BigDecimal charge = deliveryChargeService.calculateCatalogCharge(null, 2);
        assertEquals(new BigDecimal("30.00"), charge);
    }
}
