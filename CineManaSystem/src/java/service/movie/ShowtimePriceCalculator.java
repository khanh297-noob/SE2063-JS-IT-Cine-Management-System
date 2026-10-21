package service.movie;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;

/**
 * Giá vé (BR-05) dùng chung cho Quản lý suất chiếu, Đặt vé online và POS bán tại quầy.
 *
 * Giá suất chiếu (Showtime.BasePrice) không nhập tay mà tự tính khi tạo / sửa suất:
 *     BasePrice = giá chuẩn + phụ thu loại phòng (RoomType.Surcharge) + 20.000đ nếu chiếu Thứ 7 / Chủ nhật.
 * Giá 1 vé = BasePrice của suất + phụ thu loại ghế (SeatType.Surcharge).
 * Phụ thu phòng đã nằm trong BasePrice nên không cộng thêm lần nữa khi bán vé.
 */
public final class ShowtimePriceCalculator {

    /** Giá chuẩn của 1 vé ghế thường, phòng 2D, ngày thường. */
    public static final BigDecimal STANDARD_PRICE = new BigDecimal("75000");
    /** Phụ thu cuối tuần (Thứ 7, Chủ nhật). */
    public static final BigDecimal WEEKEND_SURCHARGE = new BigDecimal("20000");

    private ShowtimePriceCalculator() {
    }

    public static boolean isWeekend(LocalDate date) {
        DayOfWeek d = date.getDayOfWeek();
        return d == DayOfWeek.SATURDAY || d == DayOfWeek.SUNDAY;
    }

    /** Giá suất chiếu theo phòng và ngày chiếu. */
    public static BigDecimal basePrice(BigDecimal roomSurcharge, LocalDate date) {
        BigDecimal price = STANDARD_PRICE.add(nz(roomSurcharge));
        if (date != null && isWeekend(date)) {
            price = price.add(WEEKEND_SURCHARGE);
        }
        return price;
    }

    /** Giá 1 vé = giá suất chiếu + phụ thu loại ghế. */
    public static BigDecimal ticketPrice(BigDecimal showtimeBasePrice, BigDecimal seatSurcharge) {
        return nz(showtimeBasePrice).add(nz(seatSurcharge));
    }

    private static BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}
