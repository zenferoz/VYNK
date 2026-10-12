import React, { useState } from 'react';

export function TagsInputField({
  id,
  label,
  tags = [],
  onChange,
  placeholder = 'Add a tag and press Enter...',
  maxTags = 15,
  maxTagLength = 50,
  error,
  hint,
  disabled = false,
}) {
  const [inputValue, setInputValue] = useState('');
  const [localError, setLocalError] = useState('');

  const processRawTags = (text) => {
    if (!text) return [];
    return text
      .split(/[,;\n]+/)
      .map((t) => t.trim().replace(/^,+|,+$/g, ''))
      .filter(Boolean);
  };

  const handleKeyDown = (e) => {
    if (e.key === 'Enter' || e.key === ',') {
      e.preventDefault();
      addTag();
    }
  };

  const addTag = () => {
    if (!inputValue.trim()) return;
    addTagsFromText(inputValue);
  };

  const addTagsFromText = (text) => {
    const rawItems = processRawTags(text);
    if (rawItems.length === 0) return;

    if (tags.length >= maxTags) {
      setLocalError(`Maximum ${maxTags} tags allowed.`);
      return;
    }

    // Single item entry
    if (rawItems.length === 1) {
      const item = rawItems[0];
      if (item.length > maxTagLength) {
        setLocalError(`Tag must not exceed ${maxTagLength} characters.`);
        return;
      }
      const exists = tags.some((t) => t.toLowerCase() === item.toLowerCase());
      if (exists) {
        setLocalError(`"${item}" is already added.`);
        return;
      }
      onChange([...tags, item]);
      setInputValue('');
      setLocalError('');
      return;
    }

    // Bulk / comma-separated / pasted items
    const availableSlots = maxTags - tags.length;
    const newTags = [];
    let duplicateCount = 0;

    for (const raw of rawItems) {
      if (newTags.length >= availableSlots) {
        break;
      }
      const item = raw.length > maxTagLength ? raw.slice(0, maxTagLength) : raw;
      const isDuplicate =
        tags.some((t) => t.toLowerCase() === item.toLowerCase()) ||
        newTags.some((t) => t.toLowerCase() === item.toLowerCase());

      if (isDuplicate) {
        duplicateCount++;
        continue;
      }

      newTags.push(item);
    }

    if (newTags.length === 0) {
      if (duplicateCount > 0) {
        setLocalError('All entered tags are already added.');
      } else if (tags.length >= maxTags) {
        setLocalError(`Maximum ${maxTags} tags allowed.`);
      }
      return;
    }

    const updatedTags = [...tags, ...newTags];
    onChange(updatedTags);
    setInputValue('');

    if (updatedTags.length >= maxTags && rawItems.length > newTags.length) {
      setLocalError(`Added ${newTags.length} tag${newTags.length > 1 ? 's' : ''}. Maximum ${maxTags} tags reached.`);
    } else {
      setLocalError('');
    }
  };

  const handlePaste = (e) => {
    const pastedText = e.clipboardData?.getData('text');
    if (!pastedText) return;

    if (pastedText.includes(',') || pastedText.includes(';') || pastedText.includes('\n')) {
      e.preventDefault();
      addTagsFromText(pastedText);
    }
  };

  const removeTag = (indexToRemove) => {
    if (disabled) return;
    onChange(tags.filter((_, idx) => idx !== indexToRemove));
    setLocalError('');
  };

  const isLimitReached = tags.length >= maxTags;

  return (
    <div className="form-group">
      {label && (
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <label htmlFor={id} className="form-label" style={{ marginBottom: '0.25rem' }}>
            {label}
          </label>
          <span
            style={{
              fontSize: '0.75rem',
              color: isLimitReached ? 'var(--text-primary)' : 'var(--text-muted)',
              fontWeight: isLimitReached ? 600 : 400,
            }}
          >
            {tags.length}/{maxTags}
          </span>
        </div>
      )}

      {/* Tags Pill Container */}
      <div className={`tags-input-container ${error || localError ? 'has-error' : ''}`}>
        {tags.map((tag, idx) => (
          <span
            key={idx}
            className="badge badge-dark"
            style={{
              display: 'inline-flex',
              alignItems: 'center',
              gap: '0.35rem',
              padding: '0.25rem 0.5rem',
              fontSize: '0.8rem',
            }}
          >
            {tag}
            {!disabled && (
              <button
                type="button"
                onClick={() => removeTag(idx)}
                style={{
                  background: 'none',
                  border: 'none',
                  color: 'inherit',
                  cursor: 'pointer',
                  padding: 0,
                  fontSize: '0.9rem',
                  lineHeight: 1,
                  display: 'flex',
                  alignItems: 'center',
                  opacity: 0.75,
                  transition: 'opacity 0.15s ease, transform 0.1s ease',
                }}
                aria-label={`Remove tag ${tag}`}
              >
                &times;
              </button>
            )}
          </span>
        ))}

        <div style={{ display: 'flex', flexGrow: 1, gap: '0.4rem' }}>
          <input
            id={id}
            type="text"
            value={inputValue}
            onChange={(e) => {
              setInputValue(e.target.value);
              if (localError) setLocalError('');
            }}
            onKeyDown={handleKeyDown}
            onPaste={handlePaste}
            placeholder={!isLimitReached ? placeholder : `Tag limit reached (max ${maxTags})`}
            disabled={disabled || isLimitReached}
            style={{
              flexGrow: 1,
              border: 'none',
              outline: 'none',
              background: 'transparent',
              color: 'var(--text-primary)',
              fontSize: '0.875rem',
              minWidth: '140px',
              padding: '0.2rem',
            }}
          />
          <button
            type="button"
            onClick={addTag}
            disabled={disabled || !inputValue.trim() || isLimitReached}
            className="btn btn-sm btn-subtle"
            style={{ padding: '0.2rem 0.6rem', fontSize: '0.75rem' }}
          >
            Add
          </button>
        </div>
      </div>

      {(error || localError) && (
        <span className="form-error" role="alert">
          {error || localError}
        </span>
      )}
      {hint && !error && !localError && <span className="form-hint">{hint}</span>}
      {isLimitReached && !error && !localError && (
        <span className="form-hint" style={{ color: 'var(--text-muted)' }}>
          Maximum {maxTags} tags reached. Remove a tag to add another.
        </span>
      )}
    </div>
  );
}
