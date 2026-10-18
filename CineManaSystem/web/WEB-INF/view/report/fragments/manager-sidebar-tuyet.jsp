<%@ page pageEncoding="UTF-8" %>
<!-- Thanh Sidebar dieu huong ben trai danh cho quan ly rap -->
<aside class="sidebar">
    <div>
        <!-- Nhom 1: Tong quan he thong -->
        <div class="menu-title">Tổng quan</div>
        <ul class="menu">
            <li><a class="${activeMenu == 'dashboard' ? 'active' : ''}" href="${pageContext.request.contextPath}/report/branch-dashboard"><i class="fa-solid fa-gauge-high"></i> Dashboard Tổng Quan</a></li>
        </ul>

        <!-- Nhom 2: Quan ly phim -->
        <div class="menu-title">Quản lý phim</div>
        <ul class="menu">
            <li><span class="soon" title="Màn hình chưa được xây dựng"><i class="fa-solid fa-film"></i> Films List <em>Sắp có</em></span></li>
            <li><span class="soon" title="Màn hình chưa được xây dựng"><i class="fa-solid fa-sliders"></i> Film Config <em>Sắp có</em></span></li>
        </ul>

        <!-- Nhom 3: Bao cao doanh thu va suat chieu -->
        <div class="menu-title">Báo cáo &amp; nhân sự</div>
        <ul class="menu">
            <li><a class="${activeMenu == 'branch-report' ? 'active' : ''}" href="${pageContext.request.contextPath}/report/branch-report"><i class="fa-solid fa-chart-line"></i> Branch Report Dashboard</a></li>
            <li><a class="${activeMenu == 'revenue' ? 'active' : ''}" href="${pageContext.request.contextPath}/report/revenue-dashboard"><i class="fa-solid fa-chart-column"></i> Revenue Dashboard</a></li>
            <li><a class="${activeMenu == 'tickets' ? 'active' : ''}" href="${pageContext.request.contextPath}/report/ticket-sales"><i class="fa-solid fa-ticket"></i> Báo Cáo Vé Chi Tiết</a></li>
            <li><span class="soon" title="Màn hình chưa được xây dựng"><i class="fa-solid fa-users"></i> Staff Dashboard <em>Sắp có</em></span></li>
        </ul>
    </div>

    <!-- Nhom cuoi: Thong tin ca nhan va dang xuat -->
    <ul class="menu menu-bottom">
        <li><a href="${pageContext.request.contextPath}/customer/profile"><i class="fa-solid fa-user"></i> Hồ sơ cá nhân</a></li>
        <li><a href="${pageContext.request.contextPath}/logout"><i class="fa-solid fa-right-from-bracket"></i> Đăng xuất</a></li>
    </ul>
</aside>
