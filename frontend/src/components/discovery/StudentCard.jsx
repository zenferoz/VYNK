import React from 'react';

export function StudentCard({ student, isMatch = false, onConnect, onBlock, onReport }) {
  if (!student) return null;

  const {
    fullName,
    department,
    course,
    currentYear,
    bio,
    collegeName,
    skills = [],
    interests = [],
    availability,
    activePurposes = [],
    matchScore,
    matchReasons = [],
  } = student;

  const academicInfo = [
    currentYear ? `Year ${currentYear}` : null,
    course || null,
  ]
    .filter(Boolean)
    .join(' • ');

  const initials = (fullName || 'Student')
    .trim()
    .split(/\s+/)
    .map((w) => w[0])
    .filter(Boolean)
    .join('')
    .slice(0, 2)
    .toUpperCase();

  return (
    <article className="card student-card">
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', gap: '0.75rem', marginBottom: '0.75rem' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
          <div className="avatar-monogram" aria-hidden="true">
            {initials}
          </div>
          <div>
            <h2 style={{ fontSize: '1.15rem', fontWeight: 700, margin: 0, color: 'var(--text-primary)', lineHeight: 1.25 }}>
              {fullName || 'Student'}
            </h2>
            {academicInfo && (
              <p style={{ margin: '0.2rem 0 0', fontSize: '0.825rem', color: 'var(--text-muted)' }}>
                {academicInfo}
              </p>
            )}
          </div>
        </div>

        <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'flex-end', gap: '0.35rem', flexShrink: 0 }}>
          {isMatch && matchScore !== undefined && (
            <span
              className="match-score-pill"
              title={student.semanticAssisted ? "AI-assisted hybrid match score" : "Deterministic algorithm match compatibility score"}
            >
              <svg width="11" height="11" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
                <polyline points="20 6 9 17 4 12" />
              </svg>
              {matchScore}% Match
            </span>
          )}
          {isMatch && student.semanticAssisted && (
            <span className="badge badge-accent" style={{ fontSize: '0.65rem', padding: '0.1rem 0.4rem' }}>
              AI Enhanced
            </span>
          )}
          {collegeName && (
            <span className="badge" title={`Verified institution: ${collegeName}`}>
              {collegeName}
            </span>
          )}
        </div>
      </div>

      {department && (
        <div style={{ marginBottom: '0.6rem' }}>
          <span
            className="badge badge-dark"
            style={{ fontSize: '0.75rem', fontWeight: 500 }}
          >
            {department}
          </span>
        </div>
      )}

      {bio ? (
        <p
          style={{
            margin: '0.25rem 0 0.75rem',
            fontSize: '0.875rem',
            color: 'var(--text-secondary)',
            lineHeight: 1.5,
            wordBreak: 'break-word',
          }}
        >
          {bio}
        </p>
      ) : (
        <p
          style={{
            margin: '0.25rem 0 0.75rem',
            fontSize: '0.825rem',
            color: 'var(--text-muted)',
            fontStyle: 'italic',
          }}
        >
          No bio provided.
        </p>
      )}

      {/* Match Reasons (if in Matches mode) */}
      {isMatch && matchReasons.length > 0 && (
        <div
          style={{
            backgroundColor: 'var(--bg-subtle)',
            border: '1px solid var(--border-light)',
            padding: '0.6rem 0.8rem',
            borderRadius: 'var(--radius-sm)',
            marginBottom: '0.75rem',
            fontSize: '0.8rem',
          }}
        >
          <strong style={{ display: 'block', marginBottom: '0.25rem', color: 'var(--text-primary)' }}>
            Why you matched:
          </strong>
          <ul style={{ margin: 0, paddingLeft: '1.1rem', color: 'var(--text-secondary)' }}>
            {matchReasons.map((reason, idx) => (
              <li key={idx} style={{ marginBottom: '0.15rem' }}>
                {reason}
              </li>
            ))}
          </ul>
        </div>
      )}

      {/* Networking Purposes */}
      {activePurposes.length > 0 && (
        <div style={{ marginBottom: '0.6rem' }}>
          <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)', display: 'block', marginBottom: '0.2rem' }}>
            Looking for:
          </span>
          <div style={{ display: 'flex', flexWrap: 'wrap', gap: '0.3rem' }}>
            {activePurposes.map((p, idx) => (
              <span key={idx} className="badge badge-dark" style={{ fontSize: '0.75rem' }}>
                {p}
              </span>
            ))}
          </div>
        </div>
      )}

      {/* Skills & Interests Tags */}
      {skills.length > 0 && (
        <div style={{ marginBottom: '0.5rem' }}>
          <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)', display: 'block', marginBottom: '0.2rem' }}>
            Skills:
          </span>
          <div style={{ display: 'flex', flexWrap: 'wrap', gap: '0.3rem' }}>
            {skills.map((s, idx) => (
              <span key={idx} className="badge" style={{ fontSize: '0.75rem' }}>
                {s}
              </span>
            ))}
          </div>
        </div>
      )}

      {interests.length > 0 && (
        <div style={{ marginBottom: '0.5rem' }}>
          <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)', display: 'block', marginBottom: '0.2rem' }}>
            Interests:
          </span>
          <div style={{ display: 'flex', flexWrap: 'wrap', gap: '0.3rem' }}>
            {interests.map((item, idx) => (
              <span key={idx} className="badge" style={{ fontSize: '0.75rem' }}>
                {item}
              </span>
            ))}
          </div>
        </div>
      )}

      {/* Availability */}
      {availability && (
        <div style={{ marginBottom: '0.75rem', fontSize: '0.8rem', color: 'var(--text-secondary)' }}>
          <span style={{ color: 'var(--text-muted)', fontSize: '0.75rem', display: 'block' }}>Availability:</span>
          <span>{availability}</span>
        </div>
      )}

      <div style={{ flexGrow: 1 }} />

      {/* Card Actions: Connect, Report, Block */}
      <div
        style={{
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          gap: '0.5rem',
          paddingTop: '0.75rem',
          marginTop: '0.5rem',
          borderTop: '1px solid var(--border-light)',
        }}
      >
        {onConnect && (
          <button
            type="button"
            onClick={() => onConnect(student)}
            className="btn btn-sm btn-primary"
            style={{ flexGrow: 1 }}
          >
            Connect
          </button>
        )}

        <div style={{ display: 'flex', gap: '0.35rem' }}>
          {onReport && (
            <button
              type="button"
              onClick={() => onReport(student)}
              className="btn btn-sm btn-subtle"
              style={{ fontSize: '0.75rem', padding: '0.3rem 0.5rem' }}
              title="Report student"
            >
              Report
            </button>
          )}

          {onBlock && (
            <button
              type="button"
              onClick={() => onBlock(student)}
              className="btn btn-sm btn-subtle"
              style={{ fontSize: '0.75rem', padding: '0.3rem 0.5rem', color: 'var(--text-muted)' }}
              title="Block student"
            >
              Block
            </button>
          )}
        </div>
      </div>
    </article>
  );
}
