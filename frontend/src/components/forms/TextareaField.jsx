import React from 'react';

export function TextareaField({
  id,
  name,
  label,
  value,
  onChange,
  placeholder = '',
  required = false,
  error = '',
  hint = '',
  disabled = false,
  maxLength = 1000,
  rows = 4,
}) {
  const currentLength = value ? value.length : 0;

  const handleChange = (e) => {
    if (maxLength !== undefined && maxLength !== null && e.target.value.length > maxLength) {
      e.target.value = e.target.value.slice(0, maxLength);
    }
    if (onChange) {
      onChange(e);
    }
  };

  const handlePaste = (e) => {
    const pastedText = e.clipboardData?.getData('text');
    if (pastedText && maxLength !== undefined && maxLength !== null) {
      const textarea = e.target;
      const currentVal = textarea.value || '';
      const start = textarea.selectionStart ?? currentVal.length;
      const end = textarea.selectionEnd ?? currentVal.length;
      const nextVal = currentVal.slice(0, start) + pastedText + currentVal.slice(end);
      if (nextVal.length > maxLength) {
        e.preventDefault();
        const availableSpace = maxLength - (currentVal.length - (end - start));
        const allowedPaste = pastedText.slice(0, Math.max(0, availableSpace));
        const finalVal = (currentVal.slice(0, start) + allowedPaste + currentVal.slice(end)).slice(0, maxLength);
        const syntheticEvent = {
          ...e,
          target: { ...textarea, value: finalVal },
        };
        if (onChange) {
          onChange(syntheticEvent);
        }
      }
    }
  };

  return (
    <div className="form-group">
      {label && (
        <label htmlFor={id || name} className="form-label">
          <span>
            {label} {required && <span style={{ color: 'var(--error-text)' }}>*</span>}
          </span>
          <span className="form-label-hint" aria-live="polite">
            {currentLength}/{maxLength}
          </span>
        </label>
      )}
      <textarea
        id={id || name}
        name={name}
        value={value}
        onChange={handleChange}
        onPaste={handlePaste}
        placeholder={placeholder}
        required={required}
        disabled={disabled}
        maxLength={maxLength}
        rows={rows}
        className={`form-textarea ${error ? 'has-error' : ''}`}
        aria-invalid={Boolean(error)}
        aria-describedby={error ? `${id || name}-error` : undefined}
      />
      {error && (
        <span id={`${id || name}-error`} className="form-error" role="alert">
          {error}
        </span>
      )}
      {hint && !error && <span className="form-hint">{hint}</span>}
    </div>
  );
}
