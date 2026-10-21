/* Form thể loại: báo trùng tên ngay khi gõ, không phân biệt hoa thường (vẫn phân biệt dấu),
   giống GenreService.findSameName phía server. */
(function () {
    var input = document.getElementById('name');
    var error = document.getElementById('nameError');
    if (!input || !error) return;

    function key(s) { return s.normalize('NFC').trim().replace(/\s+/g, ' ').toLowerCase(); }

    var existing = {};
    document.querySelectorAll('#existingGenres [data-name]').forEach(function (li) {
        existing[key(li.dataset.name)] = li.dataset.name;
    });

    function check() {
        var dup = existing[key(input.value)];
        var msg = dup ? 'Thể loại "' + dup + '" đã tồn tại (không phân biệt hoa thường). Vui lòng nhập tên thể loại khác.' : '';
        input.setCustomValidity(msg); // chặn submit, trình duyệt hiện thông báo
        input.classList.toggle('is-invalid', !!dup);
        error.textContent = msg;
        error.hidden = !dup;
    }

    input.addEventListener('input', check);
    check();
})();
