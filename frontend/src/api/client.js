/**
 * VYNK API Client
 * Centralized HTTP client handling server-side sessions, relative /api paths,
 * credentials preservation, CSRF token management, and structured error propagation.
 */

class ApiError extends Error {
  constructor(message, status = 500, details = null, raw = null) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
    this.details = details; // field validation errors if provided by Spring Boot
    this.raw = raw;
  }
}

let cachedCsrfToken = null;
let cachedCsrfHeaderName = 'X-CSRF-TOKEN';

/**
 * Retrieves a fresh CSRF token from GET /api/csrf with credentials included.
 */
export async function fetchCsrfToken() {
  try {
    const response = await fetch('/api/csrf', {
      method: 'GET',
      credentials: 'include',
      headers: {
        Accept: 'application/json',
      },
    });

    if (response.ok) {
      const data = await response.json();
      if (data && data.token) {
        cachedCsrfToken = data.token;
        cachedCsrfHeaderName = data.headerName || 'X-CSRF-TOKEN';
        return { headerName: cachedCsrfHeaderName, token: cachedCsrfToken };
      }
    }
  } catch (err) {
    console.warn('Unable to retrieve CSRF token:', err);
  }
  return { headerName: cachedCsrfHeaderName, token: cachedCsrfToken };
}

/**
 * Resets the in-memory CSRF cache (e.g. after logout or session change).
 */
export function clearCsrfToken() {
  cachedCsrfToken = null;
  cachedCsrfHeaderName = 'X-CSRF-TOKEN';
}

/**
 * Checks whether an endpoint is exempt from CSRF validation.
 */
function isCsrfExempt(url) {
  return (
    url.includes('/api/auth/register') ||
    url.includes('/api/auth/login') ||
    url.includes('/api/auth/verify-email') ||
    url.includes('/api/auth/resend-verification') ||
    url.includes('/api/auth/forgot-password') ||
    url.includes('/api/auth/reset-password')
  );
}

/**
 * Main request executor.
 */
async function request(endpoint, options = {}) {
  const method = (options.method || 'GET').toUpperCase();
  const headers = new Headers(options.headers || {});
  const isFormData = typeof FormData !== 'undefined' && options.body instanceof FormData;

  if (!headers.has('Accept')) {
    headers.set('Accept', 'application/json');
  }

  // Attach Content-Type if payload is present, not FormData, and not explicitly set
  if (options.body && typeof options.body === 'string' && !headers.has('Content-Type')) {
    headers.set('Content-Type', 'application/json');
  }

  // If body is FormData, ensure Content-Type is NOT manually set so the browser generates the multipart boundary
  if (isFormData && headers.has('Content-Type')) {
    headers.delete('Content-Type');
  }

  // State-changing requests (except register/login/forgot/reset) require CSRF token
  const isMutating = ['POST', 'PUT', 'DELETE', 'PATCH'].includes(method);
  if (isMutating && !isCsrfExempt(endpoint)) {
    if (!cachedCsrfToken) {
      await fetchCsrfToken();
    }
    if (cachedCsrfToken) {
      headers.set(cachedCsrfHeaderName, cachedCsrfToken);
    }
  }

  const fetchOptions = {
    ...options,
    method,
    headers,
    credentials: 'include', // Preserves JSESSIONID cookie across requests
  };

  let response;
  try {
    response = await fetch(endpoint, fetchOptions);
  } catch (networkError) {
    throw new ApiError(
      'Network error: Unable to reach VYNK server. Please check your connection.',
      0,
      null,
      networkError
    );
  }

  // Handle empty or 204 responses
  if (response.status === 204) {
    return null;
  }

  // Parse response body safely
  const contentType = response.headers.get('content-type') || '';
  let responseData = null;

  if (contentType.includes('application/json')) {
    try {
      responseData = await response.json();
    } catch {
      responseData = null;
    }
  } else {
    try {
      responseData = await response.text();
    } catch {
      responseData = null;
    }
  }

  // Handle successful response
  if (response.ok) {
    return responseData;
  }

  // Handle error responses using Spring Boot's structured error format
  let errorMessage = 'An unexpected error occurred.';
  let errorDetails = null;

  if (responseData && typeof responseData === 'object') {
    if (responseData.message) {
      errorMessage = responseData.message;
    } else if (responseData.error) {
      errorMessage = responseData.error;
    }
    if (responseData.details) {
      errorDetails = responseData.details;
    }
  } else if (typeof responseData === 'string' && responseData.trim().length > 0) {
    errorMessage = responseData;
  } else {
    errorMessage = response.statusText || `Request failed with status ${response.status}`;
  }

  if (response.status === 401) {
    const isPublicAuth = [
      '/api/auth/session',
      '/api/auth/login',
      '/api/auth/register',
      '/api/auth/verify-email',
      '/api/auth/resend-verification',
      '/api/auth/forgot-password',
      '/api/auth/reset-password',
    ].some((path) => (endpoint && endpoint.includes(path)));

    if (!isPublicAuth && typeof unauthorizedHandler === 'function') {
      try {
        unauthorizedHandler();
      } catch (e) {
        console.warn('Error in unauthorizedHandler:', e);
      }
    }
  }

  throw new ApiError(errorMessage, response.status, errorDetails, responseData);
}

let unauthorizedHandler = null;

export const setUnauthorizedHandler = (handler) => {
  unauthorizedHandler = handler;
};

export const apiClient = {
  get: (url, options = {}) => request(url, { ...options, method: 'GET' }),
  post: (url, body, options = {}) => {
    const isFormData = typeof FormData !== 'undefined' && body instanceof FormData;
    return request(url, {
      ...options,
      method: 'POST',
      body: isFormData ? body : (body ? JSON.stringify(body) : undefined),
    });
  },
  put: (url, body, options = {}) => {
    const isFormData = typeof FormData !== 'undefined' && body instanceof FormData;
    return request(url, {
      ...options,
      method: 'PUT',
      body: isFormData ? body : (body ? JSON.stringify(body) : undefined),
    });
  },
  delete: (url, options = {}) => request(url, { ...options, method: 'DELETE' }),
  fetchCsrfToken,
  clearCsrfToken,
};

export { ApiError };
