import React, { useState, useEffect, useRef } from 'react';
import { useSearchParams, useLocation, Link } from 'react-router-dom';
import { authApi } from '../api/auth';
import { Alert } from '../components/common/Alert';
import { LoadingSpinner } from '../components/common/LoadingSpinner';
import { VynkLogo } from '../components/common/VynkLogo';

export function VerifyEmailPage() {
  const [searchParams] = useSearchParams();
  const location = useLocation();

  // Legacy link token support
  const rawToken = searchParams.get('token');
  const [token, setToken] = useState(rawToken || '');

  // Email context (from location state, search params, or manual entry)
  const initialEmail = location.state?.email || searchParams.get('email') || '';
  const [email, setEmail] = useState(initialEmail);
  const [isEditingEmail, setIsEditingEmail] = useState(false);

  // OTP inputs (6 individual digits)
  const [otp, setOtp] = useState(['', '', '', '', '', '']);
  const inputRefs = useRef([]);

  // Loading & feedback states
  const [verifying, setVerifying] = useState(false);
  const [successMessage, setSuccessMessage] = useState('');
  const [errorMessage, setErrorMessage] = useState('');

  // Resend cooldown timer (60s cooldown if redirected from register, otherwise 0 until requested)
  const [cooldown, setCooldown] = useState(location.state?.email ? 60 : 0);
  const [resending, setResending] = useState(false);
  const [resendStatus, setResendStatus] = useState(null);

  // Strip token from browser history if present (defense-in-depth)
  useEffect(() => {
    if (rawToken && window.history && window.history.replaceState) {
      window.history.replaceState({}, document.title, window.location.pathname);
    }
  }, [rawToken]);

  // Cooldown countdown timer
  useEffect(() => {
    if (cooldown <= 0) return;
    const interval = setInterval(() => {
      setCooldown((prev) => Math.max(0, prev - 1));
    }, 1000);
    return () => clearInterval(interval);
  }, [cooldown]);

  // Focus first input box on mount if in OTP mode
  useEffect(() => {
    if (!token && inputRefs.current[0]) {
      inputRefs.current[0].focus();
    }
  }, [token]);

  // Handle single digit input
  const handleDigitChange = (e, index) => {
    const rawValue = e.target.value.replace(/\D/g, '');
    const newOtp = [...otp];

    if (!rawValue) {
      newOtp[index] = '';
      setOtp(newOtp);
      return;
    }

    // Capture the latest digit entered
    newOtp[index] = rawValue.slice(-1);
    setOtp(newOtp);
    setErrorMessage('');

    // Auto-advance to next input
    if (index < 5 && rawValue) {
      inputRefs.current[index + 1]?.focus();
    }
  };

  // Handle keyboard navigation (Backspace & Arrow keys)
  const handleKeyDown = (e, index) => {
    if (e.key === 'Backspace') {
      if (!otp[index] && index > 0) {
        e.preventDefault();
        const newOtp = [...otp];
        newOtp[index - 1] = '';
        setOtp(newOtp);
        inputRefs.current[index - 1]?.focus();
      } else {
        const newOtp = [...otp];
        newOtp[index] = '';
        setOtp(newOtp);
      }
    } else if (e.key === 'ArrowLeft' && index > 0) {
      e.preventDefault();
      inputRefs.current[index - 1]?.focus();
    } else if (e.key === 'ArrowRight' && index < 5) {
      e.preventDefault();
      inputRefs.current[index + 1]?.focus();
    }
  };

  // Handle paste across all 6 input boxes
  const handlePaste = (e) => {
    e.preventDefault();
    const pasteData = e.clipboardData.getData('text');
    const digits = pasteData.replace(/\D/g, '').slice(0, 6).split('');
    if (digits.length === 0) return;

    const newOtp = [...otp];
    digits.forEach((digit, idx) => {
      newOtp[idx] = digit;
    });
    setOtp(newOtp);
    setErrorMessage('');

    const targetFocusIndex = Math.min(digits.length, 5);
    inputRefs.current[targetFocusIndex]?.focus();
  };

  // OTP Verification Submission
  const handleVerifyOtp = async (e) => {
    if (e) e.preventDefault();
    const trimmedEmail = email.trim();
    const code = otp.join('');

    if (!trimmedEmail) {
      setErrorMessage('Please enter your college email address.');
      return;
    }

    if (code.length !== 6) {
      setErrorMessage('Please enter the complete 6-digit verification code.');
      return;
    }

    if (verifying) return;

    try {
      setVerifying(true);
      setErrorMessage('');
      const response = await authApi.verifyOtp(trimmedEmail, code);
      setSuccessMessage(
        response?.message || 'Email verified successfully! Your VYNK account is now active.'
      );
    } catch (err) {
      setErrorMessage(
        err.message || 'Verification failed. The code may be invalid or expired.'
      );
    } finally {
      setVerifying(false);
    }
  };

  // Resend OTP
  const handleResendOtp = async () => {
    const trimmedEmail = email.trim();
    if (!trimmedEmail) {
      setErrorMessage('Please enter your college email address to receive a code.');
      return;
    }

    if (cooldown > 0 || resending) return;

    try {
      setResending(true);
      setResendStatus(null);
      setErrorMessage('');
      const response = await authApi.resendOtp(trimmedEmail);
      setResendStatus({
        type: 'success',
        message: response?.message || 'A new verification code has been sent to your college inbox.',
      });
      setCooldown(60);
      setOtp(['', '', '', '', '', '']);
      inputRefs.current[0]?.focus();
    } catch (err) {
      setResendStatus({
        type: 'error',
        message: err.message || 'Failed to request a new code. Please try again.',
      });
    } finally {
      setResending(false);
    }
  };

  // Legacy link confirmation handler
  const handleConfirmLegacyVerification = async () => {
    if (!token || verifying) return;

    try {
      setVerifying(true);
      setErrorMessage('');
      const response = await authApi.verifyEmail(token);
      setSuccessMessage(
        response?.message || 'Email verified successfully! Your VYNK account is now active.'
      );
    } catch (err) {
      setErrorMessage(
        err.message || 'Verification failed. The token may be expired or already used.'
      );
    } finally {
      setVerifying(false);
    }
  };

  return (
    <div className="container-narrow page-enter" style={{ paddingTop: '3.5rem', paddingBottom: '3.5rem' }}>
      <div className="card text-center">
        <div className="card-header" style={{ display: 'flex', flexDirection: 'column', alignItems: 'center' }}>
          <div style={{ marginBottom: '1rem' }}>
            <VynkLogo variant="icon" size="lg" />
          </div>
          <h1 className="card-title" style={{ fontSize: '1.75rem' }}>
            Email Verification
          </h1>
          <p className="card-subtitle">
            Authenticating your college credentials with VYNK
          </p>
        </div>

        {verifying && (
          <div style={{ padding: '2rem 0' }}>
            <LoadingSpinner size="lg" text="Verifying your credentials with VYNK security..." />
          </div>
        )}

        {/* Success View */}
        {successMessage && (
          <div>
            <Alert type="success" message={successMessage} />
            <p style={{ fontSize: '0.95rem', color: 'var(--text-secondary)', margin: '1.5rem 0' }}>
              Your institutional status has been confirmed. You can now sign in with your college credentials.
            </p>
            <Link to="/login" className="btn btn-primary btn-block">
              Sign In to VYNK &rarr;
            </Link>
          </div>
        )}

        {/* Legacy Token Confirmation View */}
        {token && !verifying && !successMessage && (
          <div>
            {errorMessage && (
              <div style={{ marginBottom: '1.25rem' }}>
                <Alert type="error" message={errorMessage} />
              </div>
            )}
            <div style={{ padding: '1rem 0' }}>
              <p style={{ fontSize: '0.95rem', color: 'var(--text-secondary)', marginBottom: '1.5rem' }}>
                You're one step away! Please click below to confirm your institutional email address and activate your student account.
              </p>
              <button
                type="button"
                className="btn btn-primary btn-block"
                onClick={handleConfirmLegacyVerification}
              >
                Confirm & Activate Account &rarr;
              </button>
              <div style={{ marginTop: '1.25rem' }}>
                <button
                  type="button"
                  onClick={() => setToken('')}
                  style={{
                    background: 'none',
                    border: 'none',
                    color: 'var(--text-secondary)',
                    textDecoration: 'underline',
                    cursor: 'pointer',
                    fontSize: '0.85rem'
                  }}
                >
                  Enter a 6-digit verification code instead
                </button>
              </div>
            </div>
          </div>
        )}

        {/* Primary 6-Digit OTP Verification View */}
        {!token && !verifying && !successMessage && (
          <div>
            {errorMessage && (
              <div style={{ marginBottom: '1.25rem', textAlign: 'left' }}>
                <Alert type="error" message={errorMessage} />
              </div>
            )}

            {/* Email context header */}
            {email ? (
              <div style={{ marginBottom: '1.5rem' }}>
                <p style={{ fontSize: '0.95rem', color: 'var(--text-secondary)', margin: 0 }}>
                  We sent a 6-digit verification code to
                </p>
                <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '0.5rem', marginTop: '0.25rem' }}>
                  <strong style={{ color: 'var(--text-primary)', wordBreak: 'break-all' }}>{email}</strong>
                  {!isEditingEmail && (
                    <button
                      type="button"
                      onClick={() => setIsEditingEmail(true)}
                      style={{
                        background: 'none',
                        border: 'none',
                        color: 'var(--text-secondary)',
                        textDecoration: 'underline',
                        fontSize: '0.85rem',
                        cursor: 'pointer',
                        padding: 0
                      }}
                    >
                      Change
                    </button>
                  )}
                </div>
                {isEditingEmail && (
                  <div style={{ marginTop: '0.75rem', display: 'flex', gap: '0.5rem', justifyContent: 'center' }}>
                    <input
                      type="email"
                      value={email}
                      onChange={(e) => setEmail(e.target.value)}
                      placeholder="student@college.edu"
                      className="form-input"
                      style={{ maxWidth: '280px' }}
                    />
                    <button
                      type="button"
                      className="btn btn-secondary"
                      onClick={() => setIsEditingEmail(false)}
                      style={{ padding: '0.4rem 0.75rem', fontSize: '0.85rem' }}
                    >
                      Done
                    </button>
                  </div>
                )}
              </div>
            ) : (
              <div style={{ marginBottom: '1.5rem', textAlign: 'left' }}>
                <label
                  htmlFor="verify-email-input"
                  style={{
                    display: 'block',
                    fontSize: '0.875rem',
                    fontWeight: 600,
                    color: 'var(--text-secondary)',
                    marginBottom: '0.375rem',
                  }}
                >
                  College Email Address
                </label>
                <input
                  id="verify-email-input"
                  type="email"
                  placeholder="student@college.edu"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  className="form-input"
                  style={{ width: '100%' }}
                  required
                />
              </div>
            )}

            {/* OTP 6-Digit Entry */}
            <form onSubmit={handleVerifyOtp}>
              <div
                style={{
                  display: 'flex',
                  gap: '0.5rem',
                  justifyContent: 'center',
                  alignItems: 'center',
                  marginBottom: '1.75rem',
                }}
              >
                {otp.map((digit, index) => (
                  <input
                    key={index}
                    ref={(el) => (inputRefs.current[index] = el)}
                    type="text"
                    inputMode="numeric"
                    pattern="[0-9]*"
                    maxLength={1}
                    autoComplete={index === 0 ? 'one-time-code' : 'off'}
                    aria-label={`Digit ${index + 1}`}
                    data-testid={`otp-input-${index}`}
                    value={digit}
                    onChange={(e) => handleDigitChange(e, index)}
                    onKeyDown={(e) => handleKeyDown(e, index)}
                    onPaste={handlePaste}
                    disabled={verifying}
                    className="form-input"
                    style={{
                      width: '46px',
                      height: '56px',
                      fontSize: '1.5rem',
                      fontWeight: 700,
                      textAlign: 'center',
                      borderRadius: '8px',
                      border: '1px solid var(--border-medium)',
                      background: 'var(--input-bg)',
                      color: 'var(--input-text)',
                      padding: 0,
                    }}
                  />
                ))}
              </div>

              <button
                type="submit"
                className="btn btn-primary btn-block"
                disabled={verifying || otp.join('').length !== 6 || !email.trim()}
              >
                Verify Code &rarr;
              </button>
            </form>

            {/* Resend Cooldown Section */}
            <div
              style={{
                marginTop: '2.25rem',
                paddingTop: '1.5rem',
                borderTop: '1px solid var(--border-light)',
                textAlign: 'center',
              }}
            >
              <h2 style={{ fontSize: '1rem', fontWeight: 600, color: 'var(--text-primary)', marginBottom: '0.35rem' }}>
                Didn't receive the code?
              </h2>
              <p style={{ fontSize: '0.85rem', color: 'var(--text-secondary)', marginBottom: '1rem' }}>
                Check your spam or junk folder, or request a fresh verification code below.
              </p>

              {resendStatus && (
                <div style={{ marginBottom: '1rem', textAlign: 'left' }}>
                  <Alert type={resendStatus.type} message={resendStatus.message} />
                </div>
              )}

              <button
                type="button"
                className="btn btn-secondary"
                onClick={handleResendOtp}
                disabled={resending || cooldown > 0 || !email.trim()}
              >
                {resending
                  ? 'Sending...'
                  : cooldown > 0
                  ? `Resend code in ${cooldown}s`
                  : 'Resend Code'}
              </button>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
