import React, { useState } from 'react';
import { safetyApi } from '../../api/safety';
import { Alert } from '../common/Alert';
import { LoadingSpinner } from '../common/LoadingSpinner';
import { useModalFocus } from '../../hooks/useModalFocus';

export function BlockConfirmModal({ student, onClose, onSuccess }) {
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState('');

  const { modalRef, handleOverlayClick } = useModalFocus({
    isOpen: Boolean(student),
    onClose,
    submitting,
    initialFocusSelector: 'button.btn-secondary',
  });

  if (!student) return null;

  const handleBlock = async () => {
    setError('');
    const targetUserId = student.userId || student.id;
    if (!targetUserId) {
      setError('Invalid student record.');
      return;
    }

    try {
      setSubmitting(true);
      await safetyApi.blockUser(targetUserId);
      if (onSuccess) onSuccess();
      onClose();
    } catch (err) {
      setError(err.message || 'Unable to block student.');
      setSubmitting(false);
    }
  };

  return (
    <div
      role="dialog"
      aria-modal="true"
      aria-labelledby="block-modal-title"
      className="modal-overlay"
      onClick={handleOverlayClick}
    >
      <div
        ref={modalRef}
        className="card modal-card"
        style={{ maxWidth: '440px' }}
        onClick={(e) => e.stopPropagation()}
        tabIndex="-1"
      >
        <div className="card-header">
          <h2 id="block-modal-title" className="card-title" style={{ fontSize: '1.25rem' }}>
            Block {student.fullName}?
          </h2>
          <p className="card-subtitle">
            Safety & Mutual Exclusion
          </p>
        </div>

        {error && <Alert type="error" message={error} />}

        <p style={{ fontSize: '0.9rem', color: 'var(--text-secondary)', lineHeight: 1.5, margin: '0 0 1.25rem' }}>
          Blocking will permanently prevent both of you from seeing each other in the directory, purpose matches, or sending connection requests. Any active connections will be isolated.
        </p>

        <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '0.75rem' }}>
          <button
            type="button"
            onClick={onClose}
            disabled={submitting}
            className="btn btn-secondary btn-sm"
          >
            Cancel
          </button>
          <button
            type="button"
            onClick={handleBlock}
            disabled={submitting}
            className="btn btn-sm btn-danger-solid"
          >
            {submitting ? <LoadingSpinner text="Blocking..." /> : 'Block Student'}
          </button>
        </div>
      </div>
    </div>
  );
}
