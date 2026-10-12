import { apiClient } from './client';

export const authApi = {
  /**
   * Registers a new student account.
   * @param {{ email: string, password: string, collegeId: number }} payload
   */
  register(payload) {
    return apiClient.post('/api/auth/register', {
      email: payload.email.trim(),
      password: payload.password,
      collegeId: Number(payload.collegeId),
    });
  },

  /**
   * Authenticates user and initiates server session cookie.
   * @param {{ email: string, password: string }} credentials
   */
  async login(credentials) {
    const response = await apiClient.post('/api/auth/login', {
      email: credentials.email.trim(),
      password: credentials.password,
    });
    // Immediately prime the CSRF token after login so subsequent mutating calls succeed
    await apiClient.fetchCsrfToken();
    return response;
  },

  /**
   * Invalidates server session and clears cookies.
   */
  async logout() {
    try {
      const response = await apiClient.post('/api/auth/logout');
      return response;
    } finally {
      apiClient.clearCsrfToken();
    }
  },

  /**
   * Verifies a student's 6-digit email OTP code.
   * @param {{ email: string, otp: string } | string} emailOrPayload
   * @param {string} [otp]
   */
  verifyOtp(emailOrPayload, otp) {
    const payload = typeof emailOrPayload === 'object' && emailOrPayload !== null
      ? emailOrPayload
      : { email: emailOrPayload, otp };
    return apiClient.post('/api/auth/verify-otp', {
      email: payload.email.trim(),
      otp: String(payload.otp || '').trim(),
    });
  },

  /**
   * Requests a new 6-digit OTP code for an unverified student account.
   * @param {{ email: string } | string} emailOrPayload
   */
  resendOtp(emailOrPayload) {
    const email = typeof emailOrPayload === 'object' && emailOrPayload !== null
      ? emailOrPayload.email
      : emailOrPayload;
    return apiClient.post('/api/auth/resend-otp', {
      email: String(email || '').trim(),
    });
  },

  /**
   * Verifies an email token via POST request to prevent scanner consumption.
   * @param {string} token
   */
  verifyEmail(token) {
    return apiClient.post('/api/auth/verify-email', {
      token: token.trim(),
    });
  },

  /**
   * Requests a new verification email for an unverified account.
   * @param {string} email
   */
  resendVerification(email) {
    return apiClient.post('/api/auth/resend-verification', {
      email: email.trim(),
    });
  },

  /**
   * Requests a password reset link for the provided email.
   * @param {string} email
   */
  forgotPassword(email) {
    return apiClient.post('/api/auth/forgot-password', {
      email: email.trim(),
    });
  },

  /**
   * Resets password using the received token.
   * @param {string} token
   * @param {string} newPassword
   */
  resetPassword(token, newPassword) {
    return apiClient.post('/api/auth/reset-password', {
      token: token.trim(),
      newPassword,
    });
  },

  /**
   * Retrieves currently authenticated session data.
   */
  getSession() {
    return apiClient.get('/api/session/me');
  },
};
