package dao.notification;

import model.notification.Notification;
import util.JdbcUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** Bảng Notification. Mọi truy vấn đọc / sửa đều lọc theo UserID của người đang đăng nhập. */
public class NotificationDAO {

    private static final String COLUMNS = "NotificationID, UserID, Type, Title, Content, Link, IsRead, CreatedAt";

    public void insert(Connection con, int userId, String type, String title, String content, String link) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(
                "INSERT INTO Notification (UserID, Type, Title, Content, Link) VALUES (?, ?, ?, ?, ?)")) {
            ps.setInt(1, userId);
            bindBody(ps, 2, type, title, content, link);
            ps.executeUpdate();
        }
    }

    /** Một thông báo cho mỗi tài khoản Active có vai trò thuộc roles. Trả về số thông báo đã tạo. */
    public int insertForRoles(Connection con, List<String> roles, String type, String title, String content, String link)
            throws SQLException {
        if (roles.isEmpty()) return 0;
        String marks = String.join(", ", java.util.Collections.nCopies(roles.size(), "?"));
        String sql = "INSERT INTO Notification (UserID, Type, Title, Content, Link) "
                + "SELECT UserID, ?, ?, ?, ? FROM [User] WHERE [Status] = 'Active' AND [Role] IN (" + marks + ")";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            int i = bindBody(ps, 1, type, title, content, link);
            for (String r : roles) ps.setString(i++, r);
            return ps.executeUpdate();
        }
    }

    /**
     * Một thông báo cho mỗi Quản lý Active của chi nhánh branchId, trừ exceptUserId (người vừa thao tác).
     * Trả về số thông báo đã tạo.
     */
    public int insertForBranchManagers(Connection con, List<String> managerRoles, int branchId, Integer exceptUserId,
                                       String type, String title, String content, String link) throws SQLException {
        String marks = String.join(", ", java.util.Collections.nCopies(managerRoles.size(), "?"));
        String sql = "INSERT INTO Notification (UserID, Type, Title, Content, Link) "
                + "SELECT UserID, ?, ?, ?, ? FROM [User] WHERE [Status] = 'Active' AND [Role] IN (" + marks + ") "
                + "AND BranchID = ? AND UserID <> ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            int i = bindBody(ps, 1, type, title, content, link);
            for (String r : managerRoles) ps.setString(i++, r);
            ps.setInt(i++, branchId);
            ps.setInt(i, exceptUserId == null ? -1 : exceptUserId);
            return ps.executeUpdate();
        }
    }

    /** Một thông báo cho mỗi Quản lý Active của các chi nhánh có phim movieId (bảng MovieBranch). */
    public int insertForManagersOfMovie(Connection con, List<String> managerRoles, int movieId,
                                        String type, String title, String content, String link) throws SQLException {
        String marks = String.join(", ", java.util.Collections.nCopies(managerRoles.size(), "?"));
        String sql = "INSERT INTO Notification (UserID, Type, Title, Content, Link) "
                + "SELECT u.UserID, ?, ?, ?, ? FROM [User] u WHERE u.[Status] = 'Active' AND u.[Role] IN (" + marks + ") "
                + "AND u.BranchID IN (SELECT mb.BranchID FROM MovieBranch mb WHERE mb.MovieID = ?)";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            int i = bindBody(ps, 1, type, title, content, link);
            for (String r : managerRoles) ps.setString(i++, r);
            ps.setInt(i, movieId);
            return ps.executeUpdate();
        }
    }

    public int countUnread(Connection con, int userId) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(
                "SELECT COUNT(*) FROM Notification WHERE UserID = ? AND IsRead = 0")) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    /** Mới nhất trước; beforeId != null thì chỉ lấy thông báo cũ hơn (phân trang "Xem thêm"). */
    public List<Notification> findByUser(Connection con, int userId, Integer beforeId, int limit) throws SQLException {
        String sql = "SELECT TOP (?) " + COLUMNS + " FROM Notification WHERE UserID = ?"
                + (beforeId != null ? " AND NotificationID < ?" : "") + " ORDER BY NotificationID DESC";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, limit);
            ps.setInt(2, userId);
            if (beforeId != null) ps.setInt(3, beforeId);
            List<Notification> list = new ArrayList<>();
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
            return list;
        }
    }

    /** Chỉ đánh dấu khi thông báo thuộc đúng userId. Trả về số dòng đổi (0 nếu không phải của người này). */
    public int markRead(Connection con, int userId, int notificationId) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(
                "UPDATE Notification SET IsRead = 1 WHERE NotificationID = ? AND UserID = ? AND IsRead = 0")) {
            ps.setInt(1, notificationId);
            ps.setInt(2, userId);
            return ps.executeUpdate();
        }
    }

    public int markAllRead(Connection con, int userId) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(
                "UPDATE Notification SET IsRead = 1 WHERE UserID = ? AND IsRead = 0")) {
            ps.setInt(1, userId);
            return ps.executeUpdate();
        }
    }

    private static int bindBody(PreparedStatement ps, int i, String type, String title, String content, String link)
            throws SQLException {
        ps.setString(i++, type);
        ps.setString(i++, cut(title, 150));      // độ dài cột Title / Content
        JdbcUtil.setString(ps, i++, cut(content, 500));
        JdbcUtil.setString(ps, i++, link);
        return i;
    }

    private static String cut(String s, int max) {
        return s == null || s.length() <= max ? s : s.substring(0, max - 1) + "…";
    }

    private static Notification map(ResultSet rs) throws SQLException {
        Notification n = new Notification();
        n.setNotificationId(rs.getInt("NotificationID"));
        n.setUserId(rs.getInt("UserID"));
        n.setType(rs.getString("Type"));
        n.setTitle(rs.getString("Title"));
        n.setContent(rs.getString("Content"));
        n.setLink(rs.getString("Link"));
        n.setRead(rs.getBoolean("IsRead"));
        n.setCreatedAt(JdbcUtil.getDateTime(rs, "CreatedAt"));
        return n;
    }
}
