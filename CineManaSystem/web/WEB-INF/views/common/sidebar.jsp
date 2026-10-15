<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<aside class="sidebar">
    <div class="sidebar-brand">
        <i class="bi bi-film"></i> CINE MANAGEMENT
    </div>
    <div class="py-3">
        <div class="px-4 py-2 text-uppercase text-muted small fw-bold" style="font-size: 0.75rem;">Hệ Thống Quản Trị</div>
        <a href="#" class="nav-item-custom"><i class="bi bi-grid-1x2"></i> Dashboard</a>

        <a href="${pageContext.request.contextPath}/admin/users"
           class="nav-item-custom ${activeTab eq 'users' ? 'active' : ''}">
            <i class="bi bi-people-fill"></i> Quản lý User
        </a>

        <a href="${pageContext.request.contextPath}/admin/branches"
           class="nav-item-custom ${activeTab eq 'branches' ? 'active' : ''}">
            <i class="bi bi-geo-alt"></i> Quản lý Cụm rạp
        </a>

        <a href="#" class="nav-item-custom"><i class="bi bi-camera-reels"></i> Kho Phim</a>

        <a href="${pageContext.request.contextPath}/admin/audit-logs"
           class="nav-item-custom ${activeTab eq 'audit-logs' ? 'active' : ''}">
            <i class="bi bi-shield-check"></i> Audit Logs
        </a>
    </div>
</aside>