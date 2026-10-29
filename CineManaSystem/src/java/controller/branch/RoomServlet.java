package controller.branch;

import model.branch.Room;
import service.branch.BranchService;
import service.branch.RoomService;
import service.branch.RoomTypeService;
import util.BusinessException;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

/**
 * Room Management.
 * GET  /manage/rooms[?branchId=]                 danh sách
 * GET  /manage/rooms?action=create[&branchId=]   form thêm
 * GET  /manage/rooms?action=edit&id=             form sửa
 * POST action=save | status | delete
 * Quản lý chi nhánh: chi nhánh luôn cố định là chi nhánh của mình (bỏ qua branchId gửi lên),
 * chỉ thao tác được với phòng thuộc chi nhánh đó.
 */
@WebServlet("/manage/rooms")
public class RoomServlet extends BranchScopedServlet {

    private final RoomService roomService = new RoomService();
    private final BranchService branchService = new BranchService();
    private final RoomTypeService roomTypeService = new RoomTypeService();

    @Override
    protected void handleGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException, SQLException, BusinessException {
        Integer managed = managedBranchId(req);
        switch (action(req, "list")) {
            case "create" -> {
                Room r = new Room();
                Integer branchId = managed != null ? managed : intParam(req, "branchId");
                if (branchId != null) r.setBranchId(branchId);
                showForm(req, resp, r);
            }
            case "edit" -> showForm(req, resp, roomInScope(req, requiredId(req, "id")));
            default -> {
                Integer branchId = managed != null ? managed : intParam(req, "branchId");
                req.setAttribute("rooms", roomService.list(branchId));
                req.setAttribute("branches", managed != null
                        ? List.of(branchService.get(managed))
                        : branchService.list(null, null));
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
                int id = roomInScope(req, requiredId(req, "id")).getRoomId();
                roomService.changeStatus(id, str(req, "status"));
                flashSuccess(req, "Đã cập nhật trạng thái phòng.");
                redirect(req, resp, "/manage/rooms" + branchQuery(req));
            }
            case "delete" -> {
                int id = roomInScope(req, requiredId(req, "id")).getRoomId();
                roomService.delete(id);
                flashSuccess(req, "Đã xoá phòng.");
                redirect(req, resp, "/manage/rooms" + branchQuery(req));
            }
            default -> {
                Room r = new Room();
                Integer id = intParam(req, "id");
                Integer branchId;
                if (id != null) {
                    branchId = roomInScope(req, id).getBranchId(); // phòng đã tạo không đổi chi nhánh (BR-RM-03)
                } else {
                    Integer managed = managedBranchId(req);
                    branchId = managed != null ? managed : intParam(req, "branchId");
                }
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

    /** Lấy phòng theo id, đồng thời kiểm tra người dùng có quyền với chi nhánh của phòng. */
    private Room roomInScope(HttpServletRequest req, int roomId) throws SQLException, BusinessException {
        Room room = roomService.get(roomId);
        requireBranch(req, room.getBranchId());
        return room;
    }

    private void showForm(HttpServletRequest req, HttpServletResponse resp, Room r)
            throws ServletException, IOException, SQLException, BusinessException {
        Integer managed = managedBranchId(req);
        req.setAttribute("room", r);
        req.setAttribute("branches", managed != null ? List.of(branchService.get(managed)) : branchService.findActive());
        req.setAttribute("allBranches", managed != null ? List.of(branchService.get(managed)) : branchService.list(null, null));
        req.setAttribute("roomTypes", roomTypeService.list());
        render(req, resp, "room/form-hoang");
    }

    private String branchQuery(HttpServletRequest req) {
        Integer b = intParam(req, "branchId");
        return b == null ? "" : "?branchId=" + b;
    }
}
