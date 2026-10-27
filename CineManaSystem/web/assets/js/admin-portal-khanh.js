document.addEventListener("DOMContentLoaded", function () {
    const search = document.getElementById("module-search");
    const clear = document.getElementById("module-clear");
    const reset = document.getElementById("module-reset");
    const count = document.getElementById("module-count");
    const empty = document.getElementById("module-empty");
    const cards = Array.from(document.querySelectorAll("[data-module-card]"));

    if (!search || !count || !empty) {
        return;
    }

    function normalize(text) {
        return text.normalize("NFD")
            .replace(/[\u0300-\u036f]/g, "")
            .replace(/[đĐ]/g, "d")
            .toLowerCase()
            .trim();
    }

    function applyFilter() {
        const keyword = normalize(search.value);
        let visible = 0;

        cards.forEach(function (card) {
            const text = normalize(card.textContent + " " + (card.dataset.keywords || ""));
            const matches = text.includes(keyword);
            card.hidden = !matches;
            if (matches) {
                visible += 1;
            }
        });

        count.textContent = visible + " phân hệ";
        empty.hidden = visible !== 0;
        if (clear) {
            clear.hidden = keyword.length === 0;
        }
    }

    function clearFilter() {
        search.value = "";
        applyFilter();
        search.focus();
    }

    search.addEventListener("input", applyFilter);
    if (clear) {
        clear.addEventListener("click", clearFilter);
    }
    if (reset) {
        reset.addEventListener("click", clearFilter);
    }
    window.addEventListener("pageshow", applyFilter);
    applyFilter();

    // Chỉ bản HTML xem trước mới có data-preview=true.
    if (document.body.dataset.preview === "true") {
        const note = document.getElementById("portal-preview-note");
        document.querySelectorAll("a[href='#']").forEach(function (link) {
            link.addEventListener("click", function (event) {
                event.preventDefault();
                if (note) {
                    note.hidden = false;
                    note.textContent = "Đây là bản xem trước giao diện. Trong project, liên kết mở đúng Servlet của phân hệ.";
                }
            });
        });
    }
});
