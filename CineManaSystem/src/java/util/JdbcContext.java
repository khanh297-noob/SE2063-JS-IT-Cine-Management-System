package util;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Quản lý kết nối JDBC cho module Chi nhánh & Phim (dao.branch, dao.movie, service.branch, service.movie).
 * Khác dao.DBContext (module Auth / Báo cáo: mỗi DAO tự mở kết nối khi khởi tạo), ở đây
 * DAO không tự mở kết nối mà nhận Connection từ Service, nhờ vậy một nghiệp vụ
 * dùng nhiều DAO vẫn nằm trong cùng một transaction.
 *
 * Thông tin kết nối đọc theo thứ tự, file sau ghi đè file trước:
 *   1. db.properties        (src/java, KHÔNG có trên git: mỗi máy tự tạo với db.url / db.user / db.password)
 *   2. db.local.properties  (src/java, tuỳ chọn, ghi đè db.properties)
 * Không sửa user/password trong file Java này, mỗi người sửa sẽ bị conflict khi merge.
 */
public final class JdbcContext {

    private static final Properties CONFIG = loadConfig();
    private static final String URL = CONFIG.getProperty("db.url");
    private static final String USER = CONFIG.getProperty("db.user");
    private static final String PASSWORD = CONFIG.getProperty("db.password");

    static {
        try {
            // Driver nằm trong WEB-INF/lib: Tomcat không tự nạp qua DriverManager nên phải nạp tay.
            Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
        } catch (ClassNotFoundException e) {
            throw new ExceptionInInitializerError("Thiếu mssql-jdbc trong WEB-INF/lib");
        }
    }

    private static Properties loadConfig() {
        Properties p = new Properties();
        for (String file : new String[] {"db.properties", "db.local.properties"}) {
            try (InputStream in = JdbcContext.class.getClassLoader().getResourceAsStream(file)) {
                if (in != null) p.load(in);
            } catch (IOException e) {
                throw new ExceptionInInitializerError("Không đọc được " + file + ": " + e.getMessage());
            }
        }
        if (p.getProperty("db.url") == null) {
            throw new ExceptionInInitializerError("Thiếu db.url trong src/java/db.properties");
        }
        return p;
    }

    /** Một đơn vị công việc dùng chung Connection. */
    @FunctionalInterface
    public interface Work<T> {
        T run(Connection con) throws SQLException, BusinessException;
    }

    private JdbcContext() {
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
