package dao.admin;

import model.User;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class AdminUserDAO {
    private static final String COLUMNS = """
            SELECT u.UserID, u.FullName, u.Email, u.Phone, u.[Role], u.[Status],u.BranchID, b.Name AS BranchName
            FROM dbo.[User] u
            """;

    public List<User> getAllUsers(Connection connection, boolean lock)
            throws SQLException {
        String sql = COLUMNS
                + (lock ? " WITH (UPDLOCK, HOLDLOCK) " : " ")
                + " LEFT JOIN dbo.Branch b ON b.BranchID = u.BranchID"
                + " ORDER BY u.UserID";
        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {
            return readUsers(result);
        }
    }

    public User findById(Connection connection, int id) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                COLUMNS + " LEFT JOIN dbo.Branch b ON b.BranchID = u.BranchID WHERE u.UserID = ?")) {
            statement.setInt(1, id);
            try (ResultSet result = statement.executeQuery()) {
                List<User> found = readUsers(result);
                return found.isEmpty() ? null : found.get(0);
            }
        }
    }

    public List<User> searchUsers(Connection connection, String keyword)
            throws SQLException {
        String sql = COLUMNS + """
                LEFT JOIN dbo.Branch b ON b.BranchID = u.BranchID
                WHERE u.FullName LIKE ? ESCAPE '\\'
                   OR u.Email LIKE ? ESCAPE '\\'
                   OR u.Phone LIKE ? ESCAPE '\\'
                   OR CONVERT(VARCHAR(20), u.UserID) LIKE ? ESCAPE '\\'
                ORDER BY u.UserID
                """;
        String literal = keyword.replace("\\", "\\\\")
                .replace("%", "\\%").replace("_", "\\_").replace("[", "\\[");
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            for (int i = 1; i <= 4; i++) {
                statement.setNString(i, "%" + literal + "%");
            }
            try (ResultSet result = statement.executeQuery()) {
                return readUsers(result);
            }
        }
    }

    public int insertEmployee(Connection connection, User user, String passwordHash)
            throws SQLException {
        String sql = """
                INSERT INTO dbo.[User]
                    (FullName, Email, Phone, PasswordHash, [Role], [Status], BranchID)
                VALUES (?, ?, ?, ?, ?, 'Active', ?)
                """;
        try (PreparedStatement statement = connection.prepareStatement(
                sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setNString(1, user.getFullName());
            statement.setString(2, user.getEmail());
            statement.setString(3, user.getPhone());
            statement.setString(4, passwordHash);
            statement.setString(5, user.getRole());
            statement.setInt(6, user.getBranchID());
            statement.executeUpdate();
            try (ResultSet result = statement.getGeneratedKeys()) {
                if (!result.next()) {
                    throw new SQLException("Không lấy được UserID vừa tạo.");
                }
                return result.getInt(1);
            }
        }
    }

    public int updateAccess(Connection connection, int id, String role,
                            String status, Integer branch) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                UPDATE dbo.[User] SET [Role] = ?, [Status] = ?, BranchID = ?
                WHERE UserID = ?
                """)) {
            statement.setString(1, role);
            statement.setString(2, status);
            if (branch == null) statement.setNull(3, Types.INTEGER);
            else statement.setInt(3, branch);
            statement.setInt(4, id);
            return statement.executeUpdate();
        }
    }

    private List<User> readUsers(ResultSet result) throws SQLException {
        List<User> users = new ArrayList<>();
        while (result.next()) {
            User user = new User();
            user.setUserID(result.getInt("UserID"));
            user.setFullName(result.getString("FullName"));
            user.setEmail(result.getString("Email"));
            user.setPhone(result.getString("Phone"));
            user.setRole(result.getString("Role"));
            user.setStatus(result.getString("Status"));
            user.setBranchID((Integer) result.getObject("BranchID"));
            user.setBranchName(result.getString("BranchName"));
            users.add(user);
        }
        return users;
    }
}
