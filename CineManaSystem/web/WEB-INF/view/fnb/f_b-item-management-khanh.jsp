                                </span>
                                <input type="text" id="searchInput"
                                class="form-control border-start-0 rounded-end-pill search-input-pill"
                                placeholder="Tìm kiếm theo tên sản phẩm...">
                            </div>
                        </div>
                        <div class="col-auto">
                            <button class="btn btn-brand-pill" data-bs-toggle="modal" data-bs-target="#itemModal"
                            onclick="openAddModal()">
                                <i class="bi bi-plus-circle-fill me-1">
                                </i> Thêm Sản Phẩm Mới
                            </button>
                        </div>
                    </div>
                </div>
                <%-- Bảng danh sách F&B --%>
                <div class="card-custom p-4">
                    <div class="d-flex justify-content-between align-items-center mb-3">
                        <span class="fw-bold text-uppercase text-secondary small">Danh Sách Sản Phẩm F&B</span>
                        <span class="text-muted small">Tổng số: <strong>${itemList.size()}</strong> sản phẩm</span>
                    </div>
                    <div class="table-responsive">
                        <table class="table align-middle table-hover" id="itemTable">
                            <thead class="table-light">
                                <tr class="text-secondary small">
                                    <th scope="col" class="py-3 ps-3">MÃ SP</th>
                                    <th scope="col" class="py-3">TÊN SẢN PHẨM</th>
                                    <th scope="col" class="py-3">LOẠI</th>
                                    <th scope="col" class="py-3">GIÁ (VNĐ)</th>
                                    <th scope="col" class="py-3">TRẠNG THÁI</th>
                                    <th scope="col" class="py-3 text-center">THAO TÁC</th>
                                </tr>
                            </thead>
                            <tbody>
                                <c:forEach var="item" items="${itemList}">
                                    <tr>
                                        <td class="ps-3 py-3 fw-bold text-secondary">#FNB-${item.itemId}</td>
                                        <td>
                                            <div class="d-flex align-items-center gap-3">
                                                <div class="avatar-initial">
                                                    <c:choose>
                                                        <c:when test="${item.category eq 'Popcorn'}">
                                                            <i class="bi bi-box2-heart text-warning fs-5">
                                                            </i>
                                                        </c:when>
                                                        <c:when test="${item.category eq 'Drink'}">
                                                            <i class="bi bi-cup-straw text-info fs-5">
                                                            </i>
                                                        </c:when>
                                                        <c:otherwise>
                                                            <i class="bi bi-bag-heart text-danger fs-5">
                                                            </i>
                                                        </c:otherwise>
                                                    </c:choose>
                                                </div>
                                                <div class="fw-bold text-dark">
                                                    <c:out value="${item.itemName}"/>
                                                </div>
                                            </div>
                                        </td>
                                        <td>
                                            <c:choose>
                                                     </form>
                                                                                            </div>
                                                                                        </td>
                                                                                    </tr>
                                                                                </c:forEach>
                                                                                <c:if test="${empty itemList}">
                                                                                    <tr>
                                                                                        <td colspan="6" class="text-center text-muted py-5">
                                                                                            <i class="bi bi-inbox fs-1 d-block mb-2">
                                                                                            </i>
                                                                                            Chưa có sản phẩm F&B nào.
                                                                                        </td>
                                                                                    </tr>
                                                                                </c:if>
                                                                            </tbody>
                                                                        </table>
                                                                    </div>
                                                                </div>
                                                            </main>
                                                        </div>
                                                        <%-- Modal thêm / sửa sản phẩm --%>
                                                        <div class="modal fade" id="itemModal" tabindex="-1" aria-labelledby="itemModalLabel" aria-hidden="true">
                                                            <div class="modal-dialog modal-dialog-centered">
                                                                <div class="modal-content rounded-4 border-0 shadow">
                                                                    <form action="${pageContext.request.contextPath}/admin/fnb-items" method="POST">
                                                                        <input type="hidden" name="csrfToken" value="${sessionScope.csrfToken}">
                                                                        <input type="hidden" name="action" value="save">
                                                                        <input type="hidden" name="itemId" id="modalItemId" value="0">
                                                                        <div class="modal-header border-bottom-0 pb-0">
                                                                            <h5 class="modal-title fw-bold" id="itemModalLabel">Thêm Sản Phẩm F&B</h5>
                                                                            <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close">
                                                                            </button>
                                                                        </div>
                                                                        <div class="modal-body py-3">
                                                                            <div class="mb-3">
                                                                                <label for="itemNameInput" class="form-label fw-semibold">Tên sản phẩm</label>
                                                                                <input type="text" class="form-control rounded-3" id="itemNameInput"
                                                                                name="itemName" maxlength="100" required
                                                                                placeholder="VD: Bắp rang bơ lớn">
                                                                            </div>
                                                                            <div class="mb-3">
                                                                                <label for="categorySelect" class="form-label fw-semibold">Loại sản phẩm</label>
                                                                                <select class="form-select rounded-3" id="categorySelect" name="category" required>
                                                                                    <option value="Popcorn">Bắp (Popcorn)</option>
                                                                                    <option value="Drink">Nước (Drink)</option>
                                                                                    <option value="Combo">Combo</option>
                                                                                </select>
                                                                            </div>
                                                                            <div class="mb-3">
                                                                                <label for="priceInput" class="form-label fw-semibold">Giá (VNĐ)</label>
                                                                                <input type="number" class="form-control rounded-3" id="priceInput"
                                                                                name="price" min="0.01" step="0.01" required
                                                                                placeholder="VD: 45000">
                                                                            </div>
                                                                        </div>
                                                                        <div class="modal-footer border-top-0 pt-0">
                                                                            <button type="button" class="btn btn-light rounded-pill px-4" data-bs-dismiss="modal">Hủy</button>
                                                                            <button type="submit" class="btn btn-brand-pill px-4" id="modalSubmitBtn">Thêm sản phẩm</button>
                                                                        </div>
                                                                    </form>
                                                                </div>
                                                            </div>
                                                        </div>
                                                        <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/js/bootstrap.bundle.min.js">
                                                        </script>
                                                        <script>
                                                    /* ---------- Modal: phân biệt Thêm vs Sửa ---------- */
                                                    function openAddModal() {
                                                        document.getElementById('itemModalLabel').textContent = 'Thêm Sản Phẩm F&B';
                                                        document.getElementById('modalSubmitBtn').textContent = 'Thêm sản phẩm';
                                                        document.getElementById('modalItemId').value = '0';
                                                        document.getElementById('itemNameInput').value = '';
                                                        document.getElementById('categorySelect').value = 'Popcorn';
                                                        document.getElementById('priceInput').value = '';
                                                    }
                                                    const itemModal = document.getElementById('itemModal');
                                                    if (itemModal) {
                                                        itemModal.addEventListener('show.bs.modal', function (event) {
                                                            const btn = event.relatedTarget;
                                                            const itemId = btn.getAttribute('data-itemid');
                                                            if (itemId && itemId !== '0') {
                                                                // Chế độ sửa
                                                                document.getElementById('itemModalLabel').textContent = 'Cập Nhật Sản Phẩm F&B';
                                                                document.getElementById('modalSubmitBtn').textContent  = 'Lưu thay đổi';
                                                                document.getElementById('modalItemId').value      = itemId;
                                                                document.getElementById('itemNameInput').value     = btn.getAttribute('data-itemname');
                                                                document.getElementById('categorySelect').value    = btn.getAttribute('data-category');
                                                                document.getElementById('priceInput').value        = btn.getAttribute('data-price');
                                                            }
                                                        });
                                                    }
                                                    /* ---------- Tìm kiếm nhanh ---------- */
                                                    document.getElementById('searchInput').addEventListener('input', function () {
                                                        const keyword = this.value.toLowerCase();
                                                        const rows = document.querySelectorAll('#itemTable tbody tr');
                                                        rows.forEach(function (row) {
                                                            const text = row.textContent.toLowerCase();
                                                            row.style.display = text.includes(keyword) ? '' : 'none';
                                                        });
                                                    });
                                                        </script>
                                                    </body>
                                                </html>
