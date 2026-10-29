<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html>
<html lang="vi">
    <head>
        <meta charset="UTF-8">
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
        <title>CineGo Admin - Quản Lý Tài Khoản</title>
        <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/css/bootstrap.min.css" rel="stylesheet">
        <link href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.1/font/bootstrap-icons.css" rel="stylesheet">
        <link href="${pageContext.request.contextPath}/assets/css/admin-style.css" rel="stylesheet">
    </head>
    <body>
        <!-- Đánh dấu active menu & nhúng Sidebar từ WEB-INF -->
        <c:set var="activeTab" value="users" scope="request"/>
        <jsp:include page="/WEB-INF/view/common/sidebar-khanh.jsp" />
        <div class="main-wrapper">
            <jsp:include page="/WEB-INF/view/common/header-khanh.jsp" />
            <main class="content-body">
                <!-- Toast thông báo -->
                <c:if test="${not empty sessionScope.toastSuccess}">
                    <div class="alert alert-success alert-dismissible fade show rounded-4" role="alert">
                        <i class="bi bi-check-circle-fill me-2">
                        </i>
                        <c:out value="${sessionScope.toastSuccess}" />
                        <button type="button" class="btn-close" data-bs-dismiss="alert">
                        </button>
                    </div>
                    <c:remove var="toastSuccess" scope="session"/>
                </c:if>
                <c:if test="${not empty sessionScope.toastError}">
                    <div class="alert alert-danger alert-dismissible fade show rounded-4" role="alert">
                        <i class="bi bi-exclamation-triangle-fill me-2">
                        </i>
                        <c:out value="${sessionScope.toastError}" />
                        <button type="button" class="btn-close" data-bs-dismiss="alert">
                        </button>
                    </div>
                    <c:remove var="toastError" scope="session"/>
                </c:if>
                <div class="card-custom p-4 mb-4">
                    <div class="d-flex justify-content-between align-items-center mb-4">
                        <div>
                            <h4 class="fw-bold text-dark mb-1">
                                <i class="bi bi-person-gear text-danger me-2">
                                </i> Quản Lý Tài Khoản Người Dùng & Nhân Viên
                            </h4>
                        </div>
                        <span class="badge bg-warning text-dark px-3 py-2 rounded-pill fw-bold">Admin Module</span>
                    </div>
<div class="row g-3 align-items-center justify-content-between">
    <div class="col-lg-8">
        <form method="get" action="${pageContext.request.contextPath}/admin/users"
              class="d-flex gap-2" role="search">
            <label for="userKeyword" class="visually-hidden">Tìm tài khoản</label>
            <input id="userKeyword" name="q" type="search" maxlength="100"
                   class="form-control rounded-pill"
                   value="<c:out value='${q}' />"
                   placeholder="Tên, email, số điện thoại hoặc UserID">
            <button type="submit" class="btn btn-outline-danger text-nowrap">Tìm kiếm</button>
            <a class="btn btn-outline-secondary text-nowrap"
               href="${pageContext.request.contextPath}/admin/users">Tất cả</a>
        </form>
    </div>
    <div class="col-auto">
        <a class="btn btn-brand-pill"
           href="${pageContext.request.contextPath}/admin/users?action=create">
            <i class="bi bi-person-plus-fill me-1" aria-hidden="true"></i>
            Thêm nhân viên mới
        </a>
    </div>
