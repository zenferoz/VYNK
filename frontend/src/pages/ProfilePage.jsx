import React, { useState, useEffect, useMemo } from 'react';
import { useAuth } from '../context/AuthContext';
import { profileApi } from '../api/profile';
import { InputField } from '../components/forms/InputField';
import { SelectField } from '../components/forms/SelectField';
import { TextareaField } from '../components/forms/TextareaField';
import { TagsInputField } from '../components/forms/TagsInputField';
import { GoalManager } from '../components/profile/GoalManager';
import { Alert } from '../components/common/Alert';
import { LoadingSpinner } from '../components/common/LoadingSpinner';
import {
  DEPARTMENT_OPTIONS,
  COURSE_OPTIONS,
  resolveDepartment,
  resolveCourse,
} from '../constants/academicOptions';

export function ProfilePage() {
  const { user, updateProfileState } = useAuth();

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

  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [fieldErrors, setFieldErrors] = useState({});
  const [formError, setFormError] = useState('');
  const [successMessage, setSuccessMessage] = useState('');

  // Fetch latest profile state from server
  useEffect(() => {
    let mounted = true;
    (async () => {
      try {
        setLoading(true);
        const data = await profileApi.getMyProfile();
        if (mounted && data) {
          const deptResolved = resolveDepartment(data.department);
          const courseResolved = resolveCourse(data.course);

          setFormData({
            fullName: data.fullName || '',
            department: deptResolved.selectedOption,
            customDepartment: deptResolved.customDepartment,
            course: courseResolved.selectedOption,
            customCourse: courseResolved.customCourse,
            currentYear: String(data.currentYear || 1),
            bio: data.bio || '',
            discoverable: data.discoverable !== undefined ? data.discoverable : true,
            skills: Array.isArray(data.skills) ? data.skills : [],
            interests: Array.isArray(data.interests) ? data.interests : [],
            availability: data.availability || '',
          });
          updateProfileState(data);
        }
      } catch (err) {
        if (mounted) {
          setFormError(err.message || 'Unable to load profile data.');
        }
      } finally {
        if (mounted) setLoading(false);
      }
    })();

    return () => {
      mounted = false;
    };
  }, [updateProfileState]);

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
    if (successMessage) setSuccessMessage('');
  };

  const handleTagsChange = (field, newTags) => {
    setFormData((prev) => ({
      ...prev,
      [field]: newTags,
    }));
    if (formError) setFormError('');
    if (successMessage) setSuccessMessage('');
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
    setSuccessMessage('');
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
      const updated = await profileApi.updateMyProfile({
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
      updateProfileState(updated);
      setSuccessMessage('Profile updated successfully.');
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
      setFormError(err.message || 'Failed to update profile. Please verify your input.');
    } finally {
      setSubmitting(false);
    }
  };

  // Student Initials
  const initials = (formData.fullName || user?.email || 'Student')
    .trim()
    .split(/\s+/)
    .map((w) => w[0])
    .filter(Boolean)
    .join('')
    .slice(0, 2)
    .toUpperCase();

  // Academic standing label
  const standingLabels = {
    '1': '1st Year • Freshman',
    '2': '2nd Year • Sophomore',
    '3': '3rd Year • Junior',
    '4': '4th Year • Senior',
    '5': '5th Year • Advanced',
    '6': 'Postgraduate / Ph.D.',
  };
  const standingLabel = standingLabels[formData.currentYear] || 'Enrolled Student';

  // Dynamic Profile Completeness Computation
  const completeness = useMemo(() => {
    let score = 0;
    if (formData.fullName.trim()) score += 20;
    if (formData.department && (formData.department !== 'Other' || formData.customDepartment.trim())) score += 15;
    if (formData.course && (formData.course !== 'Other' || formData.customCourse.trim())) score += 15;
    if (formData.skills.length > 0) score += 20;
    if (formData.interests.length > 0) score += 10;
    if (formData.bio.trim()) score += 10;
    if (formData.availability.trim()) score += 10;
    return Math.min(100, score);
  }, [formData]);

  if (loading) {
    return (
      <div className="container-medium" style={{ paddingTop: '4rem', textAlign: 'center' }}>
        <LoadingSpinner size="lg" text="Loading your student profile..." />
      </div>
    );
  }

  return (
    <div className="container-medium page-enter" style={{ paddingTop: '2.5rem', paddingBottom: '4rem' }}>
      {/* Top Banner & Identity Overview Card */}
      <div className="profile-hero-card" style={{ marginBottom: '1.75rem' }}>
        <div className="profile-hero-inner">
          <div className="profile-identity-group">
            <div className="profile-avatar-large" aria-hidden="true">
              {initials}
              {formData.discoverable && (
                <span className="profile-avatar-badge" title="Discoverable on campus" />
              )}
            </div>
            <div className="profile-hero-details">
              <div className="profile-badges-row">
                <span className="badge badge-dark">Student Profile</span>
                {user?.collegeName && (
                  <span className="badge" title={`Verified College: ${user.collegeName}`}>
                    {user.collegeName}
                  </span>
                )}
                <span className="badge">
                  {standingLabel}
                </span>
                {formData.discoverable ? (
                  <span
                    className="badge"
                    style={{
                      color: 'var(--success-text)',
                      borderColor: 'var(--success-border)',
                      backgroundColor: 'var(--success-bg)',
                    }}
                  >
                    ● Discoverable
                  </span>
                ) : (
                  <span className="badge" style={{ color: 'var(--text-muted)' }}>
                    Hidden from Directory
                  </span>
                )}
              </div>
              <h1>
                {formData.fullName.trim() || 'Your Student Profile'}
              </h1>
              <p style={{ margin: 0, fontSize: '0.875rem', color: 'var(--text-secondary)' }}>
                {user?.email} • {formData.department || 'Select Department'}
              </p>
            </div>
          </div>

          {/* Profile Completeness Box */}
          <div className="profile-completeness-box">
            <div className="profile-completeness-header">
              <span>Profile Strength</span>
              <span className="profile-completeness-pct">{completeness}%</span>
            </div>
            <div className="profile-progress-track">
              <div
                className="profile-progress-bar"
                style={{ width: `${completeness}%` }}
                role="progressbar"
                aria-valuenow={completeness}
                aria-valuemin="0"
                aria-valuemax="100"
              />
            </div>
            <p className="profile-completeness-hint">
              {completeness < 70
                ? 'Add your skills, interests, and bio to rank higher in campus purpose matches.'
                : 'Your profile looks solid and ready for peer collaboration!'}
            </p>
          </div>
        </div>
      </div>

      {successMessage && <Alert type="success" message={successMessage} />}
      {formError && <Alert type="error" message={formError} details={fieldErrors} />}

      <form onSubmit={handleSubmit} noValidate>
        {/* Section 1: Academic Identity */}
        <div className="profile-section-card">
          <div className="profile-section-card-header">
            <div className="profile-section-icon" aria-hidden="true">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                <path d="M22 10v6M2 10l10-5 10 5-10 5z" />
                <path d="M6 12v5c3 3 9 3 12 0v-5" />
              </svg>
            </div>
            <div className="profile-section-title-group">
              <h2>Academic Identity</h2>
              <p>Your verified institutional department, degree program, and standing at {user?.collegeName || 'your college'}.</p>
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
                  placeholder="e.g. Aerospace Engineering"
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
                  placeholder="e.g. B.Des, Integrated M.Tech"
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

        {/* Section 2: Bio & Campus Story */}
        <div className="profile-section-card">
          <div className="profile-section-card-header">
            <div className="profile-section-icon" aria-hidden="true">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z" />
                <polyline points="14 2 14 8 20 8" />
                <line x1="16" y1="13" x2="8" y2="13" />
                <line x1="16" y1="17" x2="8" y2="17" />
                <polyline points="10 9 9 9 8 9" />
              </svg>
            </div>
            <div className="profile-section-title-group">
              <h2>Bio & Campus Story</h2>
              <p>Introduce yourself to potential study partners, hackathon teammates, and research collaborators.</p>
            </div>
          </div>

          <TextareaField
            id="bio"
            name="bio"
            label="Bio / Academic Focus"
            value={formData.bio}
            onChange={handleChange}
            placeholder="Share what you're passionate about, current side-projects, or what kind of collaborators you're looking for..."
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
                When checked, students at {user?.collegeName || 'your college'} can find you in the campus directory and matching engine. Uncheck to browse privately.
              </p>
            </div>
          </label>
        </div>

        {/* Section 3: Skills, Passions & Schedule */}
        <div className="profile-section-card">
          <div className="profile-section-card-header">
            <div className="profile-section-icon" aria-hidden="true">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                <polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2" />
              </svg>
            </div>
            <div className="profile-section-title-group">
              <h2>Skills, Passions & Availability</h2>
              <p>Your technical competencies, intellectual topics, and weekly collaboration schedule.</p>
            </div>
          </div>

          <TagsInputField
            id="skills"
            label="Core Skills & Technologies"
            tags={formData.skills}
            onChange={(tags) => handleTagsChange('skills', tags)}
            placeholder="Add skills (e.g. React, Python, UI Design, Robotics) and press Enter..."
            hint="Add up to 25 skills you can contribute to projects."
            maxTags={25}
            disabled={submitting}
          />

          <TagsInputField
            id="interests"
            label="Academic & Creative Interests"
            tags={formData.interests}
            onChange={(tags) => handleTagsChange('interests', tags)}
            placeholder="Add topics (e.g. AI Agents, FinTech, Open Source, Startups) and press Enter..."
            hint="Add up to 25 fields or subjects you love exploring."
            maxTags={25}
            disabled={submitting}
          />

          <InputField
            id="availability"
            name="availability"
            label="Weekly Availability (Optional)"
            value={formData.availability}
            onChange={handleChange}
            placeholder="e.g. Weekday evenings (6–9 PM), Saturday hackathons, flexible"
            hint="Help potential collaborators know when you're available to meet or build."
            maxLength={200}
            error={fieldErrors.availability}
            disabled={submitting}
          />
        </div>

        {/* Sticky Profile Save Bar */}
        <div className="profile-save-bar">
          <div>
            <span style={{ fontSize: '0.9rem', fontWeight: 600, color: 'var(--text-primary)' }}>
              Ready to update your presence?
            </span>
            <p style={{ margin: 0, fontSize: '0.8rem', color: 'var(--text-muted)' }}>
              Changes are immediately reflected in campus matching and directory search.
            </p>
          </div>
          <div style={{ minWidth: '180px' }}>
            <button
              type="submit"
              disabled={submitting}
              className="btn btn-primary btn-block"
            >
              {submitting ? <LoadingSpinner text="Saving Profile..." /> : 'Save Profile Changes'}
            </button>
          </div>
        </div>
      </form>

      {/* Networking Goals Management Hub */}
      <div style={{ marginTop: '2.5rem' }}>
        <GoalManager />
      </div>
    </div>
  );
}
