<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<section class="panel table-responsive">
    <h2 class="h5">Danh sách đơn</h2>
    <table class="table align-middle">
        <thead>
            <tr>
                <th>Mã đơn</th>
                <th>Khách hàng</th>
                <th>Phải trả</th>
                <th>Booking</th>
                <th>Thanh toán</th>
                <th>Chi tiết</th>
            </tr>
        </thead>
        <tbody>
            <c:forEach var="order" items="${orderList}">
                <tr>
                    <td>#${order.bookingId}</td>
                    <td>
                        <c:out value="${order.customerName}"/>
                    </td>
                    <td>
                        <fmt:formatNumber value="${order.totalAmount}"/>
                    </td>
                    <td>
                        <c:out value="${order.status}"/>
                    </td>
                    <td>
                        <c:out value="${empty order.paymentStatus ? 'Chưa thanh toán' : order.paymentStatus}"/>
                    </td>
                    <td>
                        <a href="${pageContext.request.contextPath}${pagePath}?branchId=${selectedBranchId}&amp;bookingId=${order.bookingId}">
                            Mở đơn
                        </a>
                    </td>
                </tr>
            </c:forEach>
        </tbody>
    </table>
    <c:if test="${empty orderList}">
        <p class="text-muted">Chưa có đơn F&amp;B.</p>
    </c:if>
</section>
