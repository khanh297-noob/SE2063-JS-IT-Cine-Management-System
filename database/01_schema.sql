/* =====================================================================
   CINE MANAGEMENT SYSTEM - DATABASE GỘP CỦA CẢ NHÓM (SQL Server)
   Tổng hợp từ model + SQL trong DAO của 5 thành viên:
     - Hoàng     : Branch, BranchOperatingHour, RoomType, Room, SeatType, Seat, Genre, Movie,
                   MovieGenre, MovieMedia, MovieBranch, MovieReview
     - Đức Hiệp  : Showtime, SeatHold, Booking (Channel, StaffID, huỷ/hoàn), Ticket, [User]
     - Hoàng Nam : Payment, Promotion, F_B, Booking_F_B, AuditLog, Ticket (soát vé), WorkShift*
     - Tuyết     : [User] (đăng ký/đăng nhập), EmailVerification, báo cáo (chỉ đọc)
     - Khánh     : [User] (BranchID), AuditLog (IPAddress), F_B, F_BOrder -> Booking_F_B, Promotion
     (*) WorkShift không có bảng: ca làm được ghi vào AuditLog (SHIFT_START / SHIFT_END).

   QUY ƯỚC (theo SDS, 4/5 người đang dùng):
     - Tên bảng số ít, PascalCase: Branch, Movie, [User]... ([User] phải để trong ngoặc vuông).
     - Tên cột PascalCase, khoá chính <Bảng>ID: BranchID, MovieID, ShowtimeID...
     - Chữ tiếng Việt dùng NVARCHAR.

   CÁC ĐIỂM XUNG ĐỘT ĐÃ CHỐT (xem phần cuối file để biết ai cần sửa code):
     1. Room.RoomType, Seat.SeatType: 4 người đọc dạng CHUỖI (r.RoomType, s.SeatType).
        -> Giữ cột chuỗi, nhưng là khoá ngoại tới RoomType(Name) / SeatType(Name)
           (ON UPDATE CASCADE), để vẫn quản lý được phụ thu, màu ghế (chức năng của Hoàng).
     2. Room.Capacity: Hiệp đọc r.Capacity.
        -> Giữ cột Capacity = số ghế dùng được; module sơ đồ ghế cập nhật khi lưu sơ đồ.
     3. Movie.PosterURL / TrailerURL: Hiệp, Tuyết đọc trực tiếp từ Movie.
        -> Giữ 2 cột = poster/trailer CHÍNH; nhiều poster/trailer vẫn nằm ở MovieMedia.
     4. Seat: giữ RowLabel + SeatNumber (nhãn trên vé, mọi người dùng) và thêm RowNum + ColNum
        (toạ độ trong ma trận ghế, của Hoàng).
     5. Movie.Status: 'Coming Soon' | 'Now Showing' | 'End of Screening'.
     6. Booking_F_B (Nam) và F_BOrder (Khánh) là cùng một bảng -> dùng tên Booking_F_B,
        thêm OrderStatus, OrderDate của Khánh.
     7. MovieReview: thêm UserID (NULL được) để sau này gắn với tài khoản đăng nhập.
   ===================================================================== */

IF DB_ID(N'CineManagementSystem') IS NULL
    CREATE DATABASE CineManagementSystem;
GO
USE CineManagementSystem;
GO
/* Bắt buộc cho các filtered index bên dưới (SSMS mặc định đã bật, sqlcmd thì không). */
SET ANSI_NULLS ON;
SET QUOTED_IDENTIFIER ON;
GO

/* ---------------------------------------------------------------------
   Xoá để tạo lại từ đầu (bỏ comment khi cần). Thứ tự ngược khoá ngoại.
   --------------------------------------------------------------------- */
-- DROP TABLE IF EXISTS dbo.Booking_F_B, dbo.F_B, dbo.Payment, dbo.Ticket, dbo.Booking, dbo.Promotion,
--     dbo.SeatHold, dbo.Showtime, dbo.MovieReview, dbo.MovieBranch, dbo.MovieMedia, dbo.MovieGenre,
--     dbo.Movie, dbo.Genre, dbo.Seat, dbo.SeatType, dbo.Room, dbo.RoomType, dbo.AuditLog,
--     dbo.EmailVerification, dbo.[User], dbo.BranchOperatingHour, dbo.Branch;
-- GO

/* =====================================================================
   1. CHI NHÁNH
   ===================================================================== */

