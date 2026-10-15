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

    <style>
        * {
            box-sizing: border-box;
            margin: 0;
            padding: 0;
            font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif;
        }

        html {
            scroll-behavior: smooth;
        }

        body {
            background-color: #f8fafc;
            color: #334155;
            min-height: 100vh;
            display: flex;
            flex-direction: column;
            font-size: 13px;
        }

        a {
            text-decoration: none;
            color: inherit;
        }

        /* === 1. TOP NAVBAR === */
        .navbar {
            background-color: #ffffff;
            color: #334155;
            height: 64px;
            padding: 0 32px;
            display: flex;
            align-items: center;
            justify-content: space-between;
            position: sticky;
            top: 0;
            z-index: 50;
            border-bottom: 1px solid #e2e8f0;
            width: 100%;
            white-space: nowrap;
        }

        .brand-logo {
            display: flex;
            align-items: center;
            gap: 8px;
            text-decoration: none;
            transition: opacity 0.2s;
            flex-shrink: 0;
        }

        .brand-logo:hover {
            opacity: 0.9;
        }

        .logo-badge {
            background-color: #e11d48;
            color: #ffffff;
            font-weight: 900;
            padding: 3px 8px;
            border-radius: 6px;
            font-size: 14px;
            letter-spacing: -0.5px;
        }

        .logo-text {
            color: #e11d48;
            font-size: 22px;
            font-weight: 800;
            letter-spacing: -0.5px;
        }

        .nav-menu {
            display: flex;
            align-items: center;
            gap: 24px;
            list-style: none;
            height: 100%;
            margin: 0;
            padding: 0;
        }

        .nav-item {
            color: #475569;
            font-weight: 600;
            font-size: 13px;
            height: 64px;
            display: flex;
            align-items: center;
            position: relative;
            transition: color 0.2s;
            cursor: pointer;
        }

        .nav-item:hover {
            color: #e11d48;
        }

        .nav-item.active {
            color: #e11d48;
            font-weight: 700;
        }

        .nav-item.active::after {
            content: "";
            position: absolute;
            bottom: 0;
            left: 0;
            width: 100%;
            height: 2.5px;
            background-color: #e11d48;
        }

        .auth-group {
            display: flex;
            align-items: center;
            gap: 16px;
            flex-shrink: 0;
            position: relative;
        }

        .btn-register {
            color: #475569;
            font-weight: 600;
            font-size: 13px;
            transition: color 0.2s;
        }

        .btn-register:hover {
            color: #e11d48;
        }

        .btn-login-pill {
            background-color: #e11d48;
            color: #ffffff;
            padding: 8px 24px;
            border-radius: 20px;
            font-weight: 700;
            font-size: 13px;
            transition: all 0.2s;
            display: inline-flex;
            align-items: center;
            justify-content: center;
        }

        .btn-login-pill:hover {
            background-color: #be123c;
            box-shadow: 0 4px 12px rgba(225, 29, 72, 0.25);
        }

        /* === USER DROPDOWN MENU === */
        .user-menu-container {
            position: relative;
            display: inline-block;
            padding: 12px 0;
        }

        .user-profile-btn {
            display: flex;
            align-items: center;
            gap: 6px;
            cursor: pointer;
            padding: 6px 12px;
            border-radius: 20px;
            transition: background-color 0.2s;
            background-color: #f8fafc;
            border: 1px solid #e2e8f0;
        }

        .user-profile-btn:hover {
            background-color: #f1f5f9;
            border-color: #cbd5e1;
        }

        .user-name {
            font-weight: 700;
            color: #1e293b;
            font-size: 13px;
        }

        .user-dropdown {
            position: absolute;
            top: 100%;
            right: 0;
            width: 230px;
            background-color: #ffffff;
            border-radius: 16px;
            box-shadow: 0 10px 25px -5px rgba(0, 0, 0, 0.1), 0 8px 10px -6px rgba(0, 0, 0, 0.05);
            border: 1px solid #f1f5f9;
            padding: 16px 0 12px 0;
            z-index: 100;
            display: none;
        }

        .user-menu-container:hover .user-dropdown {
            display: block;
        }

        .dropdown-header {
            padding: 0 20px 12px 20px;
            border-bottom: 1px solid #f1f5f9;
        }

        .dropdown-header-title {
            font-weight: 800;
            color: #0f172a;
            font-size: 13px;
        }

        .dropdown-header-email {
            color: #94a3b8;
            font-size: 11px;
            margin-top: 2px;
        }

        .dropdown-list {
            list-style: none;
            padding: 8px 0 0 0;
            margin: 0;
        }

        .dropdown-item {
            display: flex;
            align-items: center;
            justify-content: space-between;
            padding: 10px 20px;
            color: #334155;
            font-weight: 600;
            font-size: 12.5px;
            transition: background-color 0.15s, color 0.15s;
        }

        .dropdown-item:hover {
            background-color: #f8fafc;
            color: #e11d48;
        }

        .dropdown-item-left {
            display: flex;
            align-items: center;
            gap: 12px;
        }

        .dropdown-item i {
            width: 16px;
            text-align: center;
            color: #64748b;
        }

        .dropdown-item:hover i {
            color: #e11d48;
        }

        .dropdown-item.logout-item {
            color: #e11d48;
            margin-top: 4px;
        }

        .dropdown-item.logout-item i {
            color: #e11d48;
        }

        .badge-count {
            background-color: #e11d48;
            color: #ffffff;
            font-size: 10px;
            font-weight: 800;
            width: 18px;
            height: 18px;
            border-radius: 50%;
            display: flex;
            align-items: center;
            justify-content: center;
        }

        /* === LAYOUT MAIN === */
        .main-container {
            width: 100%;
            padding: 24px 32px;
            flex: 1;
        }

        /* === 2. BANNER POSTER CAROUSEL DƯỚI NAVBAR === */
        .carousel-banner-wrapper {
            position: relative;
            width: 100%;
            height: 350px;
            border-radius: 16px;
            overflow: hidden;
            margin-bottom: 28px;
            box-shadow: 0 8px 25px rgba(15, 23, 42, 0.12);
            background-color: #0f172a;
        }

        .carousel-banner-track {
            display: flex;
            width: 300%;
            height: 100%;
            animation: heroCarouselAnimation 12s infinite ease-in-out;
        }

        .carousel-banner-wrapper:hover .carousel-banner-track {
            animation-play-state: paused;
        }

        @keyframes heroCarouselAnimation {
            0%, 28% { transform: translateX(0%); }
            33%, 61% { transform: translateX(-33.333%); }
            66%, 94% { transform: translateX(-66.666%); }
            100% { transform: translateX(0%); }
        }

        .carousel-slide-item {
            width: 33.333%;
            height: 100%;
            position: relative;
            background-size: cover;
            background-position: center;
            padding: 40px 52px;
            display: flex;
            flex-direction: column;
            justify-content: center;
            color: #ffffff;
        }

        .carousel-slide-overlay {
            position: absolute;
            inset: 0;
            background: linear-gradient(to right, rgba(15, 23, 42, 0.94) 38%, rgba(15, 23, 42, 0.25));
            z-index: 1;
        }

        .slide-content-box {
            position: relative;
            z-index: 2;
            max-width: 600px;
        }

        .badge-hot-tag {
            background-color: #e11d48;
            color: #ffffff;
            font-size: 10px;
            font-weight: 800;
            padding: 4px 10px;
            border-radius: 4px;
            width: fit-content;
            margin-bottom: 12px;
            text-transform: uppercase;
            letter-spacing: 0.5px;
        }

        .hero-banner-title {
            font-size: 38px;
            font-weight: 900;
            line-height: 1.1;
            margin-bottom: 10px;
            letter-spacing: -0.5px;
            color: #ffffff;
        }

        .hero-banner-meta {
            font-size: 12px;
            color: #cbd5e1;
            margin-bottom: 12px;
            font-weight: 500;
        }

        .hero-banner-desc {
            font-size: 13px;
            color: #94a3b8;
            margin-bottom: 22px;
            line-height: 1.5;
            display: -webkit-box;
            -webkit-line-clamp: 2;
            -webkit-box-orient: vertical;
            overflow: hidden;
        }

        .btn-banner-action {
            background-color: #e11d48;
            color: #ffffff;
            padding: 10px 26px;
            border-radius: 20px;
            font-weight: 800;
            font-size: 12.5px;
            display: inline-flex;
            align-items: center;
            gap: 8px;
            box-shadow: 0 4px 14px rgba(225, 29, 72, 0.35);
            transition: background-color 0.2s, transform 0.2s;
        }

        .btn-banner-action:hover {
            background-color: #be123c;
            transform: translateY(-1px);
        }

        /* === 3. FILM TOOLBAR (GỒM TABS + FILTER TÌM KIẾM THEO ẢNH) === */
        .film-toolbar {
            display: flex;
            justify-content: space-between;
            align-items: center;
            border-bottom: 1px solid #e2e8f0;
            padding-bottom: 12px;
            margin-bottom: 24px;
            width: 100%;
            gap: 16px;
            flex-wrap: wrap;
        }

        .tab-list {
            display: flex;
            gap: 28px;
            list-style: none;
            margin: 0;
            padding: 0;
        }

        .tab-item {
            font-size: 13.5px;
            font-weight: 700;
            color: #64748b;
            padding-bottom: 12px;
            position: relative;
            display: block;
        }

        .tab-item.active {
            color: #e11d48;
        }

        .tab-item.active::after {
            content: "";
            position: absolute;
            bottom: -13px;
            left: 0;
            width: 100%;
            height: 2.5px;
            background-color: #e11d48;
        }

        /* Ô BỘ LỌC VÀ TÌM KIẾM BÊN PHẢI (CHUẨN THEO MẪU ÀNH) */
        .filter-search-group {
            display: flex;
            align-items: center;
            gap: 12px;
        }

        .select-category {
            padding: 7px 12px;
            border: 1px solid #cbd5e1;
            border-radius: 8px;
            font-size: 12.5px;
            color: #334155;
            background-color: #ffffff;
            outline: none;
            cursor: pointer;
            transition: border-color 0.2s;
        }

        .select-category:focus {
            border-color: #e11d48;
        }

        .input-search-film {
            padding: 7px 12px;
            border: 1px solid #cbd5e1;
            border-radius: 8px;
            font-size: 12.5px;
            color: #334155;
            outline: none;
            width: 180px;
            transition: border-color 0.2s;
        }

        .input-search-film:focus {
            border-color: #e11d48;
        }

        /* === 4. MOVIE CARD GRID (8 PHIM) === */
        .movie-grid {
            display: grid;
            grid-template-columns: repeat(4, 1fr);
            gap: 24px;
            margin-bottom: 48px;
            width: 100%;
        }

        @media (max-width: 1280px) { .movie-grid { grid-template-columns: repeat(3, 1fr); } }
        @media (max-width: 880px) { .movie-grid { grid-template-columns: repeat(2, 1fr); } }
        @media (max-width: 540px) { .movie-grid { grid-template-columns: 1fr; } }

        .movie-card {
            background-color: #ffffff;
            border-radius: 12px;
            border: 1px solid #e2e8f0;
            overflow: hidden;
            display: flex;
            flex-direction: column;
            transition: transform 0.25s ease, box-shadow 0.25s ease;
        }

        .movie-card:hover {
            transform: translateY(-5px);
            box-shadow: 0 12px 24px -4px rgba(0, 0, 0, 0.08);
        }

        .poster-wrapper {
            position: relative;
            height: 280px;
            overflow: hidden;
            background-color: #0f172a;
            display: block;
        }

        .movie-poster {
            width: 100%;
            height: 100%;
            object-fit: cover;
            transition: transform 0.35s ease;
        }

        .movie-card:hover .movie-poster {
            transform: scale(1.05);
        }

        .age-badge {
            position: absolute;
            top: 10px;
            left: 10px;
            font-size: 9.5px;
            font-weight: 800;
            padding: 3px 7px;
            border-radius: 4px;
            color: #ffffff;
            z-index: 2;
        }

        .age-t18 { background-color: #e11d48; }
        .age-p { background-color: #f59e0b; }
        .age-t13 { background-color: #2563eb; }

        .movie-info {
            padding: 14px 16px;
            display: flex;
            flex-direction: column;
            flex-grow: 1;
            justify-content: space-between;
            gap: 12px;
        }

        .movie-title {
            font-size: 14px;
            font-weight: 800;
            color: #0f172a;
            margin-bottom: 4px;
            white-space: nowrap;
            overflow: hidden;
            text-overflow: ellipsis;
            display: block;
        }

        .movie-title:hover {
            color: #e11d48;
        }

        .movie-meta {
            font-size: 11px;
            color: #94a3b8;
            font-weight: 500;
        }

        .btn-card-buy {
            background-color: #e11d48;
            color: #ffffff;
            text-align: center;
            padding: 9px;
            border-radius: 8px;
            font-weight: 700;
            font-size: 12px;
            display: flex;
            align-items: center;
            justify-content: center;
            transition: background-color 0.2s;
            width: 100%;
        }

        .btn-card-buy:hover {
            background-color: #be123c;
        }

        .footer {
            background-color: #ffffff;
            border-top: 1px solid #e2e8f0;
            padding: 24px 32px;
            margin-top: auto;
            display: flex;
            justify-content: space-between;
            align-items: center;
            font-size: 11.5px;
            color: #64748b;
            width: 100%;
        }
    </style>
</head>
<body>

    <!-- 1. TOP NAVBAR -->
    <header class="navbar">
        <a href="<%= request.getContextPath() %>/index.jsp" class="brand-logo" title="Trang chủ CineGo">
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
                        <i class="fa-solid fa-chevron-down" style="color: #94a3b8; font-size: 11px; margin-left: 2px;"></i>
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
                <div class="carousel-slide-item" style="background-image: url('https://images.unsplash.com/photo-1518709268805-4e9042af9f23?q=80&w=1600&auto=format&fit=crop');">
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
                <div class="carousel-slide-item" style="background-image: url('https://images.unsplash.com/photo-1534447677768-be436bb09401?q=80&w=1600&auto=format&fit=crop');">
                    <div class="carousel-slide-overlay"></div>
                    <div class="slide-content-box">
                        <span class="badge-hot-tag" style="background-color: #2563eb;">SẮP CHIẾU</span>
                        <h1 class="hero-banner-title">GODZILLA X KONG 2</h1>
                        <p class="hero-banner-meta">Khởi chiếu: 15/08/2026 • Viễn Tưởng • 1g 55p • P</p>
                        <p class="hero-banner-desc">Cuộc đại chiến siêu quái thú trở lại màn ảnh rộng với chuẩn định dạng IMAX đỉnh cao kỹ xảo âm thanh siêu việt.</p>
                        <a href="#movies" class="btn-banner-action" style="background-color: #2563eb;"><i class="fa-solid fa-calendar-check"></i> ĐẶT TRƯỚC VÉ</a>
                    </div>
                </div>

                <!-- Slide 3: Lật Mặt 7 -->
                <div class="carousel-slide-item" style="background-image: url('https://images.unsplash.com/photo-1489599849927-2ee91cede3ba?q=80&w=1600&auto=format&fit=crop');">
                    <div class="carousel-slide-overlay"></div>
                    <div class="slide-content-box">
                        <span class="badge-hot-tag" style="background-color: #f59e0b;">SUẤT ĐẶC BIỆT</span>
                        <h1 class="hero-banner-title">LẬT MẶT 7: MỘT ĐIỀU ƯỚC</h1>
                        <p class="hero-banner-meta">Khởi chiếu: 30/04/2026 • Tình Cảm, Gia Đình • 2g 18p • P</p>
                        <p class="hero-banner-desc">Tác phẩm điện ảnh xúc động về tình thân gia đình của đạo diễn Lý Hải đang làm khuấy đảo cảm xúc khán giả Việt.</p>
                        <a href="#movies" class="btn-banner-action" style="background-color: #f59e0b;"><i class="fa-solid fa-fire"></i> MUA VÉ NGAY</a>
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
            <strong style="color: #0f172a; font-size: 12px;">CineGo CMS Website</strong>
            <p>Hệ thống đặt vé xem phim trực tuyến tiện lợi và nhanh chóng.</p>
        </div>
        <div style="text-align: right;">
            <strong style="color: #0f172a;">Thông tin liên hệ</strong>
            <p>Email: hotro@cinego.vn | Hotline: 1900 1234</p>
        </div>
    </footer>

</body>
</html>