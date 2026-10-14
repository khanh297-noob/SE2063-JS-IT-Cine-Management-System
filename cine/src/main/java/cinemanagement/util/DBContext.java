package cinemanagement.util;


import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Quản lý kết nối JDBC.
 * DAO không tự mở kết nối mà nhận Connection từ Service, nhờ vậy một nghiệp vụ
 * dùng nhiều DAO vẫn nằm trong cùng một transaction.
 */
public final class DBContext {

    // Tên DB có dấu cách nên đặt trong {} theo cú pháp của mssql-jdbc.
    private static final String URL = System.getProperty("db.url",
            "jdbc:sqlserver://localhost:1433;databaseName={CineManagementSystem};"
            + "encrypt=true;trustServerCertificate=true");
    private static final String USER = System.getProperty("db.user", "sa");
    private static final String PASSWORD = System.getProperty("db.password", "123456");

    /** Một đơn vị công việc dùng chung Connection. */
    @FunctionalInterface
    public interface Work<T> {
        T run(Connection con) throws SQLException, BusinessException;
    }

    private DBContext() {
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }

    /** Dùng cho thao tác chỉ đọc. */
    public static <T> T query(Work<T> work) throws SQLException, BusinessException {
        try (Connection con = getConnection()) {
            return work.run(con);
        }
    }

    /** Dùng cho thao tác ghi: commit nếu thành công, rollback nếu có lỗi bất kỳ. */
    public static <T> T transaction(Work<T> work) throws SQLException, BusinessException {
        try (Connection con = getConnection()) {
            con.setAutoCommit(false);
            try {
                T result = work.run(con);
                con.commit();
                return result;
            } catch (SQLException | BusinessException | RuntimeException e) {
                con.rollback();
                throw e;
            } finally {
                con.setAutoCommit(true);
            }
        }
    }
}
