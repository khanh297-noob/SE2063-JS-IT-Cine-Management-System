package controller.management;

import dao.pos.StaffDAO;
import model.pos.WorkShift;
import service.pos.ShiftService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Quản lý xem ca làm & tổng giờ làm của nhân viên quầy / soát vé.
 */
@WebServlet(name = "WorkingHoursServlet", urlPatterns = {"/manager/working-hours"})
public class WorkingHoursServlet extends HttpServlet {

    private ShiftService shiftService;
    private StaffDAO staffDAO;

    @Override
    public void init() throws ServletException {
        shiftService = new ShiftService();
        staffDAO = new StaffDAO();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        LocalDate from = parseDate(request.getParameter("from"), LocalDate.now().withDayOfMonth(1));
        LocalDate to = parseDate(request.getParameter("to"), LocalDate.now());
        if (from.isAfter(to)) {
            request.setAttribute("dateError", "The selected date range is invalid. Please check the start and end dates.");
            from = to.withDayOfMonth(1);
        }
        int staffId = parseInt(request.getParameter("staffId"));
        int branchId = parseInt(request.getParameter("branchId"));

        // Lọc theo rạp
        List<WorkShift> shifts = new ArrayList<>();
        for (WorkShift s : shiftService.getShifts(staffId, from, to)) {
            if (branchId == 0 || s.getBranchId() == branchId) {
                shifts.add(s);
            }
        }
        // Tổng giờ theo nhân viên: { "Tên (vai trò)" -> số giờ }
        Map<String, Double> totals = new LinkedHashMap<>();
        for (WorkShift s : shifts) {
            String key = s.getStaffName() + " (" + s.getRole() + ")";
            double old = totals.containsKey(key) ? totals.get(key) : 0;
            totals.put(key, Math.round((old + s.getHours()) * 100) / 100.0);
        }

        request.setAttribute("shifts", shifts);
        request.setAttribute("totals", totals);
        request.setAttribute("staffList", staffDAO.getStaffList());
        request.setAttribute("branches", shiftService.getBranches());
        request.setAttribute("from", from.toString());
        request.setAttribute("to", to.toString());
        request.setAttribute("staffId", staffId);
        request.setAttribute("branchId", branchId);
        request.getRequestDispatcher("/WEB-INF/view/manager/working-hours-nam.jsp").forward(request, response);
    }

    private LocalDate parseDate(String s, LocalDate defaultValue) {
        try {
            return LocalDate.parse(s);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    private int parseInt(String s) {
        try {
            return Integer.parseInt(s);
        } catch (Exception e) {
            return 0;
        }
    }
}
