import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { profileApi } from '../api/profile';
import { InputField } from '../components/forms/InputField';
import { SelectField } from '../components/forms/SelectField';
import { TextareaField } from '../components/forms/TextareaField';
import { TagsInputField } from '../components/forms/TagsInputField';
import { Alert } from '../components/common/Alert';
import { LoadingSpinner } from '../components/common/LoadingSpinner';
import { VynkLogo } from '../components/common/VynkLogo';
import {
  DEPARTMENT_OPTIONS,
  COURSE_OPTIONS,
  resolveDepartment,
  resolveCourse,
} from '../constants/academicOptions';

export function OnboardingPage() {
  const { user, refreshSession, updateProfileState } = useAuth();
  const navigate = useNavigate();

  const [formData, setFormData] = useState({
    fullName: '',
    department: '',
    customDepartment: '',
    course: '',
    customCourse: '',
    currentYear: '1',
    bio: '',
    discoverable: true,
    skills: [],
    interests: [],
    availability: '',
  });

  const [loadingInitial, setLoadingInitial] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [fieldErrors, setFieldErrors] = useState({});
  const [formError, setFormError] = useState('');

  // Fetch existing profile if already created (e.g. user revisiting onboarding)
  useEffect(() => {
    let mounted = true;
    (async () => {
      try {
        setLoadingInitial(true);
        const existing = await profileApi.getMyProfile();
        if (mounted && existing) {
          const deptResolved = resolveDepartment(existing.department);
          const courseResolved = resolveCourse(existing.course);

          setFormData({
            fullName: existing.fullName || '',
            department: deptResolved.selectedOption,
            customDepartment: deptResolved.customDepartment,
            course: courseResolved.selectedOption,
            customCourse: courseResolved.customCourse,
            currentYear: String(existing.currentYear || 1),
            bio: existing.bio || '',
            discoverable: existing.discoverable !== undefined ? existing.discoverable : true,
            skills: Array.isArray(existing.skills) ? existing.skills : [],
            interests: Array.isArray(existing.interests) ? existing.interests : [],
            availability: existing.availability || '',
          });
        }
      } catch (err) {
        console.warn('Profile fetch completed (empty profile):', err.message);
      } finally {
        if (mounted) setLoadingInitial(false);
      }
    })();

    return () => {
      mounted = false;
    };
  }, []);

  const handleChange = (e) => {
    const { name, value, type, checked } = e.target;
    setFormData((prev) => ({
      ...prev,
      [name]: type === 'checkbox' ? checked : value,
    }));
    if (fieldErrors[name]) {
      setFieldErrors((prev) => ({ ...prev, [name]: '' }));
    }
    if (name === 'department' && fieldErrors.customDepartment) {
      setFieldErrors((prev) => ({ ...prev, customDepartment: '' }));
    }
    if (name === 'course' && fieldErrors.customCourse) {
      setFieldErrors((prev) => ({ ...prev, customCourse: '' }));
    }
    if (formError) setFormError('');
  };

  const handleTagsChange = (field, newTags) => {
    setFormData((prev) => ({
      ...prev,
      [field]: newTags,
    }));
    if (formError) setFormError('');
  };

  const validate = () => {
    const errors = {};
    if (!formData.fullName.trim()) {
      errors.fullName = 'Full name is required.';
    } else if (formData.fullName.length > 150) {
      errors.fullName = 'Full name must not exceed 150 characters.';
    }

    // Department validation
    if (!formData.department) {
      errors.department = 'Department is required.';
    } else if (formData.department === 'Other') {
      if (!formData.customDepartment.trim()) {
        errors.customDepartment = 'Please specify your department.';
      } else if (formData.customDepartment.length > 100) {
        errors.customDepartment = 'Department must not exceed 100 characters.';
      }
    }

    // Course / Degree validation
    if (!formData.course) {
      errors.course = 'Course / Degree is required.';
    } else if (formData.course === 'Other') {
      if (!formData.customCourse.trim()) {
        errors.customCourse = 'Please specify your course or degree.';
      } else if (formData.customCourse.length > 100) {
        errors.customCourse = 'Course must not exceed 100 characters.';
      }
    }

    const yearNum = parseInt(formData.currentYear, 10);
    if (!formData.currentYear || isNaN(yearNum) || yearNum < 1 || yearNum > 6) {
      errors.currentYear = 'Current academic year must be between 1 and 6.';
    }

    if (formData.bio && formData.bio.length > 1000) {
      errors.bio = 'Bio must not exceed 1000 characters.';
    }

    if (formData.availability && formData.availability.length > 200) {
      errors.availability = 'Availability must not exceed 200 characters.';
    }

    setFieldErrors(errors);
    return Object.keys(errors).length === 0;
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setFormError('');
    setFieldErrors({});

    if (!validate()) return;

    const finalDepartment =
      formData.department === 'Other'
        ? formData.customDepartment.trim()
        : formData.department.trim();

    const finalCourse =
      formData.course === 'Other'
        ? formData.customCourse.trim()
        : formData.course.trim();

    try {
      setSubmitting(true);
      const savedProfile = await profileApi.updateMyProfile({
        fullName: formData.fullName,
        department: finalDepartment,
        course: finalCourse,
        currentYear: formData.currentYear,
        bio: formData.bio,
        discoverable: formData.discoverable,
        skills: formData.skills,
        interests: formData.interests,
        availability: formData.availability,
      });
      updateProfileState(savedProfile);
      await refreshSession();
      navigate('/dashboard', { replace: true });
    } catch (err) {
      if (err.details) {
        const mappedErrors = { ...err.details };
        if (err.details.department && formData.department === 'Other') {
          mappedErrors.customDepartment = err.details.department;
        }
        if (err.details.course && formData.course === 'Other') {
          mappedErrors.customCourse = err.details.course;
        }
        setFieldErrors(mappedErrors);
      }
      setFormError(err.message || 'Failed to complete profile onboarding. Please review the form.');
    } finally {
      setSubmitting(false);
    }
  };

  if (loadingInitial) {
    return (
      <div className="container-medium" style={{ paddingTop: '4rem', textAlign: 'center' }}>
        <LoadingSpinner size="lg" text="Loading onboarding form..." />
      </div>
    );
  }

  return (
    <div className="container-medium page-enter" style={{ paddingTop: '3rem', paddingBottom: '4rem' }}>
      {/* Onboarding Welcome Header Card */}
      <div className="profile-hero-card" style={{ marginBottom: '2rem' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '1rem', marginBottom: '1rem' }}>
          <VynkLogo variant="icon" size="md" />
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', flexWrap: 'wrap' }}>
            <span className="badge badge-dark">Step 1 of 1 • Onboarding</span>
            {user?.collegeName && (
              <span className="badge" title={`Verified College: ${user.collegeName}`}>
                {user.collegeName}
              </span>
            )}
          </div>
        </div>

        <h1 style={{ fontSize: '2rem', letterSpacing: '-0.03em', margin: '0 0 0.5rem 0' }}>
          Set Up Your Campus Profile
        </h1>
        <p style={{ margin: 0, fontSize: '0.95rem', color: 'var(--text-secondary)', lineHeight: 1.5 }}>
          Welcome to VYNK! Tell campus peers what you're studying and building so our matching engine can connect you with hackathon teammates, study buddies, and project collaborators.
        </p>
      </div>

      {formError && <Alert type="error" message={formError} details={fieldErrors} />}

      <form onSubmit={handleSubmit} noValidate>
        {/* Section 1: Academic Standing */}
        <div className="profile-section-card">
          <div className="profile-section-card-header">
            <div className="profile-section-icon" aria-hidden="true">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                <path d="M22 10v6M2 10l10-5 10 5-10 5z" />
                <path d="M6 12v5c3 3 9 3 12 0v-5" />
              </svg>
            </div>
            <div className="profile-section-title-group">
              <h2>1. Academic Standing</h2>
              <p>Your degree program and graduation timeline at {user?.collegeName || 'your college'}.</p>
            </div>
          </div>

          <InputField
            id="fullName"
            name="fullName"
            label="Full Name"
            value={formData.fullName}
            onChange={handleChange}
            placeholder="e.g. Alex Morgan"
            required
            error={fieldErrors.fullName}
            disabled={submitting}
            maxLength={150}
          />

          <div className="grid-2">
            <SelectField
              id="department"
              name="department"
              label="Department / Branch"
              value={formData.department}
              onChange={handleChange}
              options={DEPARTMENT_OPTIONS.map((dept) => ({ value: dept, label: dept }))}
              placeholder="Select department..."
              required
              error={fieldErrors.department}
              disabled={submitting}
            />

            <SelectField
              id="course"
              name="course"
              label="Course / Degree"
              value={formData.course}
              onChange={handleChange}
              options={COURSE_OPTIONS.map((c) => ({ value: c, label: c }))}
              placeholder="Select degree..."
              required
              error={fieldErrors.course}
              disabled={submitting}
            />
          </div>

          {/* Conditional "Other" inputs for Department or Course */}
          {(formData.department === 'Other' || formData.course === 'Other') && (
            <div className={formData.department === 'Other' && formData.course === 'Other' ? 'grid-2' : ''}>
              {formData.department === 'Other' && (
                <InputField
                  id="customDepartment"
                  name="customDepartment"
                  label="Specify Department / Branch"
                  value={formData.customDepartment}
                  onChange={handleChange}
                  placeholder="e.g. Chemical Engineering"
                  required
                  error={fieldErrors.customDepartment}
                  disabled={submitting}
                  maxLength={100}
                />
              )}

              {formData.course === 'Other' && (
                <InputField
                  id="customCourse"
                  name="customCourse"
                  label="Specify Course / Degree"
                  value={formData.customCourse}
                  onChange={handleChange}
                  placeholder="e.g. B.Arch, Integrated M.Sc."
                  required
                  error={fieldErrors.customCourse}
                  disabled={submitting}
                  maxLength={100}
                />
              )}
            </div>
          )}

          <SelectField
            id="currentYear"
            name="currentYear"
            label="Current Academic Year"
            value={formData.currentYear}
            onChange={handleChange}
            options={[
              { value: '1', label: '1st Year (Freshman)' },
              { value: '2', label: '2nd Year (Sophomore)' },
              { value: '3', label: '3rd Year (Junior)' },
              { value: '4', label: '4th Year (Senior)' },
              { value: '5', label: '5th Year (Dual / Advanced)' },
              { value: '6', label: '6th Year (Postgraduate / Ph.D.)' },
            ]}
            required
            error={fieldErrors.currentYear}
            disabled={submitting}
          />
        </div>

        {/* Section 2: Skills & Collaboration */}
        <div className="profile-section-card">
          <div className="profile-section-card-header">
            <div className="profile-section-icon" aria-hidden="true">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                <polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2" />
              </svg>
            </div>
            <div className="profile-section-title-group">
              <h2>2. Skills & Interests</h2>
              <p>What you bring to the table and what subjects you love exploring.</p>
            </div>
          </div>

          <TagsInputField
            id="skills"
            label="Skills & Tools"
            tags={formData.skills}
            onChange={(tags) => handleTagsChange('skills', tags)}
            placeholder="Add skills (e.g. React, Python, UI Design) and press Enter..."
            hint="Add your core skills (up to 25 tags)."
            maxTags={25}
            disabled={submitting}
          />

          <TagsInputField
            id="interests"
            label="Fields & Passions"
            tags={formData.interests}
            onChange={(tags) => handleTagsChange('interests', tags)}
            placeholder="Add interests (e.g. AI Agents, Web3, FinTech) and press Enter..."
            hint="Add topics you want to collaborate on (up to 25 tags)."
            maxTags={25}
            disabled={submitting}
          />

          <InputField
            id="availability"
            name="availability"
            label="Weekly Availability (Optional)"
            value={formData.availability}
            onChange={handleChange}
            placeholder="e.g. Weekday evenings (6–9 PM), Weekends"
            hint="General schedule when you're available for projects or study sessions."
            maxLength={200}
            error={fieldErrors.availability}
            disabled={submitting}
          />
        </div>

        {/* Section 3: Bio & Visibility */}
        <div className="profile-section-card">
          <div className="profile-section-card-header">
            <div className="profile-section-icon" aria-hidden="true">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z" />
                <polyline points="14 2 14 8 20 8" />
                <line x1="16" y1="13" x2="8" y2="13" />
                <line x1="16" y1="17" x2="8" y2="17" />
              </svg>
            </div>
            <div className="profile-section-title-group">
              <h2>3. Bio & Campus Visibility</h2>
              <p>Introduce yourself and choose how peers find your profile.</p>
            </div>
          </div>

          <TextareaField
            id="bio"
            name="bio"
            label="Short Bio / Focus"
            value={formData.bio}
            onChange={handleChange}
            placeholder="Tell campus peers what you're working on or what you're excited to learn next..."
            error={fieldErrors.bio}
            hint="Max 1000 characters."
            disabled={submitting}
            maxLength={1000}
            rows={4}
          />

          <label className="visibility-toggle-card">
            <input
              type="checkbox"
              id="discoverable"
              name="discoverable"
              checked={formData.discoverable}
              onChange={handleChange}
              disabled={submitting}
            />
            <div>
              <span style={{ fontWeight: 600, color: 'var(--text-primary)', display: 'block', fontSize: '0.95rem' }}>
                Keep my profile discoverable to verified campus peers
              </span>
              <p style={{ margin: '0.25rem 0 0', fontSize: '0.85rem', color: 'var(--text-muted)', lineHeight: 1.4 }}>
                Enables other students at {user?.collegeName || 'your college'} to discover your profile and match with you.
              </p>
            </div>
          </label>
        </div>

        <div style={{ marginTop: '2rem' }}>
          <button
            type="submit"
            disabled={submitting}
            className="btn btn-primary btn-block"
            style={{ padding: '0.85rem 1.5rem', fontSize: '1rem' }}
          >
            {submitting ? <LoadingSpinner text="Setting up your profile..." /> : 'Complete Setup & Launch Dashboard →'}
          </button>
        </div>
      </form>
    </div>
  );
}
