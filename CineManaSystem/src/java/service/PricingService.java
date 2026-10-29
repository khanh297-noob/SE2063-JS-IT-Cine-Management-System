package service;

import dao.PricingDao;
import dao.PricingDao.MenuItem;
import dao.PricingDao.ShowtimePrice;
import dao.PricingDao.Surcharge;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import service.movie.ShowtimePriceCalculator;
import util.MoneyUtil;

/**
 * Nghiệp vụ quản lý giá vé và giá F&B.
 *
 * Quy ước: dữ liệu sai thì ném IllegalArgumentException kèm thông báo tiếng Việt,
 * servlet bắt lỗi và hiển thị cho người dùng. Thao tác ngoài quyền thì ném SecurityException.
 *
 * Mỗi service giữ một kết nối database, nên servlet tạo một service cho mỗi request và gọi close() khi xong.
 */
public class PricingService implements AutoCloseable {

    /** Các trạng thái hợp lệ của một món trong thực đơn chi nhánh (cột Branch_F_B.Status). */
    public static final List<String> MENU_STATUSES = Arrays.asList("Available", "SoldOut", "Inactive");

    private final PricingDao dao = new PricingDao();

    @Override
    public void close() {
        dao.close();
    }

    // ==================== GIÁ VÉ: SUẤT CHIẾU ====================

    /**
     * Suất chiếu sắp tới trong phạm vi chi nhánh và bộ lọc, kèm giá gốc và giá hiện tại.
     * Giá gốc không lưu trong database mà tính lại bằng đúng công thức lúc tạo suất
     * (giá chuẩn + phụ thu loại phòng + phụ thu cuối tuần), nên không ai sửa được.
     * Giá hiện tại là cột Showtime.BasePrice, quản lý được đổi.
     */
    public List<ShowtimePrice> listShowtimePrices(List<Integer> branchScope, Integer movieId,
            LocalDate from, LocalDate to) {
        List<ShowtimePrice> rows = dao.findShowtimePrices(branchScope, movieId, from.toString(), to.toString());
        for (ShowtimePrice row : rows) {
            row.setOriginalPrice(ShowtimePriceCalculator.basePrice(row.getRoomSurcharge(), row.getShowDate().toLocalDate()));
        }
        return rows;
    }

    /** Đổi giá hiện tại của một suất chiếu. Giá gốc không đổi vì được tính lại, không lưu. */
    public void updateShowtimePrice(List<Integer> branchScope, int showtimeId, String priceText)
            throws IllegalArgumentException {
        BigDecimal price = MoneyUtil.parse(priceText, "Giá vé", false);
        if (dao.updateShowtimePrice(showtimeId, price, branchScope) == 0) {
            throw new IllegalArgumentException("Không đổi được giá: suất chiếu đã bắt đầu, đã đóng hoặc không thuộc chi nhánh của bạn!");
        }
    }

    /** Đổi giá hiện tại cho tất cả suất chiếu sắp tới khớp bộ lọc. Trả về số suất được cập nhật. */
    public int updateShowtimePricesInBulk(List<Integer> branchScope, Integer movieId,
            LocalDate from, LocalDate to, String priceText) throws IllegalArgumentException {
        if (from == null || to == null) {
            throw new IllegalArgumentException("Vui lòng chọn khoảng ngày chiếu trước khi đổi giá hàng loạt!");
        }
        BigDecimal price = MoneyUtil.parse(priceText, "Giá vé", false);
        int updated = dao.updateShowtimePrices(branchScope, movieId, from.toString(), to.toString(), price);
        if (updated == 0) {
            throw new IllegalArgumentException("Không có suất chiếu nào phù hợp để đổi giá!");
        }
        return updated;
    }

    // ==================== GIÁ VÉ: PHỤ THU (CHỈ ADMIN SỬA) ====================

    public List<Surcharge> listRoomTypes() {
        return dao.findRoomTypes();
    }

    public List<Surcharge> listSeatTypes() {
        return dao.findSeatTypes();
    }

    /** Đổi phụ thu loại phòng. Phụ thu áp dụng cho mọi chi nhánh nên chỉ Admin được sửa. */
    public void updateRoomTypeSurcharge(boolean isAdmin, int roomTypeId, String text) throws IllegalArgumentException {
        requireAdmin(isAdmin);
        BigDecimal surcharge = MoneyUtil.parse(text, "Phụ thu", true);
        if (dao.updateRoomTypeSurcharge(roomTypeId, surcharge) == 0) {
            throw new IllegalArgumentException("Không tìm thấy loại phòng này!");
        }
    }

