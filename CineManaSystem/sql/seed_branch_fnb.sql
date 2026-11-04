/* =====================================================================
   Đưa món F&B vào thực đơn từng chi nhánh (bảng Branch_F_B).

   Sau khi chạy migrate_branch_fnb_promotion.sql, bảng Branch_F_B mới được tạo nhưng RỖNG.
   Trang đặt F&B (FnbDAO.getMenu) chỉ hiện món có dòng trong Branch_F_B với
   Status = 'Available' và StockQuantity > 0, nên thực đơn trống.

   Script này thêm mọi món đang bán (F_B.IsActive = 1) cho mọi chi nhánh Active,
   giá NULL (= dùng giá gốc F_B.Price), tồn kho ban đầu @Stock.
   Chỉ thêm dòng còn thiếu, không đụng dòng đã có -> chạy lại nhiều lần vẫn an toàn.
   Sau đó có thể chỉnh giá/tồn từng chi nhánh ở trang quản lý kho F&B.
   ===================================================================== */
USE CineManagementSystem;
GO

DECLARE @Stock INT = 100;

INSERT INTO dbo.Branch_F_B (BranchID, ItemID, Price, Status, StockQuantity)
SELECT br.BranchID, f.ItemID, NULL, 'Available', @Stock
FROM dbo.Branch br
CROSS JOIN dbo.F_B f
WHERE br.Status = 'Active'
  AND f.IsActive = 1
  AND NOT EXISTS (SELECT 1 FROM dbo.Branch_F_B x
                  WHERE x.BranchID = br.BranchID AND x.ItemID = f.ItemID);

PRINT CONCAT(N'Đã thêm ', @@ROWCOUNT, N' dòng vào Branch_F_B.');
GO

/* Kiểm tra: số món đang bán online ở mỗi chi nhánh (phải > 0). */
SELECT br.BranchID, br.Name, COUNT(f.ItemID) AS SoMonDangBan
FROM dbo.Branch br
LEFT JOIN dbo.Branch_F_B bf ON bf.BranchID = br.BranchID
     AND bf.Status = 'Available' AND bf.StockQuantity > 0
LEFT JOIN dbo.F_B f ON f.ItemID = bf.ItemID AND f.IsActive = 1
WHERE br.Status = 'Active'
GROUP BY br.BranchID, br.Name
ORDER BY br.BranchID;
GO
