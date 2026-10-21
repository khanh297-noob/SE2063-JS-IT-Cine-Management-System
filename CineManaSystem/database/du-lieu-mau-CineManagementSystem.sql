/* =====================================================================
   DỮ LIỆU MẪU cho database mới CineManagementSystem (chép lại từ DB cũ CineSystemDataBase)
   Chạy SAU file tạo bảng CineSystemDataBase.sql của nhóm.
   - Chạy lại nhiều lần không bị trùng (mọi INSERT đều kiểm tra đã có chưa).
   - Suất chiếu tạo cho 20 ngày tính từ ngày chạy script.
   Tài khoản test (mật khẩu đều là 123456):
     admin@cine.vn, manager@cine.vn, cashier@cine.vn, checker@cine.vn, customer2@gmail.com
   ===================================================================== */
USE CineManagementSystem;
GO
SET NOCOUNT ON;
SET XACT_ABORT ON;
SET ANSI_NULLS ON;
SET QUOTED_IDENTIFIER ON;
GO

BEGIN TRAN;

/* ---------- 1. Chi nhánh + giờ mở cửa ---------- */
INSERT INTO Branch (Name, Address, City, Hotline, Email, Status)
SELECT v.Name, v.Address, v.City, v.Hotline, v.Email, 'Active'
FROM (VALUES
    (N'Cine Star Hà Đông',  N'Tầng 4, TTTM AEON Mall Hà Đông, Dương Nội', N'Hà Nội', '02473001234', 'hadong@cine.vn'),
    (N'Cine Star Cầu Giấy', N'Số 122 Xuân Thủy, Dịch Vọng Hậu',           N'Hà Nội', '02473005678', 'caugiay@cine.vn')
) v(Name, Address, City, Hotline, Email)
WHERE NOT EXISTS (SELECT 1 FROM Branch b WHERE b.Name = v.Name);

DECLARE @hd INT = (SELECT BranchID FROM Branch WHERE Name = N'Cine Star Hà Đông');
DECLARE @cg INT = (SELECT BranchID FROM Branch WHERE Name = N'Cine Star Cầu Giấy');

-- Mở cửa cả tuần 08:00 - 00:30 (đóng cửa sau nửa đêm)
INSERT INTO BranchOperatingHour (BranchID, DayOfWeek, IsClosed, OpenTime, CloseTime)
SELECT b.BranchID, d.n, 0, '08:00', '00:30'
FROM Branch b CROSS JOIN (VALUES (1),(2),(3),(4),(5),(6),(7)) d(n)
WHERE NOT EXISTS (SELECT 1 FROM BranchOperatingHour h WHERE h.BranchID = b.BranchID AND h.DayOfWeek = d.n);

/* ---------- 2. Người dùng (nhân viên gắn chi nhánh Hà Đông) ---------- */
INSERT INTO [User] (FullName, Email, Phone, PasswordHash, [Role], [Status], BranchID)
-- Mật khẩu 123456 đã mã hóa BCrypt (trang /login của nhóm kiểm tra bằng PasswordUtil.verify)
SELECT v.FullName, v.Email, v.Phone, '$2a$12$4SD/DonB7GvibR8Wmiz9Y.tVVVzYNSgO1.hxGgyPLrlJEBNiZTgru', v.Role, v.Status, v.BranchID
FROM (VALUES
    (N'Nguyễn Văn Admin',  'admin@cine.vn',       '0901112222', 'Administrator',     'Active', NULL),
    (N'Trần Thị Quản Lý',  'manager@cine.vn',     '0903334444', 'Branch Management', 'Active', @hd),
    (N'Lê Thu Ngân',       'cashier@cine.vn',     '0905556666', 'Ticket Cashier',    'Active', @hd),
    (N'Phạm Khách Hàng',   'customer@gmail.com',  '0988999000', 'Customer',          'Locked', NULL),
    (N'Võ Soát Vé',        'checker@cine.vn',     '0907778888', 'Ticket Checker',    'Active', @hd),
    (N'Đỗ Khách Test',     'customer2@gmail.com', '0911222333', 'Customer',          'Active', NULL)
) v(FullName, Email, Phone, Role, Status, BranchID)
WHERE NOT EXISTS (SELECT 1 FROM [User] u WHERE u.Email = v.Email);

