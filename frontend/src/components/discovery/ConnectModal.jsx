import React, { useState } from 'react';
import { connectionsApi } from '../../api/connections';
import { TextareaField } from '../forms/TextareaField';
import { Alert } from '../common/Alert';
import { LoadingSpinner } from '../common/LoadingSpinner';
import { useModalFocus } from '../../hooks/useModalFocus';

export function ConnectModal({ student, onClose, onSuccess }) {
  const [purposeNote, setPurposeNote] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState('');

  const handlePurposeNoteChange = (e) => {
    const val = e.target.value;
    if (val.length > 300) {
      setPurposeNote(val.slice(0, 300));
    } else {
      setPurposeNote(val);
    }
  };

  const { modalRef, handleOverlayClick } = useModalFocus({
    isOpen: Boolean(student),
    onClose,
    submitting,
    initialFocusSelector: '#purposeNote',
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
      await connectionsApi.sendRequest({
        receiverId: targetUserId,
        purposeNote: purposeNote.trim() || undefined,
      });
      if (onSuccess) onSuccess();
      onClose();
    } catch (err) {
      setError(err.message || 'Unable to send connection request.');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div
      role="dialog"
      aria-modal="true"
      aria-labelledby="connect-modal-title"
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
              <h2 id="connect-modal-title" className="card-title" style={{ fontSize: '1.25rem' }}>
                Connect with {student.fullName}
              </h2>
              <p className="card-subtitle">
                {student.department} {student.currentYear ? `• Year ${student.currentYear}` : ''}
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

        {error && <Alert type="error" message={error} />}

        <form onSubmit={handleSubmit}>
          <TextareaField
            id="purposeNote"
            name="purposeNote"
            label="Purpose Note (Optional)"
            value={purposeNote}
            onChange={handlePurposeNoteChange}
            placeholder="Share why you'd like to connect (e.g. project collaboration, study partner)..."
            hint="Max 300 characters. Senders are limited to 15 active outgoing requests."
            maxLength={300}
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
              {submitting ? <LoadingSpinner text="Sending..." /> : 'Send Request'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
