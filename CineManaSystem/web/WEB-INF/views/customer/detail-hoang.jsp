<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@taglib prefix="c" uri="jakarta.tags.core"%>
<%@taglib prefix="fn" uri="jakarta.tags.functions"%>
<c:set var="movie" value="${detail.movie}"/>
<c:set var="pageTitle" value="${movie.title}"/>
<%@ include file="/WEB-INF/views/common/customer-header-hoang.jspf" %>
<c:set var="liked" value="${likedMovies.containsKey(movie.movieId)}"/>
<c:set var="likeCount" value="${empty favoriteCounts[movie.movieId] ? 0 : favoriteCounts[movie.movieId]}"/>

<div class="cdetail">
    <a class="back" href="${ctx}/movies">&larr; Danh sách phim</a>

    <section class="cdetail-hero">
        <div class="cdetail-poster">
            <c:choose>
                <c:when test="${not empty detail.summary.posterUrl}"><img src="${fn:escapeXml(detail.summary.posterUrl)}" alt="Poster ${fn:escapeXml(movie.title)}"></c:when>
                <c:otherwise><span class="mcard-noposter"><c:out value="${movie.title}"/></span></c:otherwise>
            </c:choose>
        </div>

        <div class="cdetail-info">
            <h1 class="cdetail-title"><c:out value="${movie.title}"/></h1>
            <p class="cdetail-status"><c:set var="tagStatus" value="${movie.status}"/><%@ include file="/WEB-INF/views/common/status-tag-hoang.jspf" %></p>

            <div class="cdetail-rating">
                <span class="stars" role="img" aria-label="${detail.averageText} trên 5 sao">
                    <c:forEach begin="1" end="5" var="i"><span class="${i <= detail.averageStars ? 'on' : ''}">&#9733;</span></c:forEach>
                </span>
                <c:choose>
                    <c:when test="${detail.reviewCount == 0}"><span class="meta">Chưa có đánh giá</span></c:when>
                    <c:otherwise><strong>${detail.averageText}</strong><span class="meta">/ 5 · ${detail.reviewCount} đánh giá</span></c:otherwise>
                </c:choose>
            </div>

            <ul class="genre-tags">
                <c:forEach var="gn" items="${detail.summary.genreNames}"><li><c:out value="${gn}"/></li></c:forEach>
            </ul>

            <dl class="detail-list">
                <dt>Thời lượng</dt><dd>${movie.duration} phút</dd>
                <dt>Khởi chiếu</dt><dd>${movie.releaseDateText}</dd>
                <dt>Phân loại</dt><dd>${movie.ageRating}</dd>
                <dt>Ngôn ngữ</dt><dd><c:out value="${empty movie.language ? 'Đang cập nhật' : movie.language}"/></dd>
                <dt>Đạo diễn</dt><dd><c:out value="${empty movie.director ? 'Đang cập nhật' : movie.director}"/></dd>
            </dl>

            <c:if test="${movie.nowShowing}">
            <form class="fav-inline" method="post" action="${ctx}/movies/favorite">
                <input type="hidden" name="movieId" value="${movie.movieId}">
                <input type="hidden" name="back" value="/movies/detail?id=${movie.movieId}">
                <button type="submit" class="btn fav-toggle" aria-pressed="${liked}">
                    <span aria-hidden="true">&#9829;</span> ${liked ? 'Đã yêu thích' : 'Yêu thích'} · ${likeCount}
                </button>
            </form>
            </c:if>
        </div>
    </section>

    <c:if test="${not empty movie.castList}">
        <section class="cdetail-block">
            <h2 class="detail-heading">Diễn viên</h2>
            <ul class="cast-cards">
                <c:forEach var="actor" items="${movie.castList}">
                    <li class="cast-card"><span class="cast-card-name"><c:out value="${actor}"/></span></li>
                </c:forEach>
            </ul>
        </section>
    </c:if>

    <section class="cdetail-block">
        <h2 class="detail-heading">Nội dung phim</h2>
        <c:choose>
            <c:when test="${empty movie.description}"><p class="meta">Đang cập nhật.</p></c:when>
            <c:otherwise><p class="movie-synopsis"><c:out value="${movie.description}"/></p></c:otherwise>
        </c:choose>
    </section>

    <c:if test="${not empty detail.trailerEmbedUrl}">
        <section class="cdetail-block">
            <h2 class="detail-heading">Trailer</h2>
            <div class="cdetail-trailer">
                <iframe src="${detail.trailerEmbedUrl}" title="Trailer ${fn:escapeXml(movie.title)}" loading="lazy" allowfullscreen></iframe>
            </div>
        </section>
    </c:if>

    <section class="cdetail-block" id="reviews">
        <h2 class="detail-heading">Đánh giá (${detail.reviewCount})</h2>

        <c:choose>
        <c:when test="${movie.nowShowing}">
        <form class="review-form" method="post" action="${ctx}/movies/detail">
            <input type="hidden" name="id" value="${movie.movieId}">
            <fieldset class="star-field">
                <legend>Chấm điểm của bạn</legend>
                <div class="star-input">
                    <c:forEach begin="1" end="5" var="k">
                        <c:set var="v" value="${6 - k}"/>
                        <input type="radio" id="star${v}" name="rating" value="${v}" ${draftRating == v ? 'checked' : ''} ${v == 5 ? 'required' : ''}>
                        <label for="star${v}" title="${v} sao"><span class="sr-only">${v} sao</span>&#9733;</label>
                    </c:forEach>
                </div>
            </fieldset>
            <div class="field review-name">
                <label for="reviewerName" class="required">Tên của bạn</label>
                <input type="text" id="reviewerName" name="name" maxlength="50" required autocomplete="name"
                       value="${fn:escapeXml(draftName)}" placeholder="Nguyễn Văn A">
            </div>
            <div class="field">
                <label for="content" class="required">Đánh giá</label>
                <textarea id="content" name="content" maxlength="1000" required
                          placeholder="Bạn thấy phim thế nào?"><c:out value="${draftContent}"/></textarea>
            </div>
            <button type="submit" class="btn btn-primary">Gửi đánh giá</button>
        </form>
        </c:when>
        <c:otherwise>
            <p class="notice notice-info">Phim sắp chiếu nên chưa thể đánh giá. Bạn có thể đánh giá khi phim bắt đầu chiếu.</p>
        </c:otherwise>
        </c:choose>

        <c:choose>
            <c:when test="${empty detail.reviews}"><p class="meta">Chưa có đánh giá nào. Hãy là người đầu tiên!</p></c:when>
            <c:otherwise>
                <ul class="review-list">
                    <c:forEach var="r" items="${detail.reviews}">
                        <li class="review">
                            <div class="review-head">
                                <strong><c:out value="${r.reviewerName}"/></strong>
                                <span class="stars stars-sm" role="img" aria-label="${r.rating} trên 5 sao">
                                    <c:forEach begin="1" end="5" var="i"><span class="${i <= r.rating ? 'on' : ''}">&#9733;</span></c:forEach>
                                </span>
                                <span class="meta">${r.createdAtText}</span>
                            </div>
                            <p class="review-content"><c:out value="${r.content}"/></p>
                        </li>
                    </c:forEach>
                </ul>
            </c:otherwise>
        </c:choose>
    </section>
</div>
<%@ include file="/WEB-INF/views/common/footer-hoang.jspf" %>
