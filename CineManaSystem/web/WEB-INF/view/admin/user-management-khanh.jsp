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
