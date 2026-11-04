/* =====================================================================
   Sửa lỗi: khách đặt vé online, chọn F&B rồi bấm "Thanh toán" bị lỗi.

   Code hiện tại (BookingDAO.createOnlineBooking / getBooking, PricingDAO.subtotal,
   F_BOrderDAO...) đọc/ghi cột Booking_F_B.UnitPrice và OrderStatus, nhưng
   script tạo DB cũ (database/01_schema.sql ngày 20/10) chưa có cột UnitPrice.
   Máy nào chưa thêm cột sẽ gặp: "Invalid column name 'UnitPrice'".

   Script chạy lại nhiều lần vẫn an toàn (chỉ thêm khi chưa có).
   ===================================================================== */
USE CineManagementSystem;
GO

IF COL_LENGTH('dbo.Booking_F_B', 'UnitPrice') IS NULL
    ALTER TABLE dbo.Booking_F_B ADD UnitPrice DECIMAL(12,2) NULL;
GO

IF COL_LENGTH('dbo.Booking_F_B', 'OrderStatus') IS NULL
    ALTER TABLE dbo.Booking_F_B ADD OrderStatus VARCHAR(20) NOT NULL
        CONSTRAINT DF_Booking_F_B_OrderStatus DEFAULT 'Pending';
GO

IF COL_LENGTH('dbo.Booking_F_B', 'OrderDate') IS NULL
    ALTER TABLE dbo.Booking_F_B ADD OrderDate DATETIME2 NOT NULL
        CONSTRAINT DF_Booking_F_B_OrderDate DEFAULT SYSDATETIME();
GO

/* Điền đơn giá cho các dòng cũ chưa có: giá chi nhánh nếu có, không thì giá gốc F_B.
   Nếu không điền, trang checkout sẽ báo "Đơn cũ chưa lưu đơn giá F&B". */
UPDATE bf
SET bf.UnitPrice = COALESCE(bfb.Price, f.Price)
FROM dbo.Booking_F_B bf
JOIN dbo.Booking b ON b.BookingID = bf.BookingID
JOIN dbo.F_B f     ON f.ItemID = bf.ItemID
LEFT JOIN dbo.Branch_F_B bfb ON bfb.BranchID = b.BranchID AND bfb.ItemID = bf.ItemID
WHERE bf.UnitPrice IS NULL;
GO

/* Kiểm tra nhanh: phải ra đủ 5 cột BookingID, ItemID, Quantity, UnitPrice, OrderStatus (+ OrderDate). */
SELECT COLUMN_NAME, DATA_TYPE, IS_NULLABLE
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_NAME = 'Booking_F_B'
ORDER BY ORDINAL_POSITION;
GO