IF OBJECT_ID('dbo.Branch', 'U') IS NULL
CREATE TABLE dbo.Branch (
    BranchID   INT IDENTITY(1,1) PRIMARY KEY,
    Name       NVARCHAR(150) NOT NULL,
    Address    NVARCHAR(255) NOT NULL,
    City       NVARCHAR(100) NOT NULL,
    Hotline    VARCHAR(20)   NOT NULL,
    Email      VARCHAR(150)  NULL,
    Status     VARCHAR(20)   NOT NULL CONSTRAINT DF_Branch_Status DEFAULT 'Active',
    CreatedAt  DATETIME2     NOT NULL CONSTRAINT DF_Branch_CreatedAt DEFAULT SYSDATETIME(),
    CONSTRAINT UQ_Branch_Name   UNIQUE (Name),
    CONSTRAINT CK_Branch_Status CHECK (Status IN ('Active', 'Inactive'))
);
GO

/* DayOfWeek: 1 = Thứ Hai ... 7 = Chủ Nhật. CloseTime < OpenTime = đóng cửa sau nửa đêm. */
IF OBJECT_ID('dbo.BranchOperatingHour', 'U') IS NULL
CREATE TABLE dbo.BranchOperatingHour (
    BranchID   INT     NOT NULL,
    DayOfWeek  TINYINT NOT NULL,
    IsClosed   BIT     NOT NULL CONSTRAINT DF_BOH_IsClosed DEFAULT 0,
    OpenTime   TIME(0) NULL,
    CloseTime  TIME(0) NULL,
    CONSTRAINT PK_BranchOperatingHour PRIMARY KEY (BranchID, DayOfWeek),
    CONSTRAINT FK_BOH_Branch FOREIGN KEY (BranchID) REFERENCES dbo.Branch(BranchID) ON DELETE CASCADE,
    CONSTRAINT CK_BOH_Day    CHECK (DayOfWeek BETWEEN 1 AND 7),
    CONSTRAINT CK_BOH_Times  CHECK (IsClosed = 1
                                    OR (OpenTime IS NOT NULL AND CloseTime IS NOT NULL AND OpenTime <> CloseTime))
);
GO

/* =====================================================================
   2. NGƯỜI DÙNG, XÁC THỰC, NHẬT KÝ
   ===================================================================== */

/* Role: Customer | Ticket Cashier | Ticket Checker | Branch Management | Administrator.
   BranchID: chi nhánh của nhân viên (NULL với Customer, Administrator).
   Status: code hiện dùng 'Active'; các giá trị khác (khoá / ngừng) nhóm chốt thêm nên chưa đặt CHECK. */
IF OBJECT_ID('dbo.[User]', 'U') IS NULL
CREATE TABLE dbo.[User] (
    UserID        INT IDENTITY(1,1) PRIMARY KEY,
    FullName      NVARCHAR(100) NOT NULL,
    Email         VARCHAR(150)  NOT NULL,
    Phone         VARCHAR(20)   NULL,
    PasswordHash  VARCHAR(255)  NOT NULL,
    [Role]        VARCHAR(30)   NOT NULL CONSTRAINT DF_User_Role DEFAULT 'Customer',
    [Status]      VARCHAR(20)   NOT NULL CONSTRAINT DF_User_Status DEFAULT 'Active',
    BranchID      INT           NULL,
    CreatedAt     DATETIME2     NOT NULL CONSTRAINT DF_User_CreatedAt DEFAULT SYSDATETIME(),
    CONSTRAINT UQ_User_Email  UNIQUE (Email),
    CONSTRAINT FK_User_Branch FOREIGN KEY (BranchID) REFERENCES dbo.Branch(BranchID),
    CONSTRAINT CK_User_Role   CHECK ([Role] IN ('Customer', 'Ticket Cashier', 'Ticket Checker',
                                                'Branch Management', 'Administrator'))
);
GO

/* Mã OTP xác thực email khi đăng ký / quên mật khẩu (Tuyết). */
IF OBJECT_ID('dbo.EmailVerification', 'U') IS NULL
CREATE TABLE dbo.EmailVerification (
    VerificationID INT IDENTITY(1,1) PRIMARY KEY,
    Email          VARCHAR(150) NOT NULL,
    OtpCode        VARCHAR(10)  NOT NULL,
    ExpiryTime     DATETIME2    NOT NULL,
    IsUsed         BIT          NOT NULL CONSTRAINT DF_EmailVerification_IsUsed DEFAULT 0,
    CreatedAt      DATETIME2    NOT NULL CONSTRAINT DF_EmailVerification_CreatedAt DEFAULT SYSDATETIME()
);
GO
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'IX_EmailVerification_Email')
CREATE INDEX IX_EmailVerification_Email ON dbo.EmailVerification(Email, IsUsed);
GO