/* ---------- 3. Loại phòng, loại ghế (phụ thu cấu hình ở đây, code tự đọc) ---------- */
INSERT INTO RoomType (Name, Surcharge, Description)
SELECT v.Name, v.Surcharge, v.Description
FROM (VALUES
    (N'2D',   0,     N'Phòng chiếu tiêu chuẩn'),
    (N'3D',   20000, N'Phòng chiếu 3D'),
    (N'IMAX', 40000, N'Màn hình IMAX cỡ lớn')
) v(Name, Surcharge, Description)
WHERE NOT EXISTS (SELECT 1 FROM RoomType r WHERE r.Name = v.Name);

INSERT INTO SeatType (Name, Surcharge, Color)
SELECT v.Name, v.Surcharge, v.Color
FROM (VALUES
    (N'Standard', 0,     '#8a8fa8'),
    (N'VIP',      20000, '#f59e0b'),
    (N'Sweetbox', 50000, '#ec4899')
) v(Name, Surcharge, Color)
WHERE NOT EXISTS (SELECT 1 FROM SeatType s WHERE s.Name = v.Name);

/* ---------- 4. Phòng chiếu ---------- */
INSERT INTO Room (BranchID, RoomName, RoomType, Capacity, TotalRows, TotalColumns, Status)
SELECT v.BranchID, v.RoomName, v.RoomType, v.Capacity, v.TotalRows, v.TotalColumns, 'Active'
FROM (VALUES
    (@hd, N'Cinema Room 01 (IMAX)',     N'IMAX', 54, 6, 10),
    (@hd, N'Cinema Room 02 (Standard)', N'2D',   40, 5, 8),
    (@cg, N'Cinema Room 01 (3D Laser)', N'3D',   40, 5, 8)
) v(BranchID, RoomName, RoomType, Capacity, TotalRows, TotalColumns)
WHERE NOT EXISTS (SELECT 1 FROM Room r WHERE r.BranchID = v.BranchID AND r.RoomName = v.RoomName);

DECLARE @r1 INT = (SELECT RoomID FROM Room WHERE BranchID = @hd AND RoomName = N'Cinema Room 01 (IMAX)');
DECLARE @r2 INT = (SELECT RoomID FROM Room WHERE BranchID = @hd AND RoomName = N'Cinema Room 02 (Standard)');
DECLARE @r3 INT = (SELECT RoomID FROM Room WHERE BranchID = @cg AND RoomName = N'Cinema Room 01 (3D Laser)');

/* ---------- 5. Ghế: hàng A, B, C... (RowNum 1, 2, 3...), số ghế 1..N (ColNum) ----------
   Phòng IMAX : A-E x 10 ghế (D, E là VIP) + hàng F 4 ghế Sweetbox  = 54 ghế
   Phòng 2D   : A-E x 8 ghế (hàng E là VIP)                         = 40 ghế
   Phòng 3D   : A-E x 8 ghế (hàng D, E là VIP)                      = 40 ghế */
DECLARE @seatPlan TABLE (RoomID INT, RowNum INT, SeatsInRow INT, SeatType NVARCHAR(30));
INSERT INTO @seatPlan VALUES
    (@r1, 1, 10, N'Standard'), (@r1, 2, 10, N'Standard'), (@r1, 3, 10, N'Standard'),
    (@r1, 4, 10, N'VIP'),      (@r1, 5, 10, N'VIP'),      (@r1, 6, 4,  N'Sweetbox'),
    (@r2, 1, 8, N'Standard'), (@r2, 2, 8, N'Standard'), (@r2, 3, 8, N'Standard'),
    (@r2, 4, 8, N'Standard'), (@r2, 5, 8, N'VIP'),
    (@r3, 1, 8, N'Standard'), (@r3, 2, 8, N'Standard'), (@r3, 3, 8, N'Standard'),
    (@r3, 4, 8, N'VIP'),      (@r3, 5, 8, N'VIP');

