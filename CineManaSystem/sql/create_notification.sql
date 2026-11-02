-- Bảng thông báo trong web (chuông thông báo trên header).
-- Chạy một lần trên database CineManagementSystem. Chạy lại cũng không sao (có kiểm tra tồn tại).
USE CineManagementSystem;
GO

IF OBJECT_ID(N'dbo.Notification', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.Notification (
        NotificationID INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_Notification PRIMARY KEY,
        UserID         INT           NOT NULL,
        Type           VARCHAR(30)   NOT NULL,           -- MOVIE, MOVIE_STATUS, ...
        Title          NVARCHAR(150) NOT NULL,
        Content        NVARCHAR(500) NULL,
        Link           VARCHAR(300)  NULL,               -- đường dẫn trong ứng dụng, vd /movies/detail?id=6
        IsRead         BIT           NOT NULL CONSTRAINT DF_Notification_IsRead DEFAULT 0,
        CreatedAt      DATETIME2     NOT NULL CONSTRAINT DF_Notification_CreatedAt DEFAULT SYSDATETIME(),
        CONSTRAINT FK_Notification_User FOREIGN KEY (UserID) REFERENCES dbo.[User](UserID) ON DELETE CASCADE
    );
    CREATE INDEX IX_Notification_User ON dbo.Notification (UserID, IsRead, NotificationID DESC);
END
GO
