package service.booking;

import model.branch.SeatType;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.ToIntFunction;

/**
 * Ghế đôi = 2 ô ghế liền nhau trong cùng một hàng (mỗi ô là 1 bản ghi Seat, 1 vé).
 * Ghép cặp từ trái sang phải trong mỗi dãy ghế đôi liền nhau: cột 1-2, 3-4...
 * giống quy tắc BR-SM-06 của sơ đồ ghế. Hai ô của một cặp luôn được bán / giữ cùng nhau.
 */
public final class CoupleSeats {

    private CoupleSeats() {}

    public static boolean isCouple(String seatTypeName) {
        return SeatType.isCoupleName(seatTypeName);
    }

    /**
     * @return map seatId -> seatId của ô còn lại trong cặp (cả hai chiều). Ô ghế đôi lẻ không có cặp thì không có trong map.
     */
    public static <T> Map<Integer, Integer> pairs(List<T> seats, ToIntFunction<T> id, Function<T, String> row,
            ToIntFunction<T> col, Function<T, String> type) {
        List<T> sorted = new ArrayList<>(seats);
        sorted.sort(Comparator.comparing((T s) -> String.valueOf(row.apply(s))).thenComparingInt(col));
        Map<Integer, Integer> result = new HashMap<>();
        T left = null;
        for (T s : sorted) {
            if (!isCouple(type.apply(s))) {
                left = null;
                continue;
            }
            if (left != null && String.valueOf(row.apply(left)).equals(String.valueOf(row.apply(s)))
                    && col.applyAsInt(left) + 1 == col.applyAsInt(s)) {
                result.put(id.applyAsInt(left), id.applyAsInt(s));
                result.put(id.applyAsInt(s), id.applyAsInt(left));
                left = null;
            } else {
                left = s;
            }
        }
        return result;
    }
}
