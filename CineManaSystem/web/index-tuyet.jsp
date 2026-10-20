<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%
    // Lấy đối tượng User từ Session (kiểm tra cả "User" hoa và "user" thường)
    Object sessionUser = session.getAttribute("User");
    if (sessionUser == null) {
        sessionUser = session.getAttribute("user");
    }

    // Khai báo biến hiển thị mặc định
    String fullName = "Nguyễn Văn A";
    String email = "user@example.com";

    // Đọc thông tin từ Session qua Reflection để tránh lỗi ClassCastException
    if (sessionUser != null) {
        try {
            java.lang.reflect.Method getFullNameMethod = sessionUser.getClass().getMethod("getFullName");
            Object nameObj = getFullNameMethod.invoke(sessionUser);
            if (nameObj != null && !nameObj.toString().trim().isEmpty()) {
                fullName = nameObj.toString();
            }
        } catch (Exception e) {
            try {
                java.lang.reflect.Method getNameMethod = sessionUser.getClass().getMethod("getName");
                Object nameObj = getNameMethod.invoke(sessionUser);
                if (nameObj != null) fullName = nameObj.toString();
            } catch (Exception ex) {}
        }

        try {
            java.lang.reflect.Method getEmailMethod = sessionUser.getClass().getMethod("getEmail");
            Object emailObj = getEmailMethod.invoke(sessionUser);
            if (emailObj != null) email = emailObj.toString();
        } catch (Exception e) {}
    }
%>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>CineGo - Hệ thống Đặt vé Phim Trực tuyến</title>

    <!-- FontAwesome Icons -->
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css">

    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/home.css">