</div>

                                <div class="card-custom p-4">
                                    <div class="d-flex justify-content-between align-items-center mb-3">
                                        <span class="fw-bold text-uppercase text-secondary small">Danh Sách Người Dùng Hệ Thống</span>
                                        <span class="text-muted small">Tổng số: <strong>${userList.size()}</strong> tài khoản</span>
                                    </div>
                                    <div class="table-responsive">
                                        <table class="table align-middle table-hover">
                                            <thead>
                                                <tr>
                                                    <th>TÀI KHOẢN</th>
                                                    <th>EMAIL &amp; SĐT</th>
                                                    <th>VAI TRÒ</th>
                                                    <th>CHI NHÁNH</th>
                                                    <th>TRẠNG THÁI</th>
                                                    <th>THAO TÁC</th>
                                                </tr>
                                            </thead>
                                            <tbody>
                                                <c:forEach var="u" items="${userList}">
                                                    <tr>
                                                        <td class="ps-3 py-3">
                                                            <div class="d-flex align-items-center gap-3">
                                                                <div class="avatar-initial">
                                                                    <i class="bi bi-person text-secondary fs-5">
                                                                    </i>
                                                                </div>
                                                                <div>
                                                                    <div class="fw-bold text-dark">
                                                                        <c:out value="${u.fullName}"/>
                                                                    </div>
                                                                    <div class="text-muted small">Mã: #UID-${u.userId}</div>
                                                                </div>
                                                            </div>
                                                        </td>
                                                        <td>
                                                            <div class="text-dark">
                                                                <c:out value="${u.email}"/>
                                                            </div>
                                                            <div class="text-muted small">
                                                                <i class="bi bi-telephone">
                                                                </i> ${u.phone}</div>
                                                            </td>
                                                            <td class="text-dark fw-medium">
                                                                <c:choose>
                                                                    <c:when test="${u.role eq 'Ticket Cashier'}">NV Bán Vé</c:when>
                                                                    <c:when test="${u.role eq 'Ticket Checker'}">NV Soát Vé</c:when>
                                                                    <c:otherwise>${u.role}</c:otherwise>
                                                                </c:choose>
                                                            </td>
                                                            <td class="text-secondary">
                                                                <c:choose>
                                                                    <c:when test="${not empty u.branchName}">
                                                                        <i class="bi bi-geo-alt text-danger">
                                                                        </i>
                                                                    <c:out value="${u.branchName}" />
                                                                    </c:when>
                                                                    <c:otherwise>
                                                                        <span class="text-muted small">${u.role eq 'Administrator' ? 'Toàn hệ thống' : (u.role eq 'Customer' ? 'Không áp dụng' : 'Chưa gán')}</span>
                                                                    </c:otherwise>
                                                                </c:choose>
                                                            </td>
                                                            <td>
                                                                <c:choose>
                                                                    <c:when test="${u.status eq 'Active'}">
                                                                        <span class="badge badge-status status-active">Active</span>
                                                                    </c:when>
                                                                    <c:otherwise>
                                                                        <span class="badge badge-status status-locked">Locked</span>
                                                                    </c:otherwise>
                                                                </c:choose>
                                                            </td>
                                                            <td class="text-center">
                                                                <div class="d-flex justify-content-center gap-2">
                                                                    <button type="button" class="btn btn-sm btn-outline-secondary rounded-pill px-3"
                                                                    data-bs-toggle="modal"
                                                                    data-bs-target="#roleModal"
                                                                    data-userid="${u.userId}"
                                                                    data-fullname="<c:out value='${u.fullName}'/>"
                                                                    data-currentrole="${u.role}"
                                                                    data-branchid="${u.branchID}">
                                                                        <i class="bi bi-pencil-square">
                                                                        </i> Sửa Role
                                                                    </button>
                                                        <form action="${pageContext.request.contextPath}/admin/users" method="POST" class="d-inline">
                                                        <input type="hidden" name="csrfToken" value="${sessionScope.khanhCsrf}">
                                                        <input type="hidden" name="action" value="toggleStatus">
                                                        <input type="hidden" name="id" value="${u.userId}">
                                                        <input type="hidden" name="targetStatus" value="${u.status eq 'Active' ? 'Locked' : 'Active'}">
                                                        <c:choose>
                                                            <c:when test="${u.status eq 'Active'}">
                                                                <button type="submit" class="btn btn-sm btn-outline-danger rounded-pill px-3"
                                                                onclick="return confirm('Bạn có chắc muốn khóa tài khoản này?');">
                                                                    <i class="bi bi-lock-fill">
                                                                    </i> Khóa
                                                                </button>
                                                            </c:when>
                                                            <c:otherwise>
                                                                <button type="submit" class="btn btn-sm btn-outline-success rounded-pill px-3"
                                                                onclick="return confirm('Mở khóa tài khoản này?');">
                                                                    <i class="bi bi-unlock-fill">
                                                                    </i> Kích hoạt
                                                                </button>
                                                            </c:otherwise>
                                                        </c:choose>
                                                    </form>
                                                </div>
                                            </td>
                                        </tr>
                                    </c:forEach>
                                </tbody>
                            </table>
                        </div>
                    </div>
                </main>
            </div>
            <!-- Modal cập nhật Role -->
            <div class="modal fade" id="roleModal" tabindex="-1" aria-labelledby="roleModalLabel" aria-hidden="true">
                <div class="modal-dialog modal-dialog-centered">
                    <div class="modal-content rounded-4 border-0 shadow">
                        <form action="${pageContext.request.contextPath}/admin/users" method="POST">
                            <input type="hidden" name="csrfToken" value="${sessionScope.khanhCsrf}">
                            <input type="hidden" name="action" value="updateRole">
                            <input type="hidden" name="id" id="modalUserId">
                            <div class="modal-header border-bottom-0 pb-0">
                                <h5 class="modal-title fw-bold" id="roleModalLabel">Cập Nhật Vai Trò Người Dùng</h5>
                                <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close">
                                </button>
                            </div>
                            <div class="modal-body py-3">
                                <p class="text-muted small">Thay đổi quyền truy cập cho tài khoản: <strong id="modalUserName" class="text-dark">
                                </strong>
                            </p>
                            <div class="mb-3">
                                <label for="newRoleSelect" class="form-label fw-semibold">Chọn vai trò mới:</label>
                                <select class="form-select form-select-lg rounded-3 fs-6" name="newRole" id="newRoleSelect" required>
                                    <option value="Administrator">Administrator (Quản trị hệ thống)</option>
                                    <option value="Branch Management">Branch Management (Quản lý chi nhánh)</option>
                                    <option value="Ticket Cashier">Ticket Cashier (Nhân viên bán vé/quầy)</option>
                                    <option value="Ticket Checker">Ticket Checker (Nhân viên soát vé)</option>
                                    <option value="Customer">Customer (Khách hàng thành viên)</option>
                                </select>
                                <label for="branchSelect" class="form-label fw-semibold">
                                    Chi nhánh nhân viên
                                </label>
                                <select id="branchSelect" name="branchId" class="form-select rounded-3">
                                    <option value="">Không gán</option>
                                    <c:forEach var="branch" items="${branchList}">
                                        <option value="${branch.branchID}">
                                            CN-${branch.branchID} · <c:out value="${branch.name}"/>
                                        </option>
                                    </c:forEach>
                                </select>
                            </div>
                        </div>
                        <div class="modal-footer border-top-0 pt-0">
                            <button type="button" class="btn btn-light rounded-pill px-4" data-bs-dismiss="modal">Hủy</button>
                            <button type="submit" class="btn btn-brand-pill px-4">Lưu Thay Đổi</button>
                        </div>
                    </form>
                </div>
            </div>
        </div>
        <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/js/bootstrap.bundle.min.js">
        </script>
        <script>
        const roleModal = document.getElementById('roleModal');
        if (roleModal) {
            roleModal.addEventListener('show.bs.modal', function (event) {
                const button = event.relatedTarget;
                document.getElementById('modalUserId').value = button.getAttribute('data-userid');
                document.getElementById('modalUserName').textContent = button.getAttribute('data-fullname');
                document.getElementById('newRoleSelect').value = button.getAttribute('data-currentrole');
                document.getElementById('branchSelect').value = button.getAttribute('data-branchid') || '';
            });
        }
        </script>
    </body>
</html>