    /**
     * Quản lý chi nhánh chỉnh phụ thu loại phòng cho riêng chi nhánh mình: cộng (hoặc trừ, nhập số âm) một khoản
     * vào giá hiện tại các suất sắp chiếu của loại phòng đó tại chi nhánh. Không đổi bảng RoomType nên không ảnh
     * hưởng chi nhánh khác. Admin đổi phụ thu chung cho cả chuỗi bằng updateRoomTypeSurcharge.
     *
     * @return số suất chiếu được điều chỉnh
     */
    public int adjustRoomTypePriceForBranch(List<Integer> branchScope, boolean isAdmin, int roomTypeId,
            String deltaText) throws IllegalArgumentException {
        if (isAdmin) {
            throw new IllegalArgumentException("Admin hãy dùng ô phụ thu chung để đổi cho cả chuỗi!");
        }
        if (deltaText == null || !deltaText.trim().replace(" ", "").matches("[+-]?\\d{1,8}")) {
            throw new IllegalArgumentException("Khoản điều chỉnh phải là số nguyên (đồng), ví dụ 10000 hoặc -5000!");
        }
        BigDecimal delta = new BigDecimal(deltaText.trim().replace(" ", "").replace("+", ""));
        if (delta.signum() == 0) {
            throw new IllegalArgumentException("Khoản điều chỉnh phải khác 0!");
        }
        if (delta.abs().compareTo(MoneyUtil.MAX_AMOUNT) > 0) {
            throw new IllegalArgumentException("Khoản điều chỉnh không được vượt quá " + MoneyUtil.MAX_AMOUNT.toPlainString() + " ₫!");
        }
        int updated = dao.adjustShowtimePricesByRoomType(branchScope, roomTypeId, delta, MoneyUtil.MAX_AMOUNT);
        if (updated == 0) {
            throw new IllegalArgumentException("Không có suất chiếu sắp tới nào của loại phòng này tại chi nhánh bạn để điều chỉnh!");
        }
        return updated;
    }

    /** Đổi phụ thu loại ghế. Chỉ Admin được sửa. */
    public void updateSeatTypeSurcharge(boolean isAdmin, int seatTypeId, String text) throws IllegalArgumentException {
        requireAdmin(isAdmin);
        BigDecimal surcharge = MoneyUtil.parse(text, "Phụ thu", true);
        if (dao.updateSeatTypeSurcharge(seatTypeId, surcharge) == 0) {
            throw new IllegalArgumentException("Không tìm thấy loại ghế này!");
        }
    }

    // ==================== GIÁ F&B ====================

    /** Toàn bộ món của chuỗi kèm giá và trạng thái tại chi nhánh. */
    public List<MenuItem> listBranchMenu(int branchId) {
        return dao.findBranchMenu(branchId);
    }

    /**
     * Lưu giá và trạng thái của một món tại chi nhánh.
     *
     * @param priceText giá riêng của chi nhánh; để trống nghĩa là dùng giá gốc
     */
    public void saveBranchItem(int branchId, int itemId, String priceText, String status)
            throws IllegalArgumentException {
        // Bước 1: giá riêng (trống = dùng giá gốc)
        BigDecimal price = null;
        if (priceText != null && !priceText.trim().isEmpty()) {
            price = MoneyUtil.parse(priceText, "Giá bán", false);
        }
        // Bước 2: trạng thái
        if (status == null || !MENU_STATUSES.contains(status)) {
            throw new IllegalArgumentException("Trạng thái món không hợp lệ!");
        }
        // Bước 3: lưu
        if (!dao.saveBranchItem(branchId, itemId, price, status)) {
            throw new IllegalArgumentException("Không lưu được: món hoặc chi nhánh không tồn tại!");
        }
    }

    /** Đổi giá gốc của một món. Giá gốc áp dụng cho cả chuỗi nên chỉ Admin được sửa. */
    public void updateItemBasePrice(boolean isAdmin, int itemId, String priceText) throws IllegalArgumentException {
        requireAdmin(isAdmin);
        BigDecimal price = MoneyUtil.parse(priceText, "Giá gốc", false);
        if (dao.updateItemBasePrice(itemId, price) == 0) {
            throw new IllegalArgumentException("Không tìm thấy món này!");
        }
    }

    private void requireAdmin(boolean isAdmin) {
        if (!isAdmin) {
            throw new SecurityException("Chỉ Admin được sửa mục này");
        }
    }
}
