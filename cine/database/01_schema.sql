/* =====================================================================
   Cine Management System - Branch Management + Movie Management
   SQL Server. Chạy trên DB "Cine Management System".

   Thay đổi so với SDS bản nháp:
   - Tách Genre khỏi Movies  -> Genres + MovieGenres (n-n)
   - Tách loại phòng         -> RoomTypes (1-n Rooms)
   - Tách loại ghế           -> SeatTypes (1-n Seats)
   - Tách poster/trailer     -> MovieMedia (1-n Movies)
   - Thêm BranchOperatingHours, MovieBranches
   - Toàn bộ text tiếng Việt dùng NVARCHAR
   - Đặt tên thống nhất: bảng số nhiều PascalCase, cột snake_case
   ===================================================================== */

/* ---------------------------------------------------------------------
   Nếu DB đang có bảng theo SDS cũ, sao lưu rồi bỏ comment khối này
   để xoá theo đúng thứ tự khoá ngoại trước khi tạo lại.
   --------------------------------------------------------------------- */
-- DROP TABLE IF EXISTS dbo.MovieBranches, dbo.MovieMedia, dbo.MovieGenres;
-- DROP TABLE IF EXISTS dbo.Showtimes;          -- chỉ khi module Showtime đồng ý
-- DROP TABLE IF EXISTS dbo.Seats, dbo.SeatTypes;
-- DROP TABLE IF EXISTS dbo.Rooms, dbo.Room, dbo.RoomTypes;
-- DROP TABLE IF EXISTS dbo.Movies, dbo.Genres;
-- DROP TABLE IF EXISTS dbo.BranchOperatingHours, dbo.Branches;
-- GO

/* ============================ BRANCH ============================ */

IF OBJECT_ID('dbo.Branches', 'U') IS NULL
CREATE TABLE dbo.Branches (
    branch_id   INT IDENTITY(1,1) PRIMARY KEY,
    name        NVARCHAR(150) NOT NULL,
    address     NVARCHAR(255) NOT NULL,
    city        NVARCHAR(100) NOT NULL,
    hotline     VARCHAR(20)   NOT NULL,
    email       VARCHAR(150)  NULL,
    status      VARCHAR(20)   NOT NULL CONSTRAINT DF_Branches_status DEFAULT 'Active',
    created_at  DATETIME2     NOT NULL CONSTRAINT DF_Branches_created DEFAULT SYSDATETIME(),
    CONSTRAINT UQ_Branches_name   UNIQUE (name),
    CONSTRAINT CK_Branches_status CHECK (status IN ('Active', 'Inactive'))
);
GO

/* day_of_week: 1 = Thứ Hai ... 7 = Chủ Nhật (ISO-8601).
   close_time < open_time nghĩa là đóng cửa sau nửa đêm. */
IF OBJECT_ID('dbo.BranchOperatingHours', 'U') IS NULL
CREATE TABLE dbo.BranchOperatingHours (
    branch_id   INT     NOT NULL,
    day_of_week TINYINT NOT NULL,
    is_closed   BIT     NOT NULL CONSTRAINT DF_BOH_closed DEFAULT 0,
    open_time   TIME(0) NULL,
    close_time  TIME(0) NULL,
    CONSTRAINT PK_BranchOperatingHours PRIMARY KEY (branch_id, day_of_week),
    CONSTRAINT FK_BOH_Branch FOREIGN KEY (branch_id) REFERENCES dbo.Branches(branch_id) ON DELETE CASCADE,
    CONSTRAINT CK_BOH_day    CHECK (day_of_week BETWEEN 1 AND 7),
    CONSTRAINT CK_BOH_times  CHECK (is_closed = 1
                                    OR (open_time IS NOT NULL AND close_time IS NOT NULL AND open_time <> close_time))
);
GO

/* ============================ ROOM ============================ */

IF OBJECT_ID('dbo.RoomTypes', 'U') IS NULL
CREATE TABLE dbo.RoomTypes (
    room_type_id INT IDENTITY(1,1) PRIMARY KEY,
    name         NVARCHAR(50)  NOT NULL,
    surcharge    DECIMAL(12,2) NOT NULL CONSTRAINT DF_RoomTypes_surcharge DEFAULT 0,
    description  NVARCHAR(255) NULL,
    status       VARCHAR(20)   NOT NULL CONSTRAINT DF_RoomTypes_status DEFAULT 'Active',
    CONSTRAINT UQ_RoomTypes_name      UNIQUE (name),
    CONSTRAINT CK_RoomTypes_surcharge CHECK (surcharge >= 0),
    CONSTRAINT CK_RoomTypes_status    CHECK (status IN ('Active', 'Inactive'))
);
GO

/* Bỏ cột capacity của SDS: sức chứa = số ghế Active trong Seats,
   tránh hai nơi lưu cùng một con số rồi lệch nhau. */
