/* Dữ liệu mẫu để test. Chạy sau 01_schema.sql trên DB trống. */

SET NOCOUNT ON;

INSERT INTO dbo.Branches (name, address, city, hotline, email) VALUES
 (N'CineGo Cầu Giấy',  N'241 Xuân Thủy',          N'Hà Nội',        '0241234567', 'caugiay@cinego.vn'),
 (N'CineGo Long Biên', N'7 Nguyễn Văn Linh',      N'Hà Nội',        '0247654321', 'longbien@cinego.vn'),
 (N'CineGo Hải Châu',  N'255 Hùng Vương',         N'Đà Nẵng',       '02363888999', NULL),
 (N'CineGo Quận 1',    N'135 Hai Bà Trưng',       N'TP. Hồ Chí Minh', '19001234', 'q1@cinego.vn');

UPDATE dbo.Branches SET status = 'Inactive' WHERE name = N'CineGo Hải Châu';

/* Giờ mở cửa: T2-T5 9:00-23:00, T6-CN 8:00-01:00 (qua đêm) */
INSERT INTO dbo.BranchOperatingHours (branch_id, day_of_week, is_closed, open_time, close_time)
SELECT b.branch_id, d.n, 0,
       CASE WHEN d.n >= 5 THEN '08:00' ELSE '09:00' END,
       CASE WHEN d.n >= 5 THEN '01:00' ELSE '23:00' END
FROM dbo.Branches b
CROSS JOIN (VALUES (1),(2),(3),(4),(5),(6),(7)) d(n);

INSERT INTO dbo.RoomTypes (name, surcharge, description) VALUES
 (N'2D',   0,     N'Phòng chiếu tiêu chuẩn'),
 (N'3D',   30000, N'Kính 3D phân cực'),
 (N'IMAX', 70000, N'Màn hình cỡ lớn, âm thanh 12 kênh');

INSERT INTO dbo.SeatTypes (name, surcharge, color) VALUES
 (N'Standard', 0,     '#7d8299'),
 (N'VIP',      20000, '#b7791f'),
 (N'Sweetbox', 50000, '#a3263b');

INSERT INTO dbo.Rooms (branch_id, room_type_id, room_name, total_rows, total_columns)
SELECT b.branch_id, t.room_type_id, r.room_name, 8, 12
FROM dbo.Branches b
CROSS JOIN (VALUES (N'Phòng 1', N'2D'), (N'Phòng 2', N'3D')) r(room_name, type_name)
JOIN dbo.RoomTypes t ON t.name = r.type_name;

/* Ma trận ghế mẫu: 8 hàng x 12 cột, lối đi hai bên ở cột 1 và 12 (chạm cả 4 góc phòng),
   hàng D-F là VIP, hàng H là Sweetbox (10 ghế liền nhau = 5 cặp). */
DECLARE @std INT = (SELECT seat_type_id FROM dbo.SeatTypes WHERE name = N'Standard');
DECLARE @vip INT = (SELECT seat_type_id FROM dbo.SeatTypes WHERE name = N'VIP');
DECLARE @swb INT = (SELECT seat_type_id FROM dbo.SeatTypes WHERE name = N'Sweetbox');

;WITH rows_cte AS (
    SELECT v.idx, CHAR(64 + v.idx) AS label FROM (VALUES (1),(2),(3),(4),(5),(6),(7),(8)) v(idx)
), cols_cte AS (
    SELECT v.c FROM (VALUES (2),(3),(4),(5),(6),(7),(8),(9),(10),(11)) v(c)
)
INSERT INTO dbo.Seats (room_id, seat_type_id, row_num, col_num, row_label, seat_number)
SELECT r.room_id,
       CASE WHEN rw.idx = 8 THEN @swb WHEN rw.idx BETWEEN 4 AND 6 THEN @vip ELSE @std END,
       rw.idx, c.c, rw.label, c.c
FROM dbo.Rooms r CROSS JOIN rows_cte rw CROSS JOIN cols_cte c;

INSERT INTO dbo.Genres (name, description) VALUES
 (N'Hành động', NULL), (N'Hài', NULL), (N'Kinh dị', NULL),
 (N'Hoạt hình', NULL), (N'Tâm lý', NULL), (N'Khoa học viễn tưởng', NULL);

INSERT INTO dbo.Movies (title, description, director, language, duration, release_date, age_rating, status) VALUES
 (N'Mùa Hè Ở Hội An', N'Phim mẫu để test.', N'Nguyễn Văn A', N'Tiếng Việt', 112, DATEADD(DAY, -10, CAST(GETDATE() AS DATE)), 'T13', 'Now Showing'),
 (N'Trạm Không Gian Cuối', N'Phim mẫu để test.', N'J. Doe', N'Tiếng Anh', 136, DATEADD(DAY, 14, CAST(GETDATE() AS DATE)), 'T16', 'Coming Soon'),
 (N'Chú Mèo Đi Lạc', N'Phim mẫu để test.', N'Trần Thị B', N'Tiếng Việt', 88, DATEADD(DAY, -60, CAST(GETDATE() AS DATE)), 'P', 'End of Screening');

INSERT INTO dbo.MovieGenres (movie_id, genre_id)
SELECT m.movie_id, g.genre_id FROM dbo.Movies m JOIN dbo.Genres g
  ON (m.title = N'Mùa Hè Ở Hội An'     AND g.name IN (N'Tâm lý', N'Hài'))
  OR (m.title = N'Trạm Không Gian Cuối' AND g.name IN (N'Khoa học viễn tưởng', N'Hành động'))
  OR (m.title = N'Chú Mèo Đi Lạc'       AND g.name IN (N'Hoạt hình'));

INSERT INTO dbo.MovieBranches (movie_id, branch_id)
SELECT m.movie_id, b.branch_id FROM dbo.Movies m CROSS JOIN dbo.Branches b
WHERE m.title = N'Mùa Hè Ở Hội An' AND b.status = 'Active';
