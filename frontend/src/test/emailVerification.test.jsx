import React from 'react';
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { render, screen, fireEvent, waitFor, act } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { VerifyEmailPage } from '../pages/VerifyEmailPage';
import { authApi } from '../api/auth';

vi.mock('../api/auth', () => ({
  authApi: {
    verifyOtp: vi.fn(),
    resendOtp: vi.fn(),
    verifyEmail: vi.fn(),
    resendVerification: vi.fn(),
  },
}));

describe('VerifyEmailPage and OTP Flow', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  it('renders OTP interface with email context and 6 digit inputs', () => {
    render(
      <MemoryRouter initialEntries={['/verify-email?email=student@example.edu']}>
        <VerifyEmailPage />
      </MemoryRouter>
    );

    expect(screen.getByText(/We sent a 6-digit verification code to/i)).toBeDefined();
    expect(screen.getByText('student@example.edu')).toBeDefined();

    for (let i = 0; i < 6; i++) {
      expect(screen.getByTestId(`otp-input-${i}`)).toBeDefined();
      expect(screen.getByLabelText(`Digit ${i + 1}`)).toBeDefined();
    }

    const verifyBtn = screen.getByRole('button', { name: /Verify Code/i });
    expect(verifyBtn).toBeDefined();
    expect(verifyBtn.disabled).toBe(true);
  });

  it('allows manual email entry when no email parameter is provided', () => {
    render(
      <MemoryRouter initialEntries={['/verify-email']}>
        <VerifyEmailPage />
      </MemoryRouter>
    );

    const emailInput = screen.getByLabelText(/College Email Address/i);
    expect(emailInput).toBeDefined();
    fireEvent.change(emailInput, { target: { value: 'newstudent@college.edu' } });
    expect(emailInput.value).toBe('newstudent@college.edu');
  });

  it('accepts typing each digit, auto-advances, and submits verifyOtp successfully', async () => {
    authApi.verifyOtp.mockResolvedValueOnce({
      message: 'Email verified successfully! Your VYNK account is now active.',
      verified: true,
    });

    render(
      <MemoryRouter initialEntries={['/verify-email?email=student@example.edu']}>
        <VerifyEmailPage />
      </MemoryRouter>
    );

    const digits = ['1', '2', '3', '4', '5', '6'];
    for (let i = 0; i < 6; i++) {
      const input = screen.getByTestId(`otp-input-${i}`);
      fireEvent.change(input, { target: { value: digits[i] } });
    }

    const verifyBtn = screen.getByRole('button', { name: /Verify Code/i });
    expect(verifyBtn.disabled).toBe(false);

    fireEvent.click(verifyBtn);

    await waitFor(() => {
      expect(authApi.verifyOtp).toHaveBeenCalledWith('student@example.edu', '123456');
    });

    await waitFor(() => {
      expect(screen.getByText(/Email verified successfully! Your VYNK account is now active/i)).toBeDefined();
      expect(screen.getByRole('link', { name: /Sign In to VYNK/i })).toBeDefined();
    });
  });

  it('supports pasting a 6-digit code across all input boxes', async () => {
    authApi.verifyOtp.mockResolvedValueOnce({
      message: 'Email verified successfully! Your VYNK account is now active.',
    });

    render(
      <MemoryRouter initialEntries={['/verify-email?email=student@example.edu']}>
        <VerifyEmailPage />
      </MemoryRouter>
    );

    const firstInput = screen.getByTestId('otp-input-0');
    fireEvent.paste(firstInput, {
      clipboardData: {
        getData: () => '849201',
      },
    });

    for (let i = 0; i < 6; i++) {
      const input = screen.getByTestId(`otp-input-${i}`);
      expect(input.value).toBe('849201'[i]);
    }

    const verifyBtn = screen.getByRole('button', { name: /Verify Code/i });
    expect(verifyBtn.disabled).toBe(false);

    fireEvent.click(verifyBtn);

    await waitFor(() => {
      expect(authApi.verifyOtp).toHaveBeenCalledWith('student@example.edu', '849201');
    });
  });

  it('handles backspace navigation correctly across digits', () => {
    render(
      <MemoryRouter initialEntries={['/verify-email?email=student@example.edu']}>
        <VerifyEmailPage />
      </MemoryRouter>
    );

    const input0 = screen.getByTestId('otp-input-0');
    const input1 = screen.getByTestId('otp-input-1');

    fireEvent.change(input0, { target: { value: '5' } });
    fireEvent.change(input1, { target: { value: '9' } });
    expect(input1.value).toBe('9');

    // Backspace on filled input clears it
    fireEvent.keyDown(input1, { key: 'Backspace' });
    expect(input1.value).toBe('');

    // Backspace on empty input moves focus back and clears previous
    fireEvent.keyDown(input1, { key: 'Backspace' });
    expect(input0.value).toBe('');
  });

  it('displays error alert when verifyOtp fails', async () => {
    authApi.verifyOtp.mockRejectedValueOnce(
      new Error('Invalid or expired verification code. 4 attempts remaining.')
    );

    render(
      <MemoryRouter initialEntries={['/verify-email?email=student@example.edu']}>
        <VerifyEmailPage />
      </MemoryRouter>
    );

    for (let i = 0; i < 6; i++) {
      fireEvent.change(screen.getByTestId(`otp-input-${i}`), { target: { value: '0' } });
    }

    fireEvent.click(screen.getByRole('button', { name: /Verify Code/i }));

    await waitFor(() => {
      expect(screen.getByText('Invalid or expired verification code. 4 attempts remaining.')).toBeDefined();
    });
  });

  it('resends OTP and enters disabled cooldown state', async () => {
    authApi.resendOtp.mockResolvedValueOnce({
      message: 'A new verification code has been sent to your email.',
    });

    render(
      <MemoryRouter initialEntries={['/verify-email?email=student@example.edu']}>
        <VerifyEmailPage />
      </MemoryRouter>
    );

    const resendBtn = screen.getByRole('button', { name: 'Resend Code' });
    expect(resendBtn.disabled).toBe(false);

    fireEvent.click(resendBtn);

    await waitFor(() => {
      expect(authApi.resendOtp).toHaveBeenCalledWith('student@example.edu');
    });

    await waitFor(() => {
      expect(screen.getByText('A new verification code has been sent to your email.')).toBeDefined();
    });

    // Check that button enters cooldown state
    await waitFor(() => {
      expect(screen.getByRole('button', { name: /Resend code in \d+s/i })).toBeDefined();
      expect(screen.getByRole('button', { name: /Resend code in \d+s/i }).disabled).toBe(true);
    });
  });

  it('decrements registration cooldown timer down to zero and enables resend button', () => {
    vi.useFakeTimers();

    render(
      <MemoryRouter
        initialEntries={[
          { pathname: '/verify-email', state: { email: 'student@example.edu' } },
        ]}
      >
        <VerifyEmailPage />
      </MemoryRouter>
    );

    // Initial state from register has 60s cooldown
    expect(screen.getByRole('button', { name: /Resend code in 60s/i })).toBeDefined();
    expect(screen.getByRole('button', { name: /Resend code in 60s/i }).disabled).toBe(true);

    act(() => {
      vi.advanceTimersByTime(1000);
    });
    expect(screen.getByRole('button', { name: /Resend code in 59s/i })).toBeDefined();

    act(() => {
      vi.advanceTimersByTime(59000);
    });
    expect(screen.getByRole('button', { name: 'Resend Code' })).toBeDefined();
    expect(screen.getByRole('button', { name: 'Resend Code' }).disabled).toBe(false);
  });

  it('preserves legacy token flow when ?token= parameter is present in URL', async () => {
    const replaceStateSpy = vi.spyOn(window.history, 'replaceState');
    authApi.verifyEmail.mockResolvedValueOnce({
      message: 'Email verified successfully! Your VYNK account is now active.',
    });

    render(
      <MemoryRouter initialEntries={['/verify-email?token=legacy-token-789']}>
        <VerifyEmailPage />
      </MemoryRouter>
    );

    // Strips token from URL
    expect(replaceStateSpy).toHaveBeenCalledWith({}, document.title, window.location.pathname);

    // Does NOT auto-submit without click
    expect(authApi.verifyEmail).not.toHaveBeenCalled();

    const confirmBtn = screen.getByRole('button', { name: /Confirm & Activate Account/i });
    expect(confirmBtn).toBeDefined();

    fireEvent.click(confirmBtn);

    await waitFor(() => {
      expect(authApi.verifyEmail).toHaveBeenCalledWith('legacy-token-789');
    });

    await waitFor(() => {
      expect(screen.getByText(/Email verified successfully! Your VYNK account is now active/i)).toBeDefined();
    });

    replaceStateSpy.mockRestore();
  });
});
