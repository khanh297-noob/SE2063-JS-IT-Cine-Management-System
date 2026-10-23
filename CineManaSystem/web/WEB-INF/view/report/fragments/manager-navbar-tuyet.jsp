<%@ page pageEncoding="UTF-8" %>
<!-- Thanh dieu huong tren cung (Navbar): Logo thuong hieu, nut chon rap va thong tin nguoi dung -->
<header class="navbar">
    <div class="nav-left">
        <!-- Logo va ten he thong -->

        <span class="brand-name brand-name-accent">CineGo</span>

        <span class="nav-sep"></span>

        <!-- Form lua chon chi nhanh dang quan ly -->
        <c:choose>
            <c:when test="${branches.size() > 1}">
                <form method="get" action="${pageContext.request.contextPath}${shellAction}" class="branch-pill">
                    <c:if test="${not empty param.fromDate}"><input type="hidden" name="fromDate" value="<c:out value='${param.fromDate}'/>"></c:if>
                    <c:if test="${not empty param.toDate}"><input type="hidden" name="toDate" value="<c:out value='${param.toDate}'/>"></c:if>
                    <c:if test="${not empty param.type}"><input type="hidden" name="type" value="<c:out value='${param.type}'/>"></c:if>
                    <c:if test="${not empty param.movieId}"><input type="hidden" name="movieId" value="<c:out value='${param.movieId}'/>"></c:if>
                    <i class="fa-solid fa-bookmark"></i> Rạp quản lý:
                    <select name="branchId" onchange="this.form.submit()">
                        <option value="">Tất cả chi nhánh</option>
                        <c:forEach var="b" items="${branches}">
                            <option value="${b.branchID}" ${selectedBranchId == b.branchID ? 'selected' : ''}><c:out value="${b.name}"/> (Branch #${b.branchID < 10 ? '0' : ''}${b.branchID})</option>
                        </c:forEach>
                    </select>
                </form>
            </c:when>
            <c:otherwise>
                <span class="branch-pill"><i class="fa-solid fa-bookmark"></i> Rạp quản lý: <b><c:out value="${branchPill}"/></b></span>
            </c:otherwise>
        </c:choose>
    </div>

    <!-- Thong tin tai khoan nguoi dung dang dang nhap -->
    <div class="nav-right">
        <div class="user-text">
            <b><c:out value="${sessionScope.User.fullName}"/></b>
            <span>Chức vụ: <c:out value="${roleLabel}"/></span>
        </div>
        <a class="avatar" href="${pageContext.request.contextPath}/admin/profile" title="Hồ sơ cá nhân"><c:out value="${initials}"/></a>
    </div>
</header>

<!-- Thong bao neu chua cau hinh phan quyen chi nhanh -->
<c:if test="${not empty scopeWarning}">
    <div class="scope-warning"><i class="fa-solid fa-triangle-exclamation"></i><c:out value="${scopeWarning}"/></div>
</c:if>