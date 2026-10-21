package util;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Kết nối SQL Server cho phần thanh toán + POS. Các DAO kế thừa class này và gọi getConnection().
 * Mỗi hàm DAO mở 1 connection và tự đóng bằng try-with-resources.
 * Đọc chung file WEB-INF/ConnectDB.properties với dao.DBContext của nhóm (url, userID, password).
 */
public class DBContext {

    protected Connection getConnection() throws SQLException {
        Properties properties = new Properties();
        try (InputStream in = getClass().getClassLoader().getResourceAsStream("../ConnectDB.properties")) {
            if (in == null) {
                throw new SQLException("Không tìm thấy file WEB-INF/ConnectDB.properties");
            }
            properties.load(in);
        } catch (IOException e) {
            throw new SQLException("Không đọc được ConnectDB.properties", e);
        }
        try {
            Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
        } catch (ClassNotFoundException e) {
            throw new SQLException("Thiếu thư viện mssql-jdbc", e);
        }
        return DriverManager.getConnection(properties.getProperty("url"),
                properties.getProperty("userID"), properties.getProperty("password"));
    }
}
