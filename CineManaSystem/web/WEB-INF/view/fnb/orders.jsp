                                        </tr>
                                    </thead>
                                    <tbody>
                                        <c:forEach var="item" items="${saleMenu}">
                                            <tr>
                                                <td>
                                                    <c:out value="${item.itemName}"/>
                                                </td>
                                                <td>
                                                    <fmt:formatNumber value="${item.effectivePrice}"/>
                                                </td>
                                                <td>${item.stockQuantity}</td>
                                                <td>
                                                    <input type="hidden" name="itemId" value="${item.itemId}">
                                                    <label class="visually-hidden" for="qty-${item.itemId}">Số lượng</label>
                                                    <input class="form-control" id="qty-${item.itemId}" name="quantity"
                                                    type="number" min="0" max="${item.stockQuantity}" step="1" value="0" required>
                                                </td>
                                            </tr>
                                        </c:forEach>
                                    </tbody>
                                </table>
                                <p class="text-muted">Đơn mới Pending, chưa trừ kho.</p>
                                <button class="btn btn-brand-pill" type="submit">Tạo đơn</button>
                            </form>
                        </section>
                    </c:if>
                    <c:if test="${empty saleMenu}">
                        <p class="alert alert-info">Chưa có món đang bán và còn tồn tại chi nhánh.</p>
                    </c:if>
                    <jsp:include page="/WEB-INF/view/common/fnb-orders-khanh.jsp"/>
                </c:if>
                <jsp:include page="/WEB-INF/view/common/fnb-lines-khanh.jsp"/>
            </main>
        </div>
    </body>
</html>
