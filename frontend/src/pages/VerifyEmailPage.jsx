import React, { useState, useEffect } from 'react';
import { useSearchParams, Link } from 'react-router-dom';
import { authApi } from '../api/auth';
import { Alert } from '../components/common/Alert';
import { LoadingSpinner } from '../components/common/LoadingSpinner';
import { VynkLogo } from '../components/common/VynkLogo';

export function VerifyEmailPage() {
  const [searchParams] = useSearchParams();
  const rawToken = searchParams.get('token');

  // Token captured in state, then immediately stripped from URL
  const [token] = useState(rawToken || '');
  const [verifying, setVerifying] = useState(false);
  const [successMessage, setSuccessMessage] = useState('');
  const [errorMessage, setErrorMessage] = useState('');

  // Resend verification states
  const [resendEmail, setResendEmail] = useState('');
  const [resending, setResending] = useState(false);
  const [resendStatus, setResendStatus] = useState(null);

  useEffect(() => {
    if (rawToken && window.history && window.history.replaceState) {
      // Defense-in-depth: remove sensitive bearer token from visible URL and browser history immediately
      window.history.replaceState({}, document.title, window.location.pathname);
    }
  }, [rawToken]);

  const handleConfirmVerification = async () => {
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

  const handleResend = async (e) => {
    e.preventDefault();
    if (!resendEmail.trim()) return;

    try {
      setResending(true);
      setResendStatus(null);
      const res = await authApi.resendVerification(resendEmail.trim());
      setResendStatus({
        type: 'success',
        message: res?.message || 'If an eligible unverified account exists for this email, a verification link has been sent.',
      });
      setResendEmail('');
    } catch (err) {
      setResendStatus({
        type: 'error',
        message: err.message || 'Failed to request verification email. Please try again.',
      });
    } finally {
      setResending(false);
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
            <LoadingSpinner size="lg" text="Verifying your token with VYNK security..." />
          </div>
        )}

        {/* Step 1: No token in URL and no actions performed */}
        {!token && !verifying && !successMessage && !errorMessage && (
          <div>
            <Alert
              type="info"
              message="No verification token was detected in the URL. Please click the full link provided in the verification email sent to your college inbox, or request a new verification link below."
            />
          </div>
        )}

        {/* Step 2: Token present, waiting for student explicit confirmation (Prevents automated email scanners from activating) */}
        {token && !verifying && !successMessage && !errorMessage && (
          <div style={{ padding: '1rem 0' }}>
            <p style={{ fontSize: '0.95rem', color: 'var(--text-secondary)', marginBottom: '1.5rem' }}>
              You're one step away! Please click below to confirm your institutional email address and activate your student account.
            </p>
            <button
              type="button"
              className="btn btn-primary btn-block"
              onClick={handleConfirmVerification}
            >
              Confirm & Activate Account &rarr;
            </button>
          </div>
        )}

        {/* Step 3: Success state */}
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

        {/* Step 4: Error state */}
        {errorMessage && (
          <div>
            <Alert type="error" message={errorMessage} />
            <div style={{ marginTop: '1.5rem', display: 'flex', gap: '0.75rem', justifyContent: 'center' }}>
              <Link to="/login" className="btn btn-secondary">
                Try Signing In
              </Link>
              <Link to="/register" className="btn btn-primary">
                Register Again
              </Link>
            </div>
          </div>
        )}

        {/* Resend Verification Section for expired or lost tokens */}
        {(!successMessage && (errorMessage || !token)) && (
          <div style={{ marginTop: '2.5rem', paddingTop: '1.75rem', borderTop: '1px solid var(--border-light)', textAlign: 'left' }}>
            <h2 style={{ fontSize: '1.05rem', fontWeight: 600, color: 'var(--text-primary)', marginBottom: '0.5rem' }}>
              Need a new verification link?
            </h2>
            <p style={{ fontSize: '0.85rem', color: 'var(--text-secondary)', marginBottom: '1rem' }}>
              Enter your college email address to receive a fresh verification link.
            </p>

            {resendStatus && (
              <div style={{ marginBottom: '1rem' }}>
                <Alert type={resendStatus.type} message={resendStatus.message} />
              </div>
            )}

            <form onSubmit={handleResend} style={{ display: 'flex', gap: '0.5rem', flexWrap: 'wrap' }}>
              <input
                type="email"
                placeholder="student@college.edu"
                value={resendEmail}
                onChange={(e) => setResendEmail(e.target.value)}
                required
                className="form-input"
                style={{ flex: '1 1 200px' }}
                disabled={resending}
              />
              <button
                type="submit"
                className="btn btn-secondary"
                disabled={resending || !resendEmail.trim()}
              >
                {resending ? 'Sending...' : 'Resend Link'}
              </button>
            </form>
          </div>
        )}
      </div>
    </div>
  );
}
