<form action="${pageContext.request.contextPath}/admin/fnb-items"
      method="post">

    <input type="hidden"
           name="csrfToken"
           value="${sessionScope.csrfToken}">

    <input type="text"
           name="itemName"
           maxlength="100"
           required>

    <select name="category" required>
        <option value="Popcorn">Bắp</option>
        <option value="Drink">Nước</option>
        <option value="Combo">Combo</option>
    </select>

    <input type="number"
           name="price"
           min="0.01"
           step="0.01"
           required>

    <button type="submit">Thêm sản phẩm</button>
</form>