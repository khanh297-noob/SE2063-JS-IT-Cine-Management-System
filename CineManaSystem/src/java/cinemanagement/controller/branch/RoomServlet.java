package cinemanagement.controller.branch;

import cinemanagement.controller.BaseServlet;
import cinemanagement.util.BusinessException;
import cinemanagement.model.Room;
import cinemanagement.service.BranchService;
import cinemanagement.service.RoomService;
import cinemanagement.service.RoomTypeService;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;

/**
 * Room Management.
 * GET  /manage/rooms[?branchId=]                 danh sách
 * GET  /manage/rooms?action=create[&branchId=]   form thêm
 * GET  /manage/rooms?action=edit&id=             form sửa
 * POST action=save | status | delete
 */
@WebServlet("/manage/rooms")
public class RoomServlet extends BaseServlet {

    private final RoomService roomService = new RoomService();
    private final BranchService branchService = new BranchService();
    private final RoomTypeService roomTypeService = new RoomTypeService();

    @Override
    protected void handleGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException, SQLException, BusinessException {
        switch (action(req, "list")) {
            case "create" -> {
                Room r = new Room();
                Integer branchId = intParam(req, "branchId");
                if (branchId != null) r.setBranchId(branchId);
                showForm(req, resp, r);
            }
            case "edit" -> showForm(req, resp, roomService.get(requiredId(req, "id")));
            default -> {
                Integer branchId = intParam(req, "branchId");
                req.setAttribute("rooms", roomService.list(branchId));
                req.setAttribute("branches", branchService.list(null, null));
                req.setAttribute("branchId", branchId);
                render(req, resp, "room/list-hoang");
            }
        }
    }

    @Override
    protected void handlePost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException, SQLException, BusinessException {
        switch (action(req, "save")) {
            case "status" -> {
                roomService.changeStatus(requiredId(req, "id"), str(req, "status"));
                flashSuccess(req, "Đã cập nhật trạng thái phòng.");
                redirect(req, resp, "/manage/rooms" + branchQuery(req));
            }
            case "delete" -> {
                roomService.delete(requiredId(req, "id"));
                flashSuccess(req, "Đã xoá phòng.");
                redirect(req, resp, "/manage/rooms" + branchQuery(req));
            }
            default -> {
                Room r = new Room();
                Integer id = intParam(req, "id");
                Integer branchId = intParam(req, "branchId");
                r.setRoomId(id == null ? 0 : id);
                r.setBranchId(branchId == null ? 0 : branchId);
                r.setRoomName(str(req, "roomName"));
                r.setRoomTypeId(intParam(req, "roomTypeId"));
                try {
                    int savedId = roomService.save(r);
                    if (id == null) {
                        flashSuccess(req, "Đã tạo " + r.getRoomName() + ". Tiếp theo, sắp xếp ghế cho phòng.");
                        redirect(req, resp, "/manage/rooms/seats?roomId=" + savedId);
                    } else {
                        flashSuccess(req, "Đã cập nhật " + r.getRoomName() + ".");
                        redirect(req, resp, "/manage/rooms?branchId=" + r.getBranchId());
                    }
                } catch (BusinessException e) {
                    req.setAttribute("errors", e.getErrors());
                    showForm(req, resp, r);
                }
            }
        }
    }

    private void showForm(HttpServletRequest req, HttpServletResponse resp, Room r)
            throws ServletException, IOException, SQLException, BusinessException {
        req.setAttribute("room", r);
        req.setAttribute("branches", branchService.findActive());
        req.setAttribute("allBranches", branchService.list(null, null));
        req.setAttribute("roomTypes", roomTypeService.list());
        render(req, resp, "room/form-hoang");
    }

    private String branchQuery(HttpServletRequest req) {
        Integer b = intParam(req, "branchId");
        return b == null ? "" : "?branchId=" + b;
    }
}
