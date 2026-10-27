<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html>
<html lang="vi">
    <head>
        <meta charset="UTF-8">
        <meta name="viewport" content="width=device-width, initial-scale=1">
        <title>Thêm nhân viên</title>
        <link rel="stylesheet"
              href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/css/bootstrap.min.css">
        <link rel="stylesheet"
              href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.1/font/bootstrap-icons.css">
        <link rel="stylesheet"
              href="${pageContext.request.contextPath}/assets/css/admin-style.css">
    </head>
    <body>
        <jsp:include page="/WEB-INF/view/common/sidebar-khanh.jsp" />
        <div class="main-wrapper">
            <jsp:include page="/WEB-INF/view/common/header-khanh.jsp" />
            <main class="content-body">
                <h1 class="mb-3">Thêm nhân viên mới</h1>
                <p class="text-muted">
                    Chọn một vai trò và một chi nhánh cho nhân viên.
                    Chức năng này không tạo tài khoản Administrator.
                </p>
                <form method="post" class="card-custom p-4"
                      action="${pageContext.request.contextPath}/admin/users">
                    <input type="hidden" name="action" value="createEmployee">
                    <input type="hidden" name="csrfToken" value="${sessionScope.khanhCsrf}">
                    <div class="row g-3">
                        <div class="col-md-6">
                            <label for="employeeName" class="form-label">Họ tên</label>
                            <input id="employeeName" name="fullName" class="form-control"
                                   maxlength="100" required autocomplete="name">
                        </div>
                        <div class="col-md-6">
                            <label for="employeeEmail" class="form-label">Email đăng nhập</label>
                            <input id="employeeEmail" name="email" type="email"
                                   class="form-control" maxlength="150" required>
                        </div>
                        <div class="col-md-6">
                            <label for="employeePhone" class="form-label">Số điện thoại</label>
                            <input id="employeePhone" name="phone" class="form-control"
                                   maxlength="10" pattern="0[0-9]{9}" required>
                        </div>
                        <div class="col-md-6">
                            <label for="employeePassword" class="form-label">Mật khẩu ban đầu</label>
                            <input id="employeePassword" name="password" type="password"
                                   class="form-control" minlength="8" maxlength="64"
                                   autocomplete="new-password" required>
                        </div>
                        <div class="col-md-6">
                            <label for="employeeRole" class="form-label">Vai trò</label>
                            <select id="employeeRole" name="role" class="form-select" required>
                                <option value="">Chọn vai trò</option>
                                <option value="Branch Management">Quản lý chi nhánh</option>
                                <option value="Ticket Cashier">Thu ngân</option>
                                <option value="Ticket Checker">Soát vé</option>
                            </select>
                        </div>
                        <div class="col-md-6">
                            <label for="employeeBranch" class="form-label">Chi nhánh</label>
                            <select id="employeeBranch" name="branchId" class="form-select" required>
                                <option value="">Chọn chi nhánh</option>
                                <c:forEach var="branch" items="${branchList}">
                                    <option value="${branch.branchID}">
                                        CN-${branch.branchID} · <c:out value="${branch.name}" />
                                    </option>
                                </c:forEach>
                            </select>
                        </div>
                    </div>
                    <div class="d-flex gap-2 mt-4">
                        <button type="submit" class="btn btn-danger">Tạo nhân viên</button>
                        <a class="btn btn-outline-secondary"
                           href="${pageContext.request.contextPath}/admin/users">Quay lại</a>
                    </div>
                </form>
            </main>
        </div>
    </body>
</html>
