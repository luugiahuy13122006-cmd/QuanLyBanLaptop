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

  // AJAX "Thêm giỏ hàng": giữ nguyên trang hiện tại, cập nhật badge và hiện toast (riêng "Mua Ngay" vẫn chuyển /dat-hang)
  document.addEventListener('submit', function (e) {
    const form = e.target;
    if (!form || !form.getAttribute) return;
    const action = form.getAttribute('action') || '';
    if (!action.includes('/gio-hang/them/')) return;
    const submitter = e.submitter;
    if (submitter && submitter.name === 'buyNow') return;

    e.preventDefault();
    const formData = new FormData(form);
    if (submitter) submitter.disabled = true;

    fetch(form.action, {
      method: 'POST',
      body: formData,
      headers: {
        'X-Requested-With': 'XMLHttpRequest',
        'Accept': 'application/json'
      },
      credentials: 'same-origin'
    })
      .then(function (res) {
        if (res.redirected && res.url && res.url.includes('/login')) {
          window.location.href = res.url;
          return null;
        }
        const ct = res.headers.get('content-type') || '';
        if (!ct.includes('application/json')) {
          window.location.reload();
          return null;
        }
        return res.json();
      })
      .then(function (data) {
        if (!data) return;
        if (typeof data.cartCount === 'number') {
          document.querySelectorAll('a[href="/gio-hang"] .badge, a[href$="/gio-hang"] .badge').forEach(function (badge) {
            badge.textContent = String(data.cartCount);
          });
        }
        if (typeof window.NexoraToast === 'function') {
          window.NexoraToast('Giỏ hàng', data.message || 'Đã cập nhật giỏ hàng.', data.ok ? 'success' : 'danger');
        }
      })
      .catch(function () {
        if (typeof window.NexoraToast === 'function') {
          window.NexoraToast('Giỏ hàng', 'Không thể thêm vào giỏ hàng, vui lòng thử lại.', 'danger');
        }
      })
      .finally(function () {
        if (submitter) submitter.disabled = false;
      });
  });
});
