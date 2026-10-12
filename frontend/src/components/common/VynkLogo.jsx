import React from 'react';

/**
 * VYNK Brand Logo Component
 * Geometric connection emblem symbolizing converging student paths, shared purpose,
 * and academic collaboration with restrained electric lime accent.
 *
 * @param {{
 *   variant?: 'full' | 'compact' | 'icon',
 *   size?: 'sm' | 'md' | 'lg',
 *   showTagline?: boolean,
 *   className?: string
 * }} props
 */
export function VynkLogo({
  variant = 'compact',
  size = 'md',
  showTagline = false,
  className = '',
}) {
  const pixelSizes = {
    sm: { icon: 24, font: '1.15rem', tagline: '0.65rem' },
    md: { icon: 32, font: '1.45rem', tagline: '0.75rem' },
    lg: { icon: 42, font: '2rem', tagline: '0.85rem' },
  };

  const { icon: iconSize, font: fontSize, tagline: taglineSize } = pixelSizes[size] || pixelSizes.md;

  const symbolSvg = (
    <svg
      width={iconSize}
      height={iconSize}
      viewBox="0 0 36 36"
      fill="none"
      xmlns="http://www.w3.org/2000/svg"
      className="vynk-logo-symbol"
      aria-hidden="true"
    >
      {/* Background container with subtle border */}
      <rect
        width="36"
        height="36"
        rx="9"
        fill="currentColor"
        fillOpacity="0.06"
        stroke="currentColor"
        strokeOpacity="0.16"
        strokeWidth="1"
      />
      {/* Left arm of V */}
      <path
        d="M9 10L16.5 25.5C16.8 26.1 17.5 26.5 18.2 26.5C18.9 26.5 19.6 26.1 19.9 25.5L27 10"
        stroke="currentColor"
        strokeWidth="3.2"
        strokeLinecap="round"
        strokeLinejoin="round"
      />
      {/* Intersecting purpose bridge */}
      <path
        d="M13 18H23"
        stroke="currentColor"
        strokeWidth="2.4"
        strokeLinecap="round"
        strokeDasharray="1 2.5"
      />
      {/* Electric lime apex connection node */}
      <circle
        cx="18"
        cy="18"
        r="3.5"
        fill="var(--accent-lime, #b8e600)"
        stroke="currentColor"
        strokeWidth="1.5"
      />
      <circle
        cx="18"
        cy="18"
        r="1.5"
        fill="#09090b"
      />
    </svg>
  );

  if (variant === 'icon') {
    return (
      <span
        className={`vynk-logo-wrapper ${className}`}
        role="img"
        aria-label="VYNK Logo"
        style={{ display: 'inline-flex', alignItems: 'center' }}
      >
        {symbolSvg}
      </span>
    );
  }

  return (
    <div
      className={`vynk-logo-wrapper ${className}`}
      role="img"
      aria-label="VYNK — Connect With Purpose"
      style={{
        display: 'inline-flex',
        alignItems: 'center',
        gap: size === 'sm' ? '0.5rem' : '0.65rem',
        textDecoration: 'none',
      }}
    >
      {symbolSvg}
      <div style={{ display: 'flex', flexDirection: 'column', lineHeight: 1 }}>
        <span
          className="brand-logo"
          style={{
            fontFamily: 'var(--font-display)',
            fontSize,
            fontWeight: 800,
            letterSpacing: '-0.04em',
            color: 'var(--text-primary)',
            lineHeight: 1,
          }}
        >
          VYNK
        </span>
        {(showTagline || variant === 'full') && (
          <span
            className="brand-tagline"
            style={{
              fontSize: taglineSize,
              fontWeight: 600,
              textTransform: 'uppercase',
              letterSpacing: '0.08em',
              color: 'var(--text-muted)',
              marginTop: '0.2rem',
              lineHeight: 1,
            }}
          >
            Connect With Purpose
          </span>
        )}
      </div>
    </div>
  );
}

export default VynkLogo;
