import React from 'react';

export function SelectField({
  id,
  name,
  label,
  value,
  onChange,
  options = [],
  placeholder = 'Select an option...',
  required = false,
  error = '',
  hint = '',
  disabled = false,
}) {
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
      <select
        id={id || name}
        name={name}
        value={value}
        onChange={onChange}
        required={required}
        disabled={disabled}
        className={`form-select ${error ? 'has-error' : ''}`}
        aria-invalid={Boolean(error)}
        aria-describedby={error ? `${id || name}-error` : undefined}
      >
        <option value="" disabled>
          {placeholder}
        </option>
        {options.map((opt) => (
          <option key={opt.value} value={opt.value}>
            {opt.label}
          </option>
        ))}
      </select>
      {error && (
        <span id={`${id || name}-error`} className="form-error" role="alert">
          {error}
        </span>
      )}
    </div>
  );
}
