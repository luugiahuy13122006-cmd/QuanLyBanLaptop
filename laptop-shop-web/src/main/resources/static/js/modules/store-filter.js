// Live search autocomplete in navbar & interactive coupon check on checkout
document.addEventListener('DOMContentLoaded', function () {
  const searchInput = document.querySelector('.pv-search-box input[name="keyword"]');
  const acMenu = document.getElementById('nxHeaderAutocomplete');
  let acTimer = null;

  if (searchInput && acMenu) {
    searchInput.addEventListener('input', function () {
      clearTimeout(acTimer);
      const q = searchInput.value.trim();
      if (q.length < 2) {
        acMenu.classList.remove('show');
        return;
      }
      acTimer = setTimeout(function () {
        fetch('/api/store/autocomplete?q=' + encodeURIComponent(q))
          .then(r => r.json())
          .then(items => {
            if (!Array.isArray(items) || items.length === 0) {
              acMenu.classList.remove('show');
              return;
            }
            acMenu.innerHTML = items.map(it =>
              '<a class="nx-ac-item" href="/san-pham/' + it.id + '">' +
              '<img src="' + (it.image || '/images/laptop-default.svg') + '" alt=""/>' +
              '<div class="flex-grow-1"><div class="fw-semibold small">' + (it.name || '') + '</div>' +
              '<small class="text-muted">' + (it.sku || '') + '</small></div>' +
              '<strong class="text-primary small">' + Number(it.price || 0).toLocaleString('vi-VN') + ' đ</strong></a>'
            ).join('');
            acMenu.classList.add('show');
          }).catch(() => {});
      }, 220);
    });

    document.addEventListener('click', function (e) {
      if (!acMenu.contains(e.target) && e.target !== searchInput) {
        acMenu.classList.remove('show');
      }
    });
  }
});
