import React, { useState, useEffect, useCallback } from 'react';
import { goalsApi } from '../../api/goals';
import { SelectField } from '../forms/SelectField';
import { TextareaField } from '../forms/TextareaField';
import { TagsInputField } from '../forms/TagsInputField';
import { Alert } from '../common/Alert';
import { LoadingSpinner } from '../common/LoadingSpinner';

const PURPOSE_OPTIONS = [
  { value: 'PROJECT_COLLABORATION', label: 'Project Collaboration' },
  { value: 'HACKATHON_TEAM', label: 'Hackathon Team' },
  { value: 'STUDY_PARTNER', label: 'Study Partner' },
  { value: 'RESEARCH', label: 'Academic Research' },
  { value: 'PLACEMENT_PREP', label: 'Placement & Interview Prep' },
  { value: 'CASUAL_NETWORKING', label: 'Casual Networking' },
];

export function GoalManager() {
  const [goals, setGoals] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  // Form states for adding goal
  const [showAddForm, setShowAddForm] = useState(false);
  const [purpose, setPurpose] = useState('PROJECT_COLLABORATION');
  const [description, setDescription] = useState('');
  const [skills, setSkills] = useState([]);
  const [targetYear, setTargetYear] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [formError, setFormError] = useState('');

  const fetchGoals = useCallback(async () => {
    try {
      setLoading(true);
      setError('');
      const data = await goalsApi.getMyGoals();
      setGoals(Array.isArray(data) ? data : []);
    } catch (err) {
      setError(err.message || 'Unable to load networking goals.');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchGoals();
  }, [fetchGoals]);

  const handleCreate = async (e) => {
    e.preventDefault();
    setFormError('');
    setSuccess('');

    if (!purpose) {
      setFormError('Please select a networking purpose.');
      return;
    }

    try {
      setSubmitting(true);
      const payload = {
        purpose,
        description: description.trim() || undefined,
        skills,
        targetYear: targetYear ? parseInt(targetYear, 10) : undefined,
        active: true,
      };
      await goalsApi.createGoal(payload);
      setSuccess('Networking goal created successfully.');
      setShowAddForm(false);
      setDescription('');
      setSkills([]);
      setTargetYear('');
      fetchGoals();
    } catch (err) {
      setFormError(err.message || 'Failed to create networking goal.');
    } finally {
      setSubmitting(false);
    }
  };

  const handleDeactivate = async (goalId) => {
    if (!window.confirm('Are you sure you want to deactivate this networking goal? It will no longer be considered for purpose-based matching.')) {
      return;
    }
    try {
      setError('');
      setSuccess('');
      await goalsApi.deactivateGoal(goalId);
      setSuccess('Networking goal deactivated.');
      fetchGoals();
    } catch (err) {
      setError(err.message || 'Failed to deactivate goal.');
    }
  };

  return (
    <div className="card" style={{ marginTop: '2rem' }}>
      <div className="card-header">
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '0.5rem' }}>
          <div>
            <h2 className="card-title" style={{ fontSize: '1.4rem' }}>
              Active Networking Goals
            </h2>
            <p className="card-subtitle">
              Declare what you are seeking collaborators or partners for to drive purpose-based matching.
            </p>
          </div>
          {!showAddForm && (
            <button
              type="button"
              onClick={() => setShowAddForm(true)}
              className="btn btn-sm btn-primary"
            >
              + Add New Goal
            </button>
          )}
        </div>
      </div>

      {success && <Alert type="success" message={success} />}
      {error && <Alert type="error" message={error} />}

      {/* Add Goal Form */}
      {showAddForm && (
        <form
          onSubmit={handleCreate}
          style={{
            backgroundColor: 'var(--bg-secondary)',
            padding: '1.25rem',
            borderRadius: 'var(--radius-md)',
            marginBottom: '1.5rem',
            border: '1px solid var(--border-medium)',
          }}
        >
          <h3 style={{ fontSize: '1.1rem', marginBottom: '1rem' }}>Create Networking Goal</h3>
          {formError && <Alert type="error" message={formError} />}

          <div className="grid-2">
            <SelectField
              id="goalPurpose"
              name="goalPurpose"
              label="Goal Purpose Category"
              value={purpose}
              onChange={(e) => setPurpose(e.target.value)}
              options={PURPOSE_OPTIONS}
              required
              disabled={submitting}
            />

            <SelectField
              id="goalTargetYear"
              name="goalTargetYear"
              label="Target Academic Year (Optional)"
              value={targetYear}
              onChange={(e) => setTargetYear(e.target.value)}
              options={[
                { value: '', label: 'Any Year' },
                { value: '1', label: '1st Year' },
                { value: '2', label: '2nd Year' },
                { value: '3', label: '3rd Year' },
                { value: '4', label: '4th Year' },
                { value: '5', label: '5th Year' },
                { value: '6', label: '6th Year' },
              ]}
              disabled={submitting}
            />
          </div>

          <TextareaField
            id="goalDescription"
            name="goalDescription"
            label="Short Description (Optional)"
            value={description}
            onChange={(e) => setDescription(e.target.value)}
            placeholder="Briefly describe what you are looking to build or study..."
            maxLength={255}
            rows={2}
            disabled={submitting}
          />

          <TagsInputField
            id="goalSkills"
            label="Relevant Skills for this Goal (Optional)"
            tags={skills}
            onChange={setSkills}
            placeholder="e.g. PyTorch, Next.js, Figma..."
            hint="Add up to 15 skills relevant to this networking goal."
            maxTags={15}
            disabled={submitting}
          />

          <div style={{ display: 'flex', gap: '0.75rem', marginTop: '1rem' }}>
            <button
              type="submit"
              disabled={submitting}
              className="btn btn-sm btn-primary"
            >
              {submitting ? <LoadingSpinner text="Saving..." /> : 'Save Goal'}
            </button>
            <button
              type="button"
              onClick={() => {
                setShowAddForm(false);
                setFormError('');
              }}
              disabled={submitting}
              className="btn btn-sm btn-subtle"
            >
              Cancel
            </button>
          </div>
        </form>
      )}

      {/* Goals List */}
      {loading ? (
        <div style={{ padding: '2rem 0', textAlign: 'center' }}>
          <LoadingSpinner text="Loading goals..." />
        </div>
      ) : goals.length === 0 ? (
        <div style={{ textAlign: 'center', padding: '2rem 1rem', color: 'var(--text-secondary)' }}>
          <p style={{ margin: 0, fontStyle: 'italic' }}>
            No networking goals added yet. Add a goal above to get matched with peers based on shared purpose!
          </p>
        </div>
      ) : (
        <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
          {goals.map((g) => {
            const purposeLabel = PURPOSE_OPTIONS.find((opt) => opt.value === g.purpose)?.label || g.purpose;
            return (
              <div
                key={g.id}
                className="card student-card"
                style={{
                  padding: '1.25rem',
                  backgroundColor: g.active ? 'var(--bg-primary)' : 'var(--bg-subtle)',
                  opacity: g.active ? 1 : 0.65,
                }}
              >
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', flexWrap: 'wrap', gap: '0.5rem' }}>
                  <div>
                    <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginBottom: '0.35rem' }}>
                      <strong style={{ fontSize: '1rem' }}>{purposeLabel}</strong>
                      <span className={`badge ${g.active ? 'badge-dark' : ''}`}>
                        {g.active ? 'Active' : 'Inactive'}
                      </span>
                      {g.targetYear && (
                        <span className="badge" style={{ fontSize: '0.75rem' }}>
                          Target Year {g.targetYear}
                        </span>
                      )}
                    </div>
                    {g.description && (
                      <p style={{ margin: '0.25rem 0 0.5rem', fontSize: '0.875rem', color: 'var(--text-secondary)' }}>
                        {g.description}
                      </p>
                    )}
                    {Array.isArray(g.skills) && g.skills.length > 0 && (
                      <div style={{ display: 'flex', gap: '0.35rem', flexWrap: 'wrap', marginTop: '0.4rem' }}>
                        {g.skills.map((s, idx) => (
                          <span key={idx} className="badge" style={{ fontSize: '0.75rem' }}>
                            {s}
                          </span>
                        ))}
                      </div>
                    )}
                  </div>

                  {g.active && (
                    <button
                      type="button"
                      onClick={() => handleDeactivate(g.id)}
                      className="btn btn-sm btn-subtle"
                      style={{ fontSize: '0.75rem', color: 'var(--error-text)' }}
                    >
                      Deactivate
                    </button>
                  )}
                </div>
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
}
