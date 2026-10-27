package service.admin;

import dao.admin.BranchDAO;
import model.*;
import java.sql.SQLException;
import java.util.List;

public class BranchService {
    private final BranchDAO branchDAO=new BranchDAO();
    public List<Branch> getAllBranches(User actor) throws SQLException {
        if(actor==null || !"Active".equals(actor.getStatus())
                || !"Administrator".equals(actor.getRole()))
            throw new SecurityException("Chỉ Admin được xem toàn bộ chi nhánh.");
        return branchDAO.getAllBranches();
    }
}