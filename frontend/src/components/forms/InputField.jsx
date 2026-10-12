import React, { useState } from 'react';

export function InputField({
  id,
  name,
  label,
  type = 'text',
  value,
  onChange,
  placeholder = '',
  required = false,
  error = '',
  hint = '',
  disabled = false,
  autoComplete = 'off',
  maxLength = undefined,
  min = undefined,
  max = undefined,
}) {
  const [showPassword, setShowPassword] = useState(false);
  const isPasswordField = type === 'password';
  const effectiveType = isPasswordField ? (showPassword ? 'text' : 'password') : type;

  const inputElement = (
    <input
      id={id || name}
      name={name}
      type={effectiveType}
      value={value}
      onChange={onChange}
      placeholder={placeholder}
      required={required}
      disabled={disabled}
      autoComplete={autoComplete}
      maxLength={maxLength}
      min={min}
      max={max}
      className={`form-input ${error ? 'has-error' : ''}`}
      aria-invalid={Boolean(error)}
      aria-describedby={error ? `${id || name}-error` : undefined}
    />
  );

  return (
    <div className="form-group">
      {label && (
        <label htmlFor={id || name} className="form-label">
          <span>
            {label} {required && <span style={{ color: 'var(--error-text)' }}>*</span>}
          </span>
          {hint && <span className="form-label-hint">{hint}</span>}
        </label>
      )}

      {isPasswordField ? (
        <div className="input-with-action">
          {inputElement}
          <button
            type="button"
            onClick={() => setShowPassword((prev) => !prev)}
            className="input-action-btn"
            aria-label={showPassword ? 'Hide password' : 'Show password'}
            title={showPassword ? 'Hide password' : 'Show password'}
            disabled={disabled}
            tabIndex={0}
          >
            {showPassword ? (
              // Eye-off icon
              <svg
                width="18"
                height="18"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                strokeWidth="2"
                strokeLinecap="round"
                strokeLinejoin="round"
                aria-hidden="true"
              >
                <path d="M9.88 9.88a3 3 0 1 0 4.24 4.24" />
                <path d="M10.73 5.08A10.43 10.43 0 0 1 12 5c7 0 10 7 10 7a13.16 13.16 0 0 1-1.67 2.68" />
                <path d="M6.61 6.61A13.526 13.526 0 0 0 2 12s3 7 10 7a9.74 9.74 0 0 0 5.39-1.61" />
                <line x1="2" x2="22" y1="2" y2="22" />
              </svg>
            ) : (
              // Eye icon
              <svg
                width="18"
                height="18"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                strokeWidth="2"
                strokeLinecap="round"
                strokeLinejoin="round"
                aria-hidden="true"
              >
                <path d="M2 12s3-7 10-7 10 7 10 7-3 7-10 7-10-7-10-7Z" />
                <circle cx="12" cy="12" r="3" />
              </svg>
            )}
          </button>
        </div>
      ) : (
        inputElement
      )}

      {error && (
        <span id={`${id || name}-error`} className="form-error" role="alert">
          {error}
        </span>
      )}
    </div>
  );
}
