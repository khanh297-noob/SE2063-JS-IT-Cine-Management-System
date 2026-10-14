package cinemanagement.dao;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class PaymentDAO {

    public void insert(Connection c, int bookingId, BigDecimal amount, String method, String txCode, String status)
            throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("INSERT INTO Payment (BookingID, Amount, PaymentMethod, "
                + "TransactionCode, GatewayStatus) VALUES (?, ?, ?, ?, ?)")) {
            ps.setInt(1, bookingId);
            ps.setBigDecimal(2, amount);
            ps.setString(3, method);
            ps.setString(4, txCode);
            ps.setString(5, status);
            ps.executeUpdate();
        }
    }

    public void markRefunded(Connection c, int bookingId) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "UPDATE Payment SET GatewayStatus = 'Refunded' WHERE BookingID = ? AND GatewayStatus = 'Success'")) {
            ps.setInt(1, bookingId);
            ps.executeUpdate();
        }
    }
}
