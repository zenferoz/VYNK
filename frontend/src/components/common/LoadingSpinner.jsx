import React from 'react';

export function LoadingSpinner({ size = 'md', text = '' }) {
  const isLarge = size === 'lg';
  return (
    <div style={{ display: 'inline-flex', alignItems: 'center', gap: '0.6rem' }}>
      <span
        className={`spinner ${isLarge ? 'spinner-lg' : ''}`}
        aria-hidden="true"
      />
      {text && <span style={{ fontSize: isLarge ? '1rem' : '0.85rem' }}>{text}</span>}
    </div>
  );
}

export function LoadingScreen({ message = 'Loading VYNK...' }) {
  return (
    <div className="loading-screen" role="status" aria-live="polite">
      <LoadingSpinner size="lg" />
      <p style={{ marginTop: '0.75rem', fontWeight: 500 }}>{message}</p>
    </div>
  );
}
