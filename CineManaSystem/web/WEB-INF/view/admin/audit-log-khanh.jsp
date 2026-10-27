<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html>
<html lang="vi">
    <head>
        <meta charset="UTF-8">
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
        <title>CineGo Admin - Audit Logs</title>
        <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/css/bootstrap.min.css" rel="stylesheet">
        <link href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.1/font/bootstrap-icons.css" rel="stylesheet">
        <link href="${pageContext.request.contextPath}/assets/css/admin-style.css" rel="stylesheet">
    </head>
    <body>
        <!-- Đánh dấu active menu & nhúng Sidebar từ WEB-INF -->
        <c:set var="activeTab" value="audit-logs" scope="request"/>
        <jsp:include page="/WEB-INF/view/common/sidebar-khanh.jsp" />
        <div class="main-wrapper">
            <jsp:include page="/WEB-INF/view/common/header-khanh.jsp" />
            <main class="content-body">
                <div class="card-custom p-4 mb-4">
                    <div class="d-flex justify-content-between align-items-center">
                        <div>
                            <h4 class="fw-bold text-dark mb-1">
                                <i class="bi bi-journal-text text-danger me-2">
                                </i>Audit Log</h4>
                            </div>
                        </div>
                    </div>
                    <div class="card-custom p-4">
                        <div class="d-flex justify-content-between align-items-center mb-3">
                            <span class="fw-bold text-uppercase text-secondary small">Lịch Sử Ghi Nhận Sự Kiện</span>
                            <span class="text-muted small">Tổng số: <strong>${logList.size()}</strong> bản ghi</span>
                        </div>
                        <div class="table-responsive">
                            <table class="table align-middle table-hover">
                                <thead class="table-light">
                                    <tr class="text-secondary small">
                                        <th scope="col" class="py-3 ps-3">MÃ LOG</th>
                                        <th scope="col" class="py-3">THỜI GIAN</th>
                                        <th scope="col" class="py-3">NGƯỜI THỰC HIỆN</th>
                                        <th scope="col" class="py-3">HÀNH ĐỘNG (ACTION)</th>
                                        <th scope="col" class="py-3">ĐỊA CHỈ IP</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    <c:forEach var="l" items="${logList}">
                                        <tr>
                                            <td class="ps-3 py-3 fw-bold text-secondary">#LOG-${l.logId}</td>
                                            <td>
                                                <div class="text-dark fw-medium">
                                                    <fmt:formatDate value="${l.timestamp}" pattern="dd/MM/yyyy HH:mm:ss"/>
                                                </div>
                                            </td>
                                            <td>
                                                <div class="fw-bold text-dark">
                                                    <c:out value="${l.userName}"/>
                                                </div>
                                                <div class="text-muted small">UID: #${l.userId}</div>
                                            </td>
                                            <td>
                                                <span class="badge bg-info-subtle text-info-emphasis border px-3 py-2 rounded-pill">
                                                    <c:out value="${l.action}"/>
                                                </span>
                                            </td>
                                            <td class="text-muted font-monospace">
                                                <c:out value="${l.ipAddress}"/>
                                            </td>
                                        </tr>
                                    </c:forEach>
                                </tbody>
                            </table>
                        </div>
                    </div>
                </main>
            </div>
            <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/js/bootstrap.bundle.min.js">
            </script>
        </body>
    </html>
