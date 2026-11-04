/* =====================================================================
   Bổ sung cột còn thiếu cho bảng Branch_F_B ĐÃ CÓ SẴN.

   migrate_branch_fnb_promotion.sql chỉ tạo Branch_F_B khi bảng chưa tồn tại.
   DB đã có bảng Branch_F_B kiểu cũ (thiếu StockQuantity...) nên bị bỏ qua, và mọi câu
   SELECT của trang đặt F&B (FnbDAO.getMenu) lỗi "Invalid column name 'StockQuantity'".
   Lỗi này bị bắt và chỉ in ra console, nên trang hiện "chưa mở bán đồ ăn".

   Script chạy lại nhiều lần vẫn an toàn.
   ===================================================================== */
USE CineManagementSystem;
GO

IF OBJECT_ID('tempdb..#NewStockCol') IS NOT NULL DROP TABLE #NewStockCol;
CREATE TABLE #NewStockCol (Added BIT);
GO

-- Giá riêng chi nhánh (NULL = dùng giá gốc F_B.Price)
IF COL_LENGTH('dbo.Branch_F_B', 'Price') IS NULL
    ALTER TABLE dbo.Branch_F_B ADD Price DECIMAL(12, 2) NULL;
GO

-- Trạng thái món tại chi nhánh: Available | SoldOut | Inactive
IF COL_LENGTH('dbo.Branch_F_B', 'Status') IS NULL
    ALTER TABLE dbo.Branch_F_B ADD Status VARCHAR(20) NOT NULL
        CONSTRAINT DF_Branch_F_B_Status DEFAULT ('Available');
GO

-- Số lượng tồn
IF COL_LENGTH('dbo.Branch_F_B', 'StockQuantity') IS NULL
BEGIN
    ALTER TABLE dbo.Branch_F_B ADD StockQuantity INT NOT NULL
        CONSTRAINT DF_Branch_F_B_Stock DEFAULT (0);
    INSERT INTO #NewStockCol VALUES (1);
END
GO

-- Cột tồn vừa được thêm: các dòng cũ đang = 0 (sẽ bị ẩn) -> cho tồn ban đầu 100
IF EXISTS (SELECT 1 FROM #NewStockCol)
    UPDATE dbo.Branch_F_B SET StockQuantity = 100 WHERE StockQuantity = 0;
GO

IF OBJECT_ID('dbo.CK_Branch_F_B_Stock', 'C') IS NULL
    ALTER TABLE dbo.Branch_F_B ADD CONSTRAINT CK_Branch_F_B_Stock CHECK (StockQuantity >= 0);
IF OBJECT_ID('dbo.CK_Branch_F_B_Price', 'C') IS NULL
    ALTER TABLE dbo.Branch_F_B ADD CONSTRAINT CK_Branch_F_B_Price CHECK (Price IS NULL OR Price >= 0);
GO

-- F_B.IsActive (phòng trường hợp chưa có)
IF COL_LENGTH('dbo.F_B', 'IsActive') IS NULL
    ALTER TABLE dbo.F_B ADD IsActive BIT NOT NULL CONSTRAINT DF_F_B_IsActive DEFAULT (1);
GO

DROP TABLE #NewStockCol;
GO

/* Kiểm tra 1: cấu trúc bảng (phải có BranchID, ItemID, Price, Status, StockQuantity). */
SELECT COLUMN_NAME, DATA_TYPE, IS_NULLABLE
FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = 'Branch_F_B' ORDER BY ORDINAL_POSITION;

/* Kiểm tra 2: giá trị Status đang có. Code chỉ hiện món có Status = 'Available'.
   Nếu thấy giá trị khác (vd 'Active') thì sửa: UPDATE dbo.Branch_F_B SET Status = 'Available' WHERE Status = 'Active'; */
SELECT Status, COUNT(*) AS SoDong FROM dbo.Branch_F_B GROUP BY Status;
GO
