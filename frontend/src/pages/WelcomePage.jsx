import React from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

export function WelcomePage() {
  const { isAuthenticated, isOnboarded } = useAuth();

  return (
    <div className="landing-root page-enter">
      {/* 1. Hero Section */}
      <section className="landing-hero">
        <div className="container">
          <div className="landing-hero-grid">
            <div className="landing-hero-content">
              <div className="landing-pill">
                <span className="landing-pill-dot" aria-hidden="true"></span>
                <span>Campus Verified Network • Institutional Access</span>
              </div>

              <h1 className="landing-headline">
                FIND YOUR PEOPLE.
                <br />
                <span className="landing-headline-accent">MAKE YOUR MOVE.</span>
              </h1>

              <p className="landing-subhead">
                VYNK is an academic and professional networking platform designed for college-student networking through institutional email verification. Discover peers within your college community, align around real academic goals, and build purposeful connections.
              </p>

              <div className="landing-cta-row">
                {isAuthenticated ? (
                  <Link
                    to={isOnboarded ? '/dashboard' : '/onboarding'}
                    className="landing-btn-lime"
                  >
                    {isOnboarded ? 'Go to Your Dashboard →' : 'Complete Onboarding →'}
                  </Link>
                ) : (
                  <>
                    <Link to="/register" className="landing-btn-lime">
                      Join With College Email →
                    </Link>
                    <Link to="/login" className="btn btn-secondary landing-hero-btn-secondary">
                      Sign In
                    </Link>
                  </>
                )}
              </div>
            </div>

            {/* Illustrative Student Connection Preview Card */}
            <div className="landing-preview-wrapper">
              <div className="landing-preview-card">
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: '0.85rem' }}>
                  <div>
                    <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginBottom: '0.25rem' }}>
                      <strong style={{ fontSize: '1.05rem', color: 'var(--text-primary)' }}>Priya Sharma</strong>
                      <span className="badge badge-dark" style={{ fontSize: '0.72rem' }}>Verified Student</span>
                    </div>
                    <p style={{ margin: 0, fontSize: '0.85rem', color: 'var(--text-muted)' }}>
                      Computer Science & Engineering • Year 3
                    </p>
                  </div>
                  <span
                    style={{
                      fontSize: '0.75rem',
                      fontWeight: 700,
                      padding: '0.25rem 0.55rem',
                      borderRadius: 'var(--radius-sm)',
                      background: 'var(--accent-lime-subtle)',
                      border: '1px solid var(--accent-lime-border)',
                      color: 'var(--text-primary)',
                    }}
                  >
                    Shared Purpose Match
                  </span>
                </div>

                <div style={{ marginBottom: '0.75rem' }}>
                  <span style={{ fontSize: '0.75rem', fontWeight: 600, color: 'var(--text-secondary)', display: 'block', marginBottom: '0.35rem' }}>
                    Active Networking Goal:
                  </span>
                  <span className="badge" style={{ fontSize: '0.78rem' }}>
                    Hackathon Team
                  </span>
                </div>

                <p style={{ fontSize: '0.875rem', color: 'var(--text-secondary)', lineHeight: 1.5, margin: '0 0 0.85rem' }}>
                  &ldquo;Looking for a teammate focused on backend APIs and deployment for the upcoming campus hackathon.&rdquo;
                </p>

                <div style={{ display: 'flex', gap: '0.4rem', flexWrap: 'wrap' }}>
                  <span className="badge" style={{ fontSize: '0.72rem' }}>PyTorch</span>
                  <span className="badge" style={{ fontSize: '0.72rem' }}>React</span>
                  <span className="badge" style={{ fontSize: '0.72rem' }}>System Design</span>
                </div>
              </div>

              <p className="landing-preview-disclaimer">
                Illustrative preview of a purpose-matched student connection card.
              </p>
            </div>
          </div>
        </div>
      </section>

      {/* 2. Differentiator Strip */}
      <div className="container">
        <div className="landing-strip">
          <div className="landing-strip-item">
            <div className="landing-strip-icon" aria-hidden="true">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z" />
                <path d="m9 12 2 2 4-4" />
              </svg>
            </div>
            <div>
              <strong style={{ display: 'block', fontSize: '0.95rem', color: 'var(--text-primary)', marginBottom: '0.25rem' }}>
                Institutional Email Verification
              </strong>
              <p style={{ margin: 0, fontSize: '0.85rem', color: 'var(--text-secondary)', lineHeight: 1.5 }}>
                Profiles are verified through approved campus email domains, fostering an institutional environment designed for student collaboration.
              </p>
            </div>
          </div>

          <div className="landing-strip-item">
            <div className="landing-strip-icon" aria-hidden="true">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                <rect x="3" y="3" width="18" height="18" rx="2" />
                <path d="M7 7h10v10H7z" />
              </svg>
            </div>
            <div>
              <strong style={{ display: 'block', fontSize: '0.95rem', color: 'var(--text-primary)', marginBottom: '0.25rem' }}>
                Campus-Restricted Discovery
              </strong>
              <p style={{ margin: 0, fontSize: '0.85rem', color: 'var(--text-secondary)', lineHeight: 1.5 }}>
                Student directory exploration and matching stay bounded within your college community, keeping interactions contextual and relevant.
              </p>
            </div>
          </div>

          <div className="landing-strip-item">
            <div className="landing-strip-icon" aria-hidden="true">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                <circle cx="12" cy="12" r="10" />
                <path d="m10 15 5-3-5-3v6z" />
              </svg>
            </div>
            <div>
              <strong style={{ display: 'block', fontSize: '0.95rem', color: 'var(--text-primary)', marginBottom: '0.25rem' }}>
                Purpose-Focused Networking
              </strong>
              <p style={{ margin: 0, fontSize: '0.85rem', color: 'var(--text-secondary)', lineHeight: 1.5 }}>
                Connections begin with concrete project goals and intent notes rather than public metrics or unstructured outreach.
              </p>
            </div>
          </div>
        </div>
      </div>

      {/* 3. Three Pillars Section */}
      <section className="landing-section">
        <div className="container">
          <div className="landing-section-header">
            <span className="landing-section-tag">Core Architecture</span>
            <h2 className="landing-section-title">Why VYNK Hits Different</h2>
            <p className="landing-section-desc">
              Engineered specifically for undergraduate and postgraduate students who want real teammates, not endless feed scrolling.
            </p>
          </div>

          <div className="landing-pillars-grid">
            <div className="landing-pillar-card">
              <div>
                <div style={{ marginBottom: '1rem', color: 'var(--accent-lime)' }}>
                  <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                    <polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2" />
                  </svg>
                </div>
                <h3 style={{ fontSize: '1.2rem', marginBottom: '0.5rem', color: 'var(--text-primary)' }}>
                  Purpose-Driven Matching
                </h3>
                <p style={{ fontSize: '0.9rem', color: 'var(--text-secondary)', lineHeight: 1.5 }}>
                  Discover peers based on shared academic objectives, technical skills, and active networking goals such as hackathons, research, or placement prep.
                </p>
              </div>
            </div>

            <div className="landing-pillar-card">
              <div>
                <div style={{ marginBottom: '1rem', color: 'var(--accent-lime)' }}>
                  <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                    <rect x="3" y="11" width="18" height="11" rx="2" ry="2" />
                    <path d="M7 11V7a5 5 0 0 1 10 0v4" />
                  </svg>
                </div>
                <h3 style={{ fontSize: '1.2rem', marginBottom: '0.5rem', color: 'var(--text-primary)' }}>
                  Campus-Focused Privacy
                </h3>
                <p style={{ fontSize: '0.9rem', color: 'var(--text-secondary)', lineHeight: 1.5 }}>
                  Privacy-conscious networking with clear connection boundaries. Directory discovery and student profiles are designed to remain within your college community.
                </p>
              </div>
            </div>

            <div className="landing-pillar-card">
              <div>
                <div style={{ marginBottom: '1rem', color: 'var(--accent-lime)' }}>
                  <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                    <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z" />
                  </svg>
                </div>
                <h3 style={{ fontSize: '1.2rem', marginBottom: '0.5rem', color: 'var(--text-primary)' }}>
                  Student Safety Controls
                </h3>
                <p style={{ fontSize: '0.9rem', color: 'var(--text-secondary)', lineHeight: 1.5 }}>
                  Equipped with blocking, reporting tools, and connection request limits to give students control over their networking experience.
                </p>
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* 4. Three-Step Workflow */}
      <section className="landing-section" style={{ background: 'var(--bg-secondary)' }}>
        <div className="container">
          <div className="landing-section-header">
            <span className="landing-section-tag">Simple Process</span>
            <h2 className="landing-section-title">How It Works</h2>
            <p className="landing-section-desc">
              Three straightforward steps from verified registration to purposeful collaboration.
            </p>
          </div>

          <div className="landing-steps-grid">
            <div className="landing-step-card">
              <div className="landing-step-number">01</div>
              <h3 style={{ fontSize: '1.15rem', marginBottom: '0.5rem', color: 'var(--text-primary)' }}>
                Verify Your Campus Email
              </h3>
              <p style={{ fontSize: '0.9rem', color: 'var(--text-secondary)', lineHeight: 1.5, margin: 0 }}>
                Sign up with your institution's email address and confirm ownership via a secure email verification link.
              </p>
            </div>

            <div className="landing-step-card">
              <div className="landing-step-number">02</div>
              <h3 style={{ fontSize: '1.15rem', marginBottom: '0.5rem', color: 'var(--text-primary)' }}>
                Declare Goals & Skills
              </h3>
              <p style={{ fontSize: '0.9rem', color: 'var(--text-secondary)', lineHeight: 1.5, margin: 0 }}>
                Complete your profile and define active goals — whether assembling a team, seeking study buddies, or prepping for interviews.
              </p>
            </div>

            <div className="landing-step-card">
              <div className="landing-step-number">03</div>
              <h3 style={{ fontSize: '1.15rem', marginBottom: '0.5rem', color: 'var(--text-primary)' }}>
                Connect With Purpose
              </h3>
              <p style={{ fontSize: '0.9rem', color: 'var(--text-secondary)', lineHeight: 1.5, margin: 0 }}>
                Send focused connection requests with concise purpose notes and begin collaborating directly within your campus network.
              </p>
            </div>
          </div>
        </div>
      </section>

      {/* 5. Student Scenarios */}
      <section className="landing-section">
        <div className="container">
          <div className="landing-section-header">
            <span className="landing-section-tag">Campus Scenarios</span>
            <h2 className="landing-section-title">Built For Real Student Needs</h2>
            <p className="landing-section-desc">
              Tailored collaboration pathways that match how modern students form high-impact teams.
            </p>
          </div>

          <div className="landing-scenarios-grid">
            <div className="landing-scenario-card">
              <div style={{ marginBottom: '1rem' }}>
                <span className="badge badge-dark">Hackathons & Projects</span>
              </div>
              <h3 style={{ fontSize: '1.15rem', marginBottom: '0.5rem', color: 'var(--text-primary)' }}>
                Hackathon Squad Assembly
              </h3>
              <p style={{ fontSize: '0.9rem', color: 'var(--text-secondary)', lineHeight: 1.5, margin: 0 }}>
                Connect with designers, frontend builders, or ML engineers across branches who share your goal of shipping winning weekend prototypes.
              </p>
            </div>

            <div className="landing-scenario-card">
              <div style={{ marginBottom: '1rem' }}>
                <span className="badge badge-dark">Career & Placements</span>
              </div>
              <h3 style={{ fontSize: '1.15rem', marginBottom: '0.5rem', color: 'var(--text-primary)' }}>
                Placement Preparation
              </h3>
              <p style={{ fontSize: '0.9rem', color: 'var(--text-secondary)', lineHeight: 1.5, margin: 0 }}>
                Find reliable peers in your academic year for mock technical interviews, algorithmic problem sets, and behavioral prep.
              </p>
            </div>

            <div className="landing-scenario-card">
              <div style={{ marginBottom: '1rem' }}>
                <span className="badge badge-dark">Academics & Research</span>
              </div>
              <h3 style={{ fontSize: '1.15rem', marginBottom: '0.5rem', color: 'var(--text-primary)' }}>
                Research & Study Partnerships
              </h3>
              <p style={{ fontSize: '0.9rem', color: 'var(--text-secondary)', lineHeight: 1.5, margin: 0 }}>
                Collaborate on departmental lab work, literature surveys, and academic publications with motivated peers.
              </p>
            </div>
          </div>
        </div>
      </section>

      {/* 6. Final CTA Banner */}
      <div className="container">
        <div className="landing-cta-banner">
          <h2 className="landing-cta-headline">
            READY TO ELEVATE YOUR CAMPUS CIRCLE?
          </h2>
          <p className="landing-cta-desc">
            Join verified peers within your college community and build purposeful collaborations that accelerate your academic and career goals.
          </p>

          <div className="landing-cta-row" style={{ marginTop: 0 }}>
            {isAuthenticated ? (
              <Link
                to={isOnboarded ? '/dashboard' : '/onboarding'}
                className="landing-btn-lime"
              >
                {isOnboarded ? 'Go to Your Dashboard →' : 'Complete Onboarding →'}
              </Link>
            ) : (
              <>
                <Link to="/register" className="landing-btn-lime">
                  Join With College Email →
                </Link>
                <Link
                  to="/login"
                  className="landing-banner-btn-secondary"
                >
                  Sign In
                </Link>
              </>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}