/* Action dạng "LOAI|key=value|..." (vd SHIFT_START|branch=1|float=500000) - Nam, Khánh. */
IF OBJECT_ID('dbo.AuditLog', 'U') IS NULL
CREATE TABLE dbo.AuditLog (
    LogID      INT IDENTITY(1,1) PRIMARY KEY,
    UserID     INT           NULL,
    Action     NVARCHAR(500) NOT NULL,
    IPAddress  VARCHAR(45)   NULL,
    [Timestamp] DATETIME2    NOT NULL CONSTRAINT DF_AuditLog_Timestamp DEFAULT SYSDATETIME(),
    CONSTRAINT FK_AuditLog_User FOREIGN KEY (UserID) REFERENCES dbo.[User](UserID)
);
GO
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'IX_AuditLog_User_Time')
CREATE INDEX IX_AuditLog_User_Time ON dbo.AuditLog(UserID, [Timestamp]);
GO

/* =====================================================================
   3. PHÒNG CHIẾU, GHẾ
   ===================================================================== */

IF OBJECT_ID('dbo.RoomType', 'U') IS NULL
CREATE TABLE dbo.RoomType (
    RoomTypeID   INT IDENTITY(1,1) PRIMARY KEY,
    Name         NVARCHAR(50)  NOT NULL,
    Surcharge    DECIMAL(12,2) NOT NULL CONSTRAINT DF_RoomType_Surcharge DEFAULT 0,
    Description  NVARCHAR(255) NULL,
    Status       VARCHAR(20)   NOT NULL CONSTRAINT DF_RoomType_Status DEFAULT 'Active',
    CONSTRAINT UQ_RoomType_Name      UNIQUE (Name),
    CONSTRAINT CK_RoomType_Surcharge CHECK (Surcharge >= 0),
    CONSTRAINT CK_RoomType_Status    CHECK (Status IN ('Active', 'Inactive'))
);
GO

/* RoomType: tên loại phòng ('2D', '3D', 'IMAX'...) - khoá ngoại tới RoomType(Name).
   Capacity: số ghế dùng được, cập nhật khi lưu sơ đồ ghế.
   TotalRows / TotalColumns: kích thước ma trận ghế (tối đa 26 hàng A-Z x 30 cột). */
IF OBJECT_ID('dbo.Room', 'U') IS NULL
CREATE TABLE dbo.Room (
    RoomID        INT IDENTITY(1,1) PRIMARY KEY,
    BranchID      INT          NOT NULL,
    RoomName      NVARCHAR(50) NOT NULL,
    RoomType      NVARCHAR(50) NOT NULL,
    Capacity      INT          NOT NULL CONSTRAINT DF_Room_Capacity DEFAULT 0,
    TotalRows     TINYINT      NOT NULL CONSTRAINT DF_Room_TotalRows DEFAULT 0,
    TotalColumns  TINYINT      NOT NULL CONSTRAINT DF_Room_TotalColumns DEFAULT 0,
    Status        VARCHAR(20)  NOT NULL CONSTRAINT DF_Room_Status DEFAULT 'Active',
    CONSTRAINT FK_Room_Branch   FOREIGN KEY (BranchID) REFERENCES dbo.Branch(BranchID),
    CONSTRAINT FK_Room_RoomType FOREIGN KEY (RoomType) REFERENCES dbo.RoomType(Name) ON UPDATE CASCADE,
    CONSTRAINT UQ_Room_Name     UNIQUE (BranchID, RoomName),
    CONSTRAINT CK_Room_Status   CHECK (Status IN ('Active', 'Maintenance', 'Inactive')),
    CONSTRAINT CK_Room_Size     CHECK (TotalRows <= 26 AND TotalColumns <= 30 AND Capacity >= 0)
);
GO

