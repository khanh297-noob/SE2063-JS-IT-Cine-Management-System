package cinemanagement.service;

import java.math.BigDecimal;

public final class PriceCalculator {
    private PriceCalculator() {
    }

    public static BigDecimal finalPrice(BigDecimal basePrice, BigDecimal seatSurcharge, BigDecimal roomSurcharge) {
        return basePrice.add(nz(seatSurcharge)).add(nz(roomSurcharge));
    }

    private static BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}
