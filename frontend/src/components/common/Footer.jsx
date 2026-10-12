import React from 'react';
import { VynkLogo } from './VynkLogo';

const currentYear = new Date().getFullYear();

export function Footer() {
  return (
    <footer className="footer">
      <div className="container">
        <div className="footer-inner">
          <div style={{ display: 'flex', flexDirection: 'column', gap: '0.35rem' }}>
            <VynkLogo variant="compact" size="sm" showTagline={false} />
            <span style={{ fontSize: '0.8rem', color: 'var(--text-muted)' }}>
              College-exclusive purpose-driven student networking.
            </span>
          </div>
          <div style={{ fontSize: '0.8rem', color: 'var(--text-muted)' }}>
            &copy; {currentYear} VYNK. All rights reserved.
          </div>
        </div>
      </div>
    </footer>
  );
}
