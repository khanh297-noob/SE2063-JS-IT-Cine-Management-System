package service;

import dao.UserDAO;
import model.User;
import java.util.List;

public class UserService {
    private final UserDAO userDAO = new UserDAO();

    public List<User> getAllUsers() {
        return userDAO.getAllUsers();
    }

    
    public boolean toggleUserStatus(int targetUserId, int currentAdminId, String currentStatus) {
        
        if (targetUserId == currentAdminId) {
            return false;
        }

        String newStatus = "Active".equalsIgnoreCase(currentStatus) ? "Locked" : "Active";
        return userDAO.updateStatus(targetUserId, newStatus);
    }

    
    public boolean changeUserRole(int targetUserId, int currentAdminId, String newRole) {
        if (targetUserId == currentAdminId) {
            return false;
        }
        if (newRole == null || newRole.trim().isEmpty()) {
            return false;
        }
        return userDAO.updateRole(targetUserId, newRole.trim());
    }
}