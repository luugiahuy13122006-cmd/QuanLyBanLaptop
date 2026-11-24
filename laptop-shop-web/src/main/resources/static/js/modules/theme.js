// Dark/Light theme switcher (persistent in localStorage)
(function () {
  function applyTheme(theme) {
    document.documentElement.setAttribute('data-theme', theme);
    document.documentElement.setAttribute('data-bs-theme', theme);
    localStorage.setItem('nexora-theme', theme);
    document.querySelectorAll('[data-theme-icon]').forEach(function (el) {
      el.className = theme === 'dark' ? 'bi bi-sun-fill text-warning' : 'bi bi-moon-stars-fill';
    });
  }

  document.addEventListener('DOMContentLoaded', function () {
    const saved = localStorage.getItem('nexora-theme') || 'light';
    applyTheme(saved);
    document.querySelectorAll('[data-theme-toggle]').forEach(function (btn) {
      btn.addEventListener('click', function () {
        const next = document.documentElement.getAttribute('data-theme') === 'dark' ? 'light' : 'dark';
        applyTheme(next);
      });
    });
  });
})();
