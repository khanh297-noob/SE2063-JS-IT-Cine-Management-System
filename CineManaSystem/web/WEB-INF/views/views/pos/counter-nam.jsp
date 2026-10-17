<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@include file="/WEB-INF/views/common/taglibs-nam.jspf"%>
<%-- Màn hình bán vé tại quầy. Không dùng AJAX:
     - Chọn suất chiếu = link GET /pos?showtimeId=...
     - Chọn ghế, đồ ăn, voucher rồi bấm "Tính tiền" (POST action=preview) để server tính hóa đơn
     - Bấm "Xác nhận & In vé" (POST action=confirm) để tạo đơn --%>
<c:set var="posTab" value="sell"/>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8"><meta name="viewport" content="width=device-width, initial-scale=1">
    <title>POS CineGo Counter - Bán Vé</title>
    <link rel="stylesheet" href="${ctx}/assets/css/cinego.css">
</head>
<body>
<%@include file="/WEB-INF/views/common/flash-nam.jspf"%>
<%@include file="/WEB-INF/views/common/pos-header-nam.jspf"%>

<div class="container" style="max-width:1320px">
    <div class="grid grid-3-pos">

        <!-- ============ 1. Tra cứu suất chiếu ============ -->
        <div class="card">
            <div class="card-title">&#128269; Tra Cứu Suất Chiếu</div>
            <form method="get" action="${ctx}/pos">
                <div class="field"><input type="text" name="q" value="<c:out value='${q}'/>" placeholder="Tìm tên phim..."></div>
                <div class="field"><input type="date" name="date" value="${date}"></div>
                <button class="btn btn-dark btn-block" style="padding:8px">Tìm</button>
            </form>
            <div style="margin-top:14px">
                <c:if test="${empty movies}"><p class="text-muted">No search results.</p></c:if>
                <c:forEach var="m" items="${movies}">
                    <%-- m.key = tên phim, m.value = danh sách suất chiếu của phim đó --%>
                    <div class="movie-item ${st.movieTitle == m.key ? 'active' : ''}">
                        <div class="name"><span><c:out value="${m.key}"/></span><span class="age">${m.value[0].ageRating}</span></div>
                        <div class="meta">${m.value[0].duration} phút</div>
                        <div class="slots">
                            <c:forEach var="s" items="${m.value}">
                                <c:choose>
                                    <c:when test="${s.started}">
                                        <span class="slot disabled" title="Đã chiếu"><fmt:formatDate value="${s.startTime}" pattern="HH:mm"/></span>
                                    </c:when>
                                    <c:otherwise>
                                        <c:url var="slotUrl" value="/pos">
                                            <c:param name="date" value="${date}"/>
                                            <c:param name="q" value="${q}"/>
                                            <c:param name="showtimeId" value="${s.showtimeId}"/>
                                        </c:url>
                                        <a class="slot ${st.showtimeId == s.showtimeId ? 'active' : ''}" href="${slotUrl}"
                                           title="<c:out value='${s.roomName}'/> (${s.roomType})"><fmt:formatDate value="${s.startTime}" pattern="HH:mm"/></a>
                                    </c:otherwise>
                                </c:choose>
                            </c:forEach>
                        </div>
                    </div>
                </c:forEach>
            </div>
        </div>

        <!-- ============ 2. Sơ đồ ghế + đồ ăn ============ -->
        <div>
            <div class="card">
                <div class="card-title">
                    <div>Sơ Đồ Ghế
                        <div class="text-muted" style="font-weight:400;font-size:12px">
                            <c:choose>
                                <c:when test="${not empty st}"><c:out value="${st.movieTitle}"/> •
                                    <fmt:formatDate value="${st.startTime}" pattern="HH:mm"/> - <fmt:formatDate value="${st.endTime}" pattern="HH:mm"/>
                                    (<c:out value="${st.roomName}"/> - ${st.roomType})</c:when>
                                <c:otherwise>Chọn suất chiếu bên trái</c:otherwise>
                            </c:choose>
                        </div>
                    </div>
                    <div class="legend">
                        <span><i></i>Trống</span>
                        <span><i style="background:var(--red);border-color:var(--red)"></i>Chọn</span>
                        <span><i style="background:#cbd5e1;border-color:#cbd5e1"></i>Đã bán</span>
                        <span><i style="background:#fde68a;border-color:#fde68a"></i>Đang giữ</span>
                        <span><i style="border-color:#f59e0b"></i>VIP</span>
                        <span><i style="border-color:#ec4899"></i>Sweetbox</span>
                    </div>
                </div>
                <div class="screen-bar"></div>
                <div class="seat-grid">
                    <c:if test="${empty st}"><p class="text-muted">Chưa chọn suất chiếu</p></c:if>
                    <c:forEach var="row" items="${seatRows}">
                        <div class="seat-row">
                            <span class="rl">${row.key}</span>
                            <c:forEach var="s" items="${row.value}">
                                <%-- form="saleForm": checkbox nằm ngoài form nhưng vẫn gửi cùng form bán vé --%>
                                <label class="seat ${s.seatType} ${s.state == 'AVAILABLE' ? '' : s.state}"
                                       title="${s.seatType} - <fmt:formatNumber value='${s.price}' pattern='#,##0'/> ₫">
                                    <input type="checkbox" name="seatId" value="${s.seatId}" form="saleForm"
                                           ${s.selected ? 'checked' : ''} ${s.state == 'AVAILABLE' ? '' : 'disabled'}>
                                    ${s.seatNumber}
                                </label>
                            </c:forEach>
                        </div>
                    </c:forEach>
                </div>
            </div>

            <div class="card">
                <div class="card-title">&#127871; Đồ Ăn &amp; Nước Uống <span class="tag">Nhập số lượng</span></div>
                <c:forEach var="f" items="${fnbMenu}">
                    <div class="combo">
                        <div>
                            <div class="nm"><c:out value="${f.itemName}"/> <span class="text-muted" style="font-weight:400">· ${f.category}</span></div>
                            <div class="pr"><fmt:formatNumber value="${f.price}" pattern="#,##0"/> ₫</div>
                        </div>
                        <input type="number" name="fnb_${f.itemId}" value="${f.quantity}" min="0" max="20"
                               form="saleForm" style="width:70px">
                    </div>
                </c:forEach>
                <c:if test="${empty fnbMenu}"><p class="text-muted">Chưa có món F&amp;B.</p></c:if>
            </div>
        </div>

        <!-- ============ 3. Hóa đơn & thanh toán ============ -->
        <div class="card">
            <div class="card-title">Hóa Đơn &amp; Thanh Toán</div>
            <form method="post" action="${ctx}/pos" id="saleForm">
                <input type="hidden" name="showtimeId" value="${st.showtimeId}">
                <input type="hidden" name="date" value="${date}">
                <input type="hidden" name="q" value="<c:out value='${q}'/>">

                <div class="text-muted" style="font-size:12px">Ghế đã chọn</div>
                <div style="margin-bottom:8px">
                    <c:forEach var="s" items="${selectedSeats}">
                        <div class="line"><span>Ghế <b>${s.label}</b> (${s.seatType})</span>
                            <span><fmt:formatNumber value="${s.price}" pattern="#,##0"/> ₫</span></div>
                    </c:forEach>
                    <c:if test="${empty selectedSeats}"><i class="text-muted">Chưa chọn ghế</i></c:if>
                </div>
                <div class="text-muted" style="font-size:12px">Đồ ăn</div>
                <div style="margin-bottom:12px">
                    <c:forEach var="f" items="${chosenItems}">
                        <div class="line"><span>${f.quantity}x <c:out value="${f.itemName}"/></span>
                            <span><fmt:formatNumber value="${f.lineTotal}" pattern="#,##0"/> ₫</span></div>
                    </c:forEach>
                    <c:if test="${empty chosenItems}"><i class="text-muted">Chưa chọn đồ ăn</i></c:if>
                </div>

                <div class="field">
                    <label>Voucher (nếu có)</label>
                    <input type="text" name="voucherCode" maxlength="20" value="<c:out value='${voucherCode}'/>" style="text-transform:uppercase">
                    <c:if test="${not empty voucherError}"><div class="field-error"><c:out value="${voucherError}"/></div></c:if>
                    <c:if test="${not empty voucherOk}"><div class="text-green" style="font-size:12px;margin-top:6px">${voucherOk}</div></c:if>
                </div>

                <label>Phương thức thanh toán *</label>
                <div class="row" style="margin-bottom:12px">
                    <label class="pay-option" style="margin:0;text-transform:none;color:var(--text)">
                        <span><input type="radio" name="paymentMethod" value="Cash" ${paymentMethod != 'Credit Card' ? 'checked' : ''}> &#128181; Tiền mặt</span>
                    </label>
                    <label class="pay-option" style="margin:0;text-transform:none;color:var(--text)">
                        <span><input type="radio" name="paymentMethod" value="Credit Card" ${paymentMethod == 'Credit Card' ? 'checked' : ''}> &#128179; Thẻ / QR</span>
                    </label>
                </div>

                <div class="field">
                    <label>Tiền khách đưa (khi trả tiền mặt)</label>
                    <input type="number" name="cashReceived" min="0" step="1000" value="${cashReceived}" placeholder="0">
                    <c:if test="${not empty change}">
                        <div class="text-muted" style="margin-top:6px">Tiền thừa: <b><fmt:formatNumber value="${change}" pattern="#,##0"/> ₫</b></div>
                    </c:if>
                </div>

                <div class="line text-muted"><span>Tạm tính</span><span><fmt:formatNumber value="${subtotal}" pattern="#,##0"/> ₫</span></div>
                <div class="line text-muted"><span>Giảm giá</span><span class="text-green">- <fmt:formatNumber value="${discount}" pattern="#,##0"/> ₫</span></div>
                <div class="line total">
                    <div><b>TỔNG TIỀN THU KHÁCH</b><small>Chọn ghế/đồ ăn xong bấm "Tính tiền" để cập nhật</small></div>
                    <span class="amount"><fmt:formatNumber value="${total}" pattern="#,##0"/> ₫</span>
                </div>

                <div class="row" style="margin-top:14px">
                    <button class="btn btn-outline" name="action" value="preview" ${empty st ? 'disabled' : ''}>&#128290; Tính tiền</button>
                    <button class="btn btn-primary" name="action" value="confirm" ${empty st ? 'disabled' : ''}
                            onclick="return confirm('Xác nhận đã thu tiền và in vé?')">&#128424; Xác nhận &amp; In vé</button>
                </div>
            </form>
        </div>
    </div>
</div>
</body>
</html>
