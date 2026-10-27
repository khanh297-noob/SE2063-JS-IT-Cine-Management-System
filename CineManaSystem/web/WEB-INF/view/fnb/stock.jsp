                                    CN-${branch.branchID} · <c:out value="${branch.name}"/>
                                </option>
                            </c:forEach>
                        </select>
                    </div>
                    <button class="btn btn-brand-pill" type="submit">Xem</button>
                </form>
                <c:if test="${not empty selectedBranchId}">
                    <section class="panel table-responsive">
                        <h2 class="h5">Thực đơn và tồn kho</h2>
                        <table class="table align-middle">
                            <thead>
                                <tr>
                                    <th>Món</th>
                                    <th>Giá chung</th>
                                    <th>Giá riêng</th>
                                    <th>Giá bán</th>
                                    <th>Tồn</th>
                                    <th>Trạng thái</th>
                                    <c:if test="${canConfigure}">
                                        <th>Điều chỉnh tồn</th>
                                    </c:if>
                                </tr>
                            </thead>
                            <tbody>
                                <c:forEach var="item" items="${stockList}">
                                    <tr>
                                        <td>
                                            <c:out value="${item.itemName}"/>
                                        </td>
                                        <td>
                                            <fmt:formatNumber value="${item.basePrice}"/>
                                        </td>
                                        <td>
                                            <c:out value="${empty item.overridePrice ? 'Theo giá chung' : item.overridePrice}"/>
                                        </td>
                                        <td>
                                            <fmt:formatNumber value="${item.effectivePrice}"/>
                                        </td>
                                        <td>${item.stockQuantity}</td>
                                        <td>
                                            <c:out value="${item.status}"/>
                                            <c:if test="${not item.itemActive}">
                                                <small class="d-block text-danger">Ngừng bán toàn chuỗi</small>
                                            </c:if>
                                        </td>
                                        <c:if test="${canConfigure}">
                                            <td>
                                                <form method="post">
                                                    <input type="hidden" name="csrfToken" value="${sessionScope.khanhCsrf}">
                                                    <input type="hidden" name="action" value="adjust-stock">
                                                    <input type="hidden" name="branchId" value="${selectedBranchId}">
                                                    <input type="hidden" name="itemId" value="${item.itemId}">
                                                    <label class="visually-hidden" for="delta-${item.itemId}">Số tăng hoặc giảm</label>
                                                    <input class="form-control mb-2" id="delta-${item.itemId}"
                                                    name="delta" type="number" step="1" value="1" required>
                                                    <button class="btn btn-sm btn-outline-danger" type="submit">Điều chỉnh</button>
                                                </form>
                                            </td>
                                        </c:if>
                                    </tr>
                                </c:forEach>
                            </tbody>
                        </table>
                        <c:if test="${empty stockList}">
                            <p class="text-muted">Chưa cấu hình món tại chi nhánh này.</p>
                        </c:if>
                    </section>
                    <c:if test="${canConfigure}">
                        <section class="panel">
                            <h2 class="h5">Thêm / sửa cấu hình món</h2>
                            <p class="text-muted">Giá riêng trống = dùng giá chung. Cấu hình không sửa số tồn.</p>
                            <form class="form-grid" method="post">
                                <input type="hidden" name="csrfToken" value="${sessionScope.khanhCsrf}">
                                <input type="hidden" name="action" value="configure">
                                <input type="hidden" name="branchId" value="${selectedBranchId}">
                                <div>
                                    <label class="form-label" for="configureItem">Món</label>
                                    <select class="form-select" id="configureItem" name="itemId">
                                        <c:forEach var="item" items="${catalog}">
                                            <option value="${item.itemId}">
                                                <c:out value="${item.itemName}"/>
                                            </option>
                                        </c:forEach>
                                    </select>
                                </div>
                                <div>
                                    <label class="form-label" for="overridePrice">Giá riêng (đ)</label>
                                    <input class="form-control" id="overridePrice" name="overridePrice"
                                    type="number" min="0.01" step="0.01">
                                </div>
                                <div>
                                    <label class="form-label" for="menuStatus">Trạng thái</label>
                                    <select class="form-select" id="menuStatus" name="status">
                                        <option value="Available">Đang bán</option>
                                        <option value="SoldOut">Tạm hết hàng</option>
                                        <option value="Inactive">Ngừng bán tại chi nhánh</option>
                                    </select>
                                </div>
                                <button class="btn btn-brand-pill" type="submit">Lưu cấu hình</button>
                            </form>
                        </section>
                    </c:if>
                </c:if>
            </main>
        </div>
    </body>
</html>