IF OBJECT_ID('dbo.SeatType', 'U') IS NULL
CREATE TABLE dbo.SeatType (
    SeatTypeID  INT IDENTITY(1,1) PRIMARY KEY,
    Name        NVARCHAR(30)  NOT NULL,
    Surcharge   DECIMAL(12,2) NOT NULL CONSTRAINT DF_SeatType_Surcharge DEFAULT 0,
    Color       VARCHAR(7)    NOT NULL CONSTRAINT DF_SeatType_Color DEFAULT '#8a8fa8',
    CONSTRAINT UQ_SeatType_Name      UNIQUE (Name),
    CONSTRAINT CK_SeatType_Surcharge CHECK (Surcharge >= 0)
);
GO

/* RowLabel + SeatNumber: nhãn in trên vé ("C7").
   RowNum + ColNum: toạ độ 1-based trong ma trận ghế. Ô không có dòng nào = lối đi.
   SeatType: tên loại ghế ('Standard', 'VIP', 'Sweetbox'...) - khoá ngoại tới SeatType(Name). */
IF OBJECT_ID('dbo.Seat', 'U') IS NULL
CREATE TABLE dbo.Seat (
    SeatID      INT IDENTITY(1,1) PRIMARY KEY,
    RoomID      INT          NOT NULL,
    SeatType    NVARCHAR(30) NOT NULL,
    RowLabel    VARCHAR(2)   NOT NULL,
    SeatNumber  TINYINT      NOT NULL,
    RowNum      TINYINT      NOT NULL,
    ColNum      TINYINT      NOT NULL,
    Status      VARCHAR(20)  NOT NULL CONSTRAINT DF_Seat_Status DEFAULT 'Active',
    CONSTRAINT FK_Seat_Room     FOREIGN KEY (RoomID)   REFERENCES dbo.Room(RoomID) ON DELETE CASCADE,
    CONSTRAINT FK_Seat_SeatType FOREIGN KEY (SeatType) REFERENCES dbo.SeatType(Name) ON UPDATE CASCADE,
    CONSTRAINT UQ_Seat_Label    UNIQUE (RoomID, RowLabel, SeatNumber),
    CONSTRAINT UQ_Seat_Coord    UNIQUE (RoomID, RowNum, ColNum),
    CONSTRAINT CK_Seat_Coord    CHECK (RowNum BETWEEN 1 AND 26 AND ColNum BETWEEN 1 AND 30),
    CONSTRAINT CK_Seat_Status   CHECK (Status IN ('Active', 'Broken'))
);
GO

/* =====================================================================
   4. PHIM
   ===================================================================== */

IF OBJECT_ID('dbo.Genre', 'U') IS NULL
CREATE TABLE dbo.Genre (
    GenreID      INT IDENTITY(1,1) PRIMARY KEY,
    Name         NVARCHAR(50)  NOT NULL,
    Description  NVARCHAR(255) NULL,
    CONSTRAINT UQ_Genre_Name UNIQUE (Name)
);
GO

/* PosterURL / TrailerURL: poster và trailer CHÍNH (module Phim cập nhật khi chọn ảnh chính trong MovieMedia).
   AgeRating: P, K, T13, T16, T18 (bỏ 'C' vì không được phổ biến). */
IF OBJECT_ID('dbo.Movie', 'U') IS NULL
CREATE TABLE dbo.Movie (
    MovieID       INT IDENTITY(1,1) PRIMARY KEY,
    Title         NVARCHAR(200) NOT NULL,
    Description   NVARCHAR(MAX) NULL,
    Director      NVARCHAR(150) NULL,
    CastMembers   NVARCHAR(500) NULL,
    Language      NVARCHAR(50)  NULL,
    Duration      INT           NOT NULL,
    ReleaseDate   DATE          NOT NULL,
    AgeRating     VARCHAR(10)   NOT NULL,
    PosterURL     VARCHAR(500)  NULL,
    TrailerURL    VARCHAR(500)  NULL,
    Status        VARCHAR(20)   NOT NULL CONSTRAINT DF_Movie_Status DEFAULT 'Coming Soon',
    CreatedAt     DATETIME2     NOT NULL CONSTRAINT DF_Movie_CreatedAt DEFAULT SYSDATETIME(),
    CONSTRAINT CK_Movie_Duration  CHECK (Duration BETWEEN 1 AND 600),
    CONSTRAINT CK_Movie_AgeRating CHECK (AgeRating IN ('P', 'K', 'T13', 'T16', 'T18')),
    CONSTRAINT CK_Movie_Status    CHECK (Status IN ('Coming Soon', 'Now Showing', 'End of Screening'))
);
GO

