package com.skyhigh.baggage.util;

import java.math.BigDecimal;

/**
 * Constants for baggage service.
 */
public class BaggageConstants {

    public static final BigDecimal MAX_FREE_WEIGHT = new BigDecimal("25.0");
    public static final BigDecimal EXCESS_FEE_PER_KG = new BigDecimal("10.0");
    public static final String BAGGAGE_REFERENCE_PREFIX = "BAG-";

    private BaggageConstants() {
        // Utility class
    }
}
