/**
 * Listado paginado de los pedidos del usuario (más recientes primero).
 */
(() => {
  const user = UI.requireAuth();
  if (!user) return;

  UI.renderNavbar('orders');

  const PAGE_SIZE = 10;
  const content = document.getElementById('orders-content');
  const pagination = document.getElementById('pagination');
  let currentPage = 0;

  async function load(page) {
    currentPage = page;
    try {
      const result = await Api.get(`/orders?page=${page}&size=${PAGE_SIZE}&sort=createdAt,desc`);
      render(result);
    } catch (error) {
      content.innerHTML = '';
      UI.showAlert('orders-alert', error);
    }
  }

  function render(result) {
    if (!result.content.length) {
      content.innerHTML = `
        <div class="card empty-state">
          <i class="bi bi-receipt"></i>
          <p class="mt-3 mb-1 fw-semibold">Aún no tiene pedidos</p>
          <div class="mt-2"><a href="/index.html" class="btn btn-primary">Ir al catálogo</a></div>
        </div>`;
      pagination.innerHTML = '';
      return;
    }

    const rows = result.content.map(order => `
      <tr>
        <td><a class="fw-semibold" href="/order.html?folio=${encodeURIComponent(order.orderNumber)}">${UI.escapeHtml(order.orderNumber)}</a></td>
        <td>${UI.formatDateTime(order.createdAt)}</td>
        <td>${UI.statusBadge(order.status)}</td>
        <td>${UI.formatDate(order.estimatedDeliveryDate)}</td>
        <td class="text-end fw-semibold">${UI.formatMoney(order.total)}</td>
        <td class="text-end">
          <a class="btn btn-sm btn-outline-primary" href="/order.html?folio=${encodeURIComponent(order.orderNumber)}">Ver</a>
        </td>
      </tr>`).join('');

    content.innerHTML = `
      <div class="card">
        <div class="table-responsive">
          <table class="table table-hover align-middle mb-0">
            <thead class="table-light">
              <tr><th>Folio</th><th>Fecha</th><th>Estatus</th><th>Entrega estimada</th><th class="text-end">Total</th><th></th></tr>
            </thead>
            <tbody>${rows}</tbody>
          </table>
        </div>
      </div>
      <p class="small text-muted mt-2">${result.totalElements} pedido(s)</p>`;

    renderPagination(result);
  }

  function renderPagination(result) {
    if (result.totalPages <= 1) {
      pagination.innerHTML = '';
      return;
    }
    let html = '';
    for (let i = 0; i < result.totalPages; i++) {
      html += `<li class="page-item ${i === result.page ? 'active' : ''}">
                 <button class="page-link" data-page="${i}">${i + 1}</button></li>`;
    }
    pagination.innerHTML = html;
  }

  pagination.addEventListener('click', event => {
    const button = event.target.closest('[data-page]');
    if (button) load(parseInt(button.dataset.page, 10));
  });

  load(currentPage);
})();