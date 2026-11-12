/* =====================================================================
   Bổ sung cột Booking.DiscountApplied (số tiền voucher đã giảm của đơn).

   Code hiện tại (PosService.createCounterBooking -> F_BCheckoutService.finishCounter,
   PromotionDAO, F_BOrderDAO, PromotionService...) đọc/ghi Booking.DiscountApplied,
   nhưng chưa có script nào thêm cột này. Máy nào thiếu cột sẽ gặp
   "Invalid column name 'DiscountApplied'": bán vé tại quầy báo "Không lưu được đơn",
   áp voucher khi thanh toán online bị lỗi.

   Chạy SAU fix_booking_fnb_unitprice.sql (cần UnitPrice để tính tiền giảm của đơn cũ).
   Script chạy lại nhiều lần vẫn an toàn.
   ===================================================================== */
USE CineManagementSystem;
GO

IF COL_LENGTH('dbo.Booking', 'DiscountApplied') IS NULL
    ALTER TABLE dbo.Booking ADD DiscountApplied DECIMAL(12, 2) NOT NULL
        CONSTRAINT DF_Booking_DiscountApplied DEFAULT (0);
GO

IF OBJECT_ID('dbo.CK_Booking_DiscountApplied', 'C') IS NULL
    ALTER TABLE dbo.Booking ADD CONSTRAINT CK_Booking_DiscountApplied CHECK (DiscountApplied >= 0);
GO

/* Đơn cũ có voucher: tiền giảm = tiền vé + tiền F&B - tổng tiền đơn (chỉ khi > 0). */
UPDATE b
SET b.DiscountApplied = x.Subtotal - b.TotalAmount
FROM dbo.Booking b
CROSS APPLY (
    SELECT COALESCE((SELECT SUM(t.FinalPrice) FROM dbo.Ticket t WHERE t.BookingID = b.BookingID), 0)
         + COALESCE((SELECT SUM(f.Quantity * f.UnitPrice) FROM dbo.Booking_F_B f WHERE f.BookingID = b.BookingID), 0)
         AS Subtotal
) x
WHERE b.PromoID IS NOT NULL
  AND b.DiscountApplied = 0
  AND x.Subtotal > b.TotalAmount;
GO

/* Kiểm tra: phải có cột DiscountApplied. */
SELECT COLUMN_NAME, DATA_TYPE, IS_NULLABLE
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_NAME = 'Booking' AND COLUMN_NAME = 'DiscountApplied';
SELECT COUNT(*) AS DonCoVoucher, SUM(DiscountApplied) AS TongTienGiam
FROM dbo.Booking WHERE PromoID IS NOT NULL;
GO
