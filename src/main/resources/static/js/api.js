/**
 * Cliente de la API REST del B2B Marketplace.
 * - Agrega el token JWT a cada petición.
 * - Convierte los errores ProblemDetail del backend en ApiError (con code, detail, errors y correlationId).
 * - Guarda la sesión en localStorage (envuelto en try/catch: puede no estar disponible).
 */
const Api = (() => {
  const BASE_URL = '/api/v1';
  const SESSION_KEY = 'b2b.session';

  class ApiError extends Error {
    constructor(status, problem) {
      super((problem && problem.detail) || 'No fue posible completar la operación');
      this.name = 'ApiError';
      this.status = status;
      this.problem = problem || {};
      this.code = this.problem.code || 'UNKNOWN_ERROR';
      this.errors = this.problem.errors || [];
      this.correlationId = this.problem.correlationId || null;
    }
  }

  // ------------------------------------------------------------------
  // Sesión
  // ------------------------------------------------------------------

  function readSession() {
    try {
      const raw = localStorage.getItem(SESSION_KEY);
      if (!raw) return null;
      const session = JSON.parse(raw);
      if (!session.expiresAt || Date.now() >= session.expiresAt) {
        localStorage.removeItem(SESSION_KEY);
        return null;
      }
      return session;
    } catch (e) {
      return null;
    }
  }

  function saveSession(authResponse) {
    const session = {
      token: authResponse.accessToken,
      user: authResponse.user,
      // Se resta un minuto para no usar un token a punto de expirar
      expiresAt: Date.now() + (authResponse.expiresIn - 60) * 1000
    };
    try {
      localStorage.setItem(SESSION_KEY, JSON.stringify(session));
    } catch (e) {
      // Sin almacenamiento disponible: la sesión solo vive en esta página
    }
    return session;
  }

  function clearSession() {
    try {
      localStorage.removeItem(SESSION_KEY);
    } catch (e) {
      // nada que limpiar
    }
  }

  function currentUser() {
    const session = readSession();
    return session ? session.user : null;
  }

  function isLoggedIn() {
    return readSession() !== null;
  }

  function isAdmin() {
    const user = currentUser();
    return !!user && user.role === 'ADMIN';
  }

  // ------------------------------------------------------------------
  // Peticiones HTTP
  // ------------------------------------------------------------------

  /**
   * @param {string} method  GET, POST, PUT, DELETE
   * @param {string} path    ruta relativa a /api/v1 (ej. '/products?page=0')
   * @param {object} options { body, headers, raw } — raw=true devuelve { data, status, headers }
   */
  async function request(method, path, options = {}) {
    const headers = { Accept: 'application/json', ...(options.headers || {}) };
    const init = { method, headers };

    if (options.body !== undefined) {
      headers['Content-Type'] = 'application/json';
      init.body = JSON.stringify(options.body);
    }

    const session = readSession();
    if (session) {
      headers.Authorization = 'Bearer ' + session.token;
    }

    let response;
    try {
      response = await fetch(BASE_URL + path, init);
    } catch (networkError) {
      throw new ApiError(0, {
        code: 'NETWORK_ERROR',
        detail: 'No hay conexión con el servidor. Verifique que la aplicación esté en ejecución.'
      });
    }

    const text = await response.text();
    let data = null;
    if (text) {
      try {
        data = JSON.parse(text);
      } catch (e) {
        data = null;
      }
    }

    // Token vencido o inválido: se cierra la sesión y se manda al login
    if (response.status === 401 && session) {
      clearSession();
      const next = encodeURIComponent(location.pathname + location.search);
      location.href = '/login.html?expired=1&next=' + next;
    }

    if (!response.ok) {
      throw new ApiError(response.status, data);
    }

    return options.raw ? { data, status: response.status, headers: response.headers } : data;
  }

  return {
    ApiError,
    get: (path, options) => request('GET', path, options),
    post: (path, body, options = {}) => request('POST', path, { ...options, body }),
    put: (path, body, options = {}) => request('PUT', path, { ...options, body }),
    del: (path, options) => request('DELETE', path, options),
    saveSession,
    clearSession,
    currentUser,
    isLoggedIn,
    isAdmin
  };
})();