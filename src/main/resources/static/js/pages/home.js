/**
 * Inicio provisional (Paso 14). En el Paso 15 esta página se convierte en el catálogo.
 */
(() => {
  const user = UI.requireAuth();
  if (!user) return;

  UI.renderNavbar('catalog');

  document.getElementById('welcome-name').textContent = user.fullName;
  document.getElementById('welcome-company').textContent = user.companyName;
  document.getElementById('welcome-role').textContent = user.role;

  if (new URLSearchParams(location.search).get('welcome')) {
    UI.toast('¡Bienvenido! Su cuenta está lista para comprar.');
  }

  // Comprueba la conexión con la API usando el token (GET /users/me)
  Api.get('/users/me')
    .then(profile => {
      document.getElementById('api-status').innerHTML =
        `<i class="bi bi-check-circle-fill text-success me-1"></i>Conectado a la API como <strong>${UI.escapeHtml(profile.email)}</strong>`;
    })
    .catch(error => UI.showAlert('home-alert', error));
})();