</head>
<body>

    <!-- 1. TOP NAVBAR -->
    <header class="navbar">
        <a href="<%= request.getContextPath() %>/index-tuyet.jsp" class="brand-logo" title="Trang chủ CineGo">
            <div class="logo-badge">CG</div>
            <div class="logo-text">CineGo</div>
        </a>

        <nav>
            <ul class="nav-menu">
                <li><a href="#movies" class="nav-item active">Lịch chiếu theo rạp</a></li>
                <li><a href="#movies" class="nav-item">Phim</a></li>
                <li><a href="#cinemas" class="nav-item">Rạp</a></li>
                <li><a href="#pricing" class="nav-item">Giá vé</a></li>
                <li><a href="#news" class="nav-item">Tin mới & Ưu đãi</a></li>
                
                <% if (sessionUser != null) { %>
                    <li><a href="<%= request.getContextPath() %>/customer/profile" class="nav-item">Thành viên</a></li>
                    <li><a href="<%= request.getContextPath() %>/customer/profile" class="nav-item">Vé của tôi</a></li>
                <% } else { %>
                    <li><a href="<%= request.getContextPath() %>/login" class="nav-item">Thành viên</a></li>
                    <li><a href="<%= request.getContextPath() %>/login" class="nav-item">Vé của tôi</a></li>
                <% } %>
            </ul>
        </nav>

        <!-- KHU VỰC HIỂN THỊ ĐÃ ĐĂNG NHẬP / CHƯA ĐĂNG NHẬP -->
        <div class="auth-group">
            <% if (sessionUser != null) { %>
                <div class="user-menu-container">
                    <div class="user-profile-btn">
                        <span class="user-name"><%= fullName %></span>
                        <i class="fa-solid fa-chevron-down chevron-small"></i>
                    </div>

                    <!-- Dropdown Menu -->
                    <div class="user-dropdown">
                        <div class="dropdown-header">
                            <div class="dropdown-header-title">Tài khoản thành viên</div>
                            <div class="dropdown-header-email"><%= email %></div>
                        </div>
                        <ul class="dropdown-list">
                            <li>
                                <a href="<%= request.getContextPath() %>/customer/profile" class="dropdown-item">
                                    <div class="dropdown-item-left">
                                        <i class="fa-regular fa-user"></i>
                                        <span>Thông tin cá nhân</span>
                                    </div>
                                </a>
                            </li>
                            <li>
                                <a href="<%= request.getContextPath() %>/customer/tickets" class="dropdown-item">
                                    <div class="dropdown-item-left">
                                        <i class="fa-regular fa-id-card"></i>
                                        <span>Vé của tôi</span>
                                    </div>
                                </a>
                            </li>
                            <li>
                                <a href="<%= request.getContextPath() %>/customer/change-password" class="dropdown-item">
                                    <div class="dropdown-item-left">
                                        <i class="fa-solid fa-key"></i>
                                        <span>Đổi mật khẩu</span>
                                    </div>
                                </a>
                            </li>
                            <li>
                                <a href="<%= request.getContextPath() %>/customer/notifications" class="dropdown-item">
                                    <div class="dropdown-item-left">
                                        <i class="fa-regular fa-bell"></i>
                                        <span>Thông báo</span>
                                    </div>
                                    <span class="badge-count">3</span>
                                </a>
                            </li>
                            <li>
                                <a href="<%= request.getContextPath() %>/logout" class="dropdown-item logout-item">
                                    <div class="dropdown-item-left">
                                        <i class="fa-solid fa-right-from-bracket"></i>
                                        <span>Đăng xuất</span>
                                    </div>
                                </a>
                            </li>
                        </ul>
                    </div>
                </div>
            <% } else { %>
                <a href="<%= request.getContextPath() %>/register" class="btn-register">Đăng ký</a>
                <a href="<%= request.getContextPath() %>/login" class="btn-login-pill">Đăng nhập</a>
            <% } %>
        </div>
    </header>

    <!-- MAIN CONTAINER -->
    <main class="main-container">

        <!-- 2. POSTER TO CAROUSEL NGAY DƯỚI NAVBAR -->
        <section class="carousel-banner-wrapper">
            <div class="carousel-banner-track">
                
                <!-- Slide 1: Deadpool & Wolverine -->
                <div class="carousel-slide-item slide-bg-1">
                    <div class="carousel-slide-overlay"></div>
                    <div class="slide-content-box">
                        <span class="badge-hot-tag">BOM TẤN HOT</span>
                        <h1 class="hero-banner-title">DEADPOOL & WOLVERINE</h1>
                        <p class="hero-banner-meta">Khởi chiếu: 26/07/2026 • Hành Động • 2g 07p • T18</p>
                        <p class="hero-banner-desc">Trận chiến bom tấn đỉnh cao giữa Deadpool và Wolverine mang lại những pha hành động nghẹt thở và hài hước không thể bỏ lỡ tại CineGo.</p>
                        <a href="#movies" class="btn-banner-action"><i class="fa-solid fa-ticket"></i> ĐẶT VÉ NGAY</a>
                    </div>
                </div>

                <!-- Slide 2: Godzilla x Kong 2 -->
                <div class="carousel-slide-item slide-bg-2">
                    <div class="carousel-slide-overlay"></div>
                    <div class="slide-content-box">
                        <span class="badge-hot-tag bg-blue">SẮP CHIẾU</span>
                        <h1 class="hero-banner-title">GODZILLA X KONG 2</h1>
                        <p class="hero-banner-meta">Khởi chiếu: 15/08/2026 • Viễn Tưởng • 1g 55p • P</p>
                        <p class="hero-banner-desc">Cuộc đại chiến siêu quái thú trở lại màn ảnh rộng với chuẩn định dạng IMAX đỉnh cao kỹ xảo âm thanh siêu việt.</p>
                        <a href="#movies" class="btn-banner-action bg-blue"><i class="fa-solid fa-calendar-check"></i> ĐẶT TRƯỚC VÉ</a>
                    </div>
                </div>

                <!-- Slide 3: Lật Mặt 7 -->
                <div class="carousel-slide-item slide-bg-3">
                    <div class="carousel-slide-overlay"></div>
                    <div class="slide-content-box">
                        <span class="badge-hot-tag bg-amber">SUẤT ĐẶC BIỆT</span>
                        <h1 class="hero-banner-title">LẬT MẶT 7: MỘT ĐIỀU ƯỚC</h1>
                        <p class="hero-banner-meta">Khởi chiếu: 30/04/2026 • Tình Cảm, Gia Đình • 2g 18p • P</p>
                        <p class="hero-banner-desc">Tác phẩm điện ảnh xúc động về tình thân gia đình của đạo diễn Lý Hải đang làm khuấy đảo cảm xúc khán giả Việt.</p>
                        <a href="#movies" class="btn-banner-action bg-amber"><i class="fa-solid fa-fire"></i> MUA VÉ NGAY</a>
                    </div>
                </div>

            </div>
        </section>

        <!-- 3. TABS TOOLBAR VÀ FILTER TÌM KIẾM (ĐÚNG NHƯ ẢNH MẪU) -->
        <section class="film-toolbar" id="movies">
            <ul class="tab-list">
                <li><a href="#movies" class="tab-item active">PHIM ĐANG CHIẾU</a></li>
                <li><a href="#movies" class="tab-item">PHIM SẮP CHIẾU</a></li>
                <li><a href="#movies" class="tab-item">SUẤT CHIẾU ĐẶC BIỆT</a></li>
            </ul>

            <!-- Phần Bộ lọc thể loại & Ô tìm kiếm thêm mới bên phải -->
            <div class="filter-search-group">
                <select class="select-category">
                    <option value="">Tất Cả Thể Loại</option>
                    <option value="hanh-dong">Hành Động</option>
                    <option value="kinh-di">Kinh Dị</option>
                    <option value="hoat-hinh">Hoạt Hình</option>
                    <option value="tinh-cam">Tình Cảm</option>
                    <option value="vien-tuong">Viễn Tưởng</option>
                </select>
                <input type="text" class="input-search-film" placeholder="Tìm kiếm phim...">
            </div>
        </section>

        <!-- 4. MOVIE CARD GRID (8 PHIM) -->
        <section class="movie-grid">

            <!-- Card 1 -->
            <div class="movie-card">
                <a href="<%= request.getContextPath() %>/movie/detail?id=1" class="poster-wrapper">
                    <span class="age-badge age-t18">T18</span>
                    <img src="https://images.unsplash.com/photo-1509198397868-475647b2a1e5?q=80&w=600&auto=format&fit=crop" alt="Người Nhện Trở Về" class="movie-poster">
                </a>
                <div class="movie-info">
                    <div>
                        <a href="<%= request.getContextPath() %>/movie/detail?id=1" class="movie-title">Người Nhện: Trở Về</a>
                        <p class="movie-meta">Hành Động • 2g 15p</p>
                    </div>
                    <a href="<%= request.getContextPath() %>/login" class="btn-card-buy">Mua Vé</a>
                </div>
            </div>

            <!-- Card 2 -->
            <div class="movie-card">
                <a href="<%= request.getContextPath() %>/movie/detail?id=2" class="poster-wrapper">
                    <span class="age-badge age-p">P</span>
                    <img src="https://images.unsplash.com/photo-1507525428034-b723cf961d3e?q=80&w=600&auto=format&fit=crop" alt="Godzilla x Kong 2" class="movie-poster">
                </a>
                <div class="movie-info">
                    <div>
                        <a href="<%= request.getContextPath() %>/movie/detail?id=2" class="movie-title">Godzilla x Kong 2</a>
                        <p class="movie-meta">Hành Động • 1g 55p</p>
                    </div>
                    <a href="<%= request.getContextPath() %>/login" class="btn-card-buy">Mua Vé</a>
                </div>
            </div>

            <!-- Card 3 -->
            <div class="movie-card">
                <a href="<%= request.getContextPath() %>/movie/detail?id=3" class="poster-wrapper">
                    <span class="age-badge age-t13">T13</span>
                    <img src="https://images.unsplash.com/photo-1511919884226-fd3cad34687c?q=80&w=600&auto=format&fit=crop" alt="Ma Nhà Hát" class="movie-poster">
                </a>
                <div class="movie-info">
                    <div>
                        <a href="<%= request.getContextPath() %>/movie/detail?id=3" class="movie-title">Ma Nhà Hát</a>
                        <p class="movie-meta">Kinh Dị • 1g 40p</p>
                    </div>
                    <a href="<%= request.getContextPath() %>/login" class="btn-card-buy">Mua Vé</a>
                </div>
            </div>

            <!-- Card 4 -->
            <div class="movie-card">
                <a href="<%= request.getContextPath() %>/movie/detail?id=4" class="poster-wrapper">
                    <span class="age-badge age-t18">T18</span>
                    <img src="https://images.unsplash.com/photo-1544551763-46a013bb70d5?q=80&w=600&auto=format&fit=crop" alt="Biệt Đội Siêu Anh Hùng" class="movie-poster">
                </a>
                <div class="movie-info">
                    <div>
                        <a href="<%= request.getContextPath() %>/movie/detail?id=4" class="movie-title">Biệt Đội Siêu Anh Hùng</a>
                        <p class="movie-meta">Hành Động • 2g 30p</p>
                    </div>
                    <a href="<%= request.getContextPath() %>/login" class="btn-card-buy">Mua Vé</a>
                </div>
            </div>

            <!-- Card 5 -->
            <div class="movie-card">
                <a href="<%= request.getContextPath() %>/movie/detail?id=5" class="poster-wrapper">
                    <span class="age-badge age-p">P</span>
                    <img src="https://images.unsplash.com/photo-1536440136628-849c177e76a1?q=80&w=600&auto=format&fit=crop" alt="Vùng Đất Câm Lặng" class="movie-poster">
                </a>
                <div class="movie-info">
                    <div>
                        <a href="<%= request.getContextPath() %>/movie/detail?id=5" class="movie-title">Vùng Đất Câm Lặng: Ngày 1</a>
                        <p class="movie-meta">Kinh Dị, Giật Gân • 1g 40p</p>
                    </div>
                    <a href="<%= request.getContextPath() %>/login" class="btn-card-buy">Mua Vé</a>
                </div>
            </div>

            <!-- Card 6 -->
            <div class="movie-card">
                <a href="<%= request.getContextPath() %>/movie/detail?id=6" class="poster-wrapper">
                    <span class="age-badge age-t13">T13</span>
                    <img src="https://images.unsplash.com/photo-1517604931442-7e0c8ed2963c?q=80&w=600&auto=format&fit=crop" alt="Dune 2" class="movie-poster">
                </a>
                <div class="movie-info">
                    <div>
                        <a href="<%= request.getContextPath() %>/movie/detail?id=6" class="movie-title">Dune: Hành Tinh Cát 2</a>
                        <p class="movie-meta">Viễn Tưởng, Phiêu Lưu • 2g 46p</p>
                    </div>
                    <a href="<%= request.getContextPath() %>/login" class="btn-card-buy">Mua Vé</a>
                </div>
            </div>

            <!-- Card 7 -->
            <div class="movie-card">
                <a href="<%= request.getContextPath() %>/movie/detail?id=7" class="poster-wrapper">
                    <span class="age-badge age-p">P</span>
                    <img src="https://images.unsplash.com/photo-1535016120720-40c646be5580?q=80&w=600&auto=format&fit=crop" alt="Doraemon" class="movie-poster">
                </a>
                <div class="movie-info">
                    <div>
                        <a href="<%= request.getContextPath() %>/movie/detail?id=7" class="movie-title">Doraemon: Bản Tình Ca Đảo Táo</a>
                        <p class="movie-meta">Hoạt Hình, Gia Đình • 1g 55p</p>
                    </div>
                    <a href="<%= request.getContextPath() %>/login" class="btn-card-buy">Mua Vé</a>
                </div>
            </div>

            <!-- Card 8 -->
            <div class="movie-card">
                <a href="<%= request.getContextPath() %>/movie/detail?id=8" class="poster-wrapper">
                    <span class="age-badge age-t18">T18</span>
                    <img src="https://images.unsplash.com/photo-1478720568477-152d9b164e26?q=80&w=600&auto=format&fit=crop" alt="Minions 4" class="movie-poster">
                </a>
                <div class="movie-info">
                    <div>
                        <a href="<%= request.getContextPath() %>/movie/detail?id=8" class="movie-title">Kẻ Trộm Mặt Trăng 4</a>
                        <p class="movie-meta">Hoạt Hình, Hài Hước • 1g 35p</p>
                    </div>
                    <a href="<%= request.getContextPath() %>/login" class="btn-card-buy">Mua Vé</a>
                </div>
            </div>

        </section>

    </main>

    <!-- 5. FOOTER -->
    <footer class="footer">
        <div>
            <strong class="footer-brand">CineGo CMS Website</strong>
            <p>Hệ thống đặt vé xem phim trực tuyến tiện lợi và nhanh chóng.</p>
        </div>
        <div class="footer-right">
            <strong class="footer-title">Thông tin liên hệ</strong>
            <p>Email: hotro@cinego.vn | Hotline: 1900 1234</p>
        </div>
    </footer>

</body>
</html>