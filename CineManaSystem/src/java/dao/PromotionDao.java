package dao;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Lấy dữ liệu của một mã khuyến mãi để kiểm tra có áp dụng được cho đơn hàng không.
 * Bảng: Promotion, PromotionBranch (mã theo chi nhánh), Booking (đếm số lần đã dùng).
 */
public class PromotionDao extends DBContext {

    /**
     * Tìm mã khuyến mãi theo mã (không phân biệt hoa thường theo collation mặc định của SQL Server),
     * kèm 3 số liệu để kiểm tra: mã có giới hạn chi nhánh không, có áp dụng cho chi nhánh này không,
     * và đã dùng bao nhiêu lần. Không có mã thì trả về null.
     */
    public PromotionData findByCode(String code, int branchId) {
        String sql = "SELECT p.PromoID, p.PromoCode, p.DiscountAmount, p.MinSpend, p.StartDate, p.EndDate, p.UsageLimit, "
                + "(SELECT COUNT(*) FROM PromotionBranch pb WHERE pb.PromoID = p.PromoID) AS BranchRules, "
                + "(SELECT COUNT(*) FROM PromotionBranch pb WHERE pb.PromoID = p.PromoID AND pb.BranchID = ?) AS BranchMatch, "
                + "(SELECT COUNT(*) FROM Booking bk WHERE bk.PromoID = p.PromoID AND bk.Status <> 'Cancelled') AS UsedCount "
                + "FROM Promotion p WHERE p.PromoCode = ?";
        try (PreparedStatement st = connection.prepareStatement(sql)) {
            st.setInt(1, branchId);
            st.setString(2, code);
            try (ResultSet rs = st.executeQuery()) {
                if (rs.next()) {
                    PromotionData data = new PromotionData();
                    data.promoId = rs.getInt("PromoID");
                    data.code = rs.getString("PromoCode");
                    data.discountAmount = rs.getBigDecimal("DiscountAmount");
                    data.minSpend = rs.getBigDecimal("MinSpend");
                    data.startDate = rs.getDate("StartDate");
                    data.endDate = rs.getDate("EndDate");
                    data.usageLimit = rs.getInt("UsageLimit");
                    data.branchRules = rs.getInt("BranchRules");
                    data.branchMatch = rs.getInt("BranchMatch");
                    data.usedCount = rs.getInt("UsedCount");
                    return data;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Danh sách khuyến mãi để hiển thị.
     *   - branchId có giá trị: các mã dùng được ở chi nhánh đó (mã toàn chuỗi và mã gán riêng cho chi nhánh).
     *   - branchId = null    : tất cả mã của mọi chi nhánh.
     * Mã không có dòng nào trong PromotionBranch là mã toàn chuỗi.
     */
    public List<PromotionItem> findForBranch(Integer branchId) {
        List<PromotionItem> list = new ArrayList<>();
        Map<Integer, List<String>> branchNames = findBranchNamesByPromo();
        String sql = "SELECT p.PromoID, p.PromoCode, p.DiscountAmount, p.MinSpend, p.StartDate, p.EndDate, p.UsageLimit, "
                + "(SELECT COUNT(*) FROM Booking bk WHERE bk.PromoID = p.PromoID AND bk.Status <> 'Cancelled') AS UsedCount "
                + "FROM Promotion p "
                + (branchId == null ? "" : "WHERE NOT EXISTS (SELECT 1 FROM PromotionBranch pb WHERE pb.PromoID = p.PromoID) "
                        + "OR EXISTS (SELECT 1 FROM PromotionBranch pb WHERE pb.PromoID = p.PromoID AND pb.BranchID = ?) ")
                + "ORDER BY p.EndDate, p.PromoCode";
        try (PreparedStatement st = connection.prepareStatement(sql)) {
            if (branchId != null) {
                st.setInt(1, branchId);
            }
            try (ResultSet rs = st.executeQuery()) {
                while (rs.next()) {
                    PromotionItem item = new PromotionItem();
                    item.promoId = rs.getInt("PromoID");
                    item.code = rs.getString("PromoCode");
                    item.discountAmount = rs.getBigDecimal("DiscountAmount");
                    item.minSpend = rs.getBigDecimal("MinSpend");
                    item.startDate = rs.getDate("StartDate");
                    item.endDate = rs.getDate("EndDate");
                    item.usageLimit = rs.getInt("UsageLimit");
                    item.usedCount = rs.getInt("UsedCount");
                    List<String> names = branchNames.get(item.promoId);
                    item.branchNames = names == null ? new ArrayList<String>() : names;
                    list.add(item);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    /** Tên các chi nhánh được gán cho từng mã (mã toàn chuỗi không có trong kết quả). */
    private Map<Integer, List<String>> findBranchNamesByPromo() {
        Map<Integer, List<String>> map = new HashMap<>();
        String sql = "SELECT pb.PromoID, b.Name FROM PromotionBranch pb "
                + "INNER JOIN Branch b ON b.BranchID = pb.BranchID ORDER BY b.Name";
        try (PreparedStatement st = connection.prepareStatement(sql); ResultSet rs = st.executeQuery()) {
            while (rs.next()) {
                map.computeIfAbsent(rs.getInt("PromoID"), k -> new ArrayList<String>()).add(rs.getString("Name"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return map;
    }

    /** Đóng kết nối (dùng khi DAO chỉ tạo cho một request). */
    public void close() {
        try {
            if (connection != null) {
                connection.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /** Một mã khuyến mãi trong danh sách hiển thị. */
    public static class PromotionItem {
        int promoId;
        String code;
        BigDecimal discountAmount;
        BigDecimal minSpend;
        Date startDate;
        Date endDate;
        int usageLimit;
        int usedCount;
        List<String> branchNames = new ArrayList<>();

        public int getPromoId() { return promoId; }
        public String getCode() { return code; }
        public BigDecimal getDiscountAmount() { return discountAmount; }
        public BigDecimal getMinSpend() { return minSpend; }
        public Date getStartDate() { return startDate; }
        public Date getEndDate() { return endDate; }
        /** Số lượt tối đa, 0 = không giới hạn. */
        public int getUsageLimit() { return usageLimit; }
        /** Số đơn (chưa hủy) đã dùng mã này. */
        public int getUsedCount() { return usedCount; }
        /** Tên các chi nhánh được gán cho mã; rỗng nếu là mã toàn chuỗi. */
        public List<String> getBranchNames() { return branchNames; }
        public boolean isChainWide() { return branchNames.isEmpty(); }
    }

    /** Dữ liệu của một mã khuyến mãi dùng để kiểm tra. */
    public static class PromotionData {
        int promoId;
        String code;
        BigDecimal discountAmount;
        BigDecimal minSpend;
        Date startDate;
        Date endDate;
        int usageLimit;
        int branchRules;
        int branchMatch;
        int usedCount;

        public int getPromoId() { return promoId; }
        public String getCode() { return code; }
        public BigDecimal getDiscountAmount() { return discountAmount; }
        public BigDecimal getMinSpend() { return minSpend; }
        public Date getStartDate() { return startDate; }
        public Date getEndDate() { return endDate; }
        /** Số lượt tối đa, 0 = không giới hạn. */
        public int getUsageLimit() { return usageLimit; }
        /** Số dòng giới hạn chi nhánh của mã (0 = áp dụng toàn chuỗi). */
        public int getBranchRules() { return branchRules; }
        /** 1 nếu mã được gán cho chi nhánh đang kiểm tra, 0 nếu không. */
        public int getBranchMatch() { return branchMatch; }
        /** Số đơn (chưa hủy) đã dùng mã này. */
        public int getUsedCount() { return usedCount; }
    }
}
