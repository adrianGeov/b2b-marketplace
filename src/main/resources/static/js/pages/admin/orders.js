/**
 * Operación de pedidos (ADMIN): filtrar por estatus, enviar y entregar.
 */
(() => {
  const user = UI.requireAdmin();
  if (!user) return;

  UI.renderNavbar('admin');

  const PAGE_SIZE = 15;
  const statusFilter = document.getElementById('status-filter');
  const content = document.getElementById('orders-content');
  const pagination = document.getElementById('pagination');

  const params = new URLSearchParams(location.search);
  const state = { status: params.get('status') || 'CONFIRMED', page: 0 };

  statusFilter.insertAdjacentHTML('beforeend', Object.entries(UI.ORDER_STATUS)
    .map(([key, info]) => `<option value="${key}">${info.label}</option>`).join(''));
  statusFilter.value = state.status;

  async function load() {
    history.replaceState(null, '', location.pathname + (state.status ? `?status=${state.status}` : ''));
    const query = new URLSearchParams({ page: state.page, size: PAGE_SIZE, sort: 'createdAt,desc' });
    if (state.status) query.set('status', state.status);
    try {
      render(await Api.get('/admin/orders?' + query.toString()));
    } catch (error) {
      content.innerHTML = '';
      UI.showAlert('orders-alert', error);
    }
  }

  function actionButton(order) {
    if (order.status === 'CONFIRMED') {
      return `<button class="btn btn-sm btn-primary" data-action="ship" data-folio="${UI.escapeHtml(order.orderNumber)}">
                <i class="bi bi-truck me-1"></i>Enviar</button>`;
    }
    if (order.status === 'SHIPPED') {
      return `<button class="btn btn-sm btn-success" data-action="deliver" data-folio="${UI.escapeHtml(order.orderNumber)}">
                <i class="bi bi-house-check me-1"></i>Entregar</button>`;
    }
    return '';
  }

  function render(result) {
    if (!result.content.length) {
      content.innerHTML = `<div class="card empty-state"><i class="bi bi-inbox"></i>
        <p class="mt-3 mb-0">No hay pedidos con ese estatus.</p></div>`;
      pagination.innerHTML = '';
      return;
    }

    content.innerHTML = `
      <div class="card">
        <div class="table-responsive">
          <table class="table table-hover align-middle mb-0">
            <thead class="table-light">
              <tr><th>Folio</th><th>Fecha</th><th>Estatus</th><th>Entrega estimada</th><th class="text-end">Total</th><th class="text-end">Acción</th></tr>
            </thead>
            <tbody>${result.content.map(order => `
              <tr>
                <td><a class="fw-semibold" href="/order.html?folio=${encodeURIComponent(order.orderNumber)}">${UI.escapeHtml(order.orderNumber)}</a></td>
                <td>${UI.formatDateTime(order.createdAt)}</td>
                <td>${UI.statusBadge(order.status)}</td>
                <td>${UI.formatDate(order.estimatedDeliveryDate)}</td>
                <td class="text-end">${UI.formatMoney(order.total)}</td>
                <td class="text-end">${actionButton(order)}</td>
              </tr>`).join('')}
            </tbody>
          </table>
        </div>
      </div>
      <p class="small text-muted mt-2">${result.totalElements} pedido(s)</p>`;

    pagination.innerHTML = result.totalPages <= 1 ? '' : Array.from({ length: result.totalPages }, (_, i) => `
      <li class="page-item ${i === result.page ? 'active' : ''}"><button class="page-link" data-page="${i}">${i + 1}</button></li>`).join('');
  }

  content.addEventListener('click', async event => {
    const button = event.target.closest('[data-action]');
    if (!button) return;
    const { action, folio } = button.dataset;
    try {
      const order = await UI.withLoading(button, () =>
        Api.post(`/admin/orders/${encodeURIComponent(folio)}/${action}`));
      UI.toast(`Pedido ${order.orderNumber}: ${UI.ORDER_STATUS[order.status].label}`);
      load();
    } catch (error) {
      UI.showAlert('orders-alert', error);
    }
  });

  statusFilter.addEventListener('change', () => {
    state.status = statusFilter.value;
    state.page = 0;
    load();
  });

  pagination.addEventListener('click', event => {
    const button = event.target.closest('[data-page]');
    if (!button) return;
    state.page = parseInt(button.dataset.page, 10);
    load();
  });

  load();
})();