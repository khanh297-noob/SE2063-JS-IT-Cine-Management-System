package controller;

import model.User;
import service.UserService;
import jakarta.servlet.ServletException;    
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.util.List;

@WebServlet(name = "UserManagementServlet", urlPatterns = {"/admin/users"})
public class UserManagementServlet extends HttpServlet {
    private final UserService userService = new UserService();

    
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        List<User> userList = userService.getAllUsers();
        request.setAttribute("userList", userList);

        request.getRequestDispatcher("/WEB-INF/views/admin/user-management-khanh.jsp").forward(request, response);
    }

  
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String action = request.getParameter("action");
        HttpSession session = request.getSession();
        
        
        User loggedInUser = (User) session.getAttribute("currentUser");
        int loggedInAdminId = (loggedInUser != null) ? loggedInUser.getUserId() : -1;

      
        if ("toggleStatus".equals(action)) {
            String idParam = request.getParameter("id");
            String currentStatus = request.getParameter("status");

            if (idParam != null && currentStatus != null) {
                try {
                    int targetUserId = Integer.parseInt(idParam);
                    boolean success = userService.toggleUserStatus(targetUserId, loggedInAdminId, currentStatus);
                    if (success) {
                        session.setAttribute("toastSuccess", "Cập nhật trạng thái thành công!");
                    } else {
                        session.setAttribute("toastError", "Không thể tự khóa tài khoản của chính mình!");
                    }
                } catch (NumberFormatException e) {
                    session.setAttribute("toastError", "ID không hợp lệ!");
                }
            }
        } 
     
        else if ("updateRole".equals(action)) {
            String idParam = request.getParameter("id");
            String newRole = request.getParameter("newRole");

            if (idParam != null && newRole != null) {
                try {
                    int targetUserId = Integer.parseInt(idParam);
                    boolean success = userService.changeUserRole(targetUserId, loggedInAdminId, newRole);
                    if (success) {
                        session.setAttribute("toastSuccess", "Cập nhật vai trò người dùng thành công!");
                    } else {
                        session.setAttribute("toastError", "Không thể thay đổi vai trò của tài khoản đang đăng nhập!");
                    }
                } catch (NumberFormatException e) {
                    session.setAttribute("toastError", "ID không hợp lệ!");
                }
            }
        }

        response.sendRedirect(request.getContextPath() + "/admin/users");
    }
}