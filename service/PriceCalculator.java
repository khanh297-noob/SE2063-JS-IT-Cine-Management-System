package cinemanagement.service;

import java.math.BigDecimal;

import cinemanagement.util.AppConfig;

/** BR-05: FinalPrice = BasePrice + phu thu loai ghe + phu thu loai phong. Phu thu cau hinh trong SystemSetting. */
public final class PriceCalculator {

    private PriceCalculator() {
    }

    public static BigDecimal seatSurcharge(String seatType) {
        if ("VIP".equals(seatType)) {
            return AppConfig.getMoney("SURCHARGE_VIP", 20000);
        }
        if ("Sweetbox".equals(seatType)) {
            return AppConfig.getMoney("SURCHARGE_SWEETBOX", 40000);
        }
        return BigDecimal.ZERO;
    }

    public static BigDecimal roomSurcharge(String roomType) {
        if ("IMAX".equals(roomType)) {
            return AppConfig.getMoney("SURCHARGE_IMAX", 50000);
        }
        if ("3D".equals(roomType)) {
            return AppConfig.getMoney("SURCHARGE_3D", 20000);
        }
        return AppConfig.getMoney("SURCHARGE_2D", 0);
    }

    public static BigDecimal finalPrice(BigDecimal basePrice, String seatType, String roomType) {
        return basePrice.add(seatSurcharge(seatType)).add(roomSurcharge(roomType));
    }
}
