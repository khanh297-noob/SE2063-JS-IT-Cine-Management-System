package cinemanagement.controller.branch;

import cinemanagement.controller.BaseServlet;
import cinemanagement.model.SeatMatrixDTO;
import cinemanagement.util.BusinessException;
import cinemanagement.service.SeatMatrixService;

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
 */
@WebServlet("/manage/rooms/seats")
public class SeatMatrixServlet extends BaseServlet {

    private final SeatMatrixService seatMatrixService = new SeatMatrixService();

    @Override
    protected void handleGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException, SQLException, BusinessException {
        show(req, resp, seatMatrixService.getMatrix(requiredId(req, "roomId")));
    }

    @Override
    protected void handlePost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException, SQLException, BusinessException {
        int roomId = requiredId(req, "roomId");
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
