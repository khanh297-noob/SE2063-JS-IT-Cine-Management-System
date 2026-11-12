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
 * Đọc file cấu hình (url, userID, password) theo thứ tự:
 *  1. WEB-INF/ConnectDB.properties  : cấu hình riêng từng máy (không commit), nếu có
 *  2. src/java/ConnectDB.properties : file chung dao.DBContext của nhóm đang dùng
 * Máy mới clone về chưa tạo file (1) thì vẫn kết nối được bằng file (2).
 */
public class DBContext {

    protected Connection getConnection() throws SQLException {
        Properties properties = new Properties();
        ClassLoader loader = getClass().getClassLoader();
        InputStream found = loader.getResourceAsStream("../ConnectDB.properties");
        if (found == null) {
            found = loader.getResourceAsStream("ConnectDB.properties");
        }
        try (InputStream in = found) {
            if (in == null) {
                throw new SQLException("Không tìm thấy ConnectDB.properties (WEB-INF hoặc src/java)");
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
