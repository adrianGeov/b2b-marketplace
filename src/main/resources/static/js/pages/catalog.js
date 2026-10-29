/**
 * Catálogo: búsqueda por texto y categoría, orden, paginación y "Agregar al carrito".
 * Los filtros se reflejan en la URL para que al recargar o compartir el enlace se conserven.
 */
(() => {
  const user = UI.requireAuth();
  if (!user) return;

  UI.renderNavbar('catalog');

  const PAGE_SIZE = 12;
  const LOW_STOCK = 20;
  const CATEGORY_ICONS = {
    'Papelería': 'bi-journal-text',
    'Limpieza': 'bi-droplet',
    'Cafetería': 'bi-cup-hot',
    'Tecnología': 'bi-cpu',
    'Mobiliario': 'bi-lamp'
  };

  const searchInput = document.getElementById('search');
  const categorySelect = document.getElementById('category');
  const sortSelect = document.getElementById('sort');
  const grid = document.getElementById('product-grid');
  const pagination = document.getElementById('pagination');
  const resultCount = document.getElementById('result-count');

  // Estado inicial tomado de la URL (?search=&category=&sort=&page=)
  const params = new URLSearchParams(location.search);
  const state = {
    search: params.get('search') || '',
    category: params.get('category') || '',
    sort: params.get('sort') || 'name,asc',
    page: Math.max(0, parseInt(params.get('page') || '0', 10) || 0)
  };

  searchInput.value = state.search;
  sortSelect.value = state.sort;

  // ------------------------------------------------------------------
  // Carga de datos
  // ------------------------------------------------------------------

  async function loadCategories() {
    try {
      const categories = await Api.get('/products/categories');
      categorySelect.insertAdjacentHTML('beforeend', categories
        .map(c => `<option value="${UI.escapeHtml(c)}">${UI.escapeHtml(c)}</option>`).join(''));
      categorySelect.value = state.category;
    } catch (error) {
      // Sin categorías el catálogo sigue funcionando; solo se pierde el filtro
    }
  }

  async function loadProducts() {
    syncUrl();
    UI.clearAlert('catalog-alert');
    grid.innerHTML = skeleton();

    const query = new URLSearchParams({ page: state.page, size: PAGE_SIZE, sort: state.sort });
    if (state.search) query.set('search', state.search);
    if (state.category) query.set('category', state.category);

    try {
      const page = await Api.get('/products?' + query.toString());
      renderProducts(page);
      renderPagination(page);
      resultCount.textContent = page.totalElements === 1
        ? '1 producto encontrado'
        : `${page.totalElements} productos encontrados`;
    } catch (error) {
      grid.innerHTML = '';
      pagination.innerHTML = '';
      UI.showAlert('catalog-alert', error);
    }
  }

  function syncUrl() {
    const query = new URLSearchParams();
    if (state.search) query.set('search', state.search);
    if (state.category) query.set('category', state.category);
    if (state.sort !== 'name,asc') query.set('sort', state.sort);
    if (state.page > 0) query.set('page', state.page);
    const qs = query.toString();
    history.replaceState(null, '', location.pathname + (qs ? '?' + qs : ''));
  }

  // ------------------------------------------------------------------
  // Pintado
  // ------------------------------------------------------------------

  function skeleton() {
    return Array.from({ length: 4 }, () => `
      <div class="col-sm-6 col-lg-4 col-xl-3">
        <div class="card h-100" aria-hidden="true">
          <div class="product-thumb placeholder-glow"><span class="placeholder col-4"></span></div>
          <div class="card-body placeholder-glow">
            <span class="placeholder col-8"></span><span class="placeholder col-6"></span>
            <span class="placeholder col-4"></span>
          </div>
        </div>
      </div>`).join('');
  }

  function renderProducts(page) {
    if (!page.content.length) {
      grid.innerHTML = `
        <div class="col-12">
          <div class="card empty-state">
            <i class="bi bi-search"></i>
            <p class="mt-3 mb-1 fw-semibold">No encontramos productos con esos filtros</p>
            <p class="small mb-0">Pruebe con otra palabra o categoría.</p>
          </div>
        </div>`;
      return;
    }

    grid.innerHTML = page.content.map(product => {
      const outOfStock = product.stock <= 0;
      const maxQty = Math.min(product.stock, 999);
      const icon = CATEGORY_ICONS[product.category] || 'bi-box-seam';
      const stockLabel = outOfStock
        ? '<span class="text-danger"><i class="bi bi-x-circle me-1"></i>Agotado</span>'
        : product.stock < LOW_STOCK
          ? `<span class="text-warning-emphasis"><i class="bi bi-exclamation-triangle me-1"></i>Últimas ${product.stock} piezas</span>`
          : `<span class="text-success"><i class="bi bi-check-circle me-1"></i>${product.stock} disponibles</span>`;

      return `
        <div class="col-sm-6 col-lg-4 col-xl-3">
          <div class="card product-card h-100">
            <div class="product-thumb"><i class="bi ${icon}"></i></div>
            <div class="card-body d-flex flex-column">
              <div class="d-flex justify-content-between align-items-start mb-1">
                <span class="badge text-bg-light">${UI.escapeHtml(product.category)}</span>
                <code class="small">${UI.escapeHtml(product.sku)}</code>
              </div>
              <h2 class="product-name mt-1 mb-1">${UI.escapeHtml(product.name)}</h2>
              <p class="product-description small text-muted mb-2">${UI.escapeHtml(product.description || '')}</p>
              <div class="product-price mb-1">${UI.formatMoney(product.price)}</div>
              <div class="small mb-3">${stockLabel}</div>
              <form class="add-form mt-auto d-flex gap-2" data-product-id="${product.id}"
                    data-product-name="${UI.escapeHtml(product.name)}">
                <input type="number" class="form-control qty-input" name="quantity" value="1" min="1"
                       max="${maxQty}" aria-label="Cantidad" ${outOfStock ? 'disabled' : ''}>
                <button type="submit" class="btn btn-primary flex-grow-1" ${outOfStock ? 'disabled' : ''}>
                  <i class="bi bi-cart-plus me-1"></i>Agregar
                </button>
              </form>
            </div>
          </div>
        </div>`;
    }).join('');
  }

  function renderPagination(page) {
    if (page.totalPages <= 1) {
      pagination.innerHTML = '';
      return;
    }
    const item = (label, target, disabled, active) => `
      <li class="page-item ${disabled ? 'disabled' : ''} ${active ? 'active' : ''}">
        <button class="page-link" data-page="${target}" ${disabled ? 'tabindex="-1"' : ''}>${label}</button>
      </li>`;

    let html = item('&laquo;', page.page - 1, page.page === 0, false);
    for (let i = 0; i < page.totalPages; i++) {
      html += item(i + 1, i, false, i === page.page);
    }
    html += item('&raquo;', page.page + 1, page.last, false);
    pagination.innerHTML = html;
  }

  // ------------------------------------------------------------------
  // Eventos
  // ------------------------------------------------------------------

  // Búsqueda mientras se escribe, con "debounce" para no llamar a la API en cada tecla
  let searchTimer;
  searchInput.addEventListener('input', () => {
    clearTimeout(searchTimer);
    searchTimer = setTimeout(() => {
      state.search = searchInput.value.trim();
      state.page = 0;
      loadProducts();
    }, 350);
  });

  document.getElementById('filters').addEventListener('submit', event => event.preventDefault());

  categorySelect.addEventListener('change', () => {
    state.category = categorySelect.value;
    state.page = 0;
    loadProducts();
  });

  sortSelect.addEventListener('change', () => {
    state.sort = sortSelect.value;
    state.page = 0;
    loadProducts();
  });

  document.getElementById('clear-filters').addEventListener('click', () => {
    searchInput.value = '';
    categorySelect.value = '';
    sortSelect.value = 'name,asc';
    Object.assign(state, { search: '', category: '', sort: 'name,asc', page: 0 });
    loadProducts();
  });

  pagination.addEventListener('click', event => {
    const button = event.target.closest('[data-page]');
    if (!button || button.parentElement.classList.contains('disabled')) return;
    state.page = parseInt(button.dataset.page, 10);
    loadProducts();
    window.scrollTo({ top: 0, behavior: 'smooth' });
  });

  // Agregar al carrito (delegación de eventos: un solo listener para todas las tarjetas)
  grid.addEventListener('submit', async event => {
    const form = event.target.closest('.add-form');
    if (!form) return;
    event.preventDefault();

    const quantity = parseInt(form.quantity.value, 10);
    if (!form.quantity.checkValidity() || !quantity) {
      form.quantity.classList.add('is-invalid');
      return;
    }
    form.quantity.classList.remove('is-invalid');

    const button = form.querySelector('button');
    try {
      const cart = await UI.withLoading(button, () => Api.post('/cart/items', {
        productId: Number(form.dataset.productId),
        quantity
      }));
      UI.refreshCartBadge(cart.totalItems);
      UI.toast(`Agregado: ${quantity} × ${form.dataset.productName}`);
      form.quantity.value = 1;
    } catch (error) {
      UI.toast(error.message, 'danger');
    }
  });

  // ------------------------------------------------------------------
  loadCategories();
  loadProducts();
})();