/**
 * Página de registro de distribuidores y clientes.
 */
(() => {
  const form = document.getElementById('register-form');
  const submitBtn = document.getElementById('register-btn');

  if (Api.isLoggedIn()) {
    location.href = '/index.html';
    return;
  }

  // El RFC se escribe siempre en mayúsculas
  form.taxId.addEventListener('input', () => {
    form.taxId.value = form.taxId.value.toUpperCase();
  });

  form.addEventListener('submit', async event => {
    event.preventDefault();
    UI.clearAlert('register-alert');
    UI.clearFieldErrors(form);

    // Validación del navegador (HTML5) antes de llamar a la API
    if (!form.checkValidity()) {
      form.classList.add('was-validated');
      return;
    }

    const payload = {
      email: form.email.value.trim(),
      password: form.password.value,
      fullName: form.fullName.value.trim(),
      companyName: form.companyName.value.trim(),
      taxId: form.taxId.value.trim(),
      accountType: form.accountType.value
    };

    try {
      const auth = await UI.withLoading(submitBtn, () => Api.post('/auth/register', payload));
      Api.saveSession(auth);
      location.href = '/index.html?welcome=1';
    } catch (error) {
      // Los errores de validación del backend se marcan en cada campo
      form.classList.remove('was-validated');
      UI.applyFieldErrors(form, error);
      UI.showAlert('register-alert', error);
    }
  });
})();