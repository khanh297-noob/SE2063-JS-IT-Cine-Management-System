<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<!DOCTYPE html>
<html lang="vi">
    <head>
        <meta charset="UTF-8">
        <meta name="viewport" content="width=device-width, initial-scale=1">
        <title>Validate Promotion - Cine Management</title>
        <link rel="stylesheet"
        href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/css/bootstrap.min.css">
        <link rel="stylesheet"
        href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.1/font/bootstrap-icons.css">
        <link rel="stylesheet"
        href="${pageContext.request.contextPath}/assets/css/admin-style.css">
    </head>
    <body>
        <jsp:include page="/WEB-INF/view/common/sidebar-khanh.jsp"/>
        <div class="main-wrapper">
            <jsp:include page="/WEB-INF/view/common/header-khanh.jsp"/>
            <main class="content-body">
                <h1 class="mb-4">Validate Promotion</h1>
                <jsp:include page="/WEB-INF/view/common/messages-khanh.jsp"/>
                <section class="panel">
                    <p class="text-muted">Chi nhánh và tổng tiền được đọc từ đơn trong database.</p>
                    <form class="form-grid" method="post">
                        <input type="hidden" name="csrfToken" value="${sessionScope.khanhCsrf}">
                        <div>
                            <label class="form-label" for="booking">Mã đơn</label>
                            <input class="form-control" id="booking" name="bookingId" type="number" min="1" required>
                        </div>
                        <div>
                            <label class="form-label" for="code">Mã voucher</label>
                            <input class="form-control" id="code" name="promoCode" maxlength="30" required>
                        </div>
                        <button class="btn btn-outline-danger" name="action" value="preview" type="submit">Kiểm tra</button>
                        <button class="btn btn-brand-pill" name="action" value="apply" type="submit">Áp dụng</button>
                        <button class="btn btn-outline-secondary" name="action" value="remove"
                        type="submit" formnovalidate>Gỡ mã</button>
                        </form>
                    </section>
                </main>
            </div>
        </body>
    </html>
