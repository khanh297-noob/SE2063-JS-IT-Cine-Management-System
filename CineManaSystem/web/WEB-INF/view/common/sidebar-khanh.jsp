<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<aside class="sidebar">
    <div class="sidebar-brand">
        <i class="bi bi-film">
        </i>
        CINE MANAGEMENT
    </div>
    <nav class="py-3">
        <c:if test="${sessionScope.User.role eq 'Administrator'}">
            <a class="nav-item-custom" href="${pageContext.request.contextPath}/admin/system">
                <i class="bi bi-gear">
                </i>
                System Administration
            </a>
        </c:if>
        <c:if test="${sessionScope.User.role eq 'Administrator'
        or sessionScope.User.role eq 'Branch Management'
        or sessionScope.User.role eq 'Ticket Cashier'}">
            <a class="nav-item-custom" href="${pageContext.request.contextPath}/staff/fnb">
                <i class="bi bi-cup-straw">
                </i>
                F&amp;B Booking
            </a>
            <a class="nav-item-custom" href="${pageContext.request.contextPath}/staff/promotions">
                <i class="bi bi-gift">
                </i>
                Promotion &amp; Pricing
            </a>
        </c:if>
        <c:if test="${sessionScope.User.role eq 'Administrator'
        or sessionScope.User.role eq 'Branch Management'}">
            <a class="nav-item-custom" href="${pageContext.request.contextPath}/report/revenue-dashboard">
                <i class="bi bi-bar-chart">
                </i>
                Dashboard doanh thu
            </a>
        </c:if>
    </nav>
</aside>
