package service;
import dao.*;
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