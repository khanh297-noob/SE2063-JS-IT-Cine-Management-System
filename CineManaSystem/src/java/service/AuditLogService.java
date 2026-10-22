package service;
import dao.AuditLogDAO;
import model.*;
import java.sql.SQLException;
import java.util.List;

public class AuditLogService {
    private final AuditLogDAO auditLogDAO=new AuditLogDAO();
    public List<AuditLog> getAllLogs(User actor) throws SQLException {
        AccessService.admin(actor);
        return auditLogDAO.getAllLogs();
    }
}