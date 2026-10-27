<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<c:if test="${not empty selectedBooking}">
    <section class="panel table-responsive">
        <h2 class="h5">Đơn #${selectedBooking.bookingId}</h2>
        <p>Khách: <c:out value="${selectedBooking.customerName}"/>
    </p>
    <table class="table">
        <thead>
            <tr>
                <th>Món</th>
                <th>Số lượng</th>
                <th>Đơn giá lúc đặt</th>
                <th>Thành tiền</th>
                <th>Trạng thái món</th>
            </tr>
        </thead>
        <tbody>
            <c:forEach var="line" items="${orderLines}">
                <tr>
                    <td>
                        <c:out value="${line.itemName}"/>
                    </td>
                    <td>${line.quantity}</td>
                    <td>
                        <fmt:formatNumber value="${line.unitPrice}"/>
                    </td>
                    <td>
                        <fmt:formatNumber value="${line.subTotal}"/>
                    </td>
                    <td>
                        <c:out value="${line.orderStatus}"/>
                    </td>
                </tr>
            </c:forEach>
        </tbody>
    </table>
    <p>
        Đã giảm: <fmt:formatNumber value="${selectedBooking.discountApplied}"/> đ.
        Phải trả: <strong>
        <fmt:formatNumber value="${selectedBooking.totalAmount}"/> đ.</strong>
        </p>
    </section>
</c:if>
