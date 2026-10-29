/**
 * Administración del catálogo (ADMIN): listar (incluidos inactivos), crear, editar y desactivar.
 */
(() => {
  const user = UI.requireAdmin();
  if (!user) return;

  UI.renderNavbar('admin');

  const PAGE_SIZE = 15;
  const content = document.getElementById('products-content');
  const pagination = document.getElementById('pagination');
  const searchInput = document.getElementById('search');
  const includeInactive = document.getElementById('include-inactive');
  const form = document.getElementById('product-form');
  const modal = new bootstrap.Modal(document.getElementById('product-modal'));

  const state = { page: 0, editingId: null, products: [] };

  async function load() {
    const query = new URLSearchParams({
      page: state.page, size: PAGE_SIZE, sort: 'sku,asc', includeInactive: includeInactive.checked
    });
    if (searchInput.value.trim()) query.set('search', searchInput.value.trim());
    try {
      render(await Api.get('/products?' + query.toString()));
    } catch (error) {
      content.innerHTML = '';
      UI.showAlert('products-alert', error);
    }
  }

  async function loadCategories() {
    try {
      const categories = await Api.get('/products/categories');
      document.getElementById('category-list').innerHTML =
        categories.map(c => `<option value="${UI.escapeHtml(c)}">`).join('');
    } catch (e) {
      // el campo sigue siendo de texto libre
    }
  }

  function render(result) {
    state.products = result.content;
    if (!result.content.length) {
      content.innerHTML = '<div class="card empty-state"><i class="bi bi-box"></i><p class="mt-3 mb-0">Sin productos.</p></div>';
      pagination.innerHTML = '';
      return;
    }

    content.innerHTML = `
      <div class="card">
        <div class="table-responsive">
          <table class="table table-hover align-middle mb-0">
            <thead class="table-light">
              <tr><th>SKU</th><th>Nombre</th><th>Categoría</th><th class="text-end">Precio</th>
                  <th class="text-end">Existencias</th><th>Estado</th><th class="text-end">Acciones</th></tr>
            </thead>
            <tbody>${result.content.map(p => `
              <tr class="${p.active ? '' : 'table-secondary'}">
                <td><code>${UI.escapeHtml(p.sku)}</code></td>
                <td>${UI.escapeHtml(p.name)}</td>
                <td>${UI.escapeHtml(p.category)}</td>
                <td class="text-end">${UI.formatMoney(p.price)}</td>
                <td class="text-end">${p.stock}</td>
                <td>${p.active
                  ? '<span class="badge text-bg-success"><i class="bi bi-eye me-1"></i>Activo</span>'
                  : '<span class="badge text-bg-secondary"><i class="bi bi-eye-slash me-1"></i>Inactivo</span>'}</td>
                <td class="text-end text-nowrap">
                  <button class="btn btn-sm btn-outline-primary" data-action="edit" data-id="${p.id}" title="Editar">
                    <i class="bi bi-pencil"></i></button>
                  ${p.active ? `<button class="btn btn-sm btn-outline-danger" data-action="deactivate" data-id="${p.id}" title="Desactivar">
                    <i class="bi bi-eye-slash"></i></button>` : ''}
                </td>
              </tr>`).join('')}
            </tbody>
          </table>
        </div>
      </div>
      <p class="small text-muted mt-2">${result.totalElements} producto(s)</p>`;

    pagination.innerHTML = result.totalPages <= 1 ? '' : Array.from({ length: result.totalPages }, (_, i) => `
      <li class="page-item ${i === result.page ? 'active' : ''}"><button class="page-link" data-page="${i}">${i + 1}</button></li>`).join('');
  }

  // ------------------------------------------------------------------
  // Modal de alta / edición
  // ------------------------------------------------------------------

  function openForm(product) {
    state.editingId = product ? product.id : null;
    form.reset();
    form.classList.remove('was-validated');
    UI.clearFieldErrors(form);
    UI.clearAlert('form-alert');
    document.getElementById('product-modal-title').textContent = product ? `Editar ${product.sku}` : 'Nuevo producto';

    if (product) {
      form.sku.value = product.sku;
      form.name.value = product.name;
      form.description.value = product.description || '';
      form.category.value = product.category;
      form.price.value = product.price;
      form.stock.value = product.stock;
      form.active.checked = product.active;
    }
    modal.show();
  }

  form.addEventListener('submit', async event => {
    event.preventDefault();
    UI.clearFieldErrors(form);
    UI.clearAlert('form-alert');
    if (!form.checkValidity()) {
      form.classList.add('was-validated');
      return;
    }

    const payload = {
      sku: form.sku.value.trim(),
      name: form.name.value.trim(),
      description: form.description.value.trim() || null,
      category: form.category.value.trim(),
      price: Number(form.price.value),
      stock: parseInt(form.stock.value, 10),
      active: form.active.checked
    };

    try {
      const saved = await UI.withLoading(document.getElementById('save-btn'), () => state.editingId
        ? Api.put(`/products/${state.editingId}`, payload)
        : Api.post('/products', payload));
      modal.hide();
      UI.toast(`Producto ${saved.sku} guardado`);
      load();
      loadCategories();
    } catch (error) {
      form.classList.remove('was-validated');
      UI.applyFieldErrors(form, error);
      UI.showAlert('form-alert', error);
    }
  });

  // ------------------------------------------------------------------
  // Eventos
  // ------------------------------------------------------------------

  document.getElementById('new-product').addEventListener('click', () => openForm(null));

  content.addEventListener('click', async event => {
    const button = event.target.closest('[data-action]');
    if (!button) return;
    const product = state.products.find(p => p.id === Number(button.dataset.id));

    if (button.dataset.action === 'edit') {
      openForm(product);
      return;
    }

    if (!window.confirm(`¿Desactivar ${product.sku}? Dejará de mostrarse a los clientes; los pedidos históricos no cambian.`)) return;
    try {
      await Api.del(`/products/${product.id}`);
      UI.toast(`Producto ${product.sku} desactivado`, 'secondary');
      load();
    } catch (error) {
      UI.showAlert('products-alert', error);
    }
  });

  let timer;
  searchInput.addEventListener('input', () => {
    clearTimeout(timer);
    timer = setTimeout(() => { state.page = 0; load(); }, 350);
  });

  includeInactive.addEventListener('change', () => { state.page = 0; load(); });

  pagination.addEventListener('click', event => {
    const button = event.target.closest('[data-page]');
    if (!button) return;
    state.page = parseInt(button.dataset.page, 10);
    load();
  });

  loadCategories();
  load();
})();