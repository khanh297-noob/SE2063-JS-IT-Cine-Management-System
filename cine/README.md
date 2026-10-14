# Cine Management System – Branch Management + Movie Management

Servlet + JSP thuần, JDBC tới SQL Server, chạy bằng Tomcat embedded (IntelliJ).
Chưa có Login: mọi trang vào tự do qua URL.

## Chạy

1. Chạy `database/01_schema.sql`, rồi `database/02_seed.sql` (dữ liệu mẫu) trên DB `Cine Management System`.
   DB đã tạo bằng bản schema cũ: chạy thêm `database/03_migrate_seat_coords.sql` (thêm `Seats.row_num/col_num`, bỏ `Movies.original_title`).
2. IntelliJ: File > Open > thư mục chứa `pom.xml`, đợi Maven tải thư viện.
3. Sửa user/password trong `util/DBContext.java`, hoặc thêm VM options:
   `-Ddb.user=sa -Ddb.password=matkhau`
4. Chạy `src/main/java/cinemanagement/Main.java`, mở http://localhost:8080/

## Chức năng → file

| Chức năng | URL | Servlet | Service | JSP |
|---|---|---|---|---|
| Manage Branch information | /manage/branches | BranchServlet | BranchService | branch/list, branch/form |
| View Branch Details | /branch?id= | BranchDetailServlet | BranchService | branch/detail |
| Manage Operating Hours | /manage/branches/hours?branchId= | OperatingHourServlet | OperatingHourService | branch/operating-hours |
| Room Management | /manage/rooms | RoomServlet | RoomService | room/list, room/form |
| Configure Room Type | /manage/room-types | RoomTypeServlet | RoomTypeService | room-type/list, room-type/form |
| Configure Seat Matrix | /manage/rooms/seats?roomId= | SeatMatrixServlet | SeatMatrixService | room/seat-matrix |
| Manage Poster/Trailer | /manage/movies/media?movieId= | MovieMediaServlet | MovieMediaService | movie/media |
| Manage Genre | /manage/genres | GenreServlet | GenreService | genre/list, genre/form |
| Manage Movie | /manage/movies | MovieServlet | MovieService | movie/list, movie/form |
| Movie Status | /manage/movies/status?movieId= | MovieStatusServlet | MovieStatusService | movie/status |
| Configure Available Branches | /manage/movies/branches?movieId= | MovieBranchServlet | MovieBranchService | movie/branches |

Quy tắc nghiệp vụ ghi ở đầu mỗi file Service (mã BR-xx-nn) để đưa vào SRS.

## Bảng → Model → DAO

| Bảng | Model | DAO |
|---|---|---|
| Branches | Branch | BranchDAO |
| BranchOperatingHours | BranchOperatingHour | BranchOperatingHourDAO |
| RoomTypes | RoomType | RoomTypeDAO |
| Rooms | Room | RoomDAO |
| SeatTypes | SeatType | SeatTypeDAO |
| Seats | Seat | SeatDAO |
| Genres | Genre | GenreDAO |
| Movies | Movie | MovieDAO |
| MovieGenres | MovieGenre | MovieGenreDAO |
| MovieMedia | MovieMedia | MovieMediaDAO |
| MovieBranches | MovieBranch | MovieBranchDAO |
| Showtimes (module khác, chỉ đọc) | – | ShowtimeDAO |

Các class `*DTO` trong `model/` chứa dữ liệu ghép từ nhiều bảng cho một màn hình (vd RoomSummaryDTO = phòng + tên chi nhánh + số ghế).
`BusinessException`, `NotFoundException` nằm trong `util/`.
`scheduler/` (tác vụ định kỳ) và `thirdparty/` (tích hợp bên ngoài) đã được tạo sẵn theo package diagram.

Quy tắc sơ đồ ghế (kiểm tra ở `SeatLayoutValidator` và lặp lại trong `seat-matrix.js`):
- Lối đi phải liền nhau (theo 4 hướng) và thông ra ít nhất một trong 4 góc phòng; kiểm tra bằng BFS từ các góc.
- Ghế loại Sweetbox phải đi theo cặp: mỗi dãy Sweetbox liền nhau trong một hàng có số ghế chẵn.

Tìm kiếm phim, chi nhánh không phân biệt dấu (`TextUtil.fold`: "da nang" khớp "Đà Nẵng").

## Kiến trúc

- DAO nhận `Connection` từ Service, không tự mở kết nối.
  Service dùng `DBContext.query(...)` để đọc và `DBContext.transaction(...)` để ghi,
  nên một nghiệp vụ dùng nhiều DAO vẫn nằm trong một transaction (commit/rollback cùng nhau).
- Servlet kế thừa `BaseServlet`: đọc tham số, render JSP, flash message, 404 khi không tìm thấy.
- `EncodingFilter` đặt UTF-8 cho mọi request (thiếu thì tiếng Việt từ form bị lỗi).
- JSP nằm trong `WEB-INF/views` nên không truy cập trực tiếp được, chỉ qua Servlet.

## Cho module Showtime

Trước khi tạo suất chiếu, gọi:
`new MovieBranchService().isMovieAvailableAt(movieId, branchId, showDate)`
