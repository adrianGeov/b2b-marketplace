/**
 * Carrito: cambiar cantidades, quitar productos, vaciar y continuar al checkout.
 * Siempre se pinta con la respuesta del backend (fuente de verdad de precios y totales).
 */
(() => {
  const user = UI.requireAuth();
  if (!user) return;

  UI.renderNavbar('cart');

  const content = document.getElementById('cart-content');

  async function loadCart() {
    try {
      render(await Api.get('/cart'));
    } catch (error) {
      content.innerHTML = '';
      UI.showAlert('cart-alert', error);
    }
  }

  function render(cart) {
    UI.refreshCartBadge(cart.totalItems);

    if (!cart.items.length) {
      content.innerHTML = `
        <div class="card empty-state">
          <i class="bi bi-cart-x"></i>
          <p class="mt-3 mb-1 fw-semibold">Su carrito está vacío</p>
          <p class="small">Agregue productos desde el catálogo para crear un pedido.</p>
          <div><a href="/index.html" class="btn btn-primary"><i class="bi bi-grid me-1"></i>Ir al catálogo</a></div>
        </div>`;
      return;
    }

    const hasUnavailable = cart.items.some(item => !item.available);

    const rows = cart.items.map(item => `
      <div class="cart-item p-3 ${item.available ? '' : 'unavailable'}" data-product-id="${item.productId}">
        <div class="row align-items-center g-2">
          <div class="col-md-5">
            <div class="fw-semibold">${UI.escapeHtml(item.name)}</div>
            <div class="small text-muted">
              <code>${UI.escapeHtml(item.sku)}</code> · ${UI.formatMoney(item.unitPrice)} c/u
            </div>
            ${item.available ? '' : `
              <div class="small text-danger mt-1">
                <i class="bi bi-exclamation-octagon me-1"></i>No disponible en esa cantidad
                (existencias: ${item.availableStock}). Ajuste o retire el producto.
              </div>`}
          </div>
          <div class="col-6 col-md-3">
            <div class="input-group input-group-sm">
              <span class="input-group-text">Cant.</span>
              <input type="number" class="form-control qty-change" min="1" max="999"
                     value="${item.quantity}" data-previous="${item.quantity}" aria-label="Cantidad">
            </div>
          </div>
          <div class="col-4 col-md-3 text-end fw-semibold">${UI.formatMoney(item.subtotal)}</div>
          <div class="col-2 col-md-1 text-end">
            <button class="btn btn-sm btn-outline-danger remove-item" title="Quitar" aria-label="Quitar producto">
              <i class="bi bi-trash"></i>
            </button>
          </div>
        </div>
      </div>`).join('');

    content.innerHTML = `
      <div class="row g-4">
        <div class="col-lg-8">
          <div class="card">${rows}</div>
          <button id="clear-cart" class="btn btn-link text-danger mt-2 px-0">
            <i class="bi bi-trash3 me-1"></i>Vaciar carrito
          </button>
        </div>
        <div class="col-lg-4">
          <div class="card summary-card">
            <div class="card-body">
              <h2 class="h5 mb-3">Resumen</h2>
              <div class="d-flex justify-content-between mb-2">
                <span class="text-muted">Artículos</span><span>${cart.totalItems}</span>
              </div>
              <div class="d-flex justify-content-between mb-3">
                <span class="text-muted">Productos distintos</span><span>${cart.items.length}</span>
              </div>
              <hr>
              <div class="d-flex justify-content-between align-items-center mb-3">
                <span class="fw-semibold">Total</span>
                <span class="product-price">${UI.formatMoney(cart.total)}</span>
              </div>
              <a href="/checkout.html" class="btn btn-primary w-100 py-2 ${hasUnavailable ? 'disabled' : ''}"
                 ${hasUnavailable ? 'aria-disabled="true"' : ''}>
                <i class="bi bi-bag-check me-1"></i>Continuar con el pedido
              </a>
              ${hasUnavailable ? '<p class="small text-danger mt-2 mb-0">Corrija los productos marcados para continuar.</p>' : ''}
              <p class="small text-muted mt-3 mb-0">
                <i class="bi bi-info-circle me-1"></i>Los precios se confirman al crear el pedido.
              </p>
            </div>
          </div>
        </div>
      </div>`;
  }

  // ------------------------------------------------------------------
  // Eventos (delegados en el contenedor)
  // ------------------------------------------------------------------

  content.addEventListener('change', async event => {
    const input = event.target.closest('.qty-change');
    if (!input) return;
    const productId = input.closest('[data-product-id]').dataset.productId;
    const quantity = parseInt(input.value, 10);

    if (!quantity || quantity < 1 || quantity > 999) {
      input.value = input.dataset.previous;
      UI.toast('La cantidad debe estar entre 1 y 999', 'warning');
      return;
    }

    input.disabled = true;
    try {
      render(await Api.put(`/cart/items/${productId}`, { quantity }));
      UI.clearAlert('cart-alert');
    } catch (error) {
      input.value = input.dataset.previous;
      input.disabled = false;
      UI.showAlert('cart-alert', error);
    }
  });

  content.addEventListener('click', async event => {
    const removeBtn = event.target.closest('.remove-item');
    if (removeBtn) {
      const productId = removeBtn.closest('[data-product-id]').dataset.productId;
      try {
        render(await Api.del(`/cart/items/${productId}`));
        UI.toast('Producto retirado del carrito', 'secondary');
      } catch (error) {
        UI.showAlert('cart-alert', error);
      }
      return;
    }

    if (event.target.closest('#clear-cart')) {
      if (!window.confirm('¿Vaciar el carrito?')) return;
      try {
        await Api.del('/cart');
        render({ items: [], totalItems: 0, total: 0 });
      } catch (error) {
        UI.showAlert('cart-alert', error);
      }
    }
  });

  loadCart();
})();