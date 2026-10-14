package cinemanagement.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import cinemanagement.model.Branch;
import cinemanagement.util.DBContext;

public class BranchDAO {

    public List<Branch> findAll() throws SQLException {
        List<Branch> list = new ArrayList<>();
        try (Connection c = DBContext.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT * FROM Branch ORDER BY BranchID");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(map(rs));
            }
        }
        return list;
    }

    public Branch findById(int id) throws SQLException {
        try (Connection c = DBContext.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT * FROM Branch WHERE BranchID = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    private Branch map(ResultSet rs) throws SQLException {
        Branch b = new Branch();
        b.setBranchId(rs.getInt("BranchID"));
        b.setName(rs.getString("Name"));
        b.setAddress(rs.getString("Address"));
        b.setCity(rs.getString("City"));
        b.setHotline(rs.getString("Hotline"));
        return b;
    }
}
