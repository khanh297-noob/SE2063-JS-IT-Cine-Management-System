package dao.admin;

import dao.DBContext;
import model.Branch;
import java.sql.*;
import java.util.*;

public class BranchDAO {
    public List<Branch> getAllBranches() throws SQLException {
        List<Branch> list=new ArrayList<>();
        String sql="SELECT BranchID,Name,Address,City,Hotline FROM dbo.Branch ORDER BY BranchID";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps=conn.prepareStatement(sql);
             ResultSet rs=ps.executeQuery()) {
            while(rs.next())
                list.add(new Branch(rs.getInt("BranchID"),rs.getString("Name"),
                        rs.getString("Address"),rs.getString("City"),rs.getString("Hotline")));
        }
        return list;
    }
    public boolean existsActive(Connection conn,int id) throws SQLException {
        try(PreparedStatement ps=conn.prepareStatement(
                "SELECT 1 FROM dbo.Branch WHERE BranchID=? AND Status='Active'")) {
            ps.setInt(1,id);
            try(ResultSet rs=ps.executeQuery()) { return rs.next(); }
        }
    }

    public List<Branch> getActiveBranches(Integer onlyBranch) throws SQLException {
        List<Branch> result = new ArrayList<>();
        String sql = """
                SELECT BranchID, Name, Address, City, Hotline
                FROM dbo.Branch
                WHERE Status = 'Active'
                  AND BranchID = COALESCE(?, BranchID)
                ORDER BY Name
                """;
        try (Connection c = DBContext.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            if (onlyBranch == null) ps.setNull(1, Types.INTEGER);
            else ps.setInt(1, onlyBranch);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(new Branch(rs.getInt("BranchID"), rs.getString("Name"),
                            rs.getString("Address"), rs.getString("City"), rs.getString("Hotline")));
                }
            }
        }
        return result;
    }
}