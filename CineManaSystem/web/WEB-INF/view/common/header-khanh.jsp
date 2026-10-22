<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<header class="top-navbar">
    <div>
        <c:out value="${sessionScope.User.fullName}"/>
        <span class="ms-2 text-white-50">
            <c:out value="${sessionScope.User.role}"/>
        </span>
    </div>
    <nav class="d-flex gap-3">
        <c:choose>
            <c:when test="${sessionScope.User.role eq 'Administrator'}">
                <a class="text-white" href="${pageContext.request.contextPath}/admin-home.jsp">
                    Trang quản trị
                </a>
            </c:when>
            <c:when test="${sessionScope.User.role eq 'Branch Management'}">
                <a class="text-white" href="${pageContext.request.contextPath}/branch-home.jsp">
                    Quản lý chi nhánh
                </a>
            </c:when>
            <c:otherwise>
                <a class="text-white" href="${pageContext.request.contextPath}/index-tuyet.jsp">
                    Trang chủ
                </a>
            </c:otherwise>
        </c:choose>
        <a class="text-white" href="${pageContext.request.contextPath}/logout">
            Đăng xuất
        </a>
    </nav>
</header>
