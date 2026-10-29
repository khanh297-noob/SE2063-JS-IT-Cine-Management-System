<%@ page pageEncoding="UTF-8" %>
<!-- Thanh Sidebar dieu huong ben trai danh cho quan ly rap -->
<c:choose>
<c:when test="${isAdministrator}">
    <c:set var="homePath" value="/admin-home.jsp" />
</c:when>
<c:when test="${sessionScope.User.role eq 'Branch Management'}">
    <c:set var="homePath" value="/branch-home.jsp" />
</c:when>
</c:choose>
<c:if test="${empty homePath}">
    <c:set var="homePath" value="/admin-home.jsp" />
</c:if>

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
            <a href="${pageContext.request.contextPath}${homePath}"
               class="nav-item-custom"
               style="display: flex; align-items: center; gap: 8px; padding: 8px 12px; background-color: #f8fafc; border: 1px solid #e2e8f0; border-radius: 8px; color: #475569; font-size: 12px; font-weight: 700; text-decoration: none;">
                <i class="bi bi-arrow-left" style="font-size: 14px; color: #64748b;"></i>
                <span>Về trang quản trị</span>
            </a>
        </c:if>


    </div>

    <!-- Nhom cuoi: Thong tin ca nhan va dang xuat -->
    <ul class="menu menu-bottom">
        <li><a href="${pageContext.request.contextPath}/admin/profile"><i class="fa-solid fa-user"></i> Hồ sơ cá nhân</a></li>
        <li><a href="${pageContext.request.contextPath}/logout"><i class="fa-solid fa-right-from-bracket"></i> Đăng xuất</a></li>
    </ul>
</aside>
