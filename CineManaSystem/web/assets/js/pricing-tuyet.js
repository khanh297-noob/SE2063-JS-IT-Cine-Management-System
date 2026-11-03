// Lọc bảng giá vé ở trang chủ (#pricing) theo chi nhánh.
// Mỗi nút có data-branch (id chi nhánh hoặc "all"), mỗi dòng của bảng có data-branch tương ứng.
(function () {
    'use strict';

    var filter = document.getElementById('branchFilter');
    var table = document.getElementById('priceTable');
    if (!filter || !table) {
        return;
    }

    var chips = filter.querySelectorAll('.branch-chip');
    var rows = table.querySelectorAll('tbody tr');
    var empty = document.getElementById('priceEmpty');

    function apply(branch) {
        var shown = 0;
        rows.forEach(function (row) {
            var visible = branch === 'all' || row.getAttribute('data-branch') === branch;
            row.hidden = !visible;
            if (visible) {
                shown++;
            }
        });
        chips.forEach(function (chip) {
            chip.classList.toggle('active', chip.getAttribute('data-branch') === branch);
        });
        table.parentNode.hidden = shown === 0;
        if (empty) {
            empty.hidden = shown !== 0;
        }
    }

    filter.addEventListener('click', function (event) {
        var chip = event.target.closest('.branch-chip');
        if (chip) {
            apply(chip.getAttribute('data-branch'));
        }
    });
})();
