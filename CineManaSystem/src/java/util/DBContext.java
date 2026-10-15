package util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBContext {
    private static final String SERVER_NAME = "localhost";
    private static final String PORT_NUMBER = "1433";
    private static final String DATABASE_NAME = "CineManageSystem";
    private static final String USER_ID = "sa";        
    private static final String PASSWORD = "123";    

    public static Connection getConnection() {
        Connection conn = null;
        try {
            Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
            String url = "jdbc:sqlserver://" + SERVER_NAME + ":" + PORT_NUMBER + ";databaseName=" + DATABASE_NAME + 
                    ";encrypt=false;trustServerCertificate=true";
            conn = DriverManager.getConnection(url, USER_ID, PASSWORD);
        } catch (ClassNotFoundException | SQLException e) {
            e.printStackTrace();
        }
        return conn;
    }
}