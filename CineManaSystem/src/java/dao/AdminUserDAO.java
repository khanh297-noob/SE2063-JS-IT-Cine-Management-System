package dao;
import model.User;
import java.sql.*;
import java.util.*;

public class AdminUserDAO {
    public List<User> getAllUsers(Connection conn,boolean lock) throws SQLException {
        String sql="""
            SELECT u.UserID,u.FullName,u.Email,u.Phone,u.[Role],u.[Status],
                   u.BranchID,b.Name AS BranchName
            FROM dbo.[User] u
            """+(lock ? " WITH (UPDLOCK,HOLDLOCK) " : " ")
                +" LEFT JOIN dbo.Branch b ON b.BranchID=u.BranchID ORDER BY u.UserID";
        List<User> list=new ArrayList<>();
        try(PreparedStatement ps=conn.prepareStatement(sql);ResultSet rs=ps.executeQuery()) {
            while(rs.next()) {
                User u=new User();
                u.setUserID(rs.getInt("UserID"));
                u.setFullName(rs.getString("FullName")); u.setEmail(rs.getString("Email"));
                u.setPhone(rs.getString("Phone")); u.setRole(rs.getString("Role"));
                u.setStatus(rs.getString("Status"));
                int id=rs.getInt("BranchID");
                u.setBranchID(rs.wasNull() ? null : id);
                u.setBranchName(rs.getString("BranchName"));
                list.add(u);
            }
        }
        return list;
    }
    public int updateAccess(Connection conn,int id,String role,String status,Integer branch)
            throws SQLException {
        try(PreparedStatement ps=conn.prepareStatement(
                "UPDATE dbo.[User] SET [Role]=?,[Status]=?,BranchID=? WHERE UserID=?")) {
            ps.setString(1,role);ps.setString(2,status);
            if(branch==null) ps.setNull(3,Types.INTEGER); else ps.setInt(3,branch);
            ps.setInt(4,id);return ps.executeUpdate();
        }
    }
}