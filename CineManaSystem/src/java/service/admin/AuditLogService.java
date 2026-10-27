package service.admin;
import dao.admin.AuditLogDAO;
import model.*;
import service.common.AccessService;

import java.sql.SQLException;
import java.util.List;

public class AuditLogService {
    private final AuditLogDAO auditLogDAO=new AuditLogDAO();
    public List<AuditLog> getAllLogs(User actor) throws SQLException {
        AccessService.admin(actor);
        return auditLogDAO.getAllLogs();
    }
}