/* =====================================================================
   THÊM SUẤT CHIẾU CHO 10 NGÀY TIẾP THEO (database CineManagementSystem)
   - Bắt đầu từ ngày SAU ngày cuối cùng đang có suất chiếu
     (nếu lịch đã cũ hơn hôm nay thì bắt đầu từ hôm nay).
   - Mỗi lần chạy lại sẽ nối thêm 10 ngày nữa.
   - Dùng các phim đang chiếu (Now Showing) dài tối đa 150 phút, xoay vòng để ngày nào cũng đổi phim.
   - Khung giờ mỗi phòng cách nhau 3 tiếng:
       Phòng IMAX (Hà Đông)    : 09 12 15 18 21h
       Phòng 2D   (Hà Đông)    : 10 13 16 19 22h
       Phòng 3D   (Cầu Giấy)   : 11 14 17 20h
   - Giá gốc: trước 17h 75.000đ, từ 17h 95.000đ, thứ 7 / CN cộng 10.000đ
     (phụ thu phòng IMAX/3D và ghế VIP/Sweetbox lấy từ bảng RoomType / SeatType khi bán vé).
   ===================================================================== */
USE CineManagementSystem;
GO
SET NOCOUNT ON;
SET XACT_ABORT ON;
SET ANSI_NULLS ON;
SET QUOTED_IDENTIFIER ON;
GO

DECLARE @soNgay INT = 10;

-- Ngày bắt đầu = ngày sau ngày cuối cùng đang có suất chiếu, nhưng không sớm hơn hôm nay
DECLARE @tuNgay DATE = DATEADD(DAY, 1, (SELECT MAX(ShowDate) FROM Showtime));
IF @tuNgay IS NULL OR @tuNgay < CAST(GETDATE() AS DATE)
    SET @tuNgay = CAST(GETDATE() AS DATE);

-- Khung giờ theo tên phòng (không phụ thuộc RoomID)
DECLARE @slots TABLE (RoomID INT, SlotNo INT, StartTime TIME(0));
INSERT INTO @slots (RoomID, SlotNo, StartTime)
SELECT r.RoomID, s.SlotNo, s.StartTime
FROM Room r
JOIN (VALUES
    (N'Cinema Room 01 (IMAX)',     1, '09:00'), (N'Cinema Room 01 (IMAX)',     2, '12:00'),
    (N'Cinema Room 01 (IMAX)',     3, '15:00'), (N'Cinema Room 01 (IMAX)',     4, '18:00'),
    (N'Cinema Room 01 (IMAX)',     5, '21:00'),
    (N'Cinema Room 02 (Standard)', 1, '10:00'), (N'Cinema Room 02 (Standard)', 2, '13:00'),
    (N'Cinema Room 02 (Standard)', 3, '16:00'), (N'Cinema Room 02 (Standard)', 4, '19:00'),
    (N'Cinema Room 02 (Standard)', 5, '22:00'),
    (N'Cinema Room 01 (3D Laser)', 1, '11:00'), (N'Cinema Room 01 (3D Laser)', 2, '14:00'),
    (N'Cinema Room 01 (3D Laser)', 3, '17:00'), (N'Cinema Room 01 (3D Laser)', 4, '20:00')
) s(RoomName, SlotNo, StartTime) ON s.RoomName = r.RoomName
WHERE r.Status = 'Active';

DECLARE @soPhim INT = (SELECT COUNT(*) FROM Movie WHERE Status = 'Now Showing' AND Duration <= 150);
IF @soPhim = 0
BEGIN
    RAISERROR(N'Không có phim Now Showing nào dài <= 150 phút để xếp lịch.', 16, 1);
    RETURN;
END;

BEGIN TRAN;

;WITH phim AS (
    SELECT MovieID, Duration, ROW_NUMBER() OVER (ORDER BY MovieID) - 1 AS idx
    FROM Movie WHERE Status = 'Now Showing' AND Duration <= 150
),
ngay AS (
    SELECT TOP (@soNgay) ROW_NUMBER() OVER (ORDER BY (SELECT NULL)) - 1 AS n FROM sys.all_objects
),
lich AS (
    SELECT DATEADD(DAY, d.n, @tuNgay) AS ShowDate, s.RoomID, s.StartTime, p.MovieID, p.Duration,
           CAST(DATEADD(DAY, d.n, @tuNgay) AS DATETIME) + CAST(s.StartTime AS DATETIME) AS StartDT
    FROM ngay d
    CROSS JOIN @slots s
    -- xoay vòng phim theo ngày + khung giờ + phòng
    JOIN phim p ON p.idx = (DATEDIFF(DAY, '20260101', DATEADD(DAY, d.n, @tuNgay)) + s.SlotNo * 3 + s.RoomID * 7) % @soPhim
)
INSERT INTO Showtime (MovieID, RoomID, ShowDate, StartTime, EndTime, BasePrice, Status)
SELECT l.MovieID, l.RoomID, l.ShowDate, l.StartTime,
       CAST(DATEADD(MINUTE, l.Duration, l.StartDT) AS TIME(0)),          -- quá nửa đêm thì EndTime < StartTime
       CASE WHEN l.StartTime >= '17:00' THEN 95000 ELSE 75000 END
       + CASE WHEN DATEDIFF(DAY, '19000101', l.ShowDate) % 7 IN (5, 6) THEN 10000 ELSE 0 END,   -- 5 = T7, 6 = CN
       'Open'
FROM lich l
WHERE NOT EXISTS (   -- bỏ qua nếu chồng giờ (kể cả 15 phút dọn phòng) với suất đã có trong cùng phòng
    SELECT 1 FROM Showtime x JOIN Movie xm ON xm.MovieID = x.MovieID
    WHERE x.RoomID = l.RoomID
      AND x.Status <> 'Cancelled'
      AND x.ShowDate BETWEEN DATEADD(DAY, -1, l.ShowDate) AND DATEADD(DAY, 1, l.ShowDate)
      AND CAST(x.ShowDate AS DATETIME) + CAST(x.StartTime AS DATETIME) < DATEADD(MINUTE, l.Duration + 15, l.StartDT)
      AND DATEADD(MINUTE, xm.Duration + 15, CAST(x.ShowDate AS DATETIME) + CAST(x.StartTime AS DATETIME)) > l.StartDT
);

DECLARE @soSuat INT = @@ROWCOUNT;
COMMIT TRAN;

PRINT N'Đã thêm ' + CAST(@soSuat AS VARCHAR(10)) + N' suất chiếu từ '
      + CONVERT(VARCHAR(10), @tuNgay, 103) + N' đến ' + CONVERT(VARCHAR(10), DATEADD(DAY, @soNgay - 1, @tuNgay), 103);

-- Xem số suất từng ngày vừa thêm
SELECT st.ShowDate, COUNT(*) AS SoSuat, MIN(st.StartTime) AS SuatDau, MAX(st.StartTime) AS SuatCuoi
FROM Showtime st
WHERE st.ShowDate BETWEEN @tuNgay AND DATEADD(DAY, @soNgay - 1, @tuNgay)
GROUP BY st.ShowDate
ORDER BY st.ShowDate;
