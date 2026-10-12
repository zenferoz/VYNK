import React, { useState, useEffect } from 'react';
import { useSearchParams, Link } from 'react-router-dom';
import { authApi } from '../api/auth';
import { InputField } from '../components/forms/InputField';
import { Alert } from '../components/common/Alert';
import { LoadingSpinner } from '../components/common/LoadingSpinner';
import { VynkLogo } from '../components/common/VynkLogo';

export function ResetPasswordPage() {
  const [searchParams] = useSearchParams();
  const rawToken = searchParams.get('token');

  // Capture token into state, then scrub from URL immediately to protect against referrer leakage
  const [token] = useState(rawToken || '');
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [fieldErrors, setFieldErrors] = useState({});
  const [generalError, setGeneralError] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [successMessage, setSuccessMessage] = useState('');

  useEffect(() => {
    if (rawToken && window.history && window.history.replaceState) {
      window.history.replaceState({}, document.title, window.location.pathname);
    }
  }, [rawToken]);

  const validate = () => {
    const errors = {};
    if (!newPassword) {
      errors.newPassword = 'New password is required.';
    } else if (newPassword.length < 8 || newPassword.length > 72) {
      errors.newPassword = 'Password must be between 8 and 72 characters.';
    }

    if (!confirmPassword) {
      errors.confirmPassword = 'Confirm your new password.';
    } else if (newPassword !== confirmPassword) {
      errors.confirmPassword = 'Passwords do not match.';
    }

    setFieldErrors(errors);
    return Object.keys(errors).length === 0;
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setGeneralError('');
    if (!token) {
      setGeneralError('Missing password reset token. Please use the link sent to your college email.');
      return;
    }
    if (!validate() || submitting) return;

    try {
      setSubmitting(true);
      const res = await authApi.resetPassword(token, newPassword);
      setSuccessMessage(res?.message || 'Your password has been successfully reset. You can now log in.');
    } catch (err) {
      setGeneralError(err.message || 'Failed to reset password. The link may have expired or already been used.');
    } finally {
      setSubmitting(false);
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
            Reset Password
          </h1>
          <p className="card-subtitle">
            Choose a strong new password for your VYNK account
          </p>
        </div>

        {generalError && <Alert type="error" message={generalError} />}

        {!token && !successMessage && (
          <div>
            <Alert
              type="error"
              message="No reset token was detected in the URL. Please click the full link provided in the password reset email, or request a new one."
            />
            <div style={{ marginTop: '1.5rem' }}>
              <Link to="/forgot-password" className="btn btn-primary btn-block">
                Request New Reset Link
              </Link>
            </div>
          </div>
        )}

        {token && successMessage ? (
          <div>
            <Alert type="success" message={successMessage} />
            <div style={{ marginTop: '2rem' }}>
              <Link to="/login" className="btn btn-primary btn-block">
                Sign In With New Password &rarr;
              </Link>
            </div>
          </div>
        ) : token ? (
          <form onSubmit={handleSubmit} noValidate>
            <InputField
              id="newPassword"
              name="newPassword"
              label="New Password"
              type="password"
              value={newPassword}
              onChange={(e) => {
                setNewPassword(e.target.value);
                if (fieldErrors.newPassword) setFieldErrors((prev) => ({ ...prev, newPassword: '' }));
                if (generalError) setGeneralError('');
              }}
              placeholder="Enter new password (8-72 characters)"
              required
              error={fieldErrors.newPassword}
              disabled={submitting}
              autoComplete="new-password"
            />

            <InputField
              id="confirmPassword"
              name="confirmPassword"
              label="Confirm New Password"
              type="password"
              value={confirmPassword}
              onChange={(e) => {
                setConfirmPassword(e.target.value);
                if (fieldErrors.confirmPassword) setFieldErrors((prev) => ({ ...prev, confirmPassword: '' }));
                if (generalError) setGeneralError('');
              }}
              placeholder="Confirm new password"
              required
              error={fieldErrors.confirmPassword}
              disabled={submitting}
              autoComplete="new-password"
            />

            <div style={{ marginTop: '1.75rem' }}>
              <button
                type="submit"
                disabled={submitting}
                className="btn btn-primary btn-block"
              >
                {submitting ? <LoadingSpinner text="Updating password..." /> : 'Set New Password'}
              </button>
            </div>

            <div style={{ marginTop: '1.5rem', textAlign: 'center', fontSize: '0.875rem' }}>
              <Link to="/login" style={{ color: 'var(--text-secondary)' }}>
                &larr; Back to Sign In
              </Link>
            </div>
          </form>
        ) : null}
      </div>
    </div>
  );
}
