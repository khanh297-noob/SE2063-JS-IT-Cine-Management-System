<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>Không tìm thấy trang - Hanoi Bus</title>
    <jsp:include page="/common/head_imports.jsp" />

</head>
<body>
    <jsp:include page="/common/navbar.jsp" />

    <div class="main-content container my-5">
        <div class="card shadow-sm p-5 border-0 text-center" style="max-width: 500px; width: 100%; border-radius: 12px; background: #ffffff;">
            <div class="mb-4">
                <h1 class="fw-bold" style="font-size: 6rem; color: #dc3545; margin-bottom: 0;">404</h1>
            </div>
            <h2 class="fw-bold text-dark mb-3">Không Tìm Thấy Trang</h2>
            <p class="text-muted mb-4">
                Trang bạn đang tìm kiếm có thể đã bị xóa, đổi tên, hoặc tạm thời không truy cập được.
            </p>
            <div class="mb-3">
                <a href="${pageContext.request.contextPath}/home" class="btn btn-primary px-4 py-2 fw-semibold text-white shadow-sm" style="border-radius: 20px;">
                    <i class="fas fa-home me-2"></i>Quay lại Trang chủ
                </a>
            </div>
        </div>
    </div>

    <jsp:include page="/common/footer.jsp" />
</body>
</html>
