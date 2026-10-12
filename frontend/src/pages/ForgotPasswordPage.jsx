import React, { useState } from 'react';
import { Link } from 'react-router-dom';
import { authApi } from '../api/auth';
import { InputField } from '../components/forms/InputField';
import { Alert } from '../components/common/Alert';
import { LoadingSpinner } from '../components/common/LoadingSpinner';
import { VynkLogo } from '../components/common/VynkLogo';

export function ForgotPasswordPage() {
  const [email, setEmail] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [fieldError, setFieldError] = useState('');
  const [generalError, setGeneralError] = useState('');
  const [submitted, setSubmitted] = useState(false);

  const validate = () => {
    if (!email.trim()) {
      setFieldError('College email is required.');
      return false;
    }
    if (!email.includes('@')) {
      setFieldError('Enter a valid college email format.');
      return false;
    }
    setFieldError('');
    return true;
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setGeneralError('');
    if (!validate() || submitting) return;

    try {
      setSubmitting(true);
      await authApi.forgotPassword(email.trim());
      setSubmitted(true);
    } catch (err) {
      setGeneralError(err.message || 'Unable to process your request. Please try again.');
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
            Recover Account
          </h1>
          <p className="card-subtitle">
            Enter your registered college email to receive a secure password reset link
          </p>
        </div>

        {generalError && <Alert type="error" message={generalError} />}

        {submitted ? (
          <div>
            <Alert
              type="success"
              message="If an eligible account exists for this email, password reset instructions have been sent."
            />
            <p style={{ marginTop: '1rem', fontSize: '0.95rem', color: 'var(--text-secondary)' }}>
              Please check your college email inbox. The reset link will remain valid for 1 hour.
            </p>
            <div style={{ marginTop: '2rem' }}>
              <Link to="/login" className="btn btn-primary btn-block">
                Return to Sign In
              </Link>
            </div>
          </div>
        ) : (
          <form onSubmit={handleSubmit} noValidate>
            <InputField
              id="email"
              name="email"
              label="College Email"
              type="email"
              value={email}
              onChange={(e) => {
                setEmail(e.target.value);
                if (fieldError) setFieldError('');
                if (generalError) setGeneralError('');
              }}
              placeholder="student@college.edu"
              required
              error={fieldError}
              disabled={submitting}
              autoComplete="email"
            />

            <div style={{ marginTop: '1.75rem' }}>
              <button
                type="submit"
                disabled={submitting}
                className="btn btn-primary btn-block"
              >
                {submitting ? <LoadingSpinner text="Sending link..." /> : 'Send Reset Link'}
              </button>
            </div>

            <div style={{ marginTop: '1.5rem', textAlign: 'center', fontSize: '0.875rem' }}>
              <Link to="/login" style={{ color: 'var(--text-secondary)' }}>
                &larr; Back to Sign In
              </Link>
            </div>
          </form>
        )}
      </div>
    </div>
  );
}
