package cinemanagement.service.branch;

import cinemanagement.dao.ShowtimeDAO;
import cinemanagement.dao.branch.BranchDAO;
import cinemanagement.dao.branch.RoomDAO;
import cinemanagement.dao.branch.RoomTypeDAO;
import cinemanagement.model.branch.Branch;
import cinemanagement.model.branch.Room;
import cinemanagement.model.branch.RoomSummaryDTO;
import cinemanagement.model.branch.RoomType;
import cinemanagement.util.BusinessException;
import cinemanagement.util.DBContext;
import cinemanagement.util.JdbcUtil;
import cinemanagement.util.NotFoundException;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Room Management.
 *
 * BR-RM-01  Tên phòng bắt buộc, tối đa 50 ký tự, không trùng trong cùng chi nhánh.
 * BR-RM-02  Phòng mới chỉ tạo ở chi nhánh đang hoạt động, với loại phòng đang dùng.
 * BR-RM-03  Không đổi chi nhánh của phòng sau khi tạo.
 * BR-RM-04  Không chuyển sang Bảo trì / Ngưng khi còn suất chiếu sắp tới.
 * BR-RM-05  Chỉ xoá phòng chưa từng có suất chiếu; ngược lại hãy chuyển Ngưng.
 */
public class RoomService {

    private static final Set<String> STATUSES =
            Set.of(Room.STATUS_ACTIVE, Room.STATUS_MAINTENANCE, Room.STATUS_INACTIVE);

    private final RoomDAO roomDAO = new RoomDAO();
    private final BranchDAO branchDAO = new BranchDAO();
    private final RoomTypeDAO roomTypeDAO = new RoomTypeDAO();
    private final ShowtimeDAO showtimeDAO = new ShowtimeDAO();

    public List<RoomSummaryDTO> list(Integer branchId) throws SQLException, BusinessException {
        return DBContext.query(con -> roomDAO.findSummaries(con, branchId));
    }

    public Room get(int roomId) throws SQLException, BusinessException {
        Room r = DBContext.query(con -> roomDAO.findById(con, roomId));
        if (r == null) throw new NotFoundException("Không tìm thấy phòng #" + roomId);
        return r;
    }

    public int save(Room r) throws SQLException, BusinessException {
        if (r.getRoomName() != null) r.setRoomName(r.getRoomName().trim().replaceAll("\\s+", " "));
        if (r.getRoomName() != null && r.getRoomName().isEmpty()) r.setRoomName(null);

        return DBContext.transaction(con -> {
            List<String> errors = new ArrayList<>();
            Room existing = null;
            if (r.getRoomId() != 0) {
                existing = roomDAO.findById(con, r.getRoomId());
                if (existing == null) throw new NotFoundException("Không tìm thấy phòng #" + r.getRoomId());
                r.setBranchId(existing.getBranchId()); // BR-RM-03
            } else {
                Branch b = branchDAO.findById(con, r.getBranchId());
                if (b == null) errors.add("Chọn chi nhánh.");
                else if (!b.isActive()) errors.add("Chi nhánh " + b.getName() + " đang ngưng hoạt động.");
            }

            if (r.getRoomName() == null) errors.add("Nhập tên phòng.");
            else if (r.getRoomName().length() > 50) errors.add("Tên phòng tối đa 50 ký tự.");
            else if (roomDAO.existsByName(con, r.getBranchId(), r.getRoomName(), r.getRoomId()))
                errors.add("Chi nhánh đã có phòng tên \"" + r.getRoomName() + "\".");

            if (r.getRoomTypeId() == null) {
                errors.add("Chọn loại phòng.");
            } else {
                RoomType t = roomTypeDAO.findById(con, r.getRoomTypeId());
                boolean unchanged = existing != null && r.getRoomTypeId().equals(existing.getRoomTypeId());
                if (t == null) errors.add("Loại phòng không tồn tại.");
                else if (!t.isActive() && !unchanged) errors.add("Loại phòng " + t.getName() + " đã ngưng dùng.");
            }
            if (!errors.isEmpty()) throw new BusinessException(errors);

            if (existing == null) {
                r.setStatus(Room.STATUS_ACTIVE);
                r.setTotalRows(0);
                r.setTotalColumns(0);
                return roomDAO.insert(con, r);
            }
            roomDAO.update(con, r);
            return r.getRoomId();
        });
    }

    public void changeStatus(int roomId, String status) throws SQLException, BusinessException {
        if (!STATUSES.contains(status)) throw new BusinessException("Trạng thái không hợp lệ.");
        DBContext.transaction(con -> {
            Room r = roomDAO.findById(con, roomId);
            if (r == null) throw new NotFoundException("Không tìm thấy phòng #" + roomId);
            if (!Room.STATUS_ACTIVE.equals(status)) {
                int upcoming = showtimeDAO.countUpcomingByRoom(con, roomId);
                if (upcoming > 0)
                    throw new BusinessException(r.getRoomName() + " còn " + upcoming
                            + " suất chiếu sắp tới. Chuyển các suất chiếu sang phòng khác trước.");
            }
            roomDAO.updateStatus(con, roomId, status);
            return null;
        });
    }

    public void delete(int roomId) throws SQLException, BusinessException {
        DBContext.transaction(con -> {
            Room r = roomDAO.findById(con, roomId);
            if (r == null) throw new NotFoundException("Không tìm thấy phòng #" + roomId);
            if (showtimeDAO.countByRoom(con, roomId) > 0)
                throw new BusinessException(r.getRoomName() + " đã có lịch sử suất chiếu nên không xoá được. "
                        + "Hãy chuyển sang Ngưng hoạt động.");
            try {
                roomDAO.delete(con, roomId);
            } catch (SQLException e) {
                if (JdbcUtil.isForeignKeyViolation(e))
                    throw new BusinessException(r.getRoomName() + " đang được dữ liệu khác tham chiếu (vé, đặt chỗ).");
                throw e;
            }
            return null;
        });
    }
}
