/**
 * Tablero de métricas: responde las preguntas de negocio del caso de uso
 * (ventas, ticket promedio, conversión, entregas a tiempo, productos más vendidos y combinaciones).
 */
(() => {
  const user = UI.requireAdmin();
  if (!user) return;

  UI.renderNavbar('admin');

  const form = document.getElementById('range-form');
  const content = document.getElementById('metrics-content');
  const isoDate = date => date.toISOString().slice(0, 10);

  function setRange(days) {
    const to = new Date();
    const from = new Date();
    from.setDate(to.getDate() - days);
    form.from.value = isoDate(from);
    form.to.value = isoDate(to);
  }

  async function load() {
    UI.clearAlert('metrics-alert');
    try {
      const metrics = await Api.get(`/admin/metrics?from=${form.from.value}&to=${form.to.value}`);
      render(metrics);
    } catch (error) {
      content.innerHTML = '';
      UI.showAlert('metrics-alert', error);
    }
  }

  // ------------------------------------------------------------------
  // Componentes
  // ------------------------------------------------------------------

  const percent = value => `${Number(value).toFixed(1)}%`;

  function tile(label, value, hint, icon) {
    return `
      <div class="col-6 col-lg-3">
        <div class="card stat-tile h-100">
          <div class="card-body">
            <div class="stat-label"><i class="bi ${icon} me-1"></i>${label}</div>
            <div class="stat-value mt-1">${value}</div>
            <div class="stat-hint mt-1">${hint}</div>
          </div>
        </div>
      </div>`;
  }

  /** Barras horizontales de una sola serie; la etiqueta de valor va al final de cada barra. */
  function topProductsBars(products) {
    if (!products.length) return '<p class="text-muted small mb-0">Sin ventas en el periodo.</p>';
    const max = Math.max(...products.map(p => p.unitsSold));
    return products.map(p => `
      <div class="bar-row" title="${UI.escapeHtml(p.name)}: ${p.unitsSold} unidades · ${UI.formatMoney(p.revenue)}">
        <div class="small text-truncate"><code>${UI.escapeHtml(p.sku)}</code> ${UI.escapeHtml(p.name)}</div>
        <div class="bar-track"><div class="bar-fill" style="width:${(p.unitsSold / max) * 100}%"></div></div>
        <div class="bar-value"><strong>${p.unitsSold}</strong> u · ${UI.formatMoney(p.revenue)}</div>
      </div>`).join('');
  }

  /** Proporción a tiempo vs. tardías: colores de estatus + ícono + etiqueta (nunca solo color). */
  function deliveriesBlock(d) {
    if (!d.delivered) return '<p class="text-muted small mb-0">Aún no hay entregas en el periodo.</p>';
    const onTimePct = (d.onTime / d.delivered) * 100;
    const latePct = 100 - onTimePct;
    return `
      <div class="split-bar mb-3" role="img"
           aria-label="${d.onTime} entregas a tiempo y ${d.late} tardías de ${d.delivered}">
        ${d.onTime ? `<div class="bg-success" style="width:${onTimePct}%" title="A tiempo: ${d.onTime}"></div>` : ''}
        ${d.late ? `<div class="bg-danger" style="width:${latePct}%" title="Tardías: ${d.late}"></div>` : ''}
      </div>
      <div class="d-flex justify-content-between small">
        <span><i class="bi bi-check-circle-fill text-success me-1"></i>A tiempo: <strong>${d.onTime}</strong> (${percent(onTimePct)})</span>
        <span><i class="bi bi-exclamation-triangle-fill text-danger me-1"></i>Tardías: <strong>${d.late}</strong> (${percent(latePct)})</span>
      </div>
      <div class="small text-muted mt-2">${d.delivered} pedidos entregados en el periodo</div>`;
  }

  function conversionRow(label, part, total, rate) {
    return `
      <div class="mb-3">
        <div class="d-flex justify-content-between small mb-1">
          <span>${label}</span><span><strong>${part}</strong> de ${total} · ${percent(rate)}</span>
        </div>
        <div class="bar-track" style="border-radius:6px">
          <div class="bar-fill" style="width:${Math.min(rate, 100)}%; border-radius:6px"></div>
        </div>
      </div>`;
  }

  function combinationsTable(pairs) {
    if (!pairs.length) return '<p class="text-muted small mb-0">Sin pedidos con más de un producto.</p>';
    return `
      <div class="table-responsive">
        <table class="table table-sm align-middle mb-0">
          <thead><tr><th>Producto A</th><th>Producto B</th><th class="text-end">Pedidos juntos</th></tr></thead>
          <tbody>${pairs.map(p => `
            <tr>
              <td><code class="small">${UI.escapeHtml(p.skuA)}</code> ${UI.escapeHtml(p.nameA)}</td>
              <td><code class="small">${UI.escapeHtml(p.skuB)}</code> ${UI.escapeHtml(p.nameB)}</td>
              <td class="text-end fw-semibold">${p.ordersTogether}</td>
            </tr>`).join('')}
          </tbody>
        </table>
      </div>`;
  }

  // ------------------------------------------------------------------
  // Pintado
  // ------------------------------------------------------------------

  function render(m) {
    document.getElementById('period-label').textContent =
      `Periodo: ${UI.formatDate(m.period.from)} – ${UI.formatDate(m.period.to)}`;

    content.innerHTML = `
      <div class="row g-3 mb-3">
        ${tile('Ventas', UI.formatMoney(m.sales.revenue), `${m.sales.paidOrders} pedidos pagados`, 'bi-cash-stack')}
        ${tile('Ticket promedio', UI.formatMoney(m.sales.averageTicket), 'Venta promedio por pedido', 'bi-receipt')}
        ${tile('Conversión de leads', percent(m.conversion.leadConversionRate),
               `${m.conversion.buyers} de ${m.conversion.leads} clientes compraron`, 'bi-people')}
        ${tile('Entregas a tiempo', percent(m.deliveries.onTimeRate),
               `${m.deliveries.late} entregas tardías`, 'bi-truck')}
      </div>

      <div class="row g-3 mb-3">
        <div class="col-lg-7">
          <div class="card h-100">
            <div class="card-body">
              <h2 class="h6 mb-1">Productos más vendidos</h2>
              <p class="small text-muted">Unidades vendidas en pedidos pagados</p>
              ${topProductsBars(m.topProducts)}
            </div>
          </div>
        </div>
        <div class="col-lg-5">
          <div class="card mb-3">
            <div class="card-body">
              <h2 class="h6 mb-3">Entregas a tiempo vs. tardías</h2>
              ${deliveriesBlock(m.deliveries)}
            </div>
          </div>
          <div class="card">
            <div class="card-body">
              <h2 class="h6 mb-3">Conversión</h2>
              ${conversionRow('Leads que compran', m.conversion.buyers, m.conversion.leads, m.conversion.leadConversionRate)}
              ${conversionRow('Pedidos creados que se pagan', m.conversion.ordersPaid, m.conversion.ordersCreated,
                              m.conversion.checkoutConversionRate)}
            </div>
          </div>
        </div>
      </div>

      <div class="row g-3">
        <div class="col-lg-7">
          <div class="card h-100">
            <div class="card-body">
              <h2 class="h6 mb-1">Productos que se compran juntos</h2>
              <p class="small text-muted">Oportunidades de paquetes y venta cruzada</p>
              ${combinationsTable(m.topCombinations)}
            </div>
          </div>
        </div>
        <div class="col-lg-5">
          <div class="card h-100">
            <div class="card-body">
              <h2 class="h6 mb-3"><i class="bi bi-info-circle me-1"></i>Notas</h2>
              <ul class="small text-muted mb-0 ps-3">${m.notes.map(n => `<li class="mb-2">${UI.escapeHtml(n)}</li>`).join('')}</ul>
            </div>
          </div>
        </div>
      </div>`;
  }

  // ------------------------------------------------------------------

  form.addEventListener('submit', event => {
    event.preventDefault();
    load();
  });

  document.querySelectorAll('[data-days]').forEach(button => {
    button.addEventListener('click', () => {
      setRange(parseInt(button.dataset.days, 10));
      load();
    });
  });

  setRange(90);
  load();
})();