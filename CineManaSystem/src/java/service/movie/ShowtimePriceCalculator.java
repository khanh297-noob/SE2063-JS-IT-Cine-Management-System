package service.movie;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;

public final class ShowtimePriceCalculator {

    private ShowtimePriceCalculator() {
    }

    /** Ngày áp dụng giá cuối tuần: Thứ 6, Thứ 7, Chủ nhật. Còn lại (Thứ 2 - Thứ 5) là ngày thường. */
    public static boolean isWeekend(LocalDate date) {
        DayOfWeek d = date.getDayOfWeek();
        return d == DayOfWeek.FRIDAY || d == DayOfWeek.SATURDAY || d == DayOfWeek.SUNDAY;
    }

    /** Giá cơ bản theo loại ngày: cuối tuần dùng giá cuối tuần, ngày thường dùng giá ngày thường. */
    public static BigDecimal dayBasePrice(LocalDate date, BigDecimal weekdayPrice, BigDecimal weekendPrice) {
        return date != null && isWeekend(date) ? nz(weekendPrice) : nz(weekdayPrice);
    }

    /**
     * Giá lưu ở Showtime.BasePrice khi tạo suất chiếu:
     * giá cơ bản theo ngày + phụ thu loại phòng.
     */
    public static BigDecimal basePrice(BigDecimal dayBasePrice, BigDecimal roomSurcharge) {
        return nz(dayBasePrice).add(nz(roomSurcharge));
    }

    /** @deprecated Dùng {@link #basePrice(BigDecimal, BigDecimal)} — phụ thu phim đã bỏ. */
    @Deprecated
    public static BigDecimal basePrice(BigDecimal dayBasePrice, BigDecimal movieSurcharge, BigDecimal roomSurcharge) {
        return basePrice(dayBasePrice, roomSurcharge);
    }

    public static BigDecimal ticketPrice(BigDecimal showtimeBasePrice, BigDecimal seatSurcharge) {
        return nz(showtimeBasePrice).add(nz(seatSurcharge));
    }

    private static BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}
