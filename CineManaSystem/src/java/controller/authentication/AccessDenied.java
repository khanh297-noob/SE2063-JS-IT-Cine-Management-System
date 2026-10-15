/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/JSP_Servlet/Servlet.java to edit this template
 */

package controller.authentication;

import java.io.IOException;
import java.io.PrintWriter;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import util.PermissionUtil;

/**
 *
 * @author adm
 */
@WebServlet(name="accessDenied", urlPatterns={"/access-denied"})
public class AccessDenied extends HttpServlet {
   @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        // Truyền thông tin user để trang hiển thị đúng
        req.setAttribute("currentUser", PermissionUtil.getCurrentUser(req));
        req.getRequestDispatcher("/view/error/access-denied.jsp")
                .forward(req, resp);
    }
     
}