IF OBJECT_ID('dbo.MovieGenre', 'U') IS NULL
CREATE TABLE dbo.MovieGenre (
    MovieID  INT NOT NULL,
    GenreID  INT NOT NULL,
    CONSTRAINT PK_MovieGenre PRIMARY KEY (MovieID, GenreID),
    CONSTRAINT FK_MovieGenre_Movie FOREIGN KEY (MovieID) REFERENCES dbo.Movie(MovieID) ON DELETE CASCADE,
    CONSTRAINT FK_MovieGenre_Genre FOREIGN KEY (GenreID) REFERENCES dbo.Genre(GenreID)
);
GO

IF OBJECT_ID('dbo.MovieMedia', 'U') IS NULL
CREATE TABLE dbo.MovieMedia (
    MediaID    INT IDENTITY(1,1) PRIMARY KEY,
    MovieID    INT           NOT NULL,
    MediaType  VARCHAR(10)   NOT NULL,
    URL        VARCHAR(500)  NOT NULL,
    Caption    NVARCHAR(150) NULL,
    IsPrimary  BIT           NOT NULL CONSTRAINT DF_MovieMedia_IsPrimary DEFAULT 0,
    CreatedAt  DATETIME2     NOT NULL CONSTRAINT DF_MovieMedia_CreatedAt DEFAULT SYSDATETIME(),
    CONSTRAINT FK_MovieMedia_Movie FOREIGN KEY (MovieID) REFERENCES dbo.Movie(MovieID) ON DELETE CASCADE,
    CONSTRAINT CK_MovieMedia_Type  CHECK (MediaType IN ('POSTER', 'TRAILER'))
);
GO
/* Mỗi phim tối đa 1 poster chính và 1 trailer chính. */
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'UX_MovieMedia_Primary')
CREATE UNIQUE INDEX UX_MovieMedia_Primary ON dbo.MovieMedia(MovieID, MediaType) WHERE IsPrimary = 1;
GO

/* Phim được chiếu ở chi nhánh nào, từ ngày nào đến ngày nào. */
IF OBJECT_ID('dbo.MovieBranch', 'U') IS NULL
CREATE TABLE dbo.MovieBranch (
    MovieID        INT       NOT NULL,
    BranchID       INT       NOT NULL,
    AvailableFrom  DATE      NULL,
    AvailableTo    DATE      NULL,
    AssignedAt     DATETIME2 NOT NULL CONSTRAINT DF_MovieBranch_AssignedAt DEFAULT SYSDATETIME(),
    CONSTRAINT PK_MovieBranch PRIMARY KEY (MovieID, BranchID),
    CONSTRAINT FK_MovieBranch_Movie  FOREIGN KEY (MovieID)  REFERENCES dbo.Movie(MovieID) ON DELETE CASCADE,
    CONSTRAINT FK_MovieBranch_Branch FOREIGN KEY (BranchID) REFERENCES dbo.Branch(BranchID),
    CONSTRAINT CK_MovieBranch_Dates  CHECK (AvailableTo IS NULL OR AvailableFrom IS NULL OR AvailableTo >= AvailableFrom)
);
GO

/* Mỗi dòng là một trong hai loại:
     - Đánh giá      : Liked = 0, có Rating (1-5) và Content
     - Lượt yêu thích: Liked = 1, Rating và Content để NULL
   UserID: tài khoản đã đăng nhập (NULL khi khách chưa đăng nhập, dùng ReviewerName). */
IF OBJECT_ID('dbo.MovieReview', 'U') IS NULL
CREATE TABLE dbo.MovieReview (
    ReviewID      INT IDENTITY(1,1) PRIMARY KEY,
    MovieID       INT            NOT NULL,
    UserID        INT            NULL,
    ReviewerName  NVARCHAR(50)   NOT NULL,
    Rating        TINYINT        NULL,
    Content       NVARCHAR(1000) NULL,
    Liked         BIT            NOT NULL CONSTRAINT DF_MovieReview_Liked DEFAULT 0,
    CreatedAt     DATETIME2      NOT NULL CONSTRAINT DF_MovieReview_CreatedAt DEFAULT SYSDATETIME(),
    CONSTRAINT FK_MovieReview_Movie  FOREIGN KEY (MovieID) REFERENCES dbo.Movie(MovieID) ON DELETE CASCADE,
    CONSTRAINT FK_MovieReview_User   FOREIGN KEY (UserID)  REFERENCES dbo.[User](UserID),
    CONSTRAINT CK_MovieReview_Rating CHECK (Rating BETWEEN 1 AND 5),
    CONSTRAINT CK_MovieReview_Kind   CHECK (Liked = 1 OR (Rating IS NOT NULL AND Content IS NOT NULL))
);
GO
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'IX_MovieReview_Movie')
CREATE INDEX IX_MovieReview_Movie ON dbo.MovieReview(MovieID, CreatedAt DESC);
GO

