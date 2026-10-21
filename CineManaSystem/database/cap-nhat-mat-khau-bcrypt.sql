/* Dùng khi database đã nạp dữ liệu mẫu cũ (mật khẩu lưu dạng chữ thường '123456').
   Trang /login của nhóm kiểm tra mật khẩu bằng BCrypt nên phải đổi sang dạng mã hóa.
   Sau khi chạy, mật khẩu đăng nhập vẫn là 123456. */
USE CineManagementSystem;
GO
UPDATE [User]
SET PasswordHash = '$2a$12$4SD/DonB7GvibR8Wmiz9Y.tVVVzYNSgO1.hxGgyPLrlJEBNiZTgru'
WHERE PasswordHash = '123456';
GO
