import React from 'react';
import { Navigate, useLocation } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import { LoadingScreen } from './LoadingSpinner';
import { Alert } from './Alert';

export function ProtectedRoute({ children, requireOnboarded = true }) {
  const { isAuthenticated, isOnboarded, loading, networkError, refreshSession } = useAuth();
  const location = useLocation();

  if (loading) {
    return <LoadingScreen message="Checking session..." />;
  }

  if (networkError) {
    return (
      <div className="container" style={{ paddingTop: '3rem' }}>
        <Alert
          type="error"
          message={networkError}
          onRetry={refreshSession}
        />
      </div>
    );
  }

  if (!isAuthenticated) {
    return <Navigate to="/login" state={{ from: location }} replace />;
  }

  // If user is authenticated but hasn't completed onboarding, enforce /onboarding
  if (requireOnboarded && !isOnboarded && location.pathname !== '/onboarding') {
    return <Navigate to="/onboarding" replace />;
  }

  return children;
}

export function PublicOnlyRoute({ children }) {
  const { isAuthenticated, isOnboarded, loading } = useAuth();

  if (loading) {
    return <LoadingScreen message="Loading..." />;
  }

  if (isAuthenticated) {
    return <Navigate to={isOnboarded ? '/dashboard' : '/onboarding'} replace />;
  }

  return children;
}
