<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<c:if test="${not empty selectedBooking}">
    <section class="panel table-responsive">
        <h2 class="h5">Đơn #${selectedBooking.bookingId}</h2>
        <p>Khách: <c:out value="${selectedBooking.customerName}"/>
    </p>
    <p class="text-muted small">
    Đơn chưa có UnitPrice cần được đối chiếu với module bán hàng;
    không lấy giá danh mục hiện tại để thay đơn giá cũ.
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
                        <c:choose>
                            <c:when test="${empty line.unitPrice}">
                                <span class="text-warning">Chưa chốt giá</span>
                            </c:when>
                            <c:otherwise>
                                <fmt:formatNumber value="${line.unitPrice}" />
                            </c:otherwise>
                        </c:choose>
                    </td>
                    <td>
                        <c:choose>
                            <c:when test="${empty line.unitPrice}">—</c:when>
                            <c:otherwise>
                                <fmt:formatNumber value="${line.subTotal}" />
                            </c:otherwise>
                        </c:choose>
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
