package controller.branch;

import model.branch.SeatMatrixDTO;
import service.branch.RoomService;
import service.branch.SeatMatrixService;
import util.BusinessException;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;

/**
 * Configure Seat Matrix.
 * GET  /manage/rooms/seats?roomId=1
 * POST roomId, rows, cols, layout (định dạng xem SeatMatrixDTO)
 * Admin, hoặc Quản lý của chi nhánh có phòng đó.
 */
@WebServlet("/manage/rooms/seats")
public class SeatMatrixServlet extends BranchScopedServlet {

    private final SeatMatrixService seatMatrixService = new SeatMatrixService();
    private final RoomService roomService = new RoomService();

    @Override
    protected void handleGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException, SQLException, BusinessException {
        SeatMatrixDTO matrix = seatMatrixService.getMatrix(requiredId(req, "roomId"));
        requireBranch(req, matrix.getRoom().getBranchId());
        show(req, resp, matrix);
    }

    @Override
    protected void handlePost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException, SQLException, BusinessException {
        int roomId = requiredId(req, "roomId");
        requireBranch(req, roomService.get(roomId).getBranchId());
        Integer rows = intParam(req, "rows");
        Integer cols = intParam(req, "cols");
        String layout = req.getParameter("layout");
        try {
            SeatMatrixService.SaveResult r = seatMatrixService.save(roomId, rows, cols, layout);
            flashSuccess(req, "Đã lưu sơ đồ ghế: thêm " + r.getAdded() + ", bớt " + r.getRemoved()
                    + ", đổi " + r.getChanged() + " ghế.");
            redirect(req, resp, "/manage/rooms/seats?roomId=" + roomId);
        } catch (BusinessException e) {
            // Hiển thị lại đúng sơ đồ người dùng vừa vẽ.
            SeatMatrixDTO base = seatMatrixService.getMatrix(roomId);
            boolean usable = rows != null && cols != null && layout != null;
            req.setAttribute("errors", e.getErrors());
            show(req, resp, usable
                    ? new SeatMatrixDTO(base.getRoom(), base.getBranchName(), base.getSeatTypes(),
                                        rows, cols, layout, base.getUpcomingShowtimes())
                    : base);
        }
    }

    private void show(HttpServletRequest req, HttpServletResponse resp, SeatMatrixDTO matrix)
            throws ServletException, IOException {
        req.setAttribute("matrix", matrix);
        req.setAttribute("maxRows", SeatMatrixService.MAX_ROWS);
        req.setAttribute("maxCols", SeatMatrixService.MAX_COLUMNS);
        render(req, resp, "room/seat-matrix-hoang");
    }
}
