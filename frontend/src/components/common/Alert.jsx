import React from 'react';

export function Alert({ type = 'info', message, details = null, onRetry = null, className = '' }) {
  if (!message) return null;

  const typeClass =
    type === 'error'
      ? 'alert-error'
      : type === 'success'
      ? 'alert-success'
      : 'alert-info';

  return (
    <div className={`alert ${typeClass} ${className}`} role="alert">
      <div style={{ flex: 1 }}>
        <p style={{ margin: 0, fontWeight: 500 }}>{message}</p>
        {details && typeof details === 'object' && Object.keys(details).length > 0 && (
          <ul style={{ margin: '0.5rem 0 0 1.25rem', fontSize: '0.8rem', padding: 0 }}>
            {Object.entries(details).map(([key, val]) => (
              <li key={key}>
                <strong>{key}:</strong> {val}
              </li>
            ))}
          </ul>
        )}
      </div>
      {onRetry && (
        <button
          type="button"
          onClick={onRetry}
          className="btn btn-sm btn-secondary"
          style={{ marginLeft: 'auto', alignSelf: 'center' }}
        >
          Retry
        </button>
      )}
    </div>
  );
}
