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

    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/home.css?v=2">
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
                <li><a href="<%= request.getContextPath() %>/movies" class="nav-item">Phim</a></li>
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
                                <a href="<%= request.getContextPath() %>/payment/history" class="dropdown-item">
                                    <div class="dropdown-item-left">
                                        <i class="fa-solid fa-receipt"></i>
                                        <span>Lịch sử thanh toán</span>
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

        <!-- 2. CAROUSEL KHUYẾN MÃI (3 ảnh banner) -->
        <section class="promo-carousel" id="promoCarousel" aria-roledescription="carousel" aria-label="Khuyến mãi CineGo">
            <div class="carousel-track">
                <div class="carousel-slide" aria-hidden="false">
                    <img src="${pageContext.request.contextPath}/assets/img/banners/banner-1.webp" alt="CineGo - Chào mừng bạn mới, khuyến mãi 10%">
                </div>
                <div class="carousel-slide" aria-hidden="true">
                    <img src="${pageContext.request.contextPath}/assets/img/banners/banner-2.webp" alt="Khách hàng dưới 23 tuổi - giá vé tiêu chuẩn chỉ 70.000 đồng" loading="lazy">
                </div>
                <div class="carousel-slide" aria-hidden="true">
                    <img src="${pageContext.request.contextPath}/assets/img/banners/banner-3.webp" alt="Happiness Combo - chỉ từ 149K" loading="lazy">
                </div>
            </div>

            <button type="button" class="carousel-prev" aria-label="Ảnh trước"><i class="fa-solid fa-chevron-left"></i></button>
            <button type="button" class="carousel-next" aria-label="Ảnh sau"><i class="fa-solid fa-chevron-right"></i></button>

            <div class="carousel-dots">
                <button type="button" aria-label="Ảnh 1" aria-current="true"></button>
                <button type="button" aria-label="Ảnh 2"></button>
                <button type="button" aria-label="Ảnh 3"></button>
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
                <a href="<%= request.getContextPath() %>/movies/detail?id=1" class="poster-wrapper">
                    <span class="age-badge age-t18">T18</span>
                    <img src="https://images.unsplash.com/photo-1509198397868-475647b2a1e5?q=80&w=600&auto=format&fit=crop" alt="Người Nhện Trở Về" class="movie-poster">
                </a>
                <div class="movie-info">
                    <div>
                        <a href="<%= request.getContextPath() %>/movies/detail?id=1" class="movie-title">Người Nhện: Trở Về</a>
                        <p class="movie-meta">Hành Động • 2g 15p</p>
                    </div>
                    <a href="<%= request.getContextPath() %>/login" class="btn-card-buy">Mua Vé</a>
                </div>
            </div>

            <!-- Card 2 -->
            <div class="movie-card">
                <a href="<%= request.getContextPath() %>/movies/detail?id=2" class="poster-wrapper">
                    <span class="age-badge age-p">P</span>
                    <img src="https://images.unsplash.com/photo-1507525428034-b723cf961d3e?q=80&w=600&auto=format&fit=crop" alt="Godzilla x Kong 2" class="movie-poster">
                </a>
                <div class="movie-info">
                    <div>
                        <a href="<%= request.getContextPath() %>/movies/detail?id=2" class="movie-title">Godzilla x Kong 2</a>
                        <p class="movie-meta">Hành Động • 1g 55p</p>
                    </div>
                    <a href="<%= request.getContextPath() %>/login" class="btn-card-buy">Mua Vé</a>
                </div>
            </div>

            <!-- Card 3 -->
            <div class="movie-card">
                <a href="<%= request.getContextPath() %>/movies/detail?id=3" class="poster-wrapper">
                    <span class="age-badge age-t13">T13</span>
                    <img src="https://images.unsplash.com/photo-1511919884226-fd3cad34687c?q=80&w=600&auto=format&fit=crop" alt="Ma Nhà Hát" class="movie-poster">
                </a>
                <div class="movie-info">
                    <div>
                        <a href="<%= request.getContextPath() %>/movies/detail?id=3" class="movie-title">Ma Nhà Hát</a>
                        <p class="movie-meta">Kinh Dị • 1g 40p</p>
                    </div>
                    <a href="<%= request.getContextPath() %>/login" class="btn-card-buy">Mua Vé</a>
                </div>
            </div>

            <!-- Card 4 -->
            <div class="movie-card">
                <a href="<%= request.getContextPath() %>/movies/detail?id=4" class="poster-wrapper">
                    <span class="age-badge age-t18">T18</span>
                    <img src="https://images.unsplash.com/photo-1544551763-46a013bb70d5?q=80&w=600&auto=format&fit=crop" alt="Biệt Đội Siêu Anh Hùng" class="movie-poster">
                </a>
                <div class="movie-info">
                    <div>
                        <a href="<%= request.getContextPath() %>/movies/detail?id=4" class="movie-title">Biệt Đội Siêu Anh Hùng</a>
                        <p class="movie-meta">Hành Động • 2g 30p</p>
                    </div>
                    <a href="<%= request.getContextPath() %>/login" class="btn-card-buy">Mua Vé</a>
                </div>
            </div>

            <!-- Card 5 -->
            <div class="movie-card">
                <a href="<%= request.getContextPath() %>/movies/detail?id=5" class="poster-wrapper">
                    <span class="age-badge age-p">P</span>
                    <img src="https://images.unsplash.com/photo-1536440136628-849c177e76a1?q=80&w=600&auto=format&fit=crop" alt="Vùng Đất Câm Lặng" class="movie-poster">
                </a>
                <div class="movie-info">
                    <div>
                        <a href="<%= request.getContextPath() %>/movies/detail?id=5" class="movie-title">Vùng Đất Câm Lặng: Ngày 1</a>
                        <p class="movie-meta">Kinh Dị, Giật Gân • 1g 40p</p>
                    </div>
                    <a href="<%= request.getContextPath() %>/login" class="btn-card-buy">Mua Vé</a>
                </div>
            </div>

            <!-- Card 6 -->
            <div class="movie-card">
                <a href="<%= request.getContextPath() %>/movies/detail?id=6" class="poster-wrapper">
                    <span class="age-badge age-t13">T13</span>
                    <img src="https://images.unsplash.com/photo-1517604931442-7e0c8ed2963c?q=80&w=600&auto=format&fit=crop" alt="Dune 2" class="movie-poster">
                </a>
                <div class="movie-info">
                    <div>
                        <a href="<%= request.getContextPath() %>/movies/detail?id=6" class="movie-title">Dune: Hành Tinh Cát 2</a>
                        <p class="movie-meta">Viễn Tưởng, Phiêu Lưu • 2g 46p</p>
                    </div>
                    <a href="<%= request.getContextPath() %>/login" class="btn-card-buy">Mua Vé</a>
                </div>
            </div>

            <!-- Card 7 -->
            <div class="movie-card">
                <a href="<%= request.getContextPath() %>/movies/detail?id=7" class="poster-wrapper">
                    <span class="age-badge age-p">P</span>
                    <img src="https://images.unsplash.com/photo-1535016120720-40c646be5580?q=80&w=600&auto=format&fit=crop" alt="Doraemon" class="movie-poster">
                </a>
                <div class="movie-info">
                    <div>
                        <a href="<%= request.getContextPath() %>/movies/detail?id=7" class="movie-title">Doraemon: Bản Tình Ca Đảo Táo</a>
                        <p class="movie-meta">Hoạt Hình, Gia Đình • 1g 55p</p>
                    </div>
                    <a href="<%= request.getContextPath() %>/login" class="btn-card-buy">Mua Vé</a>
                </div>
            </div>

            <!-- Card 8 -->
            <div class="movie-card">
                <a href="<%= request.getContextPath() %>/movies/detail?id=8" class="poster-wrapper">
                    <span class="age-badge age-t18">T18</span>
                    <img src="https://images.unsplash.com/photo-1478720568477-152d9b164e26?q=80&w=600&auto=format&fit=crop" alt="Minions 4" class="movie-poster">
                </a>
                <div class="movie-info">
                    <div>
                        <a href="<%= request.getContextPath() %>/movies/detail?id=8" class="movie-title">Kẻ Trộm Mặt Trăng 4</a>
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

    <script src="${pageContext.request.contextPath}/assets/js/carousel-hoang.js?v=2"></script>
</body>
</html>