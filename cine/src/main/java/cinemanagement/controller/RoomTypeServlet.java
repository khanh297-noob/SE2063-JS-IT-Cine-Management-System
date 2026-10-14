package cinemanagement.controller;

import cinemanagement.util.BusinessException;
import cinemanagement.model.RoomType;
import cinemanagement.service.RoomTypeService;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;

/**
 * Configure Room Type.
 * GET  /manage/room-types[?action=create|edit&id=]
 * POST action=save | status | delete
 */
@WebServlet("/manage/room-types")
public class RoomTypeServlet extends BaseServlet {

    private final RoomTypeService roomTypeService = new RoomTypeService();

    @Override
    protected void handleGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException, SQLException, BusinessException {
        switch (action(req, "list")) {
            case "create" -> showForm(req, resp, new RoomType());
            case "edit" -> showForm(req, resp, roomTypeService.get(requiredId(req, "id")));
            default -> {
                req.setAttribute("roomTypes", roomTypeService.list());
                req.setAttribute("roomCounts", roomTypeService.countRoomsPerType());
                render(req, resp, "room-type/list");
            }
        }
    }

    @Override
    protected void handlePost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException, SQLException, BusinessException {
        switch (action(req, "save")) {
            case "status" -> {
                roomTypeService.changeStatus(requiredId(req, "id"), str(req, "status"));
                flashSuccess(req, "Đã cập nhật trạng thái loại phòng.");
                redirect(req, resp, "/manage/room-types");
            }
            case "delete" -> {
                roomTypeService.delete(requiredId(req, "id"));
                flashSuccess(req, "Đã xoá loại phòng.");
                redirect(req, resp, "/manage/room-types");
            }
            default -> {
                RoomType t = new RoomType();
                Integer id = intParam(req, "id");
                t.setRoomTypeId(id == null ? 0 : id);
                t.setName(str(req, "name"));
                t.setSurcharge(decimalParam(req, "surcharge"));
                t.setDescription(str(req, "description"));
                try {
                    roomTypeService.save(t);
                    flashSuccess(req, (id == null ? "Đã thêm loại phòng " : "Đã cập nhật loại phòng ") + t.getName() + ".");
                    redirect(req, resp, "/manage/room-types");
                } catch (BusinessException e) {
                    req.setAttribute("errors", e.getErrors());
                    showForm(req, resp, t);
                }
            }
        }
    }

    private void showForm(HttpServletRequest req, HttpServletResponse resp, RoomType t)
            throws ServletException, IOException {
        req.setAttribute("roomType", t);
        render(req, resp, "room-type/form");
    }
}
