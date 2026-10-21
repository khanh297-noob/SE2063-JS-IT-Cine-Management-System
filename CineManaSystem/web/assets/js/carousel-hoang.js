/* Carousel khuyến mãi trên trang chủ khách: tự chuyển ảnh mỗi 5 giây,
   dừng khi rê chuột / focus vào carousel, có nút trước / sau và chấm chọn ảnh. */
(function () {
    var root = document.getElementById('promoCarousel');
    if (!root) return;

    var track = root.querySelector('.carousel-track');
    var slides = root.querySelectorAll('.carousel-slide');
    var dots = root.querySelectorAll('.carousel-dots button');
    var count = slides.length, index = 0, timer = null;
    var reduceMotion = window.matchMedia && window.matchMedia('(prefers-reduced-motion: reduce)').matches;

    function go(i) {
        index = (i + count) % count;
        track.style.transform = 'translateX(' + (-index * 100) + '%)';
        slides.forEach(function (s, k) { s.setAttribute('aria-hidden', k === index ? 'false' : 'true'); });
        dots.forEach(function (d, k) {
            if (k === index) d.setAttribute('aria-current', 'true'); else d.removeAttribute('aria-current');
        });
    }

    function start() { if (!reduceMotion && count > 1) { stop(); timer = setInterval(function () { go(index + 1); }, 5000); } }
    function stop() { if (timer) { clearInterval(timer); timer = null; } }

    root.querySelector('.carousel-prev').addEventListener('click', function () { go(index - 1); start(); });
    root.querySelector('.carousel-next').addEventListener('click', function () { go(index + 1); start(); });
    dots.forEach(function (d, k) { d.addEventListener('click', function () { go(k); start(); }); });

    root.addEventListener('mouseenter', stop);
    root.addEventListener('mouseleave', start);
    root.addEventListener('focusin', stop);
    root.addEventListener('focusout', start);
    document.addEventListener('visibilitychange', function () { if (document.hidden) stop(); else start(); });

    go(0);
    start();
})();
