/**
 * Página de inicio de sesión.
 */
(() => {
  const form = document.getElementById('login-form');
  const submitBtn = document.getElementById('login-btn');
  const params = new URLSearchParams(location.search);

  // Solo se permite regresar a rutas internas (evita redirecciones abiertas a otros sitios)
  const requestedNext = params.get('next');
  const next = requestedNext && requestedNext.startsWith('/') && !requestedNext.startsWith('//')
    ? requestedNext
    : '/index.html';

  if (Api.isLoggedIn()) {
    location.href = next;
    return;
  }

  if (params.get('expired')) {
    UI.showAlert('login-alert', 'Su sesión expiró. Inicie sesión nuevamente.', 'warning');
  }
  if (params.get('registered')) {
    UI.showAlert('login-alert', 'Cuenta creada correctamente.', 'success');
  }

  // Botones de cuentas de prueba (facilitan la demo)
  document.querySelectorAll('[data-demo-email]').forEach(button => {
    button.addEventListener('click', () => {
      form.email.value = button.dataset.demoEmail;
      form.password.value = button.dataset.demoPassword;
      form.email.focus();
    });
  });

  form.addEventListener('submit', async event => {
    event.preventDefault();
    UI.clearAlert('login-alert');
    UI.clearFieldErrors(form);

    if (!form.checkValidity()) {
      form.classList.add('was-validated');
      return;
    }

    try {
      const auth = await UI.withLoading(submitBtn, () => Api.post('/auth/login', {
        email: form.email.value.trim(),
        password: form.password.value
      }));
      Api.saveSession(auth);
      location.href = next;
    } catch (error) {
      UI.applyFieldErrors(form, error);
      UI.showAlert('login-alert', error);
    }
  });
})();