/* =====================================================================
   Nâng cấp DB đã tạo bằng bản 01_schema.sql cũ. DB tạo mới thì KHÔNG cần chạy file này.
   1. Seats: thêm row_num, col_num (toạ độ ghế trong ma trận), điền từ row_label / seat_number.
   2. Movies: bỏ cột original_title (đã bỏ mục "Tên gốc" khỏi màn quản lý phim).
   Chạy được nhiều lần.
   ===================================================================== */

IF COL_LENGTH('dbo.Seats', 'row_num') IS NULL
    ALTER TABLE dbo.Seats ADD row_num TINYINT NULL;
IF COL_LENGTH('dbo.Seats', 'col_num') IS NULL
    ALTER TABLE dbo.Seats ADD col_num TINYINT NULL;
GO

/* Bản cũ: row_label = 'A'..'Z', seat_number = chỉ số cột. */
UPDATE dbo.Seats
SET row_num = ASCII(row_label) - 64,
    col_num = seat_number
WHERE row_num IS NULL OR col_num IS NULL;
GO

ALTER TABLE dbo.Seats ALTER COLUMN row_num TINYINT NOT NULL;
ALTER TABLE dbo.Seats ALTER COLUMN col_num TINYINT NOT NULL;
GO

IF NOT EXISTS (SELECT 1 FROM sys.key_constraints WHERE name = 'UQ_Seats_coord')
    ALTER TABLE dbo.Seats ADD CONSTRAINT UQ_Seats_coord UNIQUE (room_id, row_num, col_num);
IF NOT EXISTS (SELECT 1 FROM sys.check_constraints WHERE name = 'CK_Seats_coord')
    ALTER TABLE dbo.Seats ADD CONSTRAINT CK_Seats_coord
        CHECK (row_num BETWEEN 1 AND 26 AND col_num BETWEEN 1 AND 30);
GO

IF COL_LENGTH('dbo.Movies', 'original_title') IS NOT NULL
    ALTER TABLE dbo.Movies DROP COLUMN original_title;
GO
