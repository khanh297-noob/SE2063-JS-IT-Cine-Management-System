package cinemanagement.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;
import java.util.ArrayList;
import java.util.List;

import cinemanagement.model.Branch;
import cinemanagement.model.OperatingHour;
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

    public OperatingHour findOperatingHour(int branchId, int dayOfWeek) throws SQLException {
        try (Connection c = DBContext.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT IsClosed, OpenTime, CloseTime FROM BranchOperatingHour "
                     + "WHERE BranchID = ? AND DayOfWeek = ?")) {
            ps.setInt(1, branchId);
            ps.setInt(2, dayOfWeek);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                Time open = rs.getTime("OpenTime");
                Time close = rs.getTime("CloseTime");
                return new OperatingHour(rs.getBoolean("IsClosed"),
                        open == null ? null : open.toLocalTime(), close == null ? null : close.toLocalTime());
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
        b.setEmail(rs.getString("Email"));
        b.setStatus(rs.getString("Status"));
        return b;
    }
}
