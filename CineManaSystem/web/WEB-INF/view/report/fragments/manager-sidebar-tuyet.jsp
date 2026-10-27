<%@ page pageEncoding="UTF-8" %>
<!-- Thanh Sidebar dieu huong ben trai danh cho quan ly rap -->
<aside class="sidebar">
    <div>
        <!-- Cac nhom bao cao: chi hien voi Admin va Quan ly chi nhanh -->
        <c:if test="${canViewReports}">
        <!-- Nhom 1: Tong quan he thong -->
        <div class="menu-title">Tổng quan</div>
        <ul class="menu">
            <li><a class="${activeMenu == 'dashboard' ? 'active' : ''}" href="${pageContext.request.contextPath}/report/branch-dashboard"><i class="fa-solid fa-gauge-high"></i> Dashboard Tổng Quan</a></li>
        </ul>


        <!-- Nhom 3: Bao cao doanh thu va suat chieu -->
        <div class="menu-title">Báo cáo </div>
        <ul class="menu">
            <li><a class="${activeMenu == 'branch-report' ? 'active' : ''}" href="${pageContext.request.contextPath}/report/branch-report"><i class="fa-solid fa-chart-line"></i> Branch Report Dashboard</a></li>
            <li><a class="${activeMenu == 'revenue' ? 'active' : ''}" href="${pageContext.request.contextPath}/report/revenue-dashboard"><i class="fa-solid fa-chart-column"></i> Revenue Dashboard</a></li>
            <li><a class="${activeMenu == 'tickets' ? 'active' : ''}" href="${pageContext.request.contextPath}/report/ticket-sales"><i class="fa-solid fa-ticket"></i> Báo Cáo Vé Chi Tiết</a></li>
            
        </ul>
        </c:if>

        <!-- Nhom Gia & khuyen mai: moi muc chi hien voi vai tro duoc phep -->
        <div class="menu-title">Giá &amp; khuyến mãi</div>
        <ul class="menu">
            <li><a class="${activeMenu == 'promo-hub' ? 'active' : ''}" href="${pageContext.request.contextPath}/promotion-price-manage"><i class="fa-solid fa-tags"></i> Promotion &amp; Pricing</a></li>
            <c:if test="${canManagePricing}">
                <li><a class="${activeMenu == 'ticket-pricing' ? 'active' : ''}" href="${pageContext.request.contextPath}/promotion-price-manage/ticket-pricing"><i class="fa-solid fa-ticket"></i> Quản lý giá vé</a></li>
                <li><a class="${activeMenu == 'fnb-pricing' ? 'active' : ''}" href="${pageContext.request.contextPath}/promotion-price-manage/fnb-pricing"><i class="fa-solid fa-burger"></i> Quản lý giá F&amp;B</a></li>
            </c:if>
            <c:if test="${canValidatePromotion}">
                <li><a class="${activeMenu == 'promo-validate' ? 'active' : ''}" href="${pageContext.request.contextPath}/promotion-price-manage/validate-promotion"><i class="fa-solid fa-circle-check"></i> Kiểm tra mã khuyến mãi</a></li>
            </c:if>
        </ul>
    </div>

    <!-- Nhom cuoi: Thong tin ca nhan va dang xuat -->
    <ul class="menu menu-bottom">
        <li><a href="${pageContext.request.contextPath}/admin/profile"><i class="fa-solid fa-user"></i> Hồ sơ cá nhân</a></li>
        <li><a href="${pageContext.request.contextPath}/logout"><i class="fa-solid fa-right-from-bracket"></i> Đăng xuất</a></li>
    </ul>
</aside>