/* =====================================================================
   5. SUẤT CHIẾU, GIỮ GHẾ
   ===================================================================== */

/* EndTime <= StartTime = suất chiếu kết thúc sau nửa đêm.
   Status: Scheduled (chưa mở bán) | Open (đang bán) | Closed | Cancelled. */
IF OBJECT_ID('dbo.Showtime', 'U') IS NULL
CREATE TABLE dbo.Showtime (
    ShowtimeID  INT IDENTITY(1,1) PRIMARY KEY,
    MovieID     INT           NOT NULL,
    RoomID      INT           NOT NULL,
    ShowDate    DATE          NOT NULL,
    StartTime   TIME(0)       NOT NULL,
    EndTime     TIME(0)       NOT NULL,
    BasePrice   DECIMAL(12,2) NOT NULL,
    Status      VARCHAR(20)   NOT NULL CONSTRAINT DF_Showtime_Status DEFAULT 'Scheduled',
    CONSTRAINT FK_Showtime_Movie FOREIGN KEY (MovieID) REFERENCES dbo.Movie(MovieID),
    CONSTRAINT FK_Showtime_Room  FOREIGN KEY (RoomID)  REFERENCES dbo.Room(RoomID),
    CONSTRAINT CK_Showtime_Price  CHECK (BasePrice >= 0),
    CONSTRAINT CK_Showtime_Status CHECK (Status IN ('Scheduled', 'Open', 'Closed', 'Cancelled'))
);
GO
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'IX_Showtime_Movie_Date')
CREATE INDEX IX_Showtime_Movie_Date ON dbo.Showtime(MovieID, ShowDate) INCLUDE (RoomID);
GO
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'IX_Showtime_Room_Date')
CREATE INDEX IX_Showtime_Room_Date ON dbo.Showtime(RoomID, ShowDate);
GO

/* Ghế đang được giữ chờ thanh toán; hết ExpiresAt thì job SeatHoldReleaseTask xoá. */
IF OBJECT_ID('dbo.SeatHold', 'U') IS NULL
CREATE TABLE dbo.SeatHold (
    HoldID      INT IDENTITY(1,1) PRIMARY KEY,
    ShowtimeID  INT       NOT NULL,
    SeatID      INT       NOT NULL,
    UserID      INT       NOT NULL,
    ExpiresAt   DATETIME2 NOT NULL,
    CreatedAt   DATETIME2 NOT NULL CONSTRAINT DF_SeatHold_CreatedAt DEFAULT SYSDATETIME(),
    CONSTRAINT FK_SeatHold_Showtime FOREIGN KEY (ShowtimeID) REFERENCES dbo.Showtime(ShowtimeID) ON DELETE CASCADE,
    CONSTRAINT FK_SeatHold_Seat     FOREIGN KEY (SeatID)     REFERENCES dbo.Seat(SeatID),
    CONSTRAINT FK_SeatHold_User     FOREIGN KEY (UserID)     REFERENCES dbo.[User](UserID),
    CONSTRAINT UQ_SeatHold_Seat     UNIQUE (ShowtimeID, SeatID)
);
GO
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'IX_SeatHold_ExpiresAt')
CREATE INDEX IX_SeatHold_ExpiresAt ON dbo.SeatHold(ExpiresAt);
GO

/* =====================================================================
   6. KHUYẾN MÃI, ĐẶT VÉ, VÉ, THANH TOÁN, ĐỒ ĂN
   ===================================================================== */

