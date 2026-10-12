import React, { useState } from 'react';
import { Link, useNavigate, useLocation } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { InputField } from '../components/forms/InputField';
import { Alert } from '../components/common/Alert';
import { LoadingSpinner } from '../components/common/LoadingSpinner';
import { VynkLogo } from '../components/common/VynkLogo';

export function LoginPage() {
  const { login } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();

  const [formData, setFormData] = useState({
    email: location.state?.email || '',
    password: '',
  });

  const [infoMessage] = useState(location.state?.successMessage || '');
  const [fieldErrors, setFieldErrors] = useState({});
  const [formError, setFormError] = useState('');
  const [submitting, setSubmitting] = useState(false);

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData((prev) => ({ ...prev, [name]: value }));
    if (fieldErrors[name]) {
      setFieldErrors((prev) => ({ ...prev, [name]: '' }));
    }
    if (formError) setFormError('');
  };

  const validate = () => {
    const errors = {};
    if (!formData.email.trim()) {
      errors.email = 'College email is required.';
    } else if (!formData.email.includes('@')) {
      errors.email = 'Enter a valid college email format.';
    }
    if (!formData.password) {
      errors.password = 'Password is required.';
    }
    setFieldErrors(errors);
    return Object.keys(errors).length === 0;
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setFormError('');
    setFieldErrors({});

    if (!validate()) return;

    try {
      setSubmitting(true);
      const { sessionData } = await login({
        email: formData.email,
        password: formData.password,
      });

      // Destination routing based on real server onboardingCompleted flag
      if (sessionData && !sessionData.onboardingCompleted) {
        navigate('/onboarding', { replace: true });
      } else {
        const destination = location.state?.from?.pathname || '/dashboard';
        navigate(destination, { replace: true });
      }
    } catch (err) {
      if (err.status === 401) {
        setFormError('Invalid email or password. Please verify your credentials.');
      } else if (err.status === 403) {
        setFormError(
          err.message || 'This account is not currently active. Please contact support.'
        );
      } else {
        setFormError(err.message || 'Unable to sign in. Please try again.');
      }
      if (err.details) {
        setFieldErrors(err.details);
      }
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
            Sign In to VYNK
          </h1>
          <p className="card-subtitle">
            Access your verified academic and professional network
          </p>
        </div>

        {infoMessage && !formError && <Alert type="success" message={infoMessage} />}
        {formError && <Alert type="error" message={formError} details={fieldErrors} />}

        <form onSubmit={handleSubmit} noValidate>
          <InputField
            id="email"
            name="email"
            label="College Email"
            type="email"
            value={formData.email}
            onChange={handleChange}
            placeholder="student@college.edu"
            required
            error={fieldErrors.email}
            disabled={submitting}
            autoComplete="username"
          />

          <InputField
            id="password"
            name="password"
            label="Password"
            type="password"
            value={formData.password}
            onChange={handleChange}
            placeholder="Enter your password"
            required
            error={fieldErrors.password}
            disabled={submitting}
            autoComplete="current-password"
          />

          <div style={{ display: 'flex', justifyContent: 'flex-end', marginTop: '0.5rem' }}>
            <Link to="/forgot-password" style={{ fontSize: '0.85rem', color: 'var(--primary-color, #4f46e5)', fontWeight: 500 }}>
              Forgot password?
            </Link>
          </div>

          <div style={{ marginTop: '1.25rem' }}>
            <button
              type="submit"
              disabled={submitting}
              className="btn btn-primary btn-block"
            >
              {submitting ? <LoadingSpinner text="Signing in..." /> : 'Sign In'}
            </button>
          </div>
        </form>

        <div style={{ marginTop: '1.75rem', textAlign: 'center', fontSize: '0.875rem' }}>
          <span className="text-muted">New to VYNK? </span>
          <Link to="/register" style={{ fontWeight: 600 }}>
            Join with your college email
          </Link>
        </div>
      </div>
    </div>
  );
}
