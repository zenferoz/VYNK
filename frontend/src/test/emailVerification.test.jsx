import React from 'react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { VerifyEmailPage } from '../pages/VerifyEmailPage';
import { authApi } from '../api/auth';

vi.mock('../api/auth', () => ({
  authApi: {
    verifyEmail: vi.fn(),
    resendVerification: vi.fn(),
  },
}));

describe('VerifyEmailPage and Flow', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('renders missing token state and allows requesting a new link', async () => {
    render(
      <MemoryRouter initialEntries={['/verify-email']}>
        <VerifyEmailPage />
      </MemoryRouter>
    );

    expect(screen.getByText(/No verification token was detected in the URL/i)).toBeDefined();
    expect(screen.getByText('Need a new verification link?')).toBeDefined();

    authApi.resendVerification.mockResolvedValueOnce({
      message: 'If an eligible unverified account exists for this email, a verification link has been sent.',
    });

    const emailInput = screen.getByPlaceholderText('student@college.edu');
    fireEvent.change(emailInput, { target: { value: 'student@example.edu' } });

    const submitBtn = screen.getByRole('button', { name: 'Resend Link' });
    fireEvent.click(submitBtn);

    await waitFor(() => {
      expect(authApi.resendVerification).toHaveBeenCalledWith('student@example.edu');
    });

    await waitFor(() => {
      expect(screen.getByText(/If an eligible unverified account exists for this email/i)).toBeDefined();
    });
  });

  it('opening confirmation page strips token from URL but does NOT automatically submit verification', async () => {
    const replaceStateSpy = vi.spyOn(window.history, 'replaceState');

    render(
      <MemoryRouter initialEntries={['/verify-email?token=scanner-test-token-123']}>
        <VerifyEmailPage />
      </MemoryRouter>
    );

    // Verifies window.history.replaceState immediately called to strip token from address bar
    expect(replaceStateSpy).toHaveBeenCalledWith({}, document.title, window.location.pathname);

    // CRITICAL (Fix 3): Scanner opening the page does NOT automatically trigger verification
    expect(authApi.verifyEmail).not.toHaveBeenCalled();

    // Renders the explicit student confirmation screen
    expect(screen.getByText(/You're one step away! Please click below to confirm/i)).toBeDefined();
    expect(screen.getByRole('button', { name: /Confirm & Activate Account/i })).toBeDefined();

    replaceStateSpy.mockRestore();
  });

  it('submits verification only when student clicks confirmation button', async () => {
    authApi.verifyEmail.mockResolvedValueOnce({
      message: 'Email verified successfully! Your VYNK account is now active.',
    });

    render(
      <MemoryRouter initialEntries={['/verify-email?token=valid-test-token-123']}>
        <VerifyEmailPage />
      </MemoryRouter>
    );

    // Not called before click
    expect(authApi.verifyEmail).not.toHaveBeenCalled();

    // Student explicitly confirms
    const confirmBtn = screen.getByRole('button', { name: /Confirm & Activate Account/i });
    fireEvent.click(confirmBtn);

    await waitFor(() => {
      expect(authApi.verifyEmail).toHaveBeenCalledWith('valid-test-token-123');
    });

    await waitFor(() => {
      expect(screen.getByText(/Email verified successfully! Your VYNK account is now active/i)).toBeDefined();
      expect(screen.getByRole('link', { name: /Sign In to VYNK/i })).toBeDefined();
    });
  });

  it('displays generic error and resend form when explicit verification fails', async () => {
    authApi.verifyEmail.mockRejectedValueOnce(
      new Error('Verification link is invalid or has expired.')
    );

    render(
      <MemoryRouter initialEntries={['/verify-email?token=expired-test-token-456']}>
        <VerifyEmailPage />
      </MemoryRouter>
    );

    const confirmBtn = screen.getByRole('button', { name: /Confirm & Activate Account/i });
    fireEvent.click(confirmBtn);

    await waitFor(() => {
      expect(screen.getByText('Verification link is invalid or has expired.')).toBeDefined();
    });

    expect(screen.getByText('Need a new verification link?')).toBeDefined();
    expect(screen.getByRole('link', { name: 'Try Signing In' })).toBeDefined();
    expect(screen.getByRole('link', { name: 'Register Again' })).toBeDefined();
  });
});
