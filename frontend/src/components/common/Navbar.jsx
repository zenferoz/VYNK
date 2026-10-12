import React, { useState } from 'react';
import { Link, NavLink, useNavigate } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import { LoadingSpinner } from './LoadingSpinner';
import { ThemeToggle } from './ThemeToggle';
import { VynkLogo } from './VynkLogo';

export function Navbar() {
  const { user, isAuthenticated, isOnboarded, logout } = useAuth();
  const [loggingOut, setLoggingOut] = useState(false);
  const [logoutError, setLogoutError] = useState('');
  const [mobileOpen, setMobileOpen] = useState(false);
  const navigate = useNavigate();

  const handleLogout = async () => {
    try {
      setLoggingOut(true);
      setLogoutError('');
      setMobileOpen(false);
      await logout();
      navigate('/login');
    } catch (err) {
      setLogoutError(err.message || 'Logout failed.');
    } finally {
      setLoggingOut(false);
    }
  };

  const closeMobile = () => setMobileOpen(false);

  return (
    <header className="navbar">
      <div className="container">
        <div className="navbar-inner">
          <Link to={isAuthenticated ? '/dashboard' : '/'} className="brand-link" onClick={closeMobile} aria-label="VYNK Home">
            <VynkLogo variant="full" size="md" showTagline={true} />
          </Link>

          {/* Desktop Navigation */}
          <nav className="nav-links desktop-nav" aria-label="Main Navigation">
            {isAuthenticated ? (
              <>
                {user?.collegeName && (
                  <span className="badge" title={`Verified College: ${user.collegeName}`}>
                    {user.collegeName}
                  </span>
                )}

                {isOnboarded ? (
                  <>
                    <NavLink
                      to="/feed"
                      className={({ isActive }) => `nav-link ${isActive ? 'active' : ''}`}
                    >
                      Feed
                    </NavLink>
                    <NavLink
                      to="/matches"
                      className={({ isActive }) => `nav-link ${isActive ? 'active' : ''}`}
                    >
                      Matches
                    </NavLink>
                    <NavLink
                      to="/messages"
                      className={({ isActive }) => `nav-link ${isActive ? 'active' : ''}`}
                    >
                      Messages
                    </NavLink>
                    <NavLink
                      to="/communities"
                      className={({ isActive }) => `nav-link ${isActive ? 'active' : ''}`}
                    >
                      Communities
                    </NavLink>
                    <NavLink
                      to="/connections"
                      className={({ isActive }) => `nav-link ${isActive ? 'active' : ''}`}
                    >
                      Connections
                    </NavLink>
                    <NavLink
                      to="/notifications"
                      className={({ isActive }) => `nav-link ${isActive ? 'active' : ''}`}
                    >
                      Notifications
                    </NavLink>
                    <NavLink
                      to="/profile"
                      className={({ isActive }) => `nav-link ${isActive ? 'active' : ''}`}
                    >
                      Profile
                    </NavLink>
                  </>
                ) : (
                  <NavLink
                    to="/onboarding"
                    className={({ isActive }) => `nav-link ${isActive ? 'active' : ''}`}
                  >
                    Complete Onboarding
                  </NavLink>
                )}

                <ThemeToggle />

                <button
                  type="button"
                  onClick={handleLogout}
                  disabled={loggingOut}
                  className="btn btn-sm btn-subtle"
                  aria-label="Log out of your session"
                >
                  {loggingOut ? <LoadingSpinner text="Logging out..." /> : 'Logout'}
                </button>
              </>
            ) : (
              <>
                <NavLink
                  to="/login"
                  className={({ isActive }) => `nav-link ${isActive ? 'active' : ''}`}
                >
                  Login
                </NavLink>
                <Link to="/register" className="btn btn-sm btn-primary">
                  Register
                </Link>
                <ThemeToggle />
              </>
            )}
          </nav>

          {/* Mobile Right Controls: ThemeToggle + Hamburger */}
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
            <div className="mobile-toggle-wrapper" style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
              <button
                type="button"
                className="mobile-nav-toggle"
                onClick={() => setMobileOpen((prev) => !prev)}
                aria-expanded={mobileOpen}
                aria-label={mobileOpen ? 'Close navigation menu' : 'Open navigation menu'}
              >
                {mobileOpen ? (
                  <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                    <line x1="18" y1="6" x2="6" y2="18" />
                    <line x1="6" y1="6" x2="18" y2="18" />
                  </svg>
                ) : (
                  <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                    <line x1="4" y1="12" x2="20" y2="12" />
                    <line x1="4" y1="6" x2="20" y2="6" />
                    <line x1="4" y1="18" x2="20" y2="18" />
                  </svg>
                )}
              </button>
            </div>
          </div>
        </div>
      </div>

      {/* Mobile Menu Dropdown Drawer */}
      {mobileOpen && (
        <div className="mobile-nav-drawer" role="navigation" aria-label="Mobile Navigation">
          {isAuthenticated ? (
            <>
              {user?.collegeName && (
                <div style={{ marginBottom: '0.25rem' }}>
                  <span className="badge" title={`Verified College: ${user.collegeName}`}>
                    {user.collegeName}
                  </span>
                </div>
              )}

              {isOnboarded ? (
                <>
                  <NavLink
                    to="/feed"
                    onClick={closeMobile}
                    className={({ isActive }) => `nav-link ${isActive ? 'active' : ''}`}
                  >
                    Feed
                  </NavLink>
                  <NavLink
                    to="/matches"
                    onClick={closeMobile}
                    className={({ isActive }) => `nav-link ${isActive ? 'active' : ''}`}
                  >
                    Matches
                  </NavLink>
                  <NavLink
                    to="/messages"
                    onClick={closeMobile}
                    className={({ isActive }) => `nav-link ${isActive ? 'active' : ''}`}
                  >
                    Messages
                  </NavLink>
                  <NavLink
                    to="/communities"
                    onClick={closeMobile}
                    className={({ isActive }) => `nav-link ${isActive ? 'active' : ''}`}
                  >
                    Communities
                  </NavLink>
                  <NavLink
                    to="/connections"
                    onClick={closeMobile}
                    className={({ isActive }) => `nav-link ${isActive ? 'active' : ''}`}
                  >
                    Connections
                  </NavLink>
                  <NavLink
                    to="/notifications"
                    onClick={closeMobile}
                    className={({ isActive }) => `nav-link ${isActive ? 'active' : ''}`}
                  >
                    Notifications
                  </NavLink>
                  <NavLink
                    to="/profile"
                    onClick={closeMobile}
                    className={({ isActive }) => `nav-link ${isActive ? 'active' : ''}`}
                  >
                    Profile
                  </NavLink>
                </>
              ) : (
                <NavLink
                  to="/onboarding"
                  onClick={closeMobile}
                  className={({ isActive }) => `nav-link ${isActive ? 'active' : ''}`}
                >
                  Complete Onboarding
                </NavLink>
              )}

              <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', paddingTop: '0.75rem', marginTop: '0.25rem', borderTop: '1px solid var(--border-light)' }}>
                <ThemeToggle />
                <button
                  type="button"
                  onClick={handleLogout}
                  disabled={loggingOut}
                  className="btn btn-sm btn-subtle"
                  aria-label="Log out of your session"
                >
                  {loggingOut ? <LoadingSpinner text="Logging out..." /> : 'Logout'}
                </button>
              </div>
            </>
          ) : (
            <>
              <NavLink
                to="/login"
                onClick={closeMobile}
                className={({ isActive }) => `nav-link ${isActive ? 'active' : ''}`}
              >
                Login
              </NavLink>
              <Link to="/register" onClick={closeMobile} className="btn btn-sm btn-primary">
                Register
              </Link>
              <div style={{ paddingTop: '0.5rem', borderTop: '1px solid var(--border-light)' }}>
                <ThemeToggle />
              </div>
            </>
          )}
        </div>
      )}

      {logoutError && (
        <div style={{ backgroundColor: 'var(--error-bg)', color: 'var(--error-text)', padding: '0.4rem', fontSize: '0.8rem', textAlign: 'center' }}>
          {logoutError}
        </div>
      )}
    </header>
  );
}