IF OBJECT_ID('dbo.Promotion', 'U') IS NULL
CREATE TABLE dbo.Promotion (
    PromoID         INT IDENTITY(1,1) PRIMARY KEY,
    PromoCode       VARCHAR(30)   NOT NULL,
    DiscountAmount  DECIMAL(12,2) NOT NULL,
    MinSpend        DECIMAL(12,2) NOT NULL CONSTRAINT DF_Promotion_MinSpend DEFAULT 0,
    StartDate       DATE          NOT NULL,
    EndDate         DATE          NOT NULL,
    UsageLimit      INT           NOT NULL CONSTRAINT DF_Promotion_UsageLimit DEFAULT 0,
    CONSTRAINT UQ_Promotion_Code   UNIQUE (PromoCode),
    CONSTRAINT CK_Promotion_Amount CHECK (DiscountAmount >= 0 AND MinSpend >= 0 AND UsageLimit >= 0),
    CONSTRAINT CK_Promotion_Dates  CHECK (EndDate >= StartDate)
);
GO

/* Status: Pending (giữ ghế chờ trả tiền) -> Confirmed (đã trả) | Cancelled.
   Channel: kênh bán (vd Online / Counter - nhóm chốt giá trị). StaffID: thu ngân bán tại quầy.
   TotalAmount: số tiền cuối cùng phải trả (đã trừ voucher). */
IF OBJECT_ID('dbo.Booking', 'U') IS NULL
CREATE TABLE dbo.Booking (
    BookingID     INT IDENTITY(1,1) PRIMARY KEY,
    UserID        INT           NOT NULL,
    PromoID       INT           NULL,
    BookingDate   DATETIME2     NOT NULL CONSTRAINT DF_Booking_BookingDate DEFAULT SYSDATETIME(),
    TotalAmount   DECIMAL(12,2) NOT NULL CONSTRAINT DF_Booking_TotalAmount DEFAULT 0,
    Status        VARCHAR(20)   NOT NULL CONSTRAINT DF_Booking_Status DEFAULT 'Pending',
    Channel       VARCHAR(20)   NULL,
    StaffID       INT           NULL,
    CancelledAt   DATETIME2     NULL,
    RefundAmount  DECIMAL(12,2) NULL,
    CONSTRAINT FK_Booking_User      FOREIGN KEY (UserID)  REFERENCES dbo.[User](UserID),
    CONSTRAINT FK_Booking_Staff     FOREIGN KEY (StaffID) REFERENCES dbo.[User](UserID),
    CONSTRAINT FK_Booking_Promotion FOREIGN KEY (PromoID) REFERENCES dbo.Promotion(PromoID),
    CONSTRAINT CK_Booking_Amount    CHECK (TotalAmount >= 0 AND (RefundAmount IS NULL OR RefundAmount >= 0)),
    CONSTRAINT CK_Booking_Status    CHECK (Status IN ('Pending', 'Confirmed', 'Cancelled'))
);
GO
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'IX_Booking_User')
CREATE INDEX IX_Booking_User ON dbo.Booking(UserID, BookingDate DESC);
GO

/* Status: Valid | Checked-In | Cancelled. Một ghế của một suất chỉ có 1 vé còn hiệu lực. */
IF OBJECT_ID('dbo.Ticket', 'U') IS NULL
CREATE TABLE dbo.Ticket (
    TicketID    INT IDENTITY(1,1) PRIMARY KEY,
    BookingID   INT           NOT NULL,
    ShowtimeID  INT           NOT NULL,
    SeatID      INT           NOT NULL,
    TicketCode  VARCHAR(30)   NOT NULL,
    FinalPrice  DECIMAL(12,2) NOT NULL,
    Status      VARCHAR(20)   NOT NULL CONSTRAINT DF_Ticket_Status DEFAULT 'Valid',
    CONSTRAINT FK_Ticket_Booking  FOREIGN KEY (BookingID)  REFERENCES dbo.Booking(BookingID),
    CONSTRAINT FK_Ticket_Showtime FOREIGN KEY (ShowtimeID) REFERENCES dbo.Showtime(ShowtimeID),
    CONSTRAINT FK_Ticket_Seat     FOREIGN KEY (SeatID)     REFERENCES dbo.Seat(SeatID),
    CONSTRAINT UQ_Ticket_Code     UNIQUE (TicketCode),
    CONSTRAINT CK_Ticket_Price    CHECK (FinalPrice >= 0),
    CONSTRAINT CK_Ticket_Status   CHECK (Status IN ('Valid', 'Checked-In', 'Cancelled'))
);
GO
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'UX_Ticket_Seat_Active')
CREATE UNIQUE INDEX UX_Ticket_Seat_Active ON dbo.Ticket(ShowtimeID, SeatID) WHERE Status <> 'Cancelled';
GO
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'IX_Ticket_Booking')
CREATE INDEX IX_Ticket_Booking ON dbo.Ticket(BookingID);
GO

