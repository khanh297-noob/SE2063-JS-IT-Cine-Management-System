<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@include file="/WEB-INF/views/common/taglibs-nam.jspf"%>
<%-- Màn hình soát vé. Không dùng AJAX: mọi thao tác đều là form POST /checker,
     Servlet xử lý xong forward lại trang này kèm kết quả. --%>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8"><meta name="viewport" content="width=device-width, initial-scale=1">
    <title>CineGo Checker - Soát Vé</title>
    <link rel="stylesheet" href="${ctx}/assets/css/cinego.css">
    <script src="https://unpkg.com/html5-qrcode@2.3.8/html5-qrcode.min.js"></script>
</head>
<body>
<%@include file="/WEB-INF/views/common/flash-nam.jspf"%>
<header class="topbar dark">
    <div class="brand">
        <span class="logo" style="background:var(--green)">GATE</span> CineGo Checker
        <span class="crumb">
            <c:choose>
                <c:when test="${not empty shift}">
                    <span class="chip">&#128682; Vị trí: <b>Cửa Soát Vé - <c:out value="${shift.branchName}"/></b></span>
                </c:when>
                <c:otherwise>
                    <%-- Quản lý / admin không có ca -> tự chọn rạp --%>
                    <form method="get" action="${ctx}/checker" style="display:inline">
                        <select name="branchId" onchange="this.form.submit()" style="width:auto;padding:4px 8px;font-size:12px">
                            <c:forEach var="br" items="${branches}">
                                <option value="${br.branchId}" ${branchId == br.branchId ? 'selected' : ''}>Cửa Soát Vé - <c:out value="${br.name}"/></option>
                            </c:forEach>
                        </select>
                    </form>
                </c:otherwise>
            </c:choose>
        </span>
    </div>
    <div style="display:flex;align-items:center">
        <div class="user">
            <b>NV Soát Vé: <c:out value="${me.fullName}"/></b><br>
            <c:choose>
                <c:when test="${not empty shift}">Ca: từ <fmt:formatDate value="${shift.startTime}" pattern="HH:mm"/> · <a href="${ctx}/pos/shift" style="color:#86efac">Ca làm việc</a></c:when>
                <c:otherwise><c:out value="${me.role}"/></c:otherwise>
            </c:choose>
        </div>
        <span class="badge-gate">GATE</span>
    </div>
</header>

