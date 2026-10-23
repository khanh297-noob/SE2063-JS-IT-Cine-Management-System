<%@page contentType="text/html" pageEncoding="UTF-8" isErrorPage="true"%>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<c:set var="code" value="${requestScope['jakarta.servlet.error.status_code']}"/>
<c:set var="pageTitle" value="${code == 404 ? 'Không tìm thấy' : 'Có lỗi xảy ra'}"/>
<%@ include file="/WEB-INF/views/common/header-hoang.jspf" %>
<div class="error-page">
    <div class="code"><c:out value="${code}"/></div>
    <c:choose>
        <c:when test="${code == 404}">
            <h1>Không tìm thấy trang hoặc dữ liệu</h1>
            <p class="page-sub"><c:out value="${requestScope['jakarta.servlet.error.message']}"/></p>
        </c:when>
        <c:otherwise>
            <h1>Máy chủ gặp lỗi</h1>
            <p class="page-sub">Xem log Tomcat trong NetBeans (tab Output, dòng SEVERE) để biết nguyên nhân.</p>
            <c:if test="${not empty requestScope['jakarta.servlet.error.exception']}">
                <p class="meta"><c:out value="${requestScope['jakarta.servlet.error.exception'].message}"/></p>
            </c:if>
        </c:otherwise>
    </c:choose>
    <a class="btn" href="${ctx}/">Về trang chủ</a>
</div>
<%@ include file="/WEB-INF/views/common/footer-hoang.jspf" %>
