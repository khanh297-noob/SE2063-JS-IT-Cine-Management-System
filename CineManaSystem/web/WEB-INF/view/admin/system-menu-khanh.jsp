<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!doctype html>
<html lang="vi">
    <head>
        <meta charset="UTF-8">
        <meta name="viewport" content="width=device-width,initial-scale=1">
        <title>System Administration</title>
        <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/css/bootstrap.min.css">
        <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.1/font/bootstrap-icons.css">
        <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/admin-style.css">
    </head>
    <body>
        <jsp:include page="/WEB-INF/view/common/sidebar-khanh.jsp"/>
        <div class="main-wrapper">
            <jsp:include page="/WEB-INF/view/common/header-khanh.jsp"/>
            <main class="content-body">
                <h1 class="mb-3">System Administration</h1>
                <p class="text-muted mb-4">Chọn chức năng cần thực hiện.</p>
                <div class="khanh-feature-grid">
                    <a class="khanh-feature-card" href="${pageContext.request.contextPath}/admin/users">
                        <h2>Quản lý User và Role</h2>
                        <p>Tài khoản, vai trò và gán chi nhánh</p>
                        <span>Mở chức năng →</span>
                    </a>
                    <a class="khanh-feature-card" href="${pageContext.request.contextPath}/admin/branches">
                        <h2>View All Branch</h2>
                        <p>Chỉ xem chi nhánh, không sửa dữ liệu của Hoàng</p>
                        <span>Mở chức năng →</span>
                    </a>
                    <a class="khanh-feature-card" href="${pageContext.request.contextPath}/admin/audit-logs">
                        <h2>View Audit Log</h2>
                        <p>Theo dõi hoạt động hệ thống</p>
                        <span>Mở chức năng →</span>
                    </a>
                    <a class="khanh-feature-card" href="${pageContext.request.contextPath}/report/revenue-dashboard">
                        <h2>Dashboard doanh thu</h2>
                        <p>Báo cáo của Tuyết</p>
                        <span>Mở chức năng →</span>
                    </a>
                </div>
            </main>
        </div>
    </body>
</html>
