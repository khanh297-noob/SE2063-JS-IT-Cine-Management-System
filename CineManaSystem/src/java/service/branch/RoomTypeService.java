package service.branch;

import dao.branch.RoomDAO;
import dao.branch.RoomTypeDAO;
import model.branch.RoomType;
import util.BusinessException;
import util.JdbcContext;
import util.NotFoundException;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Configure Room Type.
 *
 * BR-RT-01  Tên loại phòng bắt buộc, tối đa 50 ký tự, không trùng.
 * BR-RT-02  Phụ thu từ 0 đến 1.000.000 đ.
 * BR-RT-03  Loại phòng ngưng dùng không chọn được cho phòng mới; phòng cũ giữ nguyên.
 * BR-RT-04  Chỉ xoá được loại phòng chưa có phòng nào dùng; nếu đã dùng thì ngưng dùng.
 */
public class RoomTypeService {

    private static final BigDecimal MAX_SURCHARGE = new BigDecimal("1000000");

    private final RoomTypeDAO roomTypeDAO = new RoomTypeDAO();
    private final RoomDAO roomDAO = new RoomDAO();

    public List<RoomType> list() throws SQLException, BusinessException {
        return JdbcContext.query(roomTypeDAO::findAll);
    }

    public List<RoomType> findActive() throws SQLException, BusinessException {
        return JdbcContext.query(roomTypeDAO::findActive);
    }

    /** room_type_id -> số phòng đang dùng loại này. */
    public Map<Integer, Integer> countRoomsPerType() throws SQLException, BusinessException {
        return JdbcContext.query(con -> {
            Map<Integer, Integer> map = new HashMap<>();
            for (RoomType t : roomTypeDAO.findAll(con)) {
                map.put(t.getRoomTypeId(), roomDAO.countByRoomType(con, t.getRoomTypeId()));
            }
            return map;
        });
    }

    public RoomType get(int id) throws SQLException, BusinessException {
        RoomType t = JdbcContext.query(con -> roomTypeDAO.findById(con, id));
        if (t == null) throw new NotFoundException("Không tìm thấy loại phòng #" + id);
        return t;
    }

    public int save(RoomType t) throws SQLException, BusinessException {
        t.setName(t.getName() == null ? null : t.getName().trim());
        if (t.getName() != null && t.getName().isEmpty()) t.setName(null);
        if (t.getDescription() != null && t.getDescription().isBlank()) t.setDescription(null);

        return JdbcContext.transaction(con -> {
            List<String> errors = new ArrayList<>();
            if (t.getName() == null) errors.add("Nhập tên loại phòng.");
            else if (t.getName().length() > 50) errors.add("Tên loại phòng tối đa 50 ký tự.");
            else if (roomTypeDAO.existsByName(con, t.getName(), t.getRoomTypeId()))
                errors.add("Đã có loại phòng \"" + t.getName() + "\".");
            if (t.getSurcharge() == null) errors.add("Nhập phụ thu (0 nếu không có).");
            else if (t.getSurcharge().signum() < 0 || t.getSurcharge().compareTo(MAX_SURCHARGE) > 0)
                errors.add("Phụ thu từ 0 đến 1.000.000 đ.");
            if (t.getDescription() != null && t.getDescription().length() > 255)
                errors.add("Mô tả tối đa 255 ký tự.");
            if (!errors.isEmpty()) throw new BusinessException(errors);

            if (t.getRoomTypeId() == 0) {
                t.setStatus(RoomType.STATUS_ACTIVE);
                return roomTypeDAO.insert(con, t);
            }
            if (roomTypeDAO.findById(con, t.getRoomTypeId()) == null)
                throw new NotFoundException("Không tìm thấy loại phòng #" + t.getRoomTypeId());
            roomTypeDAO.update(con, t);
            return t.getRoomTypeId();
        });
    }

    public void changeStatus(int id, String status) throws SQLException, BusinessException {
        if (!RoomType.STATUS_ACTIVE.equals(status) && !RoomType.STATUS_INACTIVE.equals(status))
            throw new BusinessException("Trạng thái không hợp lệ.");
        JdbcContext.transaction(con -> {
            if (roomTypeDAO.findById(con, id) == null) throw new NotFoundException("Không tìm thấy loại phòng #" + id);
            roomTypeDAO.updateStatus(con, id, status);
            return null;
        });
    }

    public void delete(int id) throws SQLException, BusinessException {
        JdbcContext.transaction(con -> {
            RoomType t = roomTypeDAO.findById(con, id);
            if (t == null) throw new NotFoundException("Không tìm thấy loại phòng #" + id);
            int used = roomDAO.countByRoomType(con, id);
            if (used > 0)
                throw new BusinessException("Loại phòng \"" + t.getName() + "\" đang được " + used
                        + " phòng sử dụng. Hãy ngưng dùng thay vì xoá.");
            roomTypeDAO.delete(con, id);
            return null;
        });
    }
}
