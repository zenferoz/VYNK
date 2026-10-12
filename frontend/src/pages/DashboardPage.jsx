import React from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

export function DashboardPage() {
  const { user, profile } = useAuth();

  const initials = (profile?.fullName || user?.email || 'Student')
    .trim()
    .split(/\s+/)
    .map((w) => w[0])
    .filter(Boolean)
    .join('')
    .slice(0, 2)
    .toUpperCase();

  return (
    <div className="container page-enter" style={{ paddingTop: '2.5rem', paddingBottom: '4rem' }}>
      {/* Top Banner / Welcome Header */}
      <div className="card profile-hero-card" style={{ marginBottom: '2rem' }}>
        <div className="flex-between" style={{ flexWrap: 'wrap', gap: '1.25rem' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '1.25rem' }}>
            <div style={{ position: 'relative', flexShrink: 0 }}>
              <div className="profile-avatar-large" style={{ width: '60px', height: '60px', fontSize: '1.35rem' }} aria-hidden="true">
                {initials}
              </div>
              {profile?.discoverable && (
                <span className="profile-avatar-badge" title="Active in Campus Directory" />
              )}
            </div>
            <div>
              <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginBottom: '0.35rem', flexWrap: 'wrap' }}>
                <span className="badge badge-dark">Verified Student</span>
                {user?.collegeName && (
                  <span className="badge" title={`Verified College: ${user.collegeName}`}>
                    {user.collegeName}
                  </span>
                )}
                {profile?.discoverable && (
                  <span className="badge" style={{ color: 'var(--success-text)', borderColor: 'var(--success-border)', backgroundColor: 'var(--success-bg)' }}>
                    Active in Directory
                  </span>
                )}
              </div>
              <h1 style={{ fontSize: '1.85rem', letterSpacing: '-0.02em', margin: 0 }}>
                Welcome back, {profile?.fullName || user?.email}
              </h1>
              <p style={{ margin: '0.25rem 0 0', fontSize: '0.9rem', color: 'var(--text-muted)' }}>
                Institutional Account: <code style={{ fontSize: '0.85rem' }}>{user?.email}</code>
              </p>
            </div>
          </div>

          <div style={{ display: 'flex', gap: '0.75rem', flexWrap: 'wrap' }}>
            <Link to="/matches" className="btn btn-primary">
              View Matches &rarr;
            </Link>
            <Link to="/students" className="btn btn-secondary">
              Campus Directory
            </Link>
          </div>
        </div>
      </div>

      {/* 4 Quick Nav / Metric Cards */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))', gap: '1rem', marginBottom: '2rem' }}>
        <Link to="/matches" className="metric-card">
          <span className="metric-card-title">Top Matches</span>
          <div className="metric-card-value">
            <span>Purpose Matches</span>
            <span style={{ fontSize: '1.1rem' }}>&rarr;</span>
          </div>
          <span className="metric-card-desc">Ranked peers with matching academic goals and skills.</span>
        </Link>

        <Link to="/students" className="metric-card">
          <span className="metric-card-title">Campus Directory</span>
          <div className="metric-card-value">
            <span>Explore Peers</span>
            <span style={{ fontSize: '1.1rem' }}>&rarr;</span>
          </div>
          <span className="metric-card-desc">Filter by department, academic year, and interests.</span>
        </Link>

        <Link to="/connections" className="metric-card">
          <span className="metric-card-title">Network Hub</span>
          <div className="metric-card-value">
            <span>Connections</span>
            <span style={{ fontSize: '1.1rem' }}>&rarr;</span>
          </div>
          <span className="metric-card-desc">Manage pending requests and confirmed peer contacts.</span>
        </Link>

        <Link to="/profile" className="metric-card">
          <span className="metric-card-title">Profile & Goals</span>
          <div className="metric-card-value">
            <span>{profile?.discoverable ? 'Discoverable' : 'Private'}</span>
            <span style={{ fontSize: '1.1rem' }}>&rarr;</span>
          </div>
          <span className="metric-card-desc">Update availability, skills, and networking goals.</span>
        </Link>
      </div>

      {/* Grid: Profile Snapshot & Campus Status */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(320px, 1fr))', gap: '1.5rem' }}>
        {/* Profile Card */}
        <div className="card" style={{ display: 'flex', flexDirection: 'column' }}>
          <div className="card-header">
            <h2 className="card-title">Academic Identity</h2>
            <p className="card-subtitle">Your student profile visible to peers within {user?.collegeName || 'your campus'}</p>
          </div>

          <div style={{ display: 'flex', flexDirection: 'column', gap: '1.1rem', fontSize: '0.95rem' }}>
            <div className="grid-2">
              <div>
                <span className="text-muted" style={{ display: 'block', fontSize: '0.75rem', textTransform: 'uppercase', letterSpacing: '0.05em', marginBottom: '0.15rem' }}>
                  Course / Degree
                </span>
                <strong>{profile?.course || '—'}</strong>
              </div>
              <div>
                <span className="text-muted" style={{ display: 'block', fontSize: '0.75rem', textTransform: 'uppercase', letterSpacing: '0.05em', marginBottom: '0.15rem' }}>
                  Department
                </span>
                <strong>{profile?.department || '—'}</strong>
              </div>
            </div>

            <div className="grid-2">
              <div>
                <span className="text-muted" style={{ display: 'block', fontSize: '0.75rem', textTransform: 'uppercase', letterSpacing: '0.05em', marginBottom: '0.15rem' }}>
                  Academic Year
                </span>
                <span>{profile?.currentYear ? `Year ${profile.currentYear}` : '—'}</span>
              </div>
              <div>
                <span className="text-muted" style={{ display: 'block', fontSize: '0.75rem', textTransform: 'uppercase', letterSpacing: '0.05em', marginBottom: '0.15rem' }}>
                  Weekly Availability
                </span>
                <span>{profile?.availability || 'Not specified'}</span>
              </div>
            </div>

            <div>
              <span className="text-muted" style={{ display: 'block', fontSize: '0.75rem', textTransform: 'uppercase', letterSpacing: '0.05em', marginBottom: '0.2rem' }}>
                Academic Focus / Bio
              </span>
              <p style={{ margin: 0, fontStyle: profile?.bio ? 'normal' : 'italic', color: 'var(--text-secondary)', lineHeight: 1.5 }}>
                {profile?.bio || 'No bio provided yet. Add a brief bio to tell peers what you are working on.'}
              </p>
            </div>

            {Array.isArray(profile?.skills) && profile.skills.length > 0 && (
              <div>
                <span className="text-muted" style={{ display: 'block', fontSize: '0.75rem', textTransform: 'uppercase', letterSpacing: '0.05em', marginBottom: '0.35rem' }}>
                  Skills ({profile.skills.length})
                </span>
                <div style={{ display: 'flex', flexWrap: 'wrap', gap: '0.35rem' }}>
                  {profile.skills.map((s, idx) => (
                    <span key={idx} className="badge" style={{ fontSize: '0.75rem' }}>
                      {s}
                    </span>
                  ))}
                </div>
              </div>
            )}

            {Array.isArray(profile?.interests) && profile.interests.length > 0 && (
              <div>
                <span className="text-muted" style={{ display: 'block', fontSize: '0.75rem', textTransform: 'uppercase', letterSpacing: '0.05em', marginBottom: '0.35rem' }}>
                  Interests ({profile.interests.length})
                </span>
                <div style={{ display: 'flex', flexWrap: 'wrap', gap: '0.35rem' }}>
                  {profile.interests.map((item, idx) => (
                    <span key={idx} className="badge" style={{ fontSize: '0.75rem' }}>
                      {item}
                    </span>
                  ))}
                </div>
              </div>
            )}
          </div>

          <div style={{ marginTop: 'auto', paddingTop: '1.25rem', borderTop: '1px solid var(--border-light)' }}>
            <Link to="/profile" className="btn btn-sm btn-subtle" style={{ paddingLeft: 0 }}>
              Edit Profile Details &rarr;
            </Link>
          </div>
        </div>

        {/* College Community Isolation Card */}
        <div className="card" style={{ display: 'flex', flexDirection: 'column' }}>
          <div className="card-header">
            <h2 className="card-title">Campus Boundary</h2>
            <p className="card-subtitle">Zero data leakage & verified student network guarantee</p>
          </div>

          <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem', fontSize: '0.9rem', color: 'var(--text-secondary)' }}>
            <p style={{ margin: 0, lineHeight: 1.5 }}>
              Your account belongs to <strong>{user?.collegeName}</strong>. Peer discovery, messaging, and purposeful matches are cryptographically isolated within your verified institutional perimeter.
            </p>

            <div style={{ backgroundColor: 'var(--bg-subtle)', border: '1px solid var(--border-light)', padding: '1rem 1.25rem', borderRadius: 'var(--radius-md)' }}>
              <div style={{ fontWeight: 600, color: 'var(--text-primary)', marginBottom: '0.5rem', display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                <span style={{ width: '8px', height: '8px', borderRadius: '50%', backgroundColor: 'var(--success-text)', display: 'inline-block' }} />
                Campus Isolation Active
              </div>
              <div style={{ fontSize: '0.825rem', color: 'var(--text-secondary)', display: 'flex', flexDirection: 'column', gap: '0.25rem' }}>
                <div>Verified Institution: <strong style={{ color: 'var(--text-primary)' }}>{user?.collegeName}</strong></div>
                <div>Institutional ID: <code>{user?.collegeId}</code></div>
                <div>Role: <code>{user?.role}</code></div>
              </div>
            </div>

            <p style={{ fontSize: '0.85rem', color: 'var(--text-muted)', margin: 0, lineHeight: 1.5 }}>
              Only students with an active, verified institutional email domain can participate in discovery and matches.
            </p>

            <div style={{ marginTop: 'auto', paddingTop: '1.25rem', borderTop: '1px solid var(--border-light)' }}>
              <Link to="/students" className="btn btn-sm btn-secondary btn-block">
                Browse Campus Directory &rarr;
              </Link>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
