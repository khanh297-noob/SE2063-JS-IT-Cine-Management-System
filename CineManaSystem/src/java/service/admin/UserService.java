package service.admin;

import dao.DBContext;
import dao.admin.AdminUserDAO;
import dao.admin.AuditLogDAO;
import dao.admin.BranchDAO;
import service.common.AccessService;
import model.User;
import java.sql.*;
import java.util.*;

public class UserService {
    private final AdminUserDAO userDAO=new AdminUserDAO();
    private final BranchDAO branchDAO=new BranchDAO();
    private final AuditLogDAO auditLogDAO=new AuditLogDAO();
    private static final Set<String> ROLES=Set.of("Administrator","Customer",
            "Branch Management","Ticket Cashier","Ticket Checker");
    private static final Set<String> EMPLOYEES=Set.of(
            "Branch Management","Ticket Cashier","Ticket Checker");

    public List<User> getAllUsers(User actor) throws SQLException {
        AccessService.admin(actor);
        try(Connection conn=DBContext.getConnection()) {
            return userDAO.getAllUsers(conn,false);
        }
    }
    public void changeUserRole(User actor,int id,String role,Integer branch,String ip)
            throws SQLException {

        if(role==null || !ROLES.contains(role)) throw new IllegalArgumentException("Vai trò không hợp lệ.");

        change(actor,id,true,role,null,branch,ip);
    }
    public void changeUserStatus(User actor,int id,String status,String ip) throws SQLException {
        if(status==null || !Set.of("Active","Locked").contains(status))
            throw new IllegalArgumentException("Trạng thái không hợp lệ.");
        change(actor,id,false,null,status,null,ip);
    }
    public void requireCurrentAdmin(User actor) throws SQLException {
        AccessService.active(actor);
        try (Connection connection = DBContext.getConnection()) {
            User fresh = userDAO.findById(connection, actor.getUserID());
            AccessService.admin(fresh);
        }
    }

    public List<User> searchUsers(User actor, String rawKeyword) throws SQLException {
        AccessService.admin(actor);
        String keyword = rawKeyword == null ? "" : rawKeyword.trim();
        if (keyword.length() > 100) {
            throw new IllegalArgumentException("Từ khóa tối đa 100 ký tự.");
        }
        try (Connection connection = DBContext.getConnection()) {
            return keyword.isEmpty()
                    ? userDAO.getAllUsers(connection, false)
                    : userDAO.searchUsers(connection, keyword);
        }
    }

    public int createEmployee(User actor, String fullName, String email,
                              String phone, String password, String role,
                              Integer branchId, String ip) throws SQLException {
        AccessService.admin(actor);
        String name = fullName == null ? "" : fullName.trim();
        String address = email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
        String number = phone == null ? "" : phone.trim();
        if (name.isEmpty() || name.length() > 100) {
            throw new IllegalArgumentException("Họ tên cần từ 1 đến 100 ký tự.");
        }
        if (address.length() > 150
                || !address.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            throw new IllegalArgumentException("Email không hợp lệ.");
        }
        if (!number.matches("0[0-9]{9}")) {
            throw new IllegalArgumentException("Số điện thoại cần 10 chữ số, bắt đầu bằng 0.");
        }
        if (password == null || password.length() < 8 || password.length() > 64
                || password.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72) {
            throw new IllegalArgumentException("Mật khẩu 8–64 ký tự, tối đa 72 byte UTF-8.");
        }
        if (role == null || !EMPLOYEES.contains(role)) {
            throw new IllegalArgumentException("Chỉ tạo quản lý chi nhánh, thu ngân hoặc soát vé.");
        }
        if (branchId == null || branchId <= 0) {
            throw new IllegalArgumentException("Nhân viên cần được gán chi nhánh.");
        }
        String hash = util.PasswordUtil.hash(password);
        try {
            return DBContext.transaction(connection -> {
                List<User> users = userDAO.getAllUsers(connection, true);
                User fresh = users.stream()
                        .filter(user -> user.getUserID() == actor.getUserID())
                        .findFirst()
                        .orElseThrow(() -> new SecurityException("Tài khoản thao tác không còn tồn tại."));
                AccessService.admin(fresh);
                if (!branchDAO.existsActive(connection, branchId)) {
                    throw new IllegalArgumentException("Chi nhánh không hoạt động.");
                }
                boolean duplicate = users.stream().anyMatch(user ->
                        address.equalsIgnoreCase(user.getEmail())
                                || number.equals(user.getPhone()));
                if (duplicate) {
                    throw new IllegalArgumentException("Email hoặc số điện thoại đã được sử dụng.");
                }
                User employee = new User();
                employee.setFullName(name);
                employee.setEmail(address);
                employee.setPhone(number);
                employee.setRole(role);
                employee.setBranchID(branchId);
                int id = userDAO.insertEmployee(connection, employee, hash);
                auditLogDAO.insert(connection, actor.getUserID(),
                        "EMPLOYEE_CREATE|target=" + id + "|role=" + role + "|branch=" + branchId, ip);
                return id;
            });
        } catch (SQLException exception) {
            if (exception.getErrorCode() == 2601 || exception.getErrorCode() == 2627) {
                throw new IllegalArgumentException("Email hoặc số điện thoại bị trùng.", exception);
            }
            throw exception;
        }
    }

    private void change(User actor,int id,boolean changeRole,String requestedRole, String requestedStatus,Integer requestedBranch,String ip) throws SQLException {
        AccessService.admin(actor);
        if(id<=0) throw new IllegalArgumentException("ID không hợp lệ.");

        DBContext.transaction(conn -> {
            List<User> all=userDAO.getAllUsers(conn,true);
            User freshActor=all.stream().filter(u->u.getUserID()==actor.getUserID()).findFirst().orElseThrow(()->new SecurityException("Actor không tồn tại."));
            AccessService.admin(freshActor);
            User target=all.stream().filter(u->u.getUserID()==id).findFirst().orElseThrow(()->new IllegalArgumentException("Không tìm thấy user."));
            String role=changeRole ? requestedRole : target.getRole();
            String status=changeRole ? target.getStatus() : requestedStatus;
            Integer branch=changeRole ? (EMPLOYEES.contains(role) ? requestedBranch : null) : target.getBranchID();

            if(EMPLOYEES.contains(role) && (branch==null || branch<=0 || !branchDAO.existsActive(conn,branch))) throw new IllegalArgumentException("Nhân viên cần chi nhánh đang hoạt động.");
            boolean removing="Administrator".equals(target.getRole()) && "Active".equals(target.getStatus()) && (!"Administrator".equals(role) || !"Active".equals(status));
            long admins=all.stream().filter(u->"Administrator".equals(u.getRole()) && "Active".equals(u.getStatus())).count();
            if(removing && admins<=1)
                throw new IllegalArgumentException("MSG73: Không thể vô hiệu hóa Admin hoạt động cuối.");
            if(userDAO.updateAccess(conn,id,role,status,branch)!=1)
                throw new SQLException("Không cập nhật được user.");
            auditLogDAO.insert(conn,actor.getUserID(),
                    "USER_ACCESS|target="+id+"|role="+target.getRole()+"->"+role +"|status="+target.getStatus()+"->"+status +"|branch="+target.getBranchID()+"->"+branch,ip);
            return null;
        });
    }
}
