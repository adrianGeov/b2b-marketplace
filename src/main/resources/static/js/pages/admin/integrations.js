/**
 * Monitor de integraciones (ADMIN): estado del outbox, disponibilidad del ERP/CRM simulados
 * (interruptores para la demo de caídas) y reintento manual de eventos.
 */
(() => {
  const user = UI.requireAdmin();
  if (!user) return;

  UI.renderNavbar('admin');

  const REFRESH_MS = 5000;
  const tiles = document.getElementById('status-tiles');
  const eventsContent = document.getElementById('events-content');
  const autoRefresh = document.getElementById('auto-refresh');
  let currentTab = 'PENDING';
  let timer = null;

  async function refresh() {
    clearTimeout(timer);
    try {
      const [status, events] = await Promise.all([
        Api.get('/admin/integrations/outbox'),
        Api.get(`/admin/integrations/outbox/events?status=${currentTab}`)
      ]);
      renderStatus(status);
      renderEvents(events);
    } catch (error) {
      UI.showAlert('integrations-alert', error);
    }
    if (autoRefresh.checked) {
      timer = setTimeout(refresh, REFRESH_MS);
    }
  }

  function systemCard(system, label, available) {
    return `
      <div class="col-md-6 col-lg-3">
        <div class="card h-100 ${available ? '' : 'border border-danger'}">
          <div class="card-body">
            <div class="stat-label">${label}</div>
            <div class="d-flex justify-content-between align-items-center mt-2">
              <span class="fw-semibold ${available ? 'text-success' : 'text-danger'}">
                <i class="bi ${available ? 'bi-check-circle-fill' : 'bi-x-octagon-fill'} me-1"></i>
                ${available ? 'Disponible' : 'Caído (simulado)'}
              </span>
              <div class="form-check form-switch mb-0">
                <input class="form-check-input system-switch" type="checkbox" role="switch"
                       data-system="${system}" ${available ? 'checked' : ''} aria-label="Disponibilidad ${label}">
              </div>
            </div>
          </div>
        </div>
      </div>`;
  }

  function countCard(label, value, icon, tone) {
    return `
      <div class="col-md-4 col-lg-2">
        <div class="card stat-tile h-100">
          <div class="card-body">
            <div class="stat-label"><i class="bi ${icon} me-1 ${tone}"></i>${label}</div>
            <div class="stat-value mt-1">${value}</div>
          </div>
        </div>
      </div>`;
  }

  function renderStatus(status) {
    tiles.innerHTML =
      systemCard('erp', 'ERP', status.erpAvailable) +
      systemCard('crm', 'CRM', status.crmAvailable) +
      countCard('Pendientes', status.pending, 'bi-hourglass-split', 'text-warning') +
      countCard('Procesados', status.processed, 'bi-check2-all', 'text-success') +
      countCard('Fallidos', status.failed, 'bi-exclamation-octagon', 'text-danger');
  }

  function renderEvents(events) {
    if (!events.length) {
      eventsContent.innerHTML = '<div class="empty-state py-4"><i class="bi bi-inbox"></i><p class="mt-2 mb-0">Sin eventos.</p></div>';
      return;
    }
    eventsContent.innerHTML = `
      <div class="table-responsive">
        <table class="table table-sm align-middle mb-0">
          <thead class="table-light">
            <tr><th>#</th><th>Destino</th><th>Evento</th><th>Referencia</th><th class="text-center">Intentos</th>
                <th>Último error</th><th>${currentTab === 'PROCESSED' ? 'Procesado' : 'Siguiente intento'}</th><th></th></tr>
          </thead>
          <tbody>${events.map(e => `
            <tr>
              <td>${e.id}</td>
              <td><span class="badge text-bg-${e.target === 'ERP' ? 'primary' : 'info'}">${e.target}</span></td>
              <td><code class="small">${UI.escapeHtml(e.eventType)}</code></td>
              <td class="small">${UI.escapeHtml(e.aggregateId)}</td>
              <td class="text-center">${e.attempts}</td>
              <td class="small text-danger">${UI.escapeHtml(e.lastError || '')}</td>
              <td class="small">${UI.formatDateTime(currentTab === 'PROCESSED' ? e.processedAt : e.nextAttemptAt)}</td>
              <td class="text-end">${currentTab !== 'PROCESSED'
                ? `<button class="btn btn-sm btn-outline-primary" data-retry="${e.id}"><i class="bi bi-arrow-clockwise me-1"></i>Reintentar ya</button>`
                : ''}</td>
            </tr>`).join('')}
          </tbody>
        </table>
      </div>`;
  }

  // ------------------------------------------------------------------
  // Eventos
  // ------------------------------------------------------------------

  tiles.addEventListener('change', async event => {
    const toggle = event.target.closest('.system-switch');
    if (!toggle) return;
    const { system } = toggle.dataset;
    try {
      await Api.put(`/admin/integrations/${system}/availability?available=${toggle.checked}`);
      UI.toast(`${system.toUpperCase()} ${toggle.checked ? 'disponible' : 'caído (simulación)'}`,
               toggle.checked ? 'success' : 'warning');
    } catch (error) {
      toggle.checked = !toggle.checked;
      UI.showAlert('integrations-alert', error);
    }
    refresh();
  });

  eventsContent.addEventListener('click', async event => {
    const button = event.target.closest('[data-retry]');
    if (!button) return;
    try {
      await UI.withLoading(button, () => Api.post(`/admin/integrations/outbox/events/${button.dataset.retry}/retry`));
      UI.toast('Evento reencolado; se procesará en el siguiente ciclo');
      refresh();
    } catch (error) {
      UI.showAlert('integrations-alert', error);
    }
  });

  document.getElementById('event-tabs').addEventListener('click', event => {
    const tab = event.target.closest('[data-status]');
    if (!tab) return;
    document.querySelectorAll('#event-tabs .nav-link').forEach(link => link.classList.remove('active'));
    tab.classList.add('active');
    currentTab = tab.dataset.status;
    refresh();
  });

  autoRefresh.addEventListener('change', refresh);

  refresh();
})();