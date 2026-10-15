package service;

import dao.AuditLogDAO;
import model.AuditLog;
import java.util.List;

public class AuditLogService {
    private final AuditLogDAO auditLogDAO = new AuditLogDAO();

    public List<AuditLog> getAllLogs() {
        return auditLogDAO.getAllLogs();
    }
}