IF OBJECT_ID('dbo.Rooms', 'U') IS NULL
CREATE TABLE dbo.Rooms (
    room_id       INT IDENTITY(1,1) PRIMARY KEY,
    branch_id     INT          NOT NULL,
    room_type_id  INT          NOT NULL,
    room_name     NVARCHAR(50) NOT NULL,
    total_rows    TINYINT      NOT NULL CONSTRAINT DF_Rooms_rows DEFAULT 0,
    total_columns TINYINT      NOT NULL CONSTRAINT DF_Rooms_cols DEFAULT 0,
    status        VARCHAR(20)  NOT NULL CONSTRAINT DF_Rooms_status DEFAULT 'Active',
    CONSTRAINT FK_Rooms_Branch   FOREIGN KEY (branch_id)    REFERENCES dbo.Branches(branch_id),
    CONSTRAINT FK_Rooms_RoomType FOREIGN KEY (room_type_id) REFERENCES dbo.RoomTypes(room_type_id),
    CONSTRAINT UQ_Rooms_name     UNIQUE (branch_id, room_name),
    CONSTRAINT CK_Rooms_status   CHECK (status IN ('Active', 'Maintenance', 'Inactive')),
    CONSTRAINT CK_Rooms_size     CHECK (total_rows <= 26 AND total_columns <= 30)
);
GO

IF OBJECT_ID('dbo.SeatTypes', 'U') IS NULL
CREATE TABLE dbo.SeatTypes (
    seat_type_id INT IDENTITY(1,1) PRIMARY KEY,
    name         NVARCHAR(30)  NOT NULL,
    surcharge    DECIMAL(12,2) NOT NULL CONSTRAINT DF_SeatTypes_surcharge DEFAULT 0,
    color        VARCHAR(7)    NOT NULL CONSTRAINT DF_SeatTypes_color DEFAULT '#8a8fa8',
    CONSTRAINT UQ_SeatTypes_name      UNIQUE (name),
    CONSTRAINT CK_SeatTypes_surcharge CHECK (surcharge >= 0)
);
GO

/* row_num / col_num = toạ độ 1-based của ghế trong ma trận (hàng 1 = A). Ô không có dòng nào = lối đi.
   row_label + seat_number = nhãn in trên vé, vd "C7" (seat_number hiện bằng col_num). */
IF OBJECT_ID('dbo.Seats', 'U') IS NULL
CREATE TABLE dbo.Seats (
    seat_id      INT IDENTITY(1,1) PRIMARY KEY,
    room_id      INT         NOT NULL,
    seat_type_id INT         NOT NULL,
    row_num      TINYINT     NOT NULL,
    col_num      TINYINT     NOT NULL,
    row_label    VARCHAR(2)  NOT NULL,
    seat_number  TINYINT     NOT NULL,
    status       VARCHAR(20) NOT NULL CONSTRAINT DF_Seats_status DEFAULT 'Active',
    CONSTRAINT FK_Seats_Room     FOREIGN KEY (room_id)      REFERENCES dbo.Rooms(room_id) ON DELETE CASCADE,
    CONSTRAINT FK_Seats_SeatType FOREIGN KEY (seat_type_id) REFERENCES dbo.SeatTypes(seat_type_id),
    CONSTRAINT UQ_Seats_position UNIQUE (room_id, row_label, seat_number),
    CONSTRAINT UQ_Seats_coord    UNIQUE (room_id, row_num, col_num),
    CONSTRAINT CK_Seats_coord    CHECK (row_num BETWEEN 1 AND 26 AND col_num BETWEEN 1 AND 30),
    CONSTRAINT CK_Seats_status   CHECK (status IN ('Active', 'Broken'))
);
GO

/* ============================ MOVIE ============================ */

IF OBJECT_ID('dbo.Genres', 'U') IS NULL
CREATE TABLE dbo.Genres (
    genre_id    INT IDENTITY(1,1) PRIMARY KEY,
    name        NVARCHAR(50)  NOT NULL,
    description NVARCHAR(255) NULL,
    CONSTRAINT UQ_Genres_name UNIQUE (name)
);
GO

/* Bỏ age_rating 'C': theo quy định phân loại phim của Việt Nam, 'C' là phim
   không được phép phổ biến, nên rạp không thể có phim này. */
IF OBJECT_ID('dbo.Movies', 'U') IS NULL
CREATE TABLE dbo.Movies (
    movie_id       INT IDENTITY(1,1) PRIMARY KEY,
    title          NVARCHAR(200) NOT NULL,
    description    NVARCHAR(MAX) NULL,
    director       NVARCHAR(150) NULL,
    cast_members   NVARCHAR(500) NULL,
    language       NVARCHAR(50)  NULL,
    duration       INT           NOT NULL,
    release_date   DATE          NOT NULL,
    age_rating     VARCHAR(10)   NOT NULL,
    status         VARCHAR(20)   NOT NULL CONSTRAINT DF_Movies_status DEFAULT 'Coming Soon',
    created_at     DATETIME2     NOT NULL CONSTRAINT DF_Movies_created DEFAULT SYSDATETIME(),
    CONSTRAINT CK_Movies_duration CHECK (duration BETWEEN 1 AND 600),
    CONSTRAINT CK_Movies_rating   CHECK (age_rating IN ('P', 'K', 'T13', 'T16', 'T18')),
    CONSTRAINT CK_Movies_status   CHECK (status IN ('Coming Soon', 'Now Showing', 'End of Screening'))
);
GO

