/* NEXORA LAPTOP - hiệu ứng chuyển động & tương tác giao diện (không phụ thuộc thư viện ngoài) */
(function () {
  'use strict';
  var doc = document;
  var reduce = window.matchMedia && window.matchMedia('(prefers-reduced-motion: reduce)').matches;

  function ready(fn) {
    if (doc.readyState !== 'loading') fn(); else doc.addEventListener('DOMContentLoaded', fn);
  }

  /* ---- Thanh tiến trình cuộn, nút lên đầu trang, thu gọn header ---- */
  function initScrollUi() {
    var bar = doc.createElement('div');
    bar.className = 'nx-progress';
    doc.body.appendChild(bar);
    var top = doc.createElement('button');
    top.type = 'button';
    top.className = 'nx-totop';
    top.setAttribute('aria-label', 'Lên đầu trang');
    top.innerHTML = '<i class="bi bi-arrow-up"></i>';
    top.addEventListener('click', function () { window.scrollTo({ top: 0, behavior: reduce ? 'auto' : 'smooth' }); });
    doc.body.appendChild(top);
    var header = doc.querySelector('.pv-header');
    var ticking = false;
    function update() {
      var y = window.pageYOffset || doc.documentElement.scrollTop;
      var h = Math.max(1, doc.documentElement.scrollHeight - window.innerHeight);
      bar.style.transform = 'scaleX(' + Math.min(1, y / h) + ')';
      top.classList.toggle('show', y > 520);
      if (header) header.classList.toggle('is-scrolled', y > 60);
      ticking = false;
    }
    window.addEventListener('scroll', function () {
      if (!ticking) { ticking = true; window.requestAnimationFrame(update); }
    }, { passive: true });
    update();
  }

  /* ---- Hiệu ứng xuất hiện khi cuộn (Fade / Fly-in / Zoom / Wipe ...) ---- */
  var FX_RULES = [
    ['.pv-hero-banner', 'zoom'],
    ['.pv-flash-section', 'wipe'],
    ['.pv-cat-sidebar', 'left'],
    ['.pro-stepper', 'left'],
    ['.pv-card', 'up', true],
    ['.pro-kpi-card', 'zoom', true],
    ['.pro-card', 'up'],
    ['.pro-auth-card', 'zoom'],
    ['.nx-page-hero', 'wipe'],
    ['.nx-page-card', 'up'],
    ['.nx-trust-item', 'up', true],
    ['.nx-footer .nx-col', 'up', true],
    ['.pv-brand-chip', 'fade', true],
    ['.pv-gift-box', 'right'],
    ['.pv-policy-box', 'fade']
  ];

  function initReveal() {
    var targets = [];
    FX_RULES.forEach(function (rule) {
      var counters = new Map();
      doc.querySelectorAll(rule[0]).forEach(function (el) {
        if (el.hasAttribute('data-fx') || el.closest('.modal') || el.closest('[data-no-fx]')) return;
        if (el.classList.contains('anim-fade-up') || el.classList.contains('anim-pop-in')) return;
        el.setAttribute('data-fx', el.getAttribute('data-fx-type') || rule[1]);
        if (rule[2]) {
          var p = el.parentElement;
          var idx = counters.get(p) || 0;
          counters.set(p, idx + 1);
          el.style.setProperty('--fx-delay', Math.min(idx, 9) * 70 + 'ms');
        }
        targets.push(el);
      });
    });
    if (!targets.length) return;
    if (reduce || !('IntersectionObserver' in window)) {
      targets.forEach(function (el) { el.classList.add('fx-in'); });
      return;
    }
    var io = new IntersectionObserver(function (entries) {
      entries.forEach(function (en) {
        if (en.isIntersecting) {
          en.target.classList.add('fx-in');
          io.unobserve(en.target);
        }
      });
    }, { threshold: 0.08, rootMargin: '0px 0px -4% 0px' });
    targets.forEach(function (el) { io.observe(el); });
  }

  /* ---- Số liệu KPI đếm tăng dần ---- */
  function animateNumber(el) {
    var text = el.textContent;
    var m = text.match(/\d[\d.,]*/);
    if (!m) return;
    var raw = m[0];
    if (!/^\d{1,3}([.,]\d{3})*$/.test(raw) && !/^\d+$/.test(raw)) return;
    var sep = (raw.match(/[.,]/) || [''])[0];
    var target = parseInt(raw.replace(/[.,]/g, ''), 10);
    if (!(target > 0) || target > 1e12) return;
    var start = null;
    var dur = 1000;
    function fmt(n) { return sep ? String(n).replace(/\B(?=(\d{3})+(?!\d))/g, sep) : String(n); }
    function step(ts) {
      if (start === null) start = ts;
      var t = Math.min(1, (ts - start) / dur);
      var eased = 1 - Math.pow(1 - t, 3);
      el.textContent = text.replace(raw, fmt(Math.round(target * eased)));
      if (t < 1) window.requestAnimationFrame(step); else el.textContent = text;
    }
    window.requestAnimationFrame(step);
  }

  function initCounters() {
    if (reduce || !('IntersectionObserver' in window)) return;
    var els = doc.querySelectorAll('.pro-kpi-value, [data-count]');
    if (!els.length) return;
    var io = new IntersectionObserver(function (entries) {
      entries.forEach(function (en) {
        if (en.isIntersecting) { animateNumber(en.target); io.unobserve(en.target); }
      });
    }, { threshold: 0.4 });
    els.forEach(function (el) { io.observe(el); });
  }

  /* ---- Hiệu ứng gợn sóng khi bấm nút ---- */
  function initRipple() {
    if (reduce) return;
    doc.addEventListener('pointerdown', function (e) {
      var host = e.target.closest && e.target.closest('.btn, .pro-nav-pill, .pv-brand-chip, .nx-subnav a');
      if (!host || host.disabled || host.classList.contains('btn-close') || host.classList.contains('dropdown-toggle')) return;
      if (!host.classList.contains('nx-ripple-host')) {
        if (window.getComputedStyle(host).position === 'static') host.classList.add('nx-ripple-host');
        else host.style.overflow = host.style.overflow || 'hidden';
      }
      var r = host.getBoundingClientRect();
      var size = Math.max(r.width, r.height) * 2;
      var s = doc.createElement('span');
      s.className = 'nx-ripple';
      s.style.width = s.style.height = size + 'px';
      s.style.left = (e.clientX - r.left - size / 2) + 'px';
      s.style.top = (e.clientY - r.top - size / 2) + 'px';
      host.appendChild(s);
      setTimeout(function () { if (s.parentNode) s.parentNode.removeChild(s); }, 650);
    }, { passive: true });
  }

  /* ---- Thẻ sản phẩm nghiêng 3D nhẹ theo con trỏ ---- */
  function initTilt() {
    if (reduce || !window.matchMedia || !window.matchMedia('(hover: hover) and (pointer: fine)').matches) return;
    doc.querySelectorAll('.pv-card').forEach(function (card) {
      card.addEventListener('pointermove', function (e) {
        var r = card.getBoundingClientRect();
        var px = (e.clientX - r.left) / r.width - 0.5;
        var py = (e.clientY - r.top) / r.height - 0.5;
        card.style.transform = 'perspective(900px) rotateX(' + (-py * 5).toFixed(2) + 'deg) rotateY(' + (px * 6).toFixed(2) + 'deg) translateY(-5px)';
      });
      card.addEventListener('pointerleave', function () { card.style.transform = ''; });
    });
  }

  /* ---- Hiệu ứng mờ dần khi chuyển trang ---- */
  function initPageTransitions() {
    doc.addEventListener('click', function (e) {
      var a = e.target.closest && e.target.closest('a[href]');
      if (!a || e.defaultPrevented || e.button !== 0 || e.metaKey || e.ctrlKey || e.shiftKey || e.altKey) return;
      if ((a.target && a.target !== '_self') || a.hasAttribute('download') || a.hasAttribute('data-bs-toggle') || a.hasAttribute('data-no-transition')) return;
      var href = a.getAttribute('href') || '';
      if (!href || href.charAt(0) === '#' || /^(javascript|mailto|tel):/i.test(href)) return;
      var url;
      try { url = new URL(a.href, window.location.href); } catch (err) { return; }
      if (url.origin !== window.location.origin) return;
      if (url.pathname === window.location.pathname && url.search === window.location.search) return;
      if (/\.(png|jpe?g|gif|svg|pdf|zip|csv|xlsx?)$/i.test(url.pathname)) return;
      e.preventDefault();
      doc.body.classList.add('nx-leaving');
      setTimeout(function () { window.location.href = url.href; }, reduce ? 0 : 180);
    });
    window.addEventListener('pageshow', function (e) {
      if (e.persisted) {
        doc.body.classList.remove('nx-leaving');
        unlockForms();
      }
    });
  }

  /* ---- Khóa nút gửi form để chống bấm đúp (đặt hàng, thanh toán, lưu dữ liệu) ---- */
  function unlockForms() {
    doc.querySelectorAll('form[data-nx-locked="1"]').forEach(function (f) {
      f.removeAttribute('data-nx-locked');
      f.querySelectorAll('button[disabled][data-nx-was]').forEach(function (b) {
        b.disabled = false;
        b.removeAttribute('data-nx-was');
        var sp = b.querySelector('.nx-spin');
        if (sp) sp.remove();
      });
    });
  }

  function initSubmitLock() {
    doc.addEventListener('submit', function (e) {
      var f = e.target;
      if (!(f instanceof HTMLFormElement)) return;
      if ((f.method || '').toLowerCase() !== 'post' || f.hasAttribute('data-no-lock') || (f.target && f.target !== '_self')) return;
      if (f.getAttribute('data-nx-locked') === '1') { e.preventDefault(); return; }
      var submitter = e.submitter;
      setTimeout(function () {
        if (e.defaultPrevented) return;
        f.setAttribute('data-nx-locked', '1');
        f.querySelectorAll('button[type="submit"], button:not([type])').forEach(function (b) {
          b.disabled = true;
          b.setAttribute('data-nx-was', '1');
          if (b === submitter && !b.querySelector('.nx-spin')) {
            var sp = doc.createElement('span');
            sp.className = 'nx-spin';
            b.insertBefore(sp, b.firstChild);
          }
        });
      }, 0);
    });
  }

  /* ---- Đánh dấu mục danh mục đang xem ---- */
  function initNavActive() {
    var links = doc.querySelectorAll('.nx-subnav a[href]');
    if (!links.length) return;
    var cur = window.location.pathname + window.location.search;
    var matched = false;
    links.forEach(function (a) {
      var u = new URL(a.href, window.location.href);
      if (u.pathname + u.search === cur && (u.search || cur === '/')) { a.classList.add('active'); matched = true; }
    });
    if (!matched && window.location.pathname === '/') {
      var all = doc.querySelector('.nx-subnav a[data-nav-all]');
      if (all) all.classList.add('active');
    }
  }

  ready(function () {
    initScrollUi();
    initReveal();
    initCounters();
    initRipple();
    initTilt();
    initPageTransitions();
    initSubmitLock();
    initNavActive();
  });
})();
