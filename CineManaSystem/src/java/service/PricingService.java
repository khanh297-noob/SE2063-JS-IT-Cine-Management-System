package service;

import dao.PricingConfigDao;
import dao.PricingDao;
import dao.PricingDao.ConfigInput;
import dao.PricingDao.MenuItem;
import dao.PricingDao.ShowtimePrice;
import dao.PricingDao.RoomTypePriceBoard;
import dao.PricingDao.Surcharge;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import model.User;
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

    // ==================== BẢNG GIÁ VÉ CÔNG KHAI (TRANG CHỦ) ====================

    /** Số ngày tới được tính vào bảng giá ở trang chủ và số suất tối đa đọc từ DB. */
    public static final int PUBLIC_DAYS = 30;
    public static final int PUBLIC_MAX_ROWS = 3000;

    /**
     * Bảng giá vé cho khách xem ở trang chủ: mỗi chi nhánh một bảng cho từng loại phòng (2D, 3D, ...).
     * Giá lấy từ cấu hình hiện tại của chi nhánh (giá cơ bản theo ngày + phụ thu loại phòng),
     * mỗi ô chỉ hiện một mức giá, không gom khoảng từ nhiều suất chiếu.
     */
    public List<RoomTypePriceBoard> listPublicPriceBoards() {
        return dao.findPriceBoards(PUBLIC_DAYS, PUBLIC_MAX_ROWS);
    }

    // ==================== CẤU HÌNH GIÁ HỆ THỐNG (ADMIN) ====================

    /** Giá cơ bản theo loại ngày: ngày thường (Thứ 2 - Thứ 5) và cuối tuần (Thứ 6, Thứ 7, Chủ nhật). */
    public PricingConfigDao.DayPrices getDayBasePrices() {
        return dao.findDayBasePrices();
    }

    /** Giá cơ bản theo loại ngày của một chi nhánh (nếu có giá riêng, không thì lấy giá chung). */
    public PricingConfigDao.DayPrices getDayBasePricesForBranch(int branchId) {
        return dao.findDayBasePricesForBranch(branchId);
    }

    /** Quản lý chi nhánh đổi giá cơ bản theo ngày cho chi nhánh mình. */
    public int saveDayBasePricesForBranch(User user, boolean isAdmin, Integer branchId, String weekdayText, String weekendText) {
        int branch = (!isAdmin && user != null && user.getBranchID() != null) ? user.getBranchID() : (branchId != null ? branchId : 0);
        if (branch <= 0) {
            throw new IllegalArgumentException("Vui lòng chọn chi nhánh cần đổi giá!");
        }
        BigDecimal weekday = MoneyUtil.parse(weekdayText, "Giá ngày thường", false);
        BigDecimal weekend = MoneyUtil.parse(weekendText, "Giá cuối tuần", false);
        return dao.saveDayBasePricesForBranch(branch, weekday, weekend);
    }


    /**
     * Lưu cấu hình giá hệ thống (cả cấu hình hoặc chỉ một mục, phần không gửi lên thì giữ nguyên). Chỉ Admin.
     * Kiểm tra toàn bộ dữ liệu trước, có một giá trị sai thì không lưu gì.
     * Giá của các suất sắp chiếu bị ảnh hưởng được cộng phần chênh lệch (xem PricingDao.saveConfig).
     *
     * @param roomTexts   id loại phòng -> phụ thu nhập vào
     * @param seatTexts   id loại ghế -> phụ thu nhập vào
     * @return số lượt cập nhật giá suất chiếu sắp tới
     */
    public int saveConfig(boolean isAdmin, String weekdayText, String weekendText,
            Map<Integer, String> roomTexts, Map<Integer, String> seatTexts) throws IllegalArgumentException {
        requireAdmin(isAdmin);
        ConfigInput in = new ConfigInput();
        // Mỗi mục của trang được lưu riêng nên chỉ phần có gửi lên mới được xử lý; hai giá cơ bản theo ngày đi cùng nhau
        if (weekdayText != null || weekendText != null) {
            in.weekdayPrice = MoneyUtil.parse(weekdayText, "Giá cơ bản ngày thường", false);
            in.weekendPrice = MoneyUtil.parse(weekendText, "Giá cơ bản cuối tuần", false);
        }
        for (Map.Entry<Integer, String> e : roomTexts.entrySet()) {
            in.roomSurcharges.put(e.getKey(), MoneyUtil.parse(e.getValue(), "Phụ thu loại phòng", true));
        }
        for (Map.Entry<Integer, String> e : seatTexts.entrySet()) {
            in.seatSurcharges.put(e.getKey(), MoneyUtil.parse(e.getValue(), "Phụ thu loại ghế", true));
        }
        return dao.saveConfig(in);
    }

    /**
     * Admin lưu phụ thu loại ghế / loại phòng RIÊNG cho một chi nhánh: chỉ chi nhánh đó đổi, phụ thu chung
     * và các chi nhánh khác giữ nguyên. Kiểm tra toàn bộ dữ liệu trước, có giá trị sai thì không lưu gì.
     * Giá trị bằng phụ thu chung nghĩa là chi nhánh dùng lại phụ thu chung (bỏ phụ thu riêng).
     *
     * @return số suất chiếu sắp tới được cập nhật giá (do đổi phụ thu loại phòng)
     */
    public int saveBranchConfig(boolean isAdmin, int branchId, int userId, String ip,
            Map<Integer, String> roomTexts, Map<Integer, String> seatTexts) throws IllegalArgumentException {
        requireAdmin(isAdmin);
        if (branchId <= 0) {
            throw new IllegalArgumentException("Vui lòng chọn chi nhánh cần đổi giá!");
        }
        // Bước 1: đọc và kiểm tra hết dữ liệu nhập
        Map<Integer, BigDecimal> rooms = new java.util.LinkedHashMap<>();
        for (Map.Entry<Integer, String> e : roomTexts.entrySet()) {
            rooms.put(e.getKey(), MoneyUtil.parse(e.getValue(), "Phụ thu loại phòng", true));
        }
        Map<Integer, BigDecimal> seats = new java.util.LinkedHashMap<>();
        for (Map.Entry<Integer, String> e : seatTexts.entrySet()) {
            seats.put(e.getKey(), MoneyUtil.parse(e.getValue(), "Phụ thu loại ghế", true));
        }
        // Bước 2: phụ thu loại ghế riêng của chi nhánh
        for (Surcharge t : dao.findSeatTypesForBranch(branchId)) {
            BigDecimal target = seats.get(t.getId());
            if (target == null || target.compareTo(t.getEffectiveSurcharge()) == 0) {
                continue;
            }
            boolean backToCommon = target.compareTo(t.getSurcharge()) == 0;
            boolean ok = backToCommon
                    ? dao.resetBranchSeatTypeSurcharge(userId, ip, branchId, t.getId(), t.getName())
                    : dao.saveBranchSeatTypeSurcharge(userId, ip, branchId, t.getId(), t.getName(), target);
            if (!ok) {
                throw new IllegalArgumentException("Không lưu được phụ thu loại ghế " + t.getName() + ", vui lòng thử lại!");
            }
        }
        // Bước 3: phụ thu loại phòng riêng của chi nhánh (cộng chênh lệch vào các suất sắp chiếu của chi nhánh)
        return rooms.isEmpty() ? 0 : dao.saveBranchRoomSurcharges(branchId, rooms);
    }

    /** Loại phòng kèm phụ thu riêng của một chi nhánh (branchSurcharge null = dùng phụ thu chung). */
    public List<Surcharge> listRoomTypesForBranch(int branchId) {
        return dao.findRoomTypesForBranch(branchId);
    }

    // ==================== GIÁ VÉ: SUẤT CHIẾU ====================

    /**
     * Suất chiếu sắp tới trong phạm vi chi nhánh và bộ lọc, kèm giá gốc và giá hiện tại.
     * Giá gốc không lưu trong database mà tính lại bằng đúng công thức lúc tạo suất:
     * giá cơ bản theo ngày + phụ thu loại phòng, theo cấu hình hiện hành.
     * Giá cơ bản = Showtime.BasePrice trừ phụ thu loại phòng, quản lý được đổi. Khi lưu, BasePrice = giá cơ bản + phụ thu loại phòng.
     */
    public List<ShowtimePrice> listShowtimePrices(List<Integer> branchScope, Integer movieId,
            LocalDate from, LocalDate to) {
        List<ShowtimePrice> rows = dao.findShowtimePrices(branchScope, movieId, from.toString(), to.toString());
        Map<Integer, PricingConfigDao.DayPrices> branchDayPricesCache = new java.util.HashMap<>();
        for (ShowtimePrice row : rows) {
            LocalDate date = row.getShowDate().toLocalDate();
            PricingConfigDao.DayPrices dayPrices = branchDayPricesCache.computeIfAbsent(row.getBranchId(),
                    id -> dao.findDayBasePricesForBranch(id));
            row.setOriginalPrice(ShowtimePriceCalculator.basePrice(
                    ShowtimePriceCalculator.dayBasePrice(date, dayPrices.getWeekday(), dayPrices.getWeekend()),
                    row.getRoomSurcharge()));
            // Giá cơ bản = BasePrice đã lưu trừ phụ thu loại phòng (không âm)
            BigDecimal base = row.getCurrentPrice().subtract(row.getRoomSurcharge());
            row.setBasePrice(base.signum() < 0 ? BigDecimal.ZERO : base);
        }
        return rows;
    }

    /** Đổi giá cơ bản của một suất chiếu (lưu BasePrice = giá cơ bản + phụ thu loại phòng). Giá gốc không đổi vì được tính lại, không lưu. */
    public void updateShowtimePrice(List<Integer> branchScope, int showtimeId, String priceText)
            throws IllegalArgumentException {
        BigDecimal price = MoneyUtil.parse(priceText, "Giá cơ bản", false);
        if (dao.updateShowtimePrice(showtimeId, price, branchScope) == 0) {
            throw new IllegalArgumentException("Không đổi được giá: suất chiếu đã bắt đầu, đã đóng hoặc không thuộc chi nhánh của bạn!");
        }
    }

    /** Đổi giá cơ bản cho tất cả suất chiếu sắp tới khớp bộ lọc (mỗi suất lưu giá cơ bản + phụ thu phòng của nó). Trả về số suất được cập nhật. */
    public int updateShowtimePricesInBulk(List<Integer> branchScope, Integer movieId,
            LocalDate from, LocalDate to, String priceText) throws IllegalArgumentException {
        if (from == null || to == null) {
            throw new IllegalArgumentException("Vui lòng chọn khoảng ngày chiếu trước khi đổi giá hàng loạt!");
        }
        BigDecimal price = MoneyUtil.parse(priceText, "Giá cơ bản", false);
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

    /**
     * Đổi phụ thu loại phòng. Phụ thu áp dụng cho mọi chi nhánh nên chỉ Admin được sửa.
     * Giá cơ bản của các suất sắp chiếu giữ nguyên, giá bán (BasePrice) đổi theo phụ thu mới.
     */
    public void updateRoomTypeSurcharge(boolean isAdmin, int roomTypeId, String text) throws IllegalArgumentException {
        requireAdmin(isAdmin);
        BigDecimal surcharge = MoneyUtil.parse(text, "Phụ thu", true);
        if (dao.updateRoomTypeSurchargeKeepingBasePrice(roomTypeId, surcharge) == 0) {
            throw new IllegalArgumentException("Không tìm thấy loại phòng này!");
        }
    }

    /**
     * Quản lý chi nhánh chỉnh phụ thu loại phòng cho riêng chi nhánh mình: cộng (hoặc trừ, nhập số âm) một khoản
     * vào giá (BasePrice) các suất sắp chiếu của loại phòng đó tại chi nhánh. Không đổi bảng RoomType nên không ảnh
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

    /**
     * Quản lý chi nhánh đặt phụ thu định dạng phòng (số cố định) riêng cho chi nhánh mình.
     * Lưu vào PricingSetting, cộng/trừ chênh lệch vào BasePrice các suất sắp chiếu của loại phòng tại chi nhánh.
     * Chi nhánh khác và phụ thu chung RoomType không đổi.
     *
     * @return số suất chiếu được cập nhật giá (0 nếu mức không đổi)
     */
    public int saveRoomTypeSurchargeForBranch(boolean isAdmin, Integer branchId, int roomTypeId, String text)
            throws IllegalArgumentException {
        int branch = requireManagerBranch(isAdmin, branchId);
        BigDecimal surcharge = MoneyUtil.parse(text, "Phụ thu loại phòng", true);
        java.util.Map<Integer, BigDecimal> one = new java.util.LinkedHashMap<>();
        one.put(roomTypeId, surcharge);
        return dao.saveBranchRoomSurcharges(branch, one);
    }

    /**
     * Quản lý chi nhánh bỏ phụ thu phòng riêng, quay về phụ thu chung của Admin.
     * Các suất sắp chiếu được điều chỉnh theo chênh lệch (chung - hiện tại).
     *
     * @return số suất chiếu được cập nhật giá
     */
    public int resetRoomTypeSurchargeForBranch(boolean isAdmin, Integer branchId, int roomTypeId)
            throws IllegalArgumentException {
        int branch = requireManagerBranch(isAdmin, branchId);
        Surcharge found = null;
        for (Surcharge t : dao.findRoomTypesForBranch(branch)) {
            if (t.getId() == roomTypeId) {
                found = t;
                break;
            }
        }
        if (found == null) {
            throw new IllegalArgumentException("Không tìm thấy loại phòng này!");
        }
        if (found.getBranchSurcharge() == null) {
            throw new IllegalArgumentException("Chi nhánh của bạn đang dùng phụ thu chung rồi!");
        }
        java.util.Map<Integer, BigDecimal> one = new java.util.LinkedHashMap<>();
        one.put(roomTypeId, found.getSurcharge()); // bằng phụ thu chung → saveBranchRoomSurcharges sẽ xoá override
        return dao.saveBranchRoomSurcharges(branch, one);
    }

    /** Loại ghế kèm phụ thu riêng của chi nhánh (dành cho quản lý chi nhánh). */
    public List<Surcharge> listSeatTypesForBranch(int branchId) {
        return dao.findSeatTypesForBranch(branchId);
    }

    /**
     * Quản lý chi nhánh đặt phụ thu loại ghế riêng cho chi nhánh mình. Chi nhánh khác và phụ thu chung của Admin
     * không đổi. Admin đổi phụ thu chung cho cả chuỗi bằng updateSeatTypeSurcharge.
     */
    public void saveSeatTypeSurchargeForBranch(boolean isAdmin, Integer branchId, int userId, String ip,
            int seatTypeId, String text) throws IllegalArgumentException {
        int branch = requireManagerBranch(isAdmin, branchId);
        BigDecimal surcharge = MoneyUtil.parse(text, "Phụ thu", true);
        String name = dao.findSeatTypeName(seatTypeId);
        if (name == null) {
            throw new IllegalArgumentException("Không tìm thấy loại ghế này!");
        }
        if (!dao.saveBranchSeatTypeSurcharge(userId, ip, branch, seatTypeId, name, surcharge)) {
            throw new IllegalArgumentException("Không lưu được phụ thu, vui lòng thử lại!");
        }
    }

    /** Quản lý chi nhánh bỏ phụ thu riêng, chi nhánh dùng lại phụ thu chung của Admin. */
    public void resetSeatTypeSurchargeForBranch(boolean isAdmin, Integer branchId, int userId, String ip,
            int seatTypeId) throws IllegalArgumentException {
        int branch = requireManagerBranch(isAdmin, branchId);
        String name = dao.findSeatTypeName(seatTypeId);
        if (name == null) {
            throw new IllegalArgumentException("Không tìm thấy loại ghế này!");
        }
        if (!dao.resetBranchSeatTypeSurcharge(userId, ip, branch, seatTypeId, name)) {
            throw new IllegalArgumentException("Chi nhánh của bạn đang dùng phụ thu chung rồi!");
        }
    }

    private static int requireManagerBranch(boolean isAdmin, Integer branchId) throws IllegalArgumentException {
        if (isAdmin) {
            throw new IllegalArgumentException("Admin hãy dùng ô phụ thu chung để đổi cho cả chuỗi!");
        }
        if (branchId == null) {
            throw new IllegalArgumentException("Tài khoản chưa được gán chi nhánh!");
        }
        return branchId;
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

    /** Danh mục F&B toàn chuỗi (giá gốc) — Admin chỉnh khi chọn Tất cả chi nhánh. */
    public List<MenuItem> listCatalogMenu() {
        return dao.findCatalogItems();
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