/* Mỗi Booking có 1 Payment.
   PaymentMethod: VNPay | MoMo | Cash | Credit Card. GatewayStatus: Pending | Success | Failed | Refunded. */
IF OBJECT_ID('dbo.Payment', 'U') IS NULL
CREATE TABLE dbo.Payment (
    PaymentID        INT IDENTITY(1,1) PRIMARY KEY,
    BookingID        INT           NOT NULL,
    Amount           DECIMAL(12,2) NOT NULL,
    PaymentMethod    VARCHAR(20)   NOT NULL,
    PaymentTime      DATETIME2     NULL,
    TransactionCode  VARCHAR(100)  NULL,
    GatewayStatus    VARCHAR(20)   NOT NULL CONSTRAINT DF_Payment_GatewayStatus DEFAULT 'Pending',
    CONSTRAINT FK_Payment_Booking FOREIGN KEY (BookingID) REFERENCES dbo.Booking(BookingID),
    CONSTRAINT UQ_Payment_Booking UNIQUE (BookingID),
    CONSTRAINT CK_Payment_Amount  CHECK (Amount >= 0),
    CONSTRAINT CK_Payment_Method  CHECK (PaymentMethod IN ('VNPay', 'MoMo', 'Cash', 'Credit Card')),
    CONSTRAINT CK_Payment_Status  CHECK (GatewayStatus IN ('Pending', 'Success', 'Failed', 'Refunded'))
);
GO

/* Đồ ăn / nước uống. */
IF OBJECT_ID('dbo.F_B', 'U') IS NULL
CREATE TABLE dbo.F_B (
    ItemID    INT IDENTITY(1,1) PRIMARY KEY,
    ItemName  NVARCHAR(100) NOT NULL,
    Category  NVARCHAR(50)  NOT NULL,
    Price     DECIMAL(12,2) NOT NULL,
    CONSTRAINT UQ_F_B_Name  UNIQUE (ItemName),
    CONSTRAINT CK_F_B_Price CHECK (Price >= 0)
);
GO

/* Đồ ăn trong một đơn (Booking_F_B của Nam = F_BOrder của Khánh). */
IF OBJECT_ID('dbo.Booking_F_B', 'U') IS NULL
CREATE TABLE dbo.Booking_F_B (
    BookingID    INT         NOT NULL,
    ItemID       INT         NOT NULL,
    Quantity     INT         NOT NULL,
    OrderStatus  VARCHAR(20) NOT NULL CONSTRAINT DF_Booking_F_B_OrderStatus DEFAULT 'Pending',
    OrderDate    DATETIME2   NOT NULL CONSTRAINT DF_Booking_F_B_OrderDate DEFAULT SYSDATETIME(),
    CONSTRAINT PK_Booking_F_B PRIMARY KEY (BookingID, ItemID),
    CONSTRAINT FK_Booking_F_B_Booking FOREIGN KEY (BookingID) REFERENCES dbo.Booking(BookingID) ON DELETE CASCADE,
    CONSTRAINT FK_Booking_F_B_Item    FOREIGN KEY (ItemID)    REFERENCES dbo.F_B(ItemID),
    CONSTRAINT CK_Booking_F_B_Qty     CHECK (Quantity > 0)
);
GO

/* =====================================================================
   AI CẦN SỬA CODE ĐỂ KHỚP DATABASE NÀY
   ---------------------------------------------------------------------
   Hoàng    : đã sửa xong (DAO theo tên bảng/cột mới, Capacity, PosterURL/TrailerURL).
   Đức Hiệp : Movie.Genres là cột tính (STRING_AGG từ MovieGenre + Genre) - giữ nguyên câu SELECT.
   Hoàng Nam: WorkShift giữ cách ghi vào AuditLog; Promotion, Payment, F_B khớp model.
   Tuyết    : EmailVerification thêm CreatedAt (có DEFAULT, không cần sửa INSERT).
   Khánh    : F_BOrder -> bảng Booking_F_B (BookingID, ItemID, Quantity, OrderStatus, OrderDate);
              UnitPrice lấy từ F_B.Price khi JOIN.
   Mọi người: [User] có BranchID (nhân viên) và Email UNIQUE.
   ===================================================================== */
