package dao;

import model.Branch;
import util.DBContext;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class BranchDAO {
    public List<Branch> getAllBranches() {
        List<Branch> list = new ArrayList<>();
        String sql = "SELECT BranchID, Name, Address, City, Hotline FROM Branch ORDER BY BranchID ASC";

        try (Connection conn = DBContext.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql);
        ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Branch branch = new Branch(rs.getInt("BranchID"),
                        rs.getString("Name"),
                        rs.getString("Address"),
                        rs.getString("City"),
                        rs.getString("Hotline"));
                list.add(branch);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }
}
