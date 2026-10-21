package dao;

import util.DBContext;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import model.Branch;

public class BranchDAO extends DBContext {

    public List<Branch> getAll() {
        List<Branch> list = new ArrayList<>();
        String sql = "SELECT BranchID, Name, Address, City, Hotline FROM Branch ORDER BY Name";
        try (Connection con = getConnection(); PreparedStatement ps = con.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapBranch(rs));
            }
        } catch (SQLException e) {
            System.out.println("Lỗi BranchDAO.getAll: " + e.getMessage());
        }
        return list;
    }

    public Branch getById(int branchId) {
        String sql = "SELECT BranchID, Name, Address, City, Hotline FROM Branch WHERE BranchID = ?";
        try (Connection con = getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, branchId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapBranch(rs);
                }
            }
        } catch (SQLException e) {
            System.out.println("Lỗi BranchDAO.getById: " + e.getMessage());
        }
        return null;
    }

    private Branch mapBranch(ResultSet rs) throws SQLException {
        Branch b = new Branch();
        b.setBranchId(rs.getInt("BranchID"));
        b.setName(rs.getString("Name"));
        b.setAddress(rs.getString("Address"));
        b.setCity(rs.getString("City"));
        b.setHotline(rs.getString("Hotline"));
        return b;
    }
}
