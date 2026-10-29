<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:set var="navFeature"
       value="${not empty requestScope.khanhFeature
                ? requestScope.khanhFeature : param.feature}" />
<c:set var="navActive"
       value="${not empty requestScope.khanhActive
                ? requestScope.khanhActive : param.active}" />
<c:set var="isAdministrator"
       value="${sessionScope.User.role eq 'Administrator'}" />
<c:set var="canConfigure"
       value="${isAdministrator or sessionScope.User.role eq 'Branch Management'}" />
<c:set var="homePath" value="/index-tuyet.jsp" />
<c:choose>
    <c:when test="${isAdministrator}">
        <c:set var="homePath" value="/admin-home.jsp" />
    </c:when>
    <c:when test="${sessionScope.User.role eq 'Branch Management'}">
        <c:set var="homePath" value="/branch-home.jsp" />
    </c:when>
</c:choose>
<aside class="sidebar khanh-module-sidebar" aria-label="Chức năng của phân hệ">
    <div class="sidebar-brand">
        <i class="bi bi-film" aria-hidden="true"></i>
        <span>CINE<br>MANAGEMENT</span>
    </div>
    <nav class="khanh-module-nav" aria-label="Chức năng">
        <c:choose>
            <c:when test="${navFeature eq 'system' and isAdministrator}">

                <a class="nav-item-custom ${navActive eq 'users' ? 'active' : ''}"
                   aria-current="${navActive eq 'users' ? 'page' : 'false'}"
                   href="${pageContext.request.contextPath}/admin/users">
                    <i class="bi bi-people" aria-hidden="true"></i>
                    <span>Quản lý User và Role</span>
                </a>
                <a class="nav-item-custom ${navActive eq 'branches' ? 'active' : ''}"
                   aria-current="${navActive eq 'branches' ? 'page' : 'false'}"
                   href="${pageContext.request.contextPath}/admin/branches">
                    <i class="bi bi-buildings" aria-hidden="true"></i>
                    <span>Danh sách chi nhánh</span>
                </a>
                <a class="nav-item-custom ${navActive eq 'audit' ? 'active' : ''}"
                   aria-current="${navActive eq 'audit' ? 'page' : 'false'}"
                   href="${pageContext.request.contextPath}/admin/audit-logs">
                    <i class="bi bi-clock-history" aria-hidden="true"></i>
                    <span>Audit Log</span>
                </a>
            </c:when>
            <c:when test="${navFeature eq 'fnb'}">
                <c:if test="${isAdministrator}">
                    <a class="nav-item-custom ${navActive eq 'items' ? 'active' : ''}"
                       aria-current="${navActive eq 'items' ? 'page' : 'false'}"
                       href="${pageContext.request.contextPath}/staff/fnb/items">
                        <i class="bi bi-cup-straw" aria-hidden="true"></i>
                        <span>Manage F&amp;B Item</span>
                    </a>
                </c:if>
                <a class="nav-item-custom ${navActive eq 'orders' ? 'active' : ''}"
                   aria-current="${navActive eq 'orders' ? 'page' : 'false'}"
                   href="${pageContext.request.contextPath}/staff/fnb/orders">
                    <i class="bi bi-receipt" aria-hidden="true"></i>
                    <span>Create &amp; Manage Order</span>
                </a>
                <a class="nav-item-custom ${navActive eq 'status' ? 'active' : ''}"
                   aria-current="${navActive eq 'status' ? 'page' : 'false'}"
                   href="${pageContext.request.contextPath}/staff/fnb/order-status">
                    <i class="bi bi-arrow-repeat" aria-hidden="true"></i>
                    <span>Update Order Status</span>
                </a>
                <a class="nav-item-custom ${navActive eq 'stock' ? 'active' : ''}"
                   aria-current="${navActive eq 'stock' ? 'page' : 'false'}"
                   href="${pageContext.request.contextPath}/staff/fnb/stock">
                    <i class="bi bi-box-seam" aria-hidden="true"></i>
                    <span>Monitor Stock</span>
                </a>
                <a class="nav-item-custom ${navActive eq 'quantity' ? 'active' : ''}"
                   aria-current="${navActive eq 'quantity' ? 'page' : 'false'}"
                   href="${pageContext.request.contextPath}/staff/fnb/quantity">
                    <i class="bi bi-pencil-square" aria-hidden="true"></i>
                    <span>Update Item Quantity</span>
                </a>
            </c:when>
            <c:when test="${navFeature eq 'promotion'}">
                <c:if test="${canConfigure}">
                    <a class="nav-item-custom ${navActive eq 'manage' ? 'active' : ''}"
                       aria-current="${navActive eq 'manage' ? 'page' : 'false'}"
                       href="${pageContext.request.contextPath}/staff/promotions/manage">
                        <i class="bi bi-gift" aria-hidden="true"></i>
                        <span>Manage Promotion</span>
                    </a>
                    <a class="nav-item-custom ${navActive eq 'discounts' ? 'active' : ''}"
                       aria-current="${navActive eq 'discounts' ? 'page' : 'false'}"
                       href="${pageContext.request.contextPath}/staff/promotions/discounts">
                        <i class="bi bi-percent" aria-hidden="true"></i>
                        <span>Configure Discount Amount</span>
                    </a>
                    <a class="nav-item-custom ${navActive eq 'limits' ? 'active' : ''}"
                       aria-current="${navActive eq 'limits' ? 'page' : 'false'}"
                       href="${pageContext.request.contextPath}/staff/promotions/limits">
                        <i class="bi bi-speedometer2" aria-hidden="true"></i>
                        <span>Configure Usage Limits</span>
                    </a>
                     <a class="nav-item-custom ${navActive eq 'ticket-prices' ? 'active' : ''}"
                        aria-current="${navActive eq 'ticket-prices' ? 'page' : 'false'}"
                        href="${pageContext.request.contextPath}/staff/promotions/ticket-prices">
                         <i class="bi bi-ticket-perforated" aria-hidden="true"></i>
                         <span>Manage Ticket Pricing</span>
                     </a>
                     <a class="nav-item-custom ${navActive eq 'fnb-prices' ? 'active' : ''}"
                        aria-current="${navActive eq 'fnb-prices' ? 'page' : 'false'}"
                        href="${pageContext.request.contextPath}/staff/promotions/fnb-prices">
                         <i class="bi bi-tag" aria-hidden="true"></i>
                         <span>Manage F&amp;B Pricing</span>
                     </a>
                 </c:if>
                 <a class="nav-item-custom ${navActive eq 'validate' ? 'active' : ''}"
                    aria-current="${navActive eq 'validate' ? 'page' : 'false'}"
                    href="${pageContext.request.contextPath}/staff/promotions/validate">
                     <i class="bi bi-check2-circle" aria-hidden="true"></i>
                     <span>Validate Promotion</span>
                 </a>
             </c:when>
             <c:otherwise>
                 <p class="px-3 text-muted small">
                     Chọn một phân hệ từ trang quản trị.
                 </p>
             </c:otherwise>
         </c:choose>
     </nav>
     <div class="khanh-module-footer">
         <a class="nav-item-custom"
            href="${pageContext.request.contextPath}${homePath}">
             <i class="bi bi-arrow-left" aria-hidden="true"></i>
             <span>Về trang quản trị</span>
         </a>
     </div>
 </aside>
