package com.carry.config;

import com.carry.entity.SizeClass;
import com.carry.entity.WeightClass;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

@Configuration
public class DeliveryChargeConfig {

    public static final BigDecimal BASE_CHARGE = new BigDecimal("30.00");
    public static final BigDecimal SENSITIVE_SURCHARGE = new BigDecimal("20.00");
    public static final BigDecimal PER_EXTRA_UNIT = new BigDecimal("10.00");

    public static final Map<WeightClass, BigDecimal> WEIGHT_SURCHARGES;
    public static final Map<SizeClass, BigDecimal> SIZE_SURCHARGES;

    static {
        Map<WeightClass, BigDecimal> weights = new EnumMap<>(WeightClass.class);
        weights.put(WeightClass.LIGHT, new BigDecimal("0.00"));
        weights.put(WeightClass.MEDIUM, new BigDecimal("15.00"));
        weights.put(WeightClass.HEAVY, new BigDecimal("30.00"));
        WEIGHT_SURCHARGES = Collections.unmodifiableMap(weights);

        Map<SizeClass, BigDecimal> sizes = new EnumMap<>(SizeClass.class);
        sizes.put(SizeClass.SMALL, new BigDecimal("0.00"));
        sizes.put(SizeClass.MEDIUM, new BigDecimal("10.00"));
        sizes.put(SizeClass.LARGE, new BigDecimal("25.00"));
        SIZE_SURCHARGES = Collections.unmodifiableMap(sizes);
    }
}
