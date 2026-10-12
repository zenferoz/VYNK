import React, { useState, useEffect } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { collegesApi } from '../api/colleges';
import { authApi } from '../api/auth';
import { InputField } from '../components/forms/InputField';
import { SelectField } from '../components/forms/SelectField';
import { Alert } from '../components/common/Alert';
import { LoadingSpinner } from '../components/common/LoadingSpinner';
import { VynkLogo } from '../components/common/VynkLogo';

export function RegisterPage() {
  const navigate = useNavigate();
  const [colleges, setColleges] = useState([]);
  const [collegesLoading, setCollegesLoading] = useState(true);
  const [collegesError, setCollegesError] = useState('');

  const [formData, setFormData] = useState({
    collegeId: '',
    email: '',
    password: '',
  });

  const [submitting, setSubmitting] = useState(false);
  const [fieldErrors, setFieldErrors] = useState({});
  const [formError, setFormError] = useState('');
  const [successResponse, setSuccessResponse] = useState(null);

  // Load active colleges on mount
  const loadColleges = React.useCallback(async () => {
    try {
      setCollegesLoading(true);
      setCollegesError('');
      const data = await collegesApi.getColleges();
      setColleges(data || []);
    } catch (err) {
      setCollegesError(err.message || 'Failed to load college list. Please try again.');
    } finally {
      setCollegesLoading(false);
    }
  }, []);

  useEffect(() => {
    loadColleges();
  }, [loadColleges]);

  const selectedCollege = colleges.find((c) => String(c.id) === String(formData.collegeId));

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData((prev) => ({ ...prev, [name]: value }));
    // Clear field-specific error upon typing
    if (fieldErrors[name]) {
      setFieldErrors((prev) => ({ ...prev, [name]: '' }));
    }
    if (formError) setFormError('');
  };

  const validate = () => {
    const errors = {};
    if (!formData.collegeId) {
      errors.collegeId = 'Please select your college.';
    }
    if (!formData.email.trim()) {
      errors.email = 'College email address is required.';
    } else if (!formData.email.includes('@')) {
      errors.email = 'Please enter a valid email format.';
    } else if (selectedCollege) {
      const emailDomain = formData.email.trim().split('@')[1];
      if (emailDomain && emailDomain.toLowerCase() !== selectedCollege.emailDomain.toLowerCase()) {
        errors.email = `Email domain must match @${selectedCollege.emailDomain}`;
      }
    }

    if (!formData.password) {
      errors.password = 'Password is required.';
    } else if (formData.password.length < 8 || formData.password.length > 72) {
      errors.password = 'Password must be between 8 and 72 characters.';
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
      const res = await authApi.register({
        email: formData.email,
        password: formData.password,
        collegeId: Number(formData.collegeId),
      });
      setSuccessResponse(res);
      navigate(`/verify-email?email=${encodeURIComponent(formData.email.trim())}`, {
        state: { email: formData.email.trim() }
      });
    } catch (err) {
      if (err.details) {
        setFieldErrors(err.details);
      }
      setFormError(err.message || 'Registration failed. Please review your information.');
    } finally {
      setSubmitting(false);
    }
  };

  // Verification Pending Screen upon successful registration
  if (successResponse) {
    return (
      <div className="container-narrow page-enter" style={{ paddingTop: '3.5rem', paddingBottom: '3.5rem' }}>
        <div className="card text-center">
          <div style={{ marginBottom: '1.25rem' }}>
            <span className="badge badge-dark">Step 1 of 2 Complete</span>
          </div>
          <h2 className="card-title">Check Your College Inbox</h2>
          <p style={{ marginTop: '0.75rem', fontSize: '0.95rem', color: 'var(--text-secondary)' }}>
            {successResponse.message || 'Registration successful. Verification code sent.'}
          </p>
          <div className="alert alert-info" style={{ marginTop: '1.5rem', textAlign: 'left' }}>
            <div>
              <strong>Next Step:</strong> We sent a 6-digit verification code to{' '}
              <strong>{formData.email}</strong>. Enter the code to activate your account.
            </div>
          </div>
          <div style={{ marginTop: '2rem' }}>
            <Link
              to={`/verify-email?email=${encodeURIComponent(formData.email.trim())}`}
              state={{ email: formData.email.trim() }}
              className="btn btn-primary btn-block"
            >
              Enter Verification Code &rarr;
            </Link>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="container-narrow page-enter" style={{ paddingTop: '3rem', paddingBottom: '3.5rem' }}>
      <div className="card text-center">
        <div className="card-header" style={{ display: 'flex', flexDirection: 'column', alignItems: 'center' }}>
          <div style={{ marginBottom: '1rem' }}>
            <VynkLogo variant="icon" size="lg" />
          </div>
          <h1 className="card-title" style={{ fontSize: '1.75rem' }}>
            Create Your VYNK Account
          </h1>
          <p className="card-subtitle">
            Join your verified campus network. Only students with an official college email can register.
          </p>
        </div>

        {collegesError && (
          <Alert
            type="error"
            message={collegesError}
            onRetry={loadColleges}
          />
        )}

        {formError && <Alert type="error" message={formError} details={fieldErrors} />}

        <form onSubmit={handleSubmit} noValidate>
          {collegesLoading ? (
            <div style={{ padding: '1rem 0', display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
              <LoadingSpinner size="sm" text="Loading participating colleges..." />
            </div>
          ) : (
            <SelectField
              id="collegeId"
              name="collegeId"
              label="Select Your College"
              value={formData.collegeId}
              onChange={handleChange}
              options={colleges.map((c) => ({
                value: c.id,
                label: `${c.name} (@${c.emailDomain})`,
              }))}
              placeholder="Select your college institution..."
              required
              error={fieldErrors.collegeId}
              hint="Only approved institutions are listed"
              disabled={submitting}
            />
          )}

          <InputField
            id="email"
            name="email"
            label="College Email"
            type="email"
            value={formData.email}
            onChange={handleChange}
            placeholder={selectedCollege ? `student@${selectedCollege.emailDomain}` : 'Enter your institutional email'}
            required
            error={fieldErrors.email}
            hint={selectedCollege ? `Must end in @${selectedCollege.emailDomain}` : ''}
            disabled={submitting}
            autoComplete="email"
          />

          <InputField
            id="password"
            name="password"
            label="Password"
            type="password"
            value={formData.password}
            onChange={handleChange}
            placeholder="Choose a strong password (min. 8 characters)"
            required
            error={fieldErrors.password}
            hint="8 to 72 characters"
            disabled={submitting}
            autoComplete="new-password"
          />

          <div style={{ marginTop: '1.75rem' }}>
            <button
              type="submit"
              disabled={submitting || collegesLoading}
              className="btn btn-primary btn-block"
            >
              {submitting ? <LoadingSpinner text="Creating Account..." /> : 'Register with College Email'}
            </button>
          </div>
        </form>

        <div style={{ marginTop: '1.75rem', textAlign: 'center', fontSize: '0.875rem' }}>
          <span className="text-muted">Already have a verified account? </span>
          <Link to="/login" style={{ fontWeight: 600 }}>
            Sign in
          </Link>
        </div>
      </div>
    </div>
  );
}
