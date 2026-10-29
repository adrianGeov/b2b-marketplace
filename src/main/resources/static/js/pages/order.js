/**
 * Detalle del pedido: línea de tiempo, productos con precios congelados, pago, cancelación
 * e historial de intentos de pago. Si el pedido está PAID (esperando al ERP), la página
 * consulta el estatus cada pocos segundos para mostrar la confirmación en cuanto llegue.
 */
(() => {
  const user = UI.requireAuth();
  if (!user) return;

  UI.renderNavbar('orders');

  const params = new URLSearchParams(location.search);
  const folio = params.get('folio');
  const content = document.getElementById('order-content');

  const PAYABLE = ['PENDING_PAYMENT', 'PAYMENT_FAILED'];
  const POLL_INTERVAL_MS = 3000;
  const POLL_MAX_ATTEMPTS = 40; // ~2 minutos
  let pollTimer = null;
  let pollAttempts = 0;

  const TEST_CARDS = [
    { token: 'tok_visa', label: 'Visa terminación 4242', hint: 'Aprobada (con fallas intermitentes simuladas)', icon: 'bi-credit-card' },
    { token: 'tok_declined', label: 'Mastercard terminación 0002', hint: 'Rechazada: fondos insuficientes', icon: 'bi-credit-card-2-back' },
    { token: 'tok_gateway_down', label: 'Pasarela caída', hint: 'Simula que la pasarela no responde', icon: 'bi-cloud-slash' }
  ];

  if (!folio) {
    content.innerHTML = '';
    UI.showAlert('order-alert', 'No se indicó el folio del pedido.', 'warning');
    return;
  }

  if (params.get('created')) {
    UI.toast('Pedido creado. Ahora puede realizar el pago.');
  } else if (params.get('replayed')) {
    UI.toast('Este pedido ya se había creado; no se generó un duplicado.', 'info');
  }
  // Limpia la URL para que al recargar no se repita el aviso
  history.replaceState(null, '', `${location.pathname}?folio=${encodeURIComponent(folio)}`);

  // ------------------------------------------------------------------
  // Carga
  // ------------------------------------------------------------------

  async function load() {
    try {
      const [order, payments] = await Promise.all([
        Api.get(`/orders/${encodeURIComponent(folio)}`),
        Api.get(`/orders/${encodeURIComponent(folio)}/payments`)
      ]);
      render(order, payments);
      schedulePolling(order);
    } catch (error) {
      content.innerHTML = '';
      UI.showAlert('order-alert', error);
    }
  }

  /** Mientras el ERP no confirma (PAID), se vuelve a consultar el pedido automáticamente. */
  function schedulePolling(order) {
    clearTimeout(pollTimer);
    if (order.status === 'PAID' && pollAttempts < POLL_MAX_ATTEMPTS) {
      pollAttempts++;
      pollTimer = setTimeout(load, POLL_INTERVAL_MS);
    } else if (order.status === 'CONFIRMED' && pollAttempts > 0) {
      UI.toast('El ERP confirmó su pedido.');
      pollAttempts = 0;
    }
  }

  // ------------------------------------------------------------------
  // Pintado
  // ------------------------------------------------------------------

  function timeline(order) {
    if (order.status === 'CANCELLED') {
      return `<div class="alert alert-secondary mb-0"><i class="bi bi-x-octagon me-2"></i>Pedido cancelado. Las existencias se devolvieron al inventario.</div>`;
    }
    const steps = [
      { key: 'PENDING_PAYMENT', label: 'Creado', icon: 'bi-receipt' },
      { key: 'PAID', label: 'Pagado', icon: 'bi-credit-card' },
      { key: 'CONFIRMED', label: 'Confirmado ERP', icon: 'bi-building-check' },
      { key: 'SHIPPED', label: 'Enviado', icon: 'bi-truck' },
      { key: 'DELIVERED', label: 'Entregado', icon: 'bi-house-check' }
    ];
    const flow = ['PENDING_PAYMENT', 'PAID', 'CONFIRMED', 'SHIPPED', 'DELIVERED'];
    const currentIndex = order.status === 'PAYMENT_FAILED' ? 0 : flow.indexOf(order.status);

    return `<ol class="order-timeline">${steps.map((step, index) => {
      const done = index <= currentIndex;
      const current = index === currentIndex + 1 && order.status !== 'DELIVERED';
      return `<li class="${done ? 'done' : ''} ${current ? 'current' : ''}">
                <div class="dot"><i class="bi ${done ? 'bi-check-lg' : step.icon}"></i></div>${step.label}
              </li>`;
    }).join('')}</ol>`;
  }

  function paymentPanel(order) {
    if (!PAYABLE.includes(order.status)) return '';
    const options = TEST_CARDS.map((card, index) => `
      <label class="payment-option d-flex align-items-center gap-3 border rounded p-3 mb-2">
        <input class="form-check-input mt-0" type="radio" name="cardToken" value="${card.token}" ${index === 0 ? 'checked' : ''}>
        <i class="bi ${card.icon} fs-4 text-primary"></i>
        <span><span class="fw-semibold d-block">${card.label}</span><span class="small text-muted">${card.hint}</span></span>
      </label>`).join('');

    return `
      <div class="card mb-3 border-warning">
        <div class="card-body">
          <h2 class="h5 mb-1"><i class="bi bi-credit-card me-2"></i>Pagar pedido</h2>
          <p class="small text-muted mb-3">
            Tarjetas de prueba de la pasarela simulada. En producción, el SDK de la pasarela tokeniza la tarjeta
            en el navegador y la API solo recibe el token (nunca el número de tarjeta).
          </p>
          <form id="pay-form">
            ${options}
            <div class="d-flex flex-wrap gap-2 mt-3">
              <button type="submit" id="pay-btn" class="btn btn-primary btn-lg">
                <i class="bi bi-lock-fill me-2"></i>Pagar ${UI.formatMoney(order.total)}
              </button>
              <button type="button" id="cancel-btn" class="btn btn-outline-danger">
                <i class="bi bi-x-circle me-1"></i>Cancelar pedido
              </button>
            </div>
          </form>
        </div>
      </div>`;
  }

  function paymentsHistory(payments) {
    if (!payments.length) return '';
    const badge = status => ({
      APPROVED: '<span class="badge text-bg-success">Aprobado</span>',
      DECLINED: '<span class="badge text-bg-danger">Rechazado</span>',
      FAILED: '<span class="badge text-bg-warning">Falla de pasarela</span>'
    })[status] || UI.escapeHtml(status);

    return `
      <div class="card">
        <div class="card-body">
          <h2 class="h6 mb-3"><i class="bi bi-clock-history me-2"></i>Intentos de pago</h2>
          <ul class="list-group list-group-flush">
            ${payments.map(p => `
              <li class="list-group-item px-0 d-flex justify-content-between flex-wrap gap-2">
                <span>${badge(p.status)} <span class="small text-muted ms-2">${UI.formatDateTime(p.createdAt)}</span></span>
                <span class="small">
                  ${p.providerReference ? `Ref. <code>${UI.escapeHtml(p.providerReference)}</code>` : UI.escapeHtml(p.failureReason || '')}
                  · ${p.attempts} intento(s) con la pasarela
                </span>
              </li>`).join('')}
          </ul>
        </div>
      </div>`;
  }

  function render(order, payments) {
    const waitingErp = order.status === 'PAID';
    const items = order.items.map(item => `
      <tr>
        <td><div class="fw-semibold">${UI.escapeHtml(item.productName)}</div><code class="small">${UI.escapeHtml(item.sku)}</code></td>
        <td class="text-end">${UI.formatMoney(item.unitPrice)}</td>
        <td class="text-center">${item.quantity}</td>
        <td class="text-end fw-semibold">${UI.formatMoney(item.subtotal)}</td>
      </tr>`).join('');

    content.innerHTML = `
      <div class="card mb-3">
        <div class="card-body">
          <div class="d-flex flex-wrap justify-content-between align-items-start gap-2 mb-3">
            <div>
              <h1 class="page-title h4 mb-1">Pedido ${UI.escapeHtml(order.orderNumber)}</h1>
              <div class="small text-muted">Creado el ${UI.formatDateTime(order.createdAt)}</div>
            </div>
            <div class="fs-5">${UI.statusBadge(order.status)}</div>
          </div>
          ${timeline(order)}
          ${waitingErp ? `
            <div class="alert alert-info small mt-3 mb-0">
              <span class="spinner-border spinner-border-sm me-2" aria-hidden="true"></span>
              Pago recibido. Esperando confirmación del ERP (se actualiza automáticamente).
            </div>` : ''}
          <div class="row text-center mt-3 g-2 small">
            <div class="col-md-4"><div class="text-muted">Entrega estimada</div><div class="fw-semibold">${UI.formatDate(order.estimatedDeliveryDate)}</div></div>
            <div class="col-md-4"><div class="text-muted">Folio en ERP</div><div class="fw-semibold">${UI.escapeHtml(order.erpOrderId || '—')}</div></div>
            <div class="col-md-4"><div class="text-muted">Entregado</div><div class="fw-semibold">${UI.formatDateTime(order.deliveredAt)}</div></div>
          </div>
        </div>
      </div>

      ${paymentPanel(order)}

      <div class="card mb-3">
        <div class="table-responsive">
          <table class="table align-middle mb-0">
            <thead class="table-light">
              <tr><th>Producto</th><th class="text-end">Precio</th><th class="text-center">Cant.</th><th class="text-end">Subtotal</th></tr>
            </thead>
            <tbody>${items}</tbody>
            <tfoot>
              <tr><th colspan="3" class="text-end">Total</th><th class="text-end product-price">${UI.formatMoney(order.total)}</th></tr>
            </tfoot>
          </table>
        </div>
      </div>

      ${paymentsHistory(payments)}`;

    bindActions();
  }

  // ------------------------------------------------------------------
  // Acciones
  // ------------------------------------------------------------------

  function bindActions() {
    const payForm = document.getElementById('pay-form');
    if (!payForm) return;

    payForm.addEventListener('submit', async event => {
      event.preventDefault();
      UI.clearAlert('order-alert');
      const button = document.getElementById('pay-btn');
      try {
        await UI.withLoading(button, () =>
          Api.post(`/orders/${encodeURIComponent(folio)}/payments`, { cardToken: payForm.cardToken.value }));
        UI.toast('Pago aprobado. Sincronizando con el ERP...');
      } catch (error) {
        UI.showAlert('order-alert', error, error.status === 503 ? 'warning' : 'danger');
      }
      pollAttempts = 0;
      load();
    });

    document.getElementById('cancel-btn').addEventListener('click', async () => {
      if (!window.confirm('¿Cancelar este pedido? Los productos regresarán al inventario.')) return;
      try {
        await Api.post(`/orders/${encodeURIComponent(folio)}/cancel`);
        UI.toast('Pedido cancelado', 'secondary');
        load();
      } catch (error) {
        UI.showAlert('order-alert', error);
      }
    });
  }

  load();
})();