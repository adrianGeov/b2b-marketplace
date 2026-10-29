/**
 * Confirmación del pedido.
 * Se genera UNA llave de idempotencia por intento de compra y se guarda en sessionStorage:
 * si el usuario da doble clic, recarga la página o la red reintenta, se envía la MISMA llave
 * y el backend devuelve el mismo pedido en lugar de crear uno duplicado.
 */
(() => {
  const user = UI.requireAuth();
  if (!user) return;

  UI.renderNavbar('cart');

  const KEY_STORAGE = 'b2b.checkoutKey';
  const content = document.getElementById('checkout-content');

  function newKey() {
    if (window.crypto && typeof window.crypto.randomUUID === 'function') {
      return window.crypto.randomUUID();
    }
    // Alternativa para navegadores sin randomUUID (por ejemplo, sitios sin HTTPS)
    return 'key-' + Date.now().toString(36) + '-' + Math.random().toString(36).slice(2, 12);
  }

  function getIdempotencyKey() {
    try {
      let key = sessionStorage.getItem(KEY_STORAGE);
      if (!key) {
        key = newKey();
        sessionStorage.setItem(KEY_STORAGE, key);
      }
      return key;
    } catch (e) {
      return newKey();
    }
  }

  function forgetIdempotencyKey() {
    try {
      sessionStorage.removeItem(KEY_STORAGE);
    } catch (e) {
      // nada que limpiar
    }
  }

  async function load() {
    try {
      render(await Api.get('/cart'));
    } catch (error) {
      content.innerHTML = '';
      UI.showAlert('checkout-alert', error);
    }
  }

  function render(cart) {
    if (!cart.items.length) {
      content.innerHTML = `
        <div class="card empty-state">
          <i class="bi bi-cart-x"></i>
          <p class="mt-3 mb-1 fw-semibold">No hay productos para confirmar</p>
          <div class="mt-2"><a href="/index.html" class="btn btn-primary">Ir al catálogo</a></div>
        </div>`;
      return;
    }

    const rows = cart.items.map(item => `
      <tr>
        <td>
          <div class="fw-semibold">${UI.escapeHtml(item.name)}</div>
          <code class="small">${UI.escapeHtml(item.sku)}</code>
        </td>
        <td class="text-end">${UI.formatMoney(item.unitPrice)}</td>
        <td class="text-center">${item.quantity}</td>
        <td class="text-end fw-semibold">${UI.formatMoney(item.subtotal)}</td>
      </tr>`).join('');

    content.innerHTML = `
      <div class="card mb-3">
        <div class="card-body">
          <h2 class="h6 text-muted mb-3">Empresa</h2>
          <div class="fw-semibold">${UI.escapeHtml(user.companyName)}</div>
          <div class="small text-muted">${UI.escapeHtml(user.fullName)} · ${UI.escapeHtml(user.email)}</div>
        </div>
      </div>

      <div class="card mb-3">
        <div class="table-responsive">
          <table class="table align-middle mb-0">
            <thead class="table-light">
              <tr><th>Producto</th><th class="text-end">Precio</th><th class="text-center">Cant.</th><th class="text-end">Subtotal</th></tr>
            </thead>
            <tbody>${rows}</tbody>
            <tfoot>
              <tr>
                <th colspan="3" class="text-end">Total</th>
                <th class="text-end product-price">${UI.formatMoney(cart.total)}</th>
              </tr>
            </tfoot>
          </table>
        </div>
      </div>

      <div class="card">
        <div class="card-body d-flex flex-column flex-md-row justify-content-between align-items-md-center gap-3">
          <div class="small text-muted">
            <i class="bi bi-shield-check me-1 text-success"></i>Al confirmar se apartan las existencias y se genera el folio.
            El pago se realiza en el siguiente paso.
          </div>
          <button id="confirm-btn" class="btn btn-primary btn-lg px-4 text-nowrap flex-shrink-0">
            <i class="bi bi-check2-circle me-2"></i>Confirmar pedido
          </button>
        </div>
      </div>`;

    document.getElementById('confirm-btn').addEventListener('click', confirmOrder);
  }

  async function confirmOrder(event) {
    const button = event.currentTarget;
    UI.clearAlert('checkout-alert');
    try {
      const result = await UI.withLoading(button, () => Api.post('/orders', undefined, {
        headers: { 'Idempotency-Key': getIdempotencyKey() },
        raw: true
      }));
      forgetIdempotencyKey();
      const replayed = result.headers.get('Idempotent-Replayed') === 'true';
      const folio = encodeURIComponent(result.data.orderNumber);
      location.href = `/order.html?folio=${folio}&${replayed ? 'replayed' : 'created'}=1`;
    } catch (error) {
      // Errores de negocio (carrito vacío, sin existencias): la llave ya no sirve para un nuevo intento
      if (error.status === 422) {
        forgetIdempotencyKey();
      }
      UI.showAlert('checkout-alert', error);
    }
  }

  load();
})();