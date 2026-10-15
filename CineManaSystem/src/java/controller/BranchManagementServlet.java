package controller;

import model.Branch;
import service.BranchService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

@WebServlet(name = "BranchManagementServlet", urlPatterns = {"/admin/branches"})
public class BranchManagementServlet extends HttpServlet{
    private final BranchService branchService = new BranchService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException {
        List<Branch> branchList = branchService.getAllBranches();
        request.setAttribute(("branchList"), branchList);

        request.getRequestDispatcher("/admin/branch-management.jsp").forward(request, response);
    }
}
