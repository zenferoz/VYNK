import React from 'react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { ForgotPasswordPage } from '../pages/ForgotPasswordPage';
import { ResetPasswordPage } from '../pages/ResetPasswordPage';
import { authApi } from '../api/auth';
import { apiClient } from '../api/client';
import { filesApi } from '../api/files';

vi.mock('../api/auth', () => ({
  authApi: {
    forgotPassword: vi.fn(),
    resetPassword: vi.fn(),
  },
}));

describe('SEC-03: Multipart Uploads in apiClient', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('filesApi.uploadFile sends FormData directly without manual Content-Type header', async () => {
    const postSpy = vi.spyOn(apiClient, 'post').mockResolvedValueOnce({
      id: 1,
      fileName: 'doc.pdf',
    });

    const file = new File(['content'], 'test.pdf', { type: 'application/pdf' });
    await filesApi.uploadFile(file, 'MESSAGE', 123);

    expect(postSpy).toHaveBeenCalledTimes(1);
    const [url, body, options] = postSpy.mock.calls[0];
    expect(url).toBe('/api/files/upload');
    expect(body instanceof FormData).toBe(true);
    expect(options).toBeUndefined();
  });
});

describe('SEC-04: ForgotPasswordPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('validates email and displays neutral message upon submission', async () => {
    authApi.forgotPassword.mockResolvedValueOnce({
      message: 'If an eligible account exists for this email, password reset instructions have been sent.',
    });

    render(
      <MemoryRouter>
        <ForgotPasswordPage />
      </MemoryRouter>
    );

    const emailInput = screen.getByPlaceholderText('student@college.edu');
    const submitBtn = screen.getByRole('button', { name: /Send Reset Link/i });

    // Empty validation
    fireEvent.click(submitBtn);
    expect(screen.getByText(/College email is required/i)).toBeDefined();

    // Valid email
    fireEvent.change(emailInput, { target: { value: 'student@college.edu' } });
    fireEvent.click(submitBtn);

    await waitFor(() => {
      expect(authApi.forgotPassword).toHaveBeenCalledWith('student@college.edu');
    });

    await waitFor(() => {
      expect(screen.getByText(/If an eligible account exists for this email/i)).toBeDefined();
    });
  });
});

describe('SEC-04: ResetPasswordPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('renders missing token message when no token parameter is present', () => {
    render(
      <MemoryRouter initialEntries={['/reset-password']}>
        <ResetPasswordPage />
      </MemoryRouter>
    );

    expect(screen.getByText(/No reset token was detected in the URL/i)).toBeDefined();
  });

  it('strips token from URL and allows password reset when token is provided', async () => {
    const replaceStateSpy = vi.spyOn(window.history, 'replaceState');
    authApi.resetPassword.mockResolvedValueOnce({
      message: 'Your password has been successfully reset. You can now log in.',
    });

    render(
      <MemoryRouter initialEntries={['/reset-password?token=secureResetToken123']}>
        <ResetPasswordPage />
      </MemoryRouter>
    );

    expect(replaceStateSpy).toHaveBeenCalledWith({}, document.title, window.location.pathname);

    const newPassInput = screen.getByPlaceholderText(/Enter new password/i);
    const confirmPassInput = screen.getByPlaceholderText(/Confirm new password/i);
    const submitBtn = screen.getByRole('button', { name: /Set New Password/i });

    // Mismatched passwords
    fireEvent.change(newPassInput, { target: { value: 'ValidPassword123' } });
    fireEvent.change(confirmPassInput, { target: { value: 'DifferentPassword' } });
    fireEvent.click(submitBtn);

    expect(screen.getByText(/Passwords do not match/i)).toBeDefined();

    // Matching passwords
    fireEvent.change(confirmPassInput, { target: { value: 'ValidPassword123' } });
    fireEvent.click(submitBtn);

    await waitFor(() => {
      expect(authApi.resetPassword).toHaveBeenCalledWith('secureResetToken123', 'ValidPassword123');
    });

    await waitFor(() => {
      expect(screen.getByText(/Your password has been successfully reset/i)).toBeDefined();
    });
  });
});

describe('SEC-09: Global 401 Session Interceptor in apiClient', () => {
  it('triggers unauthorizedHandler on 401 for protected endpoints but not public auth', async () => {
    const { setUnauthorizedHandler } = await import('../api/client');
    const handler = vi.fn();
    setUnauthorizedHandler(handler);

    const originalFetch = global.fetch;
    global.fetch = vi.fn().mockResolvedValue({
      ok: false,
      status: 401,
      statusText: 'Unauthorized',
      headers: { get: () => 'application/json' },
      json: async () => ({ message: 'Session expired.' }),
    });

    try {
      await expect(apiClient.get('/api/posts/feed')).rejects.toThrow();
      expect(handler).toHaveBeenCalledTimes(1);

      handler.mockClear();
      await expect(apiClient.get('/api/auth/session')).rejects.toThrow();
      expect(handler).not.toHaveBeenCalled();
    } finally {
      global.fetch = originalFetch;
      setUnauthorizedHandler(null);
    }
  });
});
