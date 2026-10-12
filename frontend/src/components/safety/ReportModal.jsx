import React, { useState } from 'react';
import { safetyApi } from '../../api/safety';
import { SelectField } from '../forms/SelectField';
import { TextareaField } from '../forms/TextareaField';
import { Alert } from '../common/Alert';
import { LoadingSpinner } from '../common/LoadingSpinner';
import { useModalFocus } from '../../hooks/useModalFocus';

const REPORT_REASONS = [
  { value: 'INAPPROPRIATE_BEHAVIOR', label: 'Inappropriate or Offensive Behavior' },
  { value: 'HARASSMENT', label: 'Harassment or Bullying' },
  { value: 'SPAM', label: 'Spamming or Unsolicited Promotion' },
  { value: 'IMPERSONATION', label: 'Impersonation or False Information' },
  { value: 'OTHER', label: 'Other Campus Safety Concern' },
];

export function ReportModal({ student, onClose, onSuccess }) {
  const [reason, setReason] = useState('INAPPROPRIATE_BEHAVIOR');
  const [description, setDescription] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState('');
  const [submitted, setSubmitted] = useState(false);

  const { modalRef, handleOverlayClick } = useModalFocus({
    isOpen: Boolean(student),
    onClose,
    submitting,
    initialFocusSelector: '#reportReason',
  });

  if (!student) return null;

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');

    const targetUserId = student.userId || student.id;
    if (!targetUserId) {
      setError('Invalid student record.');
      return;
    }

    try {
      setSubmitting(true);
      await safetyApi.reportUser(targetUserId, {
        reason,
        description: description.trim() || undefined,
      });
      setSubmitted(true);
      setTimeout(() => {
        if (onSuccess) onSuccess();
        onClose();
      }, 1500);
    } catch (err) {
      setError(err.message || 'Unable to submit report.');
      setSubmitting(false);
    }
  };

  return (
    <div
      role="dialog"
      aria-modal="true"
      aria-labelledby="report-modal-title"
      className="modal-overlay"
      onClick={handleOverlayClick}
    >
      <div
        ref={modalRef}
        className="card modal-card"
        style={{ maxWidth: '480px' }}
        onClick={(e) => e.stopPropagation()}
        tabIndex="-1"
      >
        <div className="card-header">
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
            <div>
              <h2 id="report-modal-title" className="card-title" style={{ fontSize: '1.25rem' }}>
                Report {student.fullName}
              </h2>
              <p className="card-subtitle">
                Help maintain a safe and respectful campus environment
              </p>
            </div>
            <button
              type="button"
              onClick={onClose}
              disabled={submitting}
              style={{
                background: 'none',
                border: 'none',
                fontSize: '1.5rem',
                lineHeight: 1,
                cursor: 'pointer',
                color: 'var(--text-muted)',
              }}
              aria-label="Close dialog"
            >
              &times;
            </button>
          </div>
        </div>

        {submitted ? (
          <Alert
            type="success"
            message="Report submitted safely. Our moderation team will review this incident."
          />
        ) : (
          <>
            {error && <Alert type="error" message={error} />}

            <form onSubmit={handleSubmit}>
              <SelectField
                id="reportReason"
                name="reportReason"
                label="Reason for Report"
                value={reason}
                onChange={(e) => setReason(e.target.value)}
                options={REPORT_REASONS}
                required
                disabled={submitting}
              />

              <TextareaField
                id="reportDescription"
                name="reportDescription"
                label="Details (Optional)"
                value={description}
                onChange={(e) => setDescription(e.target.value)}
                placeholder="Provide objective context regarding the behavior..."
                hint="Max 500 characters."
                maxLength={500}
                rows={3}
                disabled={submitting}
              />

              <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '0.75rem', marginTop: '1.5rem' }}>
                <button
                  type="button"
                  onClick={onClose}
                  disabled={submitting}
                  className="btn btn-secondary btn-sm"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={submitting}
                  className="btn btn-primary btn-sm"
                >
                  {submitting ? <LoadingSpinner text="Submitting..." /> : 'Submit Report'}
                </button>
              </div>
            </form>
          </>
        )}
      </div>
    </div>
  );
}
