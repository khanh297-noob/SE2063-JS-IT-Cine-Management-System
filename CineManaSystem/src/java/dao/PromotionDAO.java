package dao;

import util.DBContext;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import model.Promotion;

public class PromotionDAO extends DBContext {

    public Promotion getByCode(String code) {
        String sql = "SELECT PromoID, PromoCode, DiscountAmount, MinSpend, StartDate, EndDate, UsageLimit "
                + "FROM Promotion WHERE PromoCode = ?";
        try (Connection con = getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, code);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Promotion p = new Promotion();
                    p.setPromoId(rs.getInt("PromoID"));
                    p.setPromoCode(rs.getString("PromoCode"));
                    p.setDiscountAmount(rs.getBigDecimal("DiscountAmount"));
                    p.setMinSpend(rs.getBigDecimal("MinSpend"));
                    p.setStartDate(rs.getDate("StartDate"));
                    p.setEndDate(rs.getDate("EndDate"));
                    p.setUsageLimit(rs.getInt("UsageLimit"));
                    return p;
                }
            }
        } catch (SQLException e) {
            System.out.println("Lỗi PromotionDAO.getByCode: " + e.getMessage());
        }
        return null;
    }

    // Số lần voucher đã được dùng (đơn bị hủy thì không tính), bỏ qua đơn hiện tại
    public int countUsed(int promoId, int exceptBookingId) {
        String sql = "SELECT COUNT(*) FROM Booking WHERE PromoID = ? AND Status <> 'Cancelled' AND BookingID <> ?";
        try (Connection con = getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, promoId);
            ps.setInt(2, exceptBookingId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            System.out.println("Lỗi countUsed: " + e.getMessage());
        }
        return 0;
    }
}
