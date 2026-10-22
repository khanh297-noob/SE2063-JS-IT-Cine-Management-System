package dao;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class DBContext {
    protected Connection connection;

    public DBContext() {
        try {
            connection = getConnection();
        } catch (SQLException exception) {
            throw new IllegalStateException(
                    "Không kết nối được database. Kiểm tra log SQL gốc.",
                    exception
            );
        }
    }

    public static Connection getConnection() throws SQLException {
        Properties properties = new Properties();
        try (InputStream input = DBContext.class.getClassLoader()
                .getResourceAsStream("ConnectDB.properties")) {
            if (input == null) {
                throw new SQLException("Thiếu ConnectDB.properties trong classpath.");
            }
            properties.load(input);
        } catch (IOException exception) {
            throw new SQLException("Không đọc được ConnectDB.properties.", exception);
        }

        String url = properties.getProperty("url");
        String username = properties.getProperty("userID");
        String password = properties.getProperty("password");
        if (url == null || url.isBlank() || username == null || password == null) {
            throw new SQLException("Thiếu url, userID hoặc password trong cấu hình.");
        }

        try {
            Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
        } catch (ClassNotFoundException exception) {
            throw new SQLException("Thiếu thư viện JDBC SQL Server.", exception);
        }
        return DriverManager.getConnection(url, username, password);
    }

    @FunctionalInterface
    public interface Work<T> {
        T execute(Connection connection) throws SQLException;
    }

    public static <T> T transaction(Work<T> work) throws SQLException {
        try (Connection connection = getConnection()) {
            connection.setAutoCommit(false);
            try {
                T result = work.execute(connection);
                connection.commit();
                return result;
            } catch (SQLException | RuntimeException exception) {
                try {
                    connection.rollback();
                } catch (SQLException rollbackError) {
                    exception.addSuppressed(rollbackError);
                }
                throw exception;
            }
        }
    }
}
