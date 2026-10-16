package util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Kết nối SQL Server. Các DAO kế thừa class này và gọi getConnection().
 * Mỗi hàm DAO mở 1 connection và tự đóng bằng try-with-resources.
 */
public class DBContext {

    private static final String URL = "jdbc:sqlserver://localhost:1433;"
            + "databaseName=CineSystemDataBase;encrypt=true;trustServerCertificate=true";
    private static final String USER = "sa";
    private static final String PASS = "123456";

    protected Connection getConnection() throws SQLException {
        try {
            Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
        } catch (ClassNotFoundException e) {
            throw new SQLException("Thiếu thư viện mssql-jdbc", e);
        }
        return DriverManager.getConnection(URL, USER, PASS);
    }
}
