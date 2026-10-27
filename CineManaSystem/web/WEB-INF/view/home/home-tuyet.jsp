<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%--
  Trang chủ khách hàng. HomeServlet (/home) đặt sẵn:
    movies       danh sách phim của tab đang chọn (model.movie.MovieSummaryDTO: movie, posterUrl, genreText)
    tab          showing | coming
    currentUser  người đang đăng nhập, null nếu là khách
--%>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>CineGo - Hệ thống Đặt vé Phim Trực tuyến</title>

    <!-- FontAwesome Icons -->
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css">

    <link rel="stylesheet" href="${ctx}/assets/css/home.css?v=3">
</head>
<body>

    <!-- 1. TOP NAVBAR -->
    <header class="navbar">
        <a href="${ctx}/home" class="brand-logo" title="Trang chủ CineGo">
            <div class="logo-badge">CG</div>
            <div class="logo-text">CineGo</div>
        </a>

        <nav>
            <ul class="nav-menu">
                <li><a href="#movies" class="nav-item active">Lịch chiếu theo rạp</a></li>
                <li><a href="${ctx}/movies" class="nav-item">Phim</a></li>
                <li><a href="${ctx}/branch" class="nav-item">Rạp</a></li>
                <li><a href="#pricing" class="nav-item">Giá vé</a></li>
                <li><a href="#news" class="nav-item">Tin mới & Ưu đãi</a></li>
                <c:set var="memberUrl" value="${empty currentUser ? '/login' : '/customer/profile'}"/>
                <li><a href="${ctx}${memberUrl}" class="nav-item">Thành viên</a></li>
                <%-- Trang vé đã đặt (/my-ticket) do module Booking làm; khách chưa đăng nhập
                     sẽ được AuthFilter chuyển sang /login trước --%>
                <li><a href="${ctx}/customer/bookings" class="nav-item">Vé của tôi</a></li>
            </ul>
        </nav>

        <!-- KHU VỰC HIỂN THỊ ĐÃ ĐĂNG NHẬP / CHƯA ĐĂNG NHẬP -->
        <div class="auth-group">
            <c:choose>
                <c:when test="${not empty currentUser}">
                    <div class="user-menu-container">
                        <div class="user-profile-btn">
                            <span class="user-name"><c:out value="${currentUser.fullName}"/></span>
                            <i class="fa-solid fa-chevron-down chevron-small"></i>
                        </div>

                        <!-- Dropdown Menu -->
                        <div class="user-dropdown">
                            <div class="dropdown-header">
                                <div class="dropdown-header-title">Tài khoản thành viên</div>
                                <div class="dropdown-header-email"><c:out value="${currentUser.email}"/></div>
                            </div>
                            <ul class="dropdown-list">
                                <li>
                                    <a href="${ctx}/customer/profile" class="dropdown-item">
                                        <div class="dropdown-item-left">
                                            <i class="fa-regular fa-user"></i>
                                            <span>Thông tin cá nhân</span>
                                        </div>
                                    </a>
                                </li>
                                <li>
                                    <a href="${ctx}/customer/bookings" class="dropdown-item">
                                        <div class="dropdown-item-left">
                                            <i class="fa-regular fa-id-card"></i>
                                            <span>Vé của tôi</span>
                                        </div>
                                    </a>
                                </li>
                                <li>
                                    <a href="${ctx}/payment/history" class="dropdown-item">
                                        <div class="dropdown-item-left">
                                            <i class="fa-solid fa-receipt"></i>
                                            <span>Lịch sử thanh toán</span>
                                        </div>
                                    </a>
                                </li>
                                <li>
                                    <a href="${ctx}/customer/change-password" class="dropdown-item">
                                        <div class="dropdown-item-left">
                                            <i class="fa-solid fa-key"></i>
                                            <span>Đổi mật khẩu</span>
                                        </div>
                                    </a>
                                </li>
                                <li>
                                    <a href="${ctx}/customer/notifications" class="dropdown-item">
                                        <div class="dropdown-item-left">
                                            <i class="fa-regular fa-bell"></i>
                                            <span>Thông báo</span>
                                        </div>
                                    </a>
                                </li>
                                <li>
                                    <a href="${ctx}/logout" class="dropdown-item logout-item">
                                        <div class="dropdown-item-left">
                                            <i class="fa-solid fa-right-from-bracket"></i>
                                            <span>Đăng xuất</span>
                                        </div>
                                    </a>
                                </li>
                            </ul>
                        </div>
                    </div>
                </c:when>
                <c:otherwise>
                    <a href="${ctx}/register" class="btn-register">Đăng ký</a>
                    <a href="${ctx}/login" class="btn-login-pill">Đăng nhập</a>
                </c:otherwise>
            </c:choose>
        </div>
    </header>

    <!-- MAIN CONTAINER -->
    <main class="main-container">

        <!-- 2. CAROUSEL KHUYẾN MÃI (3 ảnh banner) -->
        <section class="promo-carousel" id="promoCarousel" aria-roledescription="carousel" aria-label="Khuyến mãi CineGo">
            <div class="carousel-track">
                <div class="carousel-slide" aria-hidden="false">
                    <img src="${ctx}/assets/img/banners/banner-1.webp" alt="CineGo - Chào mừng bạn mới, khuyến mãi 10%">
                </div>
                <div class="carousel-slide" aria-hidden="true">
                    <img src="${ctx}/assets/img/banners/banner-2.webp" alt="Khách hàng dưới 23 tuổi - giá vé tiêu chuẩn chỉ 70.000 đồng" loading="lazy">
                </div>
                <div class="carousel-slide" aria-hidden="true">
                    <img src="${ctx}/assets/img/banners/banner-3.webp" alt="Happiness Combo - chỉ từ 149K" loading="lazy">
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

        <!-- 3. TABS PHIM -->
        <section class="film-toolbar" id="movies">
            <ul class="tab-list">
                <li><a href="${ctx}/home?tab=showing#movies" class="tab-item ${tab == 'showing' ? 'active' : ''}">PHIM ĐANG CHIẾU</a></li>
                <li><a href="${ctx}/home?tab=coming#movies" class="tab-item ${tab == 'coming' ? 'active' : ''}">PHIM SẮP CHIẾU</a></li>
            </ul>
        </section>

        <!-- 4. MOVIE CARD GRID (dữ liệu từ database) -->
        <c:choose>
            <c:when test="${empty movies}">
                <div class="movie-empty">
                    ${tab == 'coming' ? 'Hiện chưa có phim sắp chiếu.' : 'Hiện chưa có phim đang chiếu.'}
                </div>
            </c:when>
            <c:otherwise>
                <section class="movie-grid">
                    <c:forEach var="ms" items="${movies}">
                        <c:set var="m" value="${ms.movie}"/>
                        <c:set var="detailUrl" value="${ctx}/movies/detail?id=${m.movieId}"/>
                        <div class="movie-card">
                            <a href="${detailUrl}" class="poster-wrapper">
                                <span class="age-badge age-${fn:toLowerCase(m.ageRating)}"><c:out value="${m.ageRating}"/></span>
                                <span class="poster-placeholder"><c:out value="${m.title}"/></span>
                                <c:if test="${not empty ms.posterUrl}">
                                    <img src="${fn:escapeXml(ms.posterUrl)}" alt="${fn:escapeXml(m.title)}" class="movie-poster" loading="lazy" onerror="this.remove()">
                                </c:if>
                            </a>
                            <div class="movie-info">
                                <div>
                                    <a href="${detailUrl}" class="movie-title" title="${fn:escapeXml(m.title)}"><c:out value="${m.title}"/></a>
                                    <p class="movie-meta">
                                        <c:out value="${empty ms.genreText ? 'Chưa phân loại' : ms.genreText}"/> • ${m.durationText}
                                    </p>
                                    <c:if test="${tab == 'coming'}">
                                        <p class="movie-meta">Khởi chiếu ${m.releaseDateText}</p>
                                    </c:if>
                                </div>
                                <c:choose>
                                    <c:when test="${tab == 'coming'}">
                                        <a href="${detailUrl}" class="btn-card-buy btn-card-outline">Xem Chi Tiết</a>
                                    </c:when>
                                    <c:otherwise>
                                        <%-- Trang đặt vé (/booking) do module Booking làm; khách chưa đăng nhập
                                             sẽ được AuthFilter chuyển sang /login trước --%>
                                        <a href="${ctx}/booking?movieId=${m.movieId}" class="btn-card-buy">Mua Vé</a>
                                    </c:otherwise>
                                </c:choose>
                            </div>
                        </div>
                    </c:forEach>
                </section>
            </c:otherwise>
        </c:choose>

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

    <script src="${ctx}/assets/js/carousel-hoang.js?v=2"></script>
</body>
</html>
