package service;

import dao.BranchDAO;
import model.Branch;
import java.util.List;

public class BranchService {
    private final BranchDAO branchDAO = new BranchDAO();

    public List<Branch> getAllBranches() {
        return branchDAO.getAllBranches();
    }
}