// Micro-interactions: scroll reveal, countdown timer, subtle 3D hover tilt
document.addEventListener('DOMContentLoaded', function () {
  // 1. Intersection Observer reveal (+ immediate check & scroll fallback)
  const reveals = document.querySelectorAll('.nx-reveal');
  const checkVisible = function () {
    const h = window.innerHeight || document.documentElement.clientHeight || 900;
    reveals.forEach(function (el) {
      if (!el.classList.contains('is-visible')) {
        const rect = el.getBoundingClientRect();
        if (rect.top <= h * 1.15) {
          el.classList.add('is-visible');
        }
      }
    });
  };
  if ('IntersectionObserver' in window) {
    const io = new IntersectionObserver(function (entries) {
      entries.forEach(function (entry) {
        if (entry.isIntersecting) {
          entry.target.classList.add('is-visible');
          io.unobserve(entry.target);
        }
      });
    }, { threshold: 0.02, rootMargin: '120px 0px' });
    reveals.forEach(el => io.observe(el));
    window.addEventListener('scroll', checkVisible, { passive: true });
    checkVisible();
  } else {
    reveals.forEach(el => el.classList.add('is-visible'));
  }

  // 2. Flash sale daily countdown
  const countdownEl = document.getElementById('nxFlashCountdown');
  if (countdownEl) {
    const updateClock = function () {
      const now = new Date();
      const end = new Date();
      end.setHours(23, 59, 59, 999);
      const diff = Math.max(0, Math.floor((end - now) / 1000));
      const h = String(Math.floor(diff / 3600)).padStart(2, '0');
      const m = String(Math.floor((diff % 3600) / 60)).padStart(2, '0');
      const s = String(diff % 60).padStart(2, '0');
      countdownEl.textContent = h + ':' + m + ':' + s;
    };
    updateClock();
    setInterval(updateClock, 1000);
  }

  // 3. Register PWA Service Worker if supported
  if ('serviceWorker' in navigator) {
    navigator.serviceWorker.register('/sw.js').catch(() => {});
  }
});
