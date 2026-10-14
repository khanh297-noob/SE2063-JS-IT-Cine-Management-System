/* Màn Configure Available Branches: đồng bộ checkbox, ô ngày, dải ghế và bộ đếm thay đổi. */
(function () {
    var rows = Array.prototype.slice.call(document.querySelectorAll('.branch-row'));
    if (!rows.length) return;
    var onCount = document.getElementById('onCount');
    var pending = document.getElementById('pending');

    function box(row) { return row.querySelector('.branch-toggle'); }
    function dates(row) { return row.querySelectorAll('.dates input'); }

    function sync() {
        var on = 0, changed = 0;
        rows.forEach(function (row) {
            var checked = box(row).checked;
            if (checked) on++;
            row.classList.toggle('is-on', checked);
            var seat = document.querySelector('[data-seat="' + row.dataset.id + '"]');
            if (seat) seat.classList.toggle('on', checked);

            var dateChanged = false;
            dates(row).forEach(function (input) {
                if (!input.hasAttribute('data-readonly')) input.disabled = !checked;
                if (input.value !== input.defaultValue) dateChanged = true;
            });
            if (String(checked) !== row.dataset.initial || (checked && dateChanged)) changed++;
        });
        if (onCount) onCount.textContent = on;
        if (pending) {
            pending.textContent = changed === 0 ? 'Chưa có thay đổi' : changed + ' chi nhánh sẽ thay đổi';
            pending.classList.toggle('has-changes', changed > 0);
        }
    }

    rows.forEach(function (row) {
        box(row).addEventListener('change', sync);
        dates(row).forEach(function (i) { i.addEventListener('input', sync); });
    });

    function editableVisible() {
        return rows.filter(function (r) { return !r.classList.contains('is-hidden'); })
                   .map(box).filter(function (b) { return !b.disabled; });
    }

    var selectAll = document.getElementById('selectAll');
    if (selectAll) selectAll.addEventListener('click', function () {
        editableVisible().forEach(function (b) {
            if (!b.closest('.branch-row').querySelector('.tag-inactive')) b.checked = true;
        });
        sync();
    });
    var clearAll = document.getElementById('clearAll');
    if (clearAll) clearAll.addEventListener('click', function () {
        editableVisible().forEach(function (b) { b.checked = false; });
        sync();
    });
    // Bỏ dấu tiếng Việt để "da nang" khớp "Đà Nẵng" (giống TextUtil.fold phía server).
    function fold(s) {
        return s.normalize('NFD').replace(/[̀-ͯ]/g, '').replace(/đ/g, 'd').replace(/Đ/g, 'D')
                .toLowerCase().replace(/\s+/g, ' ').trim();
    }
    var filter = document.getElementById('branchFilter');
    if (filter) filter.addEventListener('input', function () {
        var q = fold(filter.value);
        rows.forEach(function (r) { r.classList.toggle('is-hidden', q !== '' && fold(r.dataset.search).indexOf(q) === -1); });
    });

    var form = document.getElementById('branchForm');
    form.addEventListener('submit', function (e) {
        var removed = rows.filter(function (r) { return r.dataset.initial === 'true' && !box(r).checked; }).length;
        if (removed > 0 && !confirm('Gỡ phim khỏi ' + removed + ' chi nhánh? Khách sẽ không thấy phim ở các chi nhánh này nữa.')) {
            e.preventDefault();
        }
    });

    sync();
})();