IF OBJECT_ID('dbo.MovieGenres', 'U') IS NULL
CREATE TABLE dbo.MovieGenres (
    movie_id INT NOT NULL,
    genre_id INT NOT NULL,
    CONSTRAINT PK_MovieGenres PRIMARY KEY (movie_id, genre_id),
    CONSTRAINT FK_MovieGenres_Movie FOREIGN KEY (movie_id) REFERENCES dbo.Movies(movie_id) ON DELETE CASCADE,
    CONSTRAINT FK_MovieGenres_Genre FOREIGN KEY (genre_id) REFERENCES dbo.Genres(genre_id)
);
GO

IF OBJECT_ID('dbo.MovieMedia', 'U') IS NULL
CREATE TABLE dbo.MovieMedia (
    media_id   INT IDENTITY(1,1) PRIMARY KEY,
    movie_id   INT           NOT NULL,
    media_type VARCHAR(10)   NOT NULL,
    url        VARCHAR(500)  NOT NULL,
    caption    NVARCHAR(150) NULL,
    is_primary BIT           NOT NULL CONSTRAINT DF_MovieMedia_primary DEFAULT 0,
    created_at DATETIME2     NOT NULL CONSTRAINT DF_MovieMedia_created DEFAULT SYSDATETIME(),
    CONSTRAINT FK_MovieMedia_Movie FOREIGN KEY (movie_id) REFERENCES dbo.Movies(movie_id) ON DELETE CASCADE,
    CONSTRAINT CK_MovieMedia_type  CHECK (media_type IN ('POSTER', 'TRAILER'))
);
GO
/* Mỗi phim chỉ có tối đa 1 poster chính và 1 trailer chính. */
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'UX_MovieMedia_primary')
CREATE UNIQUE INDEX UX_MovieMedia_primary ON dbo.MovieMedia(movie_id, media_type) WHERE is_primary = 1;
GO

IF OBJECT_ID('dbo.MovieBranches', 'U') IS NULL
CREATE TABLE dbo.MovieBranches (
    movie_id       INT       NOT NULL,
    branch_id      INT       NOT NULL,
    available_from DATE      NULL,
    available_to   DATE      NULL,
    assigned_at    DATETIME2 NOT NULL CONSTRAINT DF_MovieBranches_at DEFAULT SYSDATETIME(),
    CONSTRAINT PK_MovieBranches PRIMARY KEY (movie_id, branch_id),
    CONSTRAINT FK_MovieBranches_Movie  FOREIGN KEY (movie_id)  REFERENCES dbo.Movies(movie_id) ON DELETE CASCADE,
    CONSTRAINT FK_MovieBranches_Branch FOREIGN KEY (branch_id) REFERENCES dbo.Branches(branch_id),
    CONSTRAINT CK_MovieBranches_dates  CHECK (available_to IS NULL OR available_from IS NULL
                                              OR available_to >= available_from)
);
GO
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'IX_MovieBranches_branch')
CREATE INDEX IX_MovieBranches_branch ON dbo.MovieBranches(branch_id);
GO

/* ===================== SHOWTIME (module khác) =====================
   Module Branch/Movie chỉ ĐỌC bảng này để kiểm tra ràng buộc
   (không gỡ phòng/phim/chi nhánh đang có suất chiếu).
   Tạo tạm theo SDS nếu nhóm Showtime chưa tạo. */
IF OBJECT_ID('dbo.Showtimes', 'U') IS NULL
CREATE TABLE dbo.Showtimes (
    showtime_id INT IDENTITY(1,1) PRIMARY KEY,
    movie_id    INT           NOT NULL REFERENCES dbo.Movies(movie_id),
    room_id     INT           NOT NULL REFERENCES dbo.Rooms(room_id),
    show_date   DATE          NOT NULL,
    start_time  TIME(0)       NOT NULL,
    end_time    TIME(0)       NOT NULL,
    base_price  DECIMAL(12,2) NOT NULL
);
GO
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'IX_Showtimes_movie_date')
CREATE INDEX IX_Showtimes_movie_date ON dbo.Showtimes(movie_id, show_date) INCLUDE (room_id);
GO
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'IX_Showtimes_room_date')
CREATE INDEX IX_Showtimes_room_date ON dbo.Showtimes(room_id, show_date);
GO
