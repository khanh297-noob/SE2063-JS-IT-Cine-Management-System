<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>CineGo Admin - Quản Lý Cụm Rạp</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/css/bootstrap.min.css" rel="stylesheet">
    <link href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.1/font/bootstrap-icons.css" rel="stylesheet">
    <link href="${pageContext.request.contextPath}/assets/css/admin-style.css" rel="stylesheet">
</head>
<body>

    <!-- Đánh dấu active menu & nhúng Sidebar từ WEB-INF -->
    <c:set var="activeTab" value="branches" scope="request"/>
    <jsp:include page="/WEB-INF/views/common/sidebar.jsp" />

    <div class="main-wrapper">
        <jsp:include page="/WEB-INF/views/common/header.jsp" />

        <main class="content-body">
            <div class="card-custom p-4 mb-4">
                <div class="d-flex justify-content-between align-items-center">
                    <div>
                        <h4 class="fw-bold text-dark mb-1"><i class="bi bi-geo-alt-fill text-danger me-2"></i> Danh Sách Cụm Rạp Chiếu Phim</h4>

                    </div>

                </div>
            </div>

            <div class="card-custom p-4">
                <div class="d-flex justify-content-between align-items-center mb-3">
                    <span class="fw-bold text-uppercase text-secondary small">Cụm Rạp Đang Hoạt Động</span>
                    <span class="text-muted small">Tổng số: <strong>${branchList.size()}</strong> chi nhánh</span>
                </div>

                <div class="table-responsive">
                    <table class="table align-middle table-hover">
                        <thead class="table-light">
                            <tr class="text-secondary small">
                                <th scope="col" class="py-3 ps-3">MÃ RẠP</th>
                                <th scope="col" class="py-3">TÊN CHI NHÁNH</th>
                                <th scope="col" class="py-3">ĐỊA CHỈ</th>
                                <th scope="col" class="py-3">KHU VỰC / THÀNH PHỐ</th>
                                <th scope="col" class="py-3">HOTLINE</th>
                            </tr>
                        </thead>
                        <tbody>
                            <c:forEach var="b" items="${branchList}">
                                <tr>
                                    <td class="ps-3 py-3 fw-bold text-secondary">#BRA-${b.branchId}</td>
                                    <td class="fw-bold text-dark">${b.name}</td>
                                    <td class="text-muted">${b.address}</td>
                                    <td><span class="badge bg-light text-dark border">${b.city}</span></td>
                                    <td class="text-dark"><i class="bi bi-telephone me-1 text-danger"></i> ${b.hotline}</td>
                                </tr>
                            </c:forEach>
                        </tbody>
                    </table>
                </div>
            </div>
        </main>
    </div>
    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/js/bootstrap.bundle.min.js"></script>
</body>
</html>