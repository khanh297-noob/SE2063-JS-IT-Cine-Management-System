package cinemanagement.util;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/** Hàm tiện ích JDBC dùng chung cho các DAO. */
public final class JdbcUtil {

    private static final int SQLSERVER_FK_VIOLATION = 547;
    private static final int SQLSERVER_UNIQUE_CONSTRAINT = 2627;
    private static final int SQLSERVER_UNIQUE_INDEX = 2601;

    private JdbcUtil() {
    }

    public static void setInteger(PreparedStatement ps, int i, Integer v) throws SQLException {
        if (v == null) ps.setNull(i, Types.INTEGER); else ps.setInt(i, v);
    }

    public static void setString(PreparedStatement ps, int i, String v) throws SQLException {
        if (v == null) ps.setNull(i, Types.NVARCHAR); else ps.setString(i, v);
    }

    public static void setDecimal(PreparedStatement ps, int i, BigDecimal v) throws SQLException {
        if (v == null) ps.setNull(i, Types.DECIMAL); else ps.setBigDecimal(i, v);
    }

    public static void setDate(PreparedStatement ps, int i, LocalDate v) throws SQLException {
        if (v == null) ps.setNull(i, Types.DATE); else ps.setDate(i, Date.valueOf(v));
    }

    public static void setTime(PreparedStatement ps, int i, LocalTime v) throws SQLException {
        if (v == null) ps.setNull(i, Types.TIME); else ps.setTime(i, Time.valueOf(v));
    }

    public static Integer getInteger(ResultSet rs, String col) throws SQLException {
        int v = rs.getInt(col);
        return rs.wasNull() ? null : v;
    }

    public static LocalDate getDate(ResultSet rs, String col) throws SQLException {
        Date d = rs.getDate(col);
        return d == null ? null : d.toLocalDate();
    }

    public static LocalTime getTime(ResultSet rs, String col) throws SQLException {
        Time t = rs.getTime(col);
        return t == null ? null : t.toLocalTime();
    }

    public static LocalDateTime getDateTime(ResultSet rs, String col) throws SQLException {
        Timestamp t = rs.getTimestamp(col);
        return t == null ? null : t.toLocalDateTime();
    }

    /** Lấy khoá tự tăng vừa sinh ra sau INSERT. */
    public static int generatedKey(PreparedStatement ps) throws SQLException {
        try (ResultSet rs = ps.getGeneratedKeys()) {
            if (rs.next()) return rs.getInt(1);
        }
        throw new SQLException("Không lấy được khoá vừa tạo");
    }

    public static boolean isForeignKeyViolation(SQLException e) {
        return e.getErrorCode() == SQLSERVER_FK_VIOLATION;
    }

    public static boolean isUniqueViolation(SQLException e) {
        return e.getErrorCode() == SQLSERVER_UNIQUE_CONSTRAINT || e.getErrorCode() == SQLSERVER_UNIQUE_INDEX;
    }
}
