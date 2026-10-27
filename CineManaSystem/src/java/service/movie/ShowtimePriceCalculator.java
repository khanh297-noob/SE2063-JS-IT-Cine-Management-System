package service.movie;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;

public final class ShowtimePriceCalculator {

    public static final BigDecimal STANDARD_PRICE = new BigDecimal("75000");
    public static final BigDecimal WEEKEND_SURCHARGE = new BigDecimal("20000");

    private ShowtimePriceCalculator() {
    }

    public static boolean isWeekend(LocalDate date) {
        DayOfWeek d = date.getDayOfWeek();
        return d == DayOfWeek.SATURDAY || d == DayOfWeek.SUNDAY;
    }

    public static BigDecimal basePrice(BigDecimal roomSurcharge, LocalDate date) {
        BigDecimal price = STANDARD_PRICE.add(nz(roomSurcharge));
        if (date != null && isWeekend(date)) {
            price = price.add(WEEKEND_SURCHARGE);
        }
        return price;
    }

    public static BigDecimal ticketPrice(BigDecimal showtimeBasePrice, BigDecimal seatSurcharge) {
        return nz(showtimeBasePrice).add(nz(seatSurcharge));
    }

    private static BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}