<div class="container" style="max-width:1240px">
    <div class="grid grid-checker">
        <!-- ============ Cột trái: quét QR + nhập tay ============ -->
        <div>
            <div class="card">
                <div class="card-title">&#9638; Quét Mã QR Bằng Camera</div>
                <div class="scanner-box">
                    <div id="qr-reader"></div>
                    <div id="scanHint" class="center" style="padding:20px">
                        <div style="font-size:34px">&#128247;</div>
                        Đưa mã QR của khách hàng trước camera
                    </div>
                    <button type="button" class="btn btn-green btn-sm" id="btnCam" style="margin-top:10px">Bật Camera</button>
                </div>
            </div>

            <div class="card">
                <div class="card-title">&#9000; Nhập Mã Thủ Công</div>
                <div class="text-muted" style="font-size:12px;margin-bottom:10px">
                    Nhập mã vé (TK...) để soát vé. Nhập mã đơn (CG-...), SĐT hoặc email để tra cứu đơn.
                </div>
                <form method="post" action="${ctx}/checker" id="searchForm">
                    <input type="hidden" name="action" value="search">
                    <input type="hidden" name="branchId" value="${branchId}">
                    <input type="text" name="keyword" id="keyword" value="<c:out value='${keyword}'/>"
                           placeholder="TKxxxx hoặc CG-00000012 hoặc 09xxxxxxxx" autofocus autocomplete="off">
                    <button class="btn btn-dark" style="margin-top:10px">&#128269; Soát Vé / Tra Cứu</button>
                </form>
            </div>
        </div>

        <!-- ============ Cột phải: kết quả ============ -->
        <div>
            <c:choose>
                <c:when test="${not empty cleared and cleared}">
                    <div class="clearance ok">
                        <div class="icon">&#10003;</div>
                        <span class="pill">CLEARED - HỢP LỆ</span>
                        <div class="big">${resultMsg}</div>
                    </div>
                </c:when>
                <c:when test="${not empty cleared and not cleared}">
                    <div class="clearance no">
                        <div class="icon">&#10005;</div>
                        <span class="pill">DENIED - TỪ CHỐI</span>
                        <div class="big"><c:out value="${resultMsg}"/></div>
                    </div>
                </c:when>
                <c:otherwise>
                    <div class="clearance">
                        <div class="icon">&#9675;</div>
                        <span class="pill">ĐANG CHỜ</span>
                        <div class="big">Sẵn sàng soát vé</div>
                    </div>
                </c:otherwise>
            </c:choose>

            <%-- Thông tin vé vừa soát --%>
            <c:if test="${not empty ticket}">
                <div class="card" style="margin-top:18px">
                    <div class="card-title">Thông Tin Vé <span class="st st-${ticket.status}">${ticket.status}</span></div>
                    <div class="info-grid">
                        <div><small>Mã đơn hàng</small><b>#${ticket.bookingCode}</b></div>
                        <div><small>Tên phim</small><b><c:out value="${ticket.movieTitle}"/></b></div>
                        <div><small>Suất chiếu &amp; Phòng</small>
                            <fmt:formatDate value="${ticket.startTime}" pattern="HH:mm"/> - <fmt:formatDate value="${ticket.showDate}" pattern="dd/MM/yyyy"/>
                            · <c:out value="${ticket.roomName}"/> (${ticket.roomType})</div>
                        <div><small>Ghế</small><b class="text-red" style="font-size:18px">${ticket.seatLabel} (${ticket.seatType})</b></div>
                    </div>
                </div>
            </c:if>

            <%-- Kết quả tra cứu đơn --%>
            <c:if test="${not empty bookings or searched}">
                <div class="card" style="margin-top:18px">
                    <div class="card-title">Kết Quả Tra Cứu</div>
                    <c:if test="${empty bookings}"><p class="text-muted">No search results.</p></c:if>
                    <c:forEach var="b" items="${bookings}">
                        <div class="movie-box">
                            <div class="title">#${b.bookingCode} · <c:out value="${b.movieTitle}"/>
                                <span class="st st-${b.status}">${b.status}</span></div>
                            <div>${b.customerRole == 'Customer' ? b.customerName : 'Khách tại quầy'}
                                · <fmt:formatDate value="${b.startTime}" pattern="HH:mm"/> <fmt:formatDate value="${b.showDate}" pattern="dd/MM/yyyy"/>
                                · <c:out value="${b.roomName}"/></div>
                            <table class="tbl" style="margin-top:8px">
                                <c:forEach var="t" items="${b.tickets}">
                                    <tr>
                                        <td><b>${t.seatLabel}</b> <span class="text-muted">${t.seatType}</span></td>
                                        <td>${t.ticketCode}</td>
                                        <td><span class="st st-${t.status}">${t.status}</span></td>
                                        <td class="right">
                                            <c:if test="${t.status == 'Valid' && b.status == 'Confirmed'}">
                                                <form method="post" action="${ctx}/checker" style="margin:0">
                                                    <input type="hidden" name="action" value="checkin">
                                                    <input type="hidden" name="ticketId" value="${t.ticketId}">
                                                    <input type="hidden" name="keyword" value="<c:out value='${keyword}'/>">
                                                    <input type="hidden" name="branchId" value="${branchId}">
                                                    <button class="btn btn-green btn-sm">Check-in</button>
                                                </form>
                                            </c:if>
                                        </td>
                                    </tr>
                                </c:forEach>
                            </table>
                            <c:if test="${b.status == 'Confirmed' && !b.allCheckedIn}">
                                <div style="margin-top:8px;text-align:right">
                                    <a class="btn btn-dark btn-sm" target="_blank"
                                       href="${ctx}/pos/print?bookingId=${b.bookingId}&reprint=1">&#128424; In Lại Vé</a>
                                </div>
                            </c:if>
                        </div>
                    </c:forEach>
                </div>
            </c:if>
        </div>
    </div>
</div>

<script>
    // Camera quét QR: đọc được mã thì điền vào ô nhập và gửi form (giống bấm nút Soát Vé)
    document.getElementById('btnCam').addEventListener('click', function () {
        if (typeof Html5Qrcode === 'undefined') {
            alert('Không tải được thư viện quét QR.');
            return;
        }
        var qr = new Html5Qrcode('qr-reader');
        qr.start({facingMode: 'environment'}, {fps: 10, qrbox: 220}, function (code) {
            qr.stop();
            document.getElementById('keyword').value = code;
            document.getElementById('searchForm').submit();
        }).then(function () {
            document.getElementById('scanHint').style.display = 'none';
            document.getElementById('btnCam').style.display = 'none';
        }).catch(function (err) {
            alert('Không mở được camera: ' + err);
        });
    });
</script>
</body>
</html>
