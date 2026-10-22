<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:if test="${not empty sessionScope.toastSuccess}">
    <div class="alert alert-success" role="status">
        <c:out value="${sessionScope.toastSuccess}"/>
    </div>
    <c:remove var="toastSuccess" scope="session"/>
</c:if>
<c:if test="${not empty sessionScope.toastError}">
    <div class="alert alert-danger" role="alert">
        <c:out value="${sessionScope.toastError}"/>
    </div>
    <c:remove var="toastError" scope="session"/>
</c:if>
