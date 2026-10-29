/**
 * Utilidades de interfaz compartidas por todas las páginas:
 * barra de navegación, protección de páginas, alertas, formatos y errores de formulario.
 */
const UI = (() => {

  // ------------------------------------------------------------------
  // Formatos
  // ------------------------------------------------------------------

  const money = new Intl.NumberFormat('es-MX', { style: 'currency', currency: 'MXN' });
  const dateTime = new Intl.DateTimeFormat('es-MX', { dateStyle: 'medium', timeStyle: 'short' });
  const dateOnly = new Intl.DateTimeFormat('es-MX', { dateStyle: 'medium', timeZone: 'UTC' });

  function formatMoney(value) {
    return money.format(Number(value || 0));
  }

  function formatDateTime(isoInstant) {
    return isoInstant ? dateTime.format(new Date(isoInstant)) : '—';
  }

  /** Para fechas sin hora (ej. 2026-10-31): se interpretan en UTC para no "recorrer" el día. */
  function formatDate(isoDate) {
    return isoDate ? dateOnly.format(new Date(isoDate + 'T00:00:00Z')) : '—';
  }

  /** Evita inyección de HTML al pintar datos que vienen de la API. */
  function escapeHtml(value) {
    return String(value == null ? '' : value)
      .replaceAll('&', '&amp;')
      .replaceAll('<', '&lt;')
      .replaceAll('>', '&gt;')
      .replaceAll('"', '&quot;')
      .replaceAll("'", '&#39;');
  }

  // ------------------------------------------------------------------
  // Estatus del pedido
  // ------------------------------------------------------------------

  const ORDER_STATUS = {
    PENDING_PAYMENT: { label: 'Pendiente de pago', color: 'warning' },
    PAYMENT_FAILED: { label: 'Pago fallido', color: 'danger' },
    PAID: { label: 'Pagado', color: 'info' },
    CONFIRMED: { label: 'Confirmado por ERP', color: 'primary' },
    SHIPPED: { label: 'Enviado', color: 'secondary' },
    DELIVERED: { label: 'Entregado', color: 'success' },
    CANCELLED: { label: 'Cancelado', color: 'dark' }
  };

  function statusBadge(status) {
    const info = ORDER_STATUS[status] || { label: status, color: 'light' };
    return `<span class="badge text-bg-${info.color}">${escapeHtml(info.label)}</span>`;
  }

  // ------------------------------------------------------------------
  // Protección de páginas
  // ------------------------------------------------------------------

  function redirectToLogin() {
    const next = encodeURIComponent(location.pathname + location.search);
    location.href = '/login.html?next=' + next;
  }

  /** Llamar al inicio de cada página privada. Devuelve el usuario o redirige al login. */
  function requireAuth() {
    const user = Api.currentUser();
    if (!user) {
      redirectToLogin();
      return null;
    }
    return user;
  }

  function requireAdmin() {
    const user = requireAuth();
    if (user && user.role !== 'ADMIN') {
      location.href = '/index.html';
      return null;
    }
    return user;
  }

  // ------------------------------------------------------------------
  // Barra de navegación
  // ------------------------------------------------------------------

  /**
   * Pinta la barra en <nav id="app-navbar">.
   * @param {string} active clave de la sección activa: catalog, cart, orders, admin
   */
  function renderNavbar(active) {
    const container = document.getElementById('app-navbar');
    if (!container) return;

    const user = Api.currentUser();
    const isAdmin = user && user.role === 'ADMIN';
    const link = (key, href, icon, text) => `
      <li class="nav-item">
        <a class="nav-link ${active === key ? 'active' : ''}" href="${href}">
          <i class="bi ${icon} me-1"></i>${text}
        </a>
      </li>`;

    const adminMenu = isAdmin ? `
      <li class="nav-item dropdown">
        <a class="nav-link dropdown-toggle ${active === 'admin' ? 'active' : ''}" href="#" role="button"
           data-bs-toggle="dropdown" aria-expanded="false">
          <i class="bi bi-speedometer2 me-1"></i>Administración
        </a>
        <ul class="dropdown-menu">
          <li><a class="dropdown-item" href="/admin/metrics.html"><i class="bi bi-graph-up me-2"></i>Métricas</a></li>
          <li><a class="dropdown-item" href="/admin/orders.html"><i class="bi bi-truck me-2"></i>Pedidos</a></li>
          <li><a class="dropdown-item" href="/admin/products.html"><i class="bi bi-box-seam me-2"></i>Productos</a></li>
          <li><a class="dropdown-item" href="/admin/integrations.html"><i class="bi bi-plug me-2"></i>Integraciones ERP/CRM</a></li>
        </ul>
      </li>` : '';

    const userMenu = user ? `
      <div class="d-flex align-items-center gap-3">
        <a class="nav-link position-relative text-white ${active === 'cart' ? 'fw-semibold' : ''}" href="/cart.html"
           title="Carrito">
          <i class="bi bi-cart3 fs-5"></i>
          <span id="cart-badge" class="position-absolute top-0 start-100 translate-middle badge rounded-pill bg-warning text-dark d-none">0</span>
        </a>
        <div class="dropdown">
          <button class="btn btn-sm btn-outline-light dropdown-toggle" data-bs-toggle="dropdown" aria-expanded="false">
            <i class="bi bi-person-circle me-1"></i>${escapeHtml(user.fullName)}
          </button>
          <ul class="dropdown-menu dropdown-menu-end">
            <li><h6 class="dropdown-header">${escapeHtml(user.companyName)}</h6></li>
            <li><span class="dropdown-item-text small text-muted">${escapeHtml(user.email)} · ${escapeHtml(user.role)}</span></li>
            <li><hr class="dropdown-divider"></li>
            <li><button class="dropdown-item" id="logout-btn"><i class="bi bi-box-arrow-right me-2"></i>Cerrar sesión</button></li>
          </ul>
        </div>
      </div>` : `
      <a class="btn btn-sm btn-outline-light" href="/login.html">Iniciar sesión</a>`;

    container.className = 'navbar navbar-expand-lg navbar-dark app-navbar sticky-top';
    container.innerHTML = `
      <div class="container">
        <a class="navbar-brand fw-semibold" href="/index.html">
          <i class="bi bi-boxes me-2"></i>B2B Marketplace
        </a>
        <button class="navbar-toggler" type="button" data-bs-toggle="collapse" data-bs-target="#main-nav"
                aria-controls="main-nav" aria-expanded="false" aria-label="Menú">
          <span class="navbar-toggler-icon"></span>
        </button>
        <div class="collapse navbar-collapse" id="main-nav">
          <ul class="navbar-nav me-auto mb-2 mb-lg-0">
            ${user ? link('catalog', '/index.html', 'bi-grid', 'Catálogo') : ''}
            ${user ? link('orders', '/orders.html', 'bi-receipt', 'Mis pedidos') : ''}
            ${adminMenu}
          </ul>
          ${userMenu}
        </div>
      </div>`;

    const logoutBtn = document.getElementById('logout-btn');
    if (logoutBtn) {
      logoutBtn.addEventListener('click', () => {
        Api.clearSession();
        location.href = '/login.html';
      });
    }

    if (user) {
      refreshCartBadge();
    }
  }

  /** Actualiza el número de artículos del carrito en la barra. */
  async function refreshCartBadge(totalItems) {
    const badge = document.getElementById('cart-badge');
    if (!badge) return;
    try {
      const count = totalItems !== undefined ? totalItems : (await Api.get('/cart')).totalItems;
      badge.textContent = count;
      badge.classList.toggle('d-none', !count);
    } catch (e) {
      badge.classList.add('d-none');
    }
  }

  // ------------------------------------------------------------------
  // Alertas y errores
  // ------------------------------------------------------------------

  /**
   * Muestra una alerta en el contenedor indicado.
   * @param {HTMLElement|string} target elemento o id
   * @param {string|Error} content mensaje o error (ApiError muestra detalle, errores por campo y referencia)
   * @param {string} type success, danger, warning, info
   */
  function showAlert(target, content, type = 'danger') {
    const container = typeof target === 'string' ? document.getElementById(target) : target;
    if (!container) return;

    let html;
    if (content instanceof Api.ApiError) {
      const fieldList = content.errors.length
        ? `<ul class="mb-0 mt-2 small">${content.errors
            .map(e => `<li><strong>${escapeHtml(e.field)}:</strong> ${escapeHtml(e.message)}</li>`).join('')}</ul>`
        : '';
      const reference = content.correlationId
        ? `<div class="small text-muted mt-2">Referencia para soporte: <code>${escapeHtml(content.correlationId)}</code></div>`
        : '';
      html = `${escapeHtml(content.message)}${fieldList}${reference}`;
    } else if (content instanceof Error) {
      html = escapeHtml(content.message);
    } else {
      html = escapeHtml(content);
    }

    container.innerHTML = `
      <div class="alert alert-${type} alert-dismissible fade show" role="alert">
        ${html}
        <button type="button" class="btn-close" data-bs-dismiss="alert" aria-label="Cerrar"></button>
      </div>`;
  }

  function clearAlert(target) {
    const container = typeof target === 'string' ? document.getElementById(target) : target;
    if (container) container.innerHTML = '';
  }

  /** Notificación breve en la esquina (no bloquea la pantalla). */
  function toast(message, type = 'success') {
    let area = document.getElementById('toast-area');
    if (!area) {
      area = document.createElement('div');
      area.id = 'toast-area';
      area.className = 'toast-container position-fixed bottom-0 end-0 p-3';
      document.body.appendChild(area);
    }
    const element = document.createElement('div');
    element.className = `toast align-items-center text-bg-${type} border-0`;
    element.setAttribute('role', 'status');
    element.innerHTML = `
      <div class="d-flex">
        <div class="toast-body">${escapeHtml(message)}</div>
        <button type="button" class="btn-close btn-close-white me-2 m-auto" data-bs-dismiss="toast" aria-label="Cerrar"></button>
      </div>`;
    area.appendChild(element);
    const instance = bootstrap.Toast.getOrCreateInstance(element, { delay: 3500 });
    element.addEventListener('hidden.bs.toast', () => element.remove());
    instance.show();
  }

  /**
   * Marca en rojo los campos que el backend rechazó (lista "errors" del ProblemDetail).
   * Cada input debe tener name igual al campo del DTO y un <div class="invalid-feedback"> después.
   */
  function applyFieldErrors(form, error) {
    clearFieldErrors(form);
    if (!(error instanceof Api.ApiError)) return;
    error.errors.forEach(fieldError => {
      const input = form.querySelector(`[name="${fieldError.field}"]`);
      if (!input) return;
      input.classList.add('is-invalid');
      const feedback = input.parentElement.querySelector('.invalid-feedback');
      if (feedback) feedback.textContent = fieldError.message;
    });
  }

  function clearFieldErrors(form) {
    form.querySelectorAll('.is-invalid').forEach(input => input.classList.remove('is-invalid'));
  }

  /** Deshabilita el botón y muestra un spinner mientras se ejecuta la acción. */
  async function withLoading(button, action) {
    const original = button.innerHTML;
    button.disabled = true;
    button.innerHTML = '<span class="spinner-border spinner-border-sm me-2" aria-hidden="true"></span>Procesando...';
    try {
      return await action();
    } finally {
      button.disabled = false;
      button.innerHTML = original;
    }
  }

  return {
    formatMoney,
    formatDateTime,
    formatDate,
    escapeHtml,
    statusBadge,
    ORDER_STATUS,
    requireAuth,
    requireAdmin,
    renderNavbar,
    refreshCartBadge,
    showAlert,
    clearAlert,
    toast,
    applyFieldErrors,
    clearFieldErrors,
    withLoading
  };
})();