;WITH so AS (SELECT TOP (30) ROW_NUMBER() OVER (ORDER BY (SELECT NULL)) AS n FROM sys.all_objects)
INSERT INTO Seat (RoomID, SeatType, RowLabel, SeatNumber, RowNum, ColNum, Status)
SELECT p.RoomID, p.SeatType, CHAR(64 + p.RowNum), so.n, p.RowNum, so.n, 'Active'
FROM @seatPlan p JOIN so ON so.n <= p.SeatsInRow
WHERE NOT EXISTS (SELECT 1 FROM Seat s WHERE s.RoomID = p.RoomID AND s.RowNum = p.RowNum AND s.ColNum = so.n);

-- 1 ghế hỏng để test (phòng 2D, ghế C8)
UPDATE Seat SET Status = 'Broken' WHERE RoomID = @r2 AND RowLabel = 'C' AND SeatNumber = 8;

/* ---------- 6. Thể loại + phim ---------- */
INSERT INTO Genre (Name)
SELECT v.Name FROM (VALUES (N'Hành động'), (N'Khoa học viễn tưởng'), (N'Phiêu lưu'),
                           (N'Tâm lý'), (N'Tình cảm'), (N'Hoạt hình'), (N'Kinh dị'), (N'Hài')) v(Name)
WHERE NOT EXISTS (SELECT 1 FROM Genre g WHERE g.Name = v.Name);

DECLARE @movies TABLE (Title NVARCHAR(200), Duration INT, ReleaseDate DATE, AgeRating VARCHAR(10), Genres NVARCHAR(200));
INSERT INTO @movies VALUES
    (N'Avatar: Dòng Chảy Của Nước',                 192, '2025-12-15', 'T13', N'Hành động,Khoa học viễn tưởng,Phiêu lưu'),
    (N'Mai',                                        131, '2026-02-10', 'T18', N'Tâm lý,Tình cảm'),
    (N'Lật Mặt 8',                                  120, '2026-10-01', 'T13', N'Hành động,Tâm lý'),
    (N'Biệt Đội Săn Bão',                           125, '2026-10-15', 'T13', N'Hành động,Phiêu lưu'),
    (N'Ngày Mai Nắng Lên',                          110, '2026-10-15', 'P',   N'Tâm lý,Tình cảm'),
    (N'Hành Tinh Xanh',                             100, '2026-10-15', 'K',   N'Khoa học viễn tưởng,Phiêu lưu'),
    (N'Doraemon: Nobita và Bản Giao Hưởng Địa Cầu', 115, '2026-09-20', 'P',   N'Hoạt hình,Phiêu lưu'),
    (N'Những Mảnh Ghép Cảm Xúc 2',                   97, '2026-09-25', 'P',   N'Hoạt hình,Tâm lý'),
    (N'Kung Fu Panda 4',                             94, '2026-10-01', 'P',   N'Hoạt hình,Hài'),
    (N'Moana 2: Hành Trình Của Moana',              100, '2026-10-05', 'P',   N'Hoạt hình,Phiêu lưu'),
    (N'Godzilla x Kong: Đế Chế Mới',                115, '2026-09-28', 'T13', N'Hành động,Khoa học viễn tưởng'),
    (N'Đất Rừng Phương Nam',                        110, '2026-10-02', 'T13', N'Phiêu lưu,Tâm lý'),
    (N'Lật Mặt 7: Một Điều Ước',                    138, '2026-10-08', 'K',   N'Tâm lý,Tình cảm'),
    (N'Nhà Bà Nữ',                                  102, '2026-10-10', 'T16', N'Tâm lý,Hài'),
    (N'Deadpool & Wolverine',                       128, '2026-10-12', 'T18', N'Hành động,Hài'),
    (N'Quỷ Cẩu',                                    105, '2026-10-14', 'T18', N'Kinh dị');

INSERT INTO Movie (Title, Duration, ReleaseDate, AgeRating, Status)
SELECT m.Title, m.Duration, m.ReleaseDate, m.AgeRating, 'Now Showing'
FROM @movies m
WHERE NOT EXISTS (SELECT 1 FROM Movie x WHERE x.Title = m.Title);

-- Thể loại của từng phim (tách chuỗi Genres theo dấu phẩy)
INSERT INTO MovieGenre (MovieID, GenreID)
SELECT mv.MovieID, g.GenreID
FROM @movies m
JOIN Movie mv ON mv.Title = m.Title
CROSS APPLY STRING_SPLIT(m.Genres, ',') s
JOIN Genre g ON g.Name = LTRIM(RTRIM(s.value))
WHERE NOT EXISTS (SELECT 1 FROM MovieGenre x WHERE x.MovieID = mv.MovieID AND x.GenreID = g.GenreID);

-- Phim chiếu ở cả 2 chi nhánh
INSERT INTO MovieBranch (MovieID, BranchID, AvailableFrom)
SELECT mv.MovieID, b.BranchID, mv.ReleaseDate
FROM Movie mv CROSS JOIN Branch b
WHERE mv.Title IN (SELECT Title FROM @movies)
  AND NOT EXISTS (SELECT 1 FROM MovieBranch x WHERE x.MovieID = mv.MovieID AND x.BranchID = b.BranchID);

/* ---------- 7. Suất chiếu 20 ngày (hôm nay + 19 ngày) ----------
   Phòng IMAX: 09 12 15 18 21h | Phòng 2D: 10 13 16 19 22h | Phòng 3D: 11 14 17 20h
   Giá gốc: trước 17h 75.000, từ 17h 95.000, T7/CN +10.000. Phim > 150 phút không xếp vào khung 3 tiếng. */
DECLARE @slots TABLE (RoomID INT, SlotNo INT, StartTime TIME(0));
INSERT INTO @slots VALUES
    (@r1, 1, '09:00'), (@r1, 2, '12:00'), (@r1, 3, '15:00'), (@r1, 4, '18:00'), (@r1, 5, '21:00'),
    (@r2, 1, '10:00'), (@r2, 2, '13:00'), (@r2, 3, '16:00'), (@r2, 4, '19:00'), (@r2, 5, '22:00'),
    (@r3, 1, '11:00'), (@r3, 2, '14:00'), (@r3, 3, '17:00'), (@r3, 4, '20:00');

DECLARE @soPhim INT = (SELECT COUNT(*) FROM Movie WHERE Title IN (SELECT Title FROM @movies) AND Duration <= 150);

;WITH phim AS (
    SELECT MovieID, Duration, ROW_NUMBER() OVER (ORDER BY MovieID) - 1 AS idx
    FROM Movie WHERE Title IN (SELECT Title FROM @movies) AND Duration <= 150
),
ngay AS (SELECT TOP (20) ROW_NUMBER() OVER (ORDER BY (SELECT NULL)) - 1 AS n FROM sys.all_objects),
lich AS (
    SELECT DATEADD(DAY, d.n, CAST(GETDATE() AS DATE)) AS ShowDate, s.RoomID, s.StartTime, p.MovieID, p.Duration,
           CAST(DATEADD(DAY, d.n, CAST(GETDATE() AS DATE)) AS DATETIME) + CAST(s.StartTime AS DATETIME) AS StartDT
    FROM ngay d CROSS JOIN @slots s
    JOIN phim p ON p.idx = (d.n + s.SlotNo * 3 + s.RoomID * 7) % @soPhim
)
INSERT INTO Showtime (MovieID, RoomID, ShowDate, StartTime, EndTime, BasePrice, Status)
SELECT l.MovieID, l.RoomID, l.ShowDate, l.StartTime,
       CAST(DATEADD(MINUTE, l.Duration, l.StartDT) AS TIME(0)),
       CASE WHEN l.StartTime >= '17:00' THEN 95000 ELSE 75000 END
       + CASE WHEN DATEDIFF(DAY, '19000101', l.ShowDate) % 7 IN (5, 6) THEN 10000 ELSE 0 END,
       'Open'
FROM lich l
WHERE NOT EXISTS (SELECT 1 FROM Showtime x WHERE x.RoomID = l.RoomID AND x.ShowDate = l.ShowDate AND x.StartTime = l.StartTime);

/* ---------- 8. Đồ ăn + thực đơn từng chi nhánh ---------- */
INSERT INTO F_B (ItemName, Category, Price)
SELECT v.ItemName, v.Category, v.Price
FROM (VALUES
    (N'Combo Đơn (1 Bắp + 1 Coke)',     N'Combo',   79000),
    (N'Combo Đôi (1 Bắp Lớn + 2 Coke)', N'Combo',   110000),
    (N'Bắp Rang Bơ Lớn',                N'Popcorn', 59000),
    (N'Coca-Cola 32oz',                 N'Drink',   35000)
) v(ItemName, Category, Price)
WHERE NOT EXISTS (SELECT 1 FROM F_B f WHERE f.ItemName = v.ItemName);

-- Cả 2 chi nhánh bán đủ món, giá gốc (Price NULL). Riêng Cầu Giấy bán Coca giá 39.000 để test giá theo chi nhánh.
INSERT INTO Branch_F_B (BranchID, ItemID, Price, Status)
SELECT b.BranchID, f.ItemID,
       CASE WHEN b.BranchID = @cg AND f.ItemName = N'Coca-Cola 32oz' THEN 39000 ELSE NULL END,
       'Available'
FROM Branch b CROSS JOIN F_B f
WHERE NOT EXISTS (SELECT 1 FROM Branch_F_B x WHERE x.BranchID = b.BranchID AND x.ItemID = f.ItemID);

/* ---------- 9. Mã giảm giá ---------- */
INSERT INTO Promotion (PromoCode, DiscountAmount, MinSpend, StartDate, EndDate, UsageLimit)
SELECT v.PromoCode, v.DiscountAmount, v.MinSpend, v.StartDate, v.EndDate, v.UsageLimit
FROM (VALUES
    ('CINEGO50', 50000, 100000, CAST('2026-01-01' AS DATE), CAST('2026-12-31' AS DATE), 100),  -- toàn chuỗi
    ('HETHAN',   30000, 0,      CAST('2025-01-01' AS DATE), CAST('2025-12-31' AS DATE), 100),  -- đã hết hạn
    ('TEST20K',  20000, 100000, CAST('2026-01-01' AS DATE), CAST('2026-12-31' AS DATE), 2),    -- chỉ 2 lượt
    ('HADONG30', 30000, 0,      CAST('2026-01-01' AS DATE), CAST('2026-12-31' AS DATE), 100)   -- chỉ Hà Đông
) v(PromoCode, DiscountAmount, MinSpend, StartDate, EndDate, UsageLimit)
WHERE NOT EXISTS (SELECT 1 FROM Promotion p WHERE p.PromoCode = v.PromoCode);

INSERT INTO PromotionBranch (PromoID, BranchID)
SELECT p.PromoID, @hd FROM Promotion p
WHERE p.PromoCode = 'HADONG30'
  AND NOT EXISTS (SELECT 1 FROM PromotionBranch x WHERE x.PromoID = p.PromoID AND x.BranchID = @hd);

COMMIT TRAN;

/* ---------- Kiểm tra nhanh ---------- */
SELECT 'Branch' AS Bang, COUNT(*) AS SoDong FROM Branch
UNION ALL SELECT 'User', COUNT(*) FROM [User]
UNION ALL SELECT 'RoomType', COUNT(*) FROM RoomType
UNION ALL SELECT 'SeatType', COUNT(*) FROM SeatType
UNION ALL SELECT 'Room', COUNT(*) FROM Room
UNION ALL SELECT 'Seat', COUNT(*) FROM Seat
UNION ALL SELECT 'Movie', COUNT(*) FROM Movie
UNION ALL SELECT 'Showtime', COUNT(*) FROM Showtime
UNION ALL SELECT 'F_B', COUNT(*) FROM F_B
UNION ALL SELECT 'Branch_F_B', COUNT(*) FROM Branch_F_B
UNION ALL SELECT 'Promotion', COUNT(*) FROM Promotion;
