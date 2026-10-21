package dao.branch;

import model.branch.Branch;
import util.JdbcUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/** Bảng Branch. */
public class BranchDAO {

    private static final String COLUMNS =
            "BranchID, name, address, city, hotline, email, status, CreatedAt";

    /** status null = mọi trạng thái. Tìm theo từ khoá làm ở BranchService để không phân biệt dấu. */
    public List<Branch> findAll(Connection con, String status) throws SQLException {
        StringBuilder sql = new StringBuilder("SELECT " + COLUMNS + " FROM Branch WHERE 1 = 1 ");
        List<Object> params = new ArrayList<>();
        if (status != null) {
            sql.append("AND status = ? ");
            params.add(status);
        }
        sql.append("ORDER BY city, name");

        try (PreparedStatement ps = con.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) ps.setObject(i + 1, params.get(i));
            return mapList(ps);
        }
    }

    public List<Branch> findActive(Connection con) throws SQLException {
        return findAll(con, Branch.STATUS_ACTIVE);
    }

    public Branch findById(Connection con, int branchId) throws SQLException {
        String sql = "SELECT " + COLUMNS + " FROM Branch WHERE BranchID = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, branchId);
            List<Branch> list = mapList(ps);
            return list.isEmpty() ? null : list.get(0);
        }
    }

    public boolean existsByName(Connection con, String name, int excludeId) throws SQLException {
        String sql = "SELECT 1 FROM Branch WHERE name = ? AND BranchID <> ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, name);
            ps.setInt(2, excludeId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public int insert(Connection con, Branch b) throws SQLException {
        String sql = "INSERT INTO Branch (name, address, city, hotline, email, status) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, b.getName());
            ps.setString(2, b.getAddress());
            ps.setString(3, b.getCity());
            ps.setString(4, b.getHotline());
            JdbcUtil.setString(ps, 5, b.getEmail());
            ps.setString(6, b.getStatus());
            ps.executeUpdate();
            return JdbcUtil.generatedKey(ps);
        }
    }

    public void update(Connection con, Branch b) throws SQLException {
        String sql = "UPDATE Branch SET name = ?, address = ?, city = ?, hotline = ?, email = ? WHERE BranchID = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, b.getName());
            ps.setString(2, b.getAddress());
            ps.setString(3, b.getCity());
            ps.setString(4, b.getHotline());
            JdbcUtil.setString(ps, 5, b.getEmail());
            ps.setInt(6, b.getBranchId());
            ps.executeUpdate();
        }
    }

    public void updateStatus(Connection con, int branchId, String status) throws SQLException {
        String sql = "UPDATE Branch SET status = ? WHERE BranchID = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setInt(2, branchId);
            ps.executeUpdate();
        }
    }

    private List<Branch> mapList(PreparedStatement ps) throws SQLException {
        List<Branch> list = new ArrayList<>();
        try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Branch b = new Branch();
                b.setBranchId(rs.getInt("BranchID"));
                b.setName(rs.getString("name"));
                b.setAddress(rs.getString("address"));
                b.setCity(rs.getString("city"));
                b.setHotline(rs.getString("hotline"));
                b.setEmail(rs.getString("email"));
                b.setStatus(rs.getString("status"));
                b.setCreatedAt(JdbcUtil.getDateTime(rs, "CreatedAt"));
                list.add(b);
            }
        }
        return list;
    }
}
