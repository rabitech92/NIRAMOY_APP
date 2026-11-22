(function () {
  const sidebar = document.getElementById('sidebar');
  const overlay = document.getElementById('navOverlay');
  const toggle = () => { sidebar.classList.toggle('open'); overlay.classList.toggle('open'); };
  document.querySelectorAll('[data-toggle-nav]').forEach(b => b.addEventListener('click', toggle));
  if (overlay) overlay.addEventListener('click', toggle);
  window.addEventListener('load', () => { if (window.lucide) window.lucide.createIcons(); });
})();
