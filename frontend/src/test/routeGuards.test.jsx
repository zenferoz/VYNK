import React from 'react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import { MemoryRouter, Routes, Route, useLocation } from 'react-router-dom';
import { ProtectedRoute, PublicOnlyRoute } from '../components/common/ProtectedRoute';
import * as AuthContextModule from '../context/AuthContext';

// Mock useAuth
vi.mock('../context/AuthContext', () => ({
  useAuth: vi.fn(),
}));

function LocationDisplay() {
  const location = useLocation();
  return (
    <div>
      <span data-testid="location-display">{location.pathname}</span>
      <span data-testid="location-state-from">{location.state?.from?.pathname || 'none'}</span>
    </div>
  );
}

describe('Route Guards', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  describe('ProtectedRoute', () => {
    it('displays loading screen while session is being checked', () => {
      AuthContextModule.useAuth.mockReturnValue({
        loading: true,
        isAuthenticated: false,
        isOnboarded: false,
        networkError: null,
      });

      render(
        <MemoryRouter initialEntries={['/dashboard']}>
          <ProtectedRoute>
            <div data-testid="protected-content">Dashboard Content</div>
          </ProtectedRoute>
        </MemoryRouter>
      );

      expect(screen.getByText('Checking session...')).toBeDefined();
      expect(screen.queryByTestId('protected-content')).toBeNull();
    });

    it('displays network error alert with retry button when network error occurs', () => {
      const mockRefresh = vi.fn();
      AuthContextModule.useAuth.mockReturnValue({
        loading: false,
        isAuthenticated: false,
        isOnboarded: false,
        networkError: 'Unable to connect to VYNK backend server.',
        refreshSession: mockRefresh,
      });

      render(
        <MemoryRouter initialEntries={['/dashboard']}>
          <ProtectedRoute>
            <div data-testid="protected-content">Dashboard Content</div>
          </ProtectedRoute>
        </MemoryRouter>
      );

      expect(screen.getByText('Unable to connect to VYNK backend server.')).toBeDefined();
      expect(screen.queryByTestId('protected-content')).toBeNull();

      const retryButton = screen.getByRole('button', { name: /retry/i });
      fireEvent.click(retryButton);
      expect(mockRefresh).toHaveBeenCalledTimes(1);
    });

    it('redirects unauthenticated user to /login and saves current location in state', () => {
      AuthContextModule.useAuth.mockReturnValue({
        loading: false,
        isAuthenticated: false,
        isOnboarded: false,
        networkError: null,
      });

      render(
        <MemoryRouter initialEntries={['/dashboard']}>
          <Routes>
            <Route
              path="/dashboard"
              element={
                <ProtectedRoute>
                  <div data-testid="protected-content">Dashboard Content</div>
                </ProtectedRoute>
              }
            />
            <Route path="/login" element={<LocationDisplay />} />
          </Routes>
        </MemoryRouter>
      );

      expect(screen.queryByTestId('protected-content')).toBeNull();
      expect(screen.getByTestId('location-display').textContent).toBe('/login');
      expect(screen.getByTestId('location-state-from').textContent).toBe('/dashboard');
    });

    it('redirects authenticated user without completed onboarding to /onboarding', () => {
      AuthContextModule.useAuth.mockReturnValue({
        loading: false,
        isAuthenticated: true,
        isOnboarded: false,
        networkError: null,
      });

      render(
        <MemoryRouter initialEntries={['/dashboard']}>
          <Routes>
            <Route
              path="/dashboard"
              element={
                <ProtectedRoute requireOnboarded={true}>
                  <div data-testid="protected-content">Dashboard Content</div>
                </ProtectedRoute>
              }
            />
            <Route path="/onboarding" element={<LocationDisplay />} />
          </Routes>
        </MemoryRouter>
      );

      expect(screen.queryByTestId('protected-content')).toBeNull();
      expect(screen.getByTestId('location-display').textContent).toBe('/onboarding');
    });

    it('allows access to /onboarding for authenticated users with incomplete onboarding when requireOnboarded is false', () => {
      AuthContextModule.useAuth.mockReturnValue({
        loading: false,
        isAuthenticated: true,
        isOnboarded: false,
        networkError: null,
      });

      render(
        <MemoryRouter initialEntries={['/onboarding']}>
          <Routes>
            <Route
              path="/onboarding"
              element={
                <ProtectedRoute requireOnboarded={false}>
                  <div data-testid="onboarding-content">Onboarding Form</div>
                </ProtectedRoute>
              }
            />
          </Routes>
        </MemoryRouter>
      );

      expect(screen.getByTestId('onboarding-content').textContent).toBe('Onboarding Form');
    });

    it('renders protected child component for authenticated and onboarded users', () => {
      AuthContextModule.useAuth.mockReturnValue({
        loading: false,
        isAuthenticated: true,
        isOnboarded: true,
        networkError: null,
      });

      render(
        <MemoryRouter initialEntries={['/dashboard']}>
          <Routes>
            <Route
              path="/dashboard"
              element={
                <ProtectedRoute>
                  <div data-testid="protected-content">Protected Dashboard</div>
                </ProtectedRoute>
              }
            />
          </Routes>
        </MemoryRouter>
      );

      expect(screen.getByTestId('protected-content').textContent).toBe('Protected Dashboard');
    });
  });

  describe('PublicOnlyRoute', () => {
    it('displays loading screen when auth session is loading', () => {
      AuthContextModule.useAuth.mockReturnValue({
        loading: true,
        isAuthenticated: false,
        isOnboarded: false,
      });

      render(
        <MemoryRouter initialEntries={['/login']}>
          <PublicOnlyRoute>
            <div data-testid="public-content">Login Page</div>
          </PublicOnlyRoute>
        </MemoryRouter>
      );

      expect(screen.getByText('Loading...')).toBeDefined();
      expect(screen.queryByTestId('public-content')).toBeNull();
    });

    it('renders public children for unauthenticated visitors', () => {
      AuthContextModule.useAuth.mockReturnValue({
        loading: false,
        isAuthenticated: false,
        isOnboarded: false,
      });

      render(
        <MemoryRouter initialEntries={['/login']}>
          <PublicOnlyRoute>
            <div data-testid="public-content">Login Page</div>
          </PublicOnlyRoute>
        </MemoryRouter>
      );

      expect(screen.getByTestId('public-content').textContent).toBe('Login Page');
    });

    it('redirects authenticated and onboarded users to /dashboard', () => {
      AuthContextModule.useAuth.mockReturnValue({
        loading: false,
        isAuthenticated: true,
        isOnboarded: true,
      });

      render(
        <MemoryRouter initialEntries={['/login']}>
          <Routes>
            <Route
              path="/login"
              element={
                <PublicOnlyRoute>
                  <div data-testid="public-content">Login Page</div>
                </PublicOnlyRoute>
              }
            />
            <Route path="/dashboard" element={<LocationDisplay />} />
          </Routes>
        </MemoryRouter>
      );

      expect(screen.queryByTestId('public-content')).toBeNull();
      expect(screen.getByTestId('location-display').textContent).toBe('/dashboard');
    });

    it('redirects authenticated but non-onboarded users to /onboarding', () => {
      AuthContextModule.useAuth.mockReturnValue({
        loading: false,
        isAuthenticated: true,
        isOnboarded: false,
      });

      render(
        <MemoryRouter initialEntries={['/login']}>
          <Routes>
            <Route
              path="/login"
              element={
                <PublicOnlyRoute>
                  <div data-testid="public-content">Login Page</div>
                </PublicOnlyRoute>
              }
            />
            <Route path="/onboarding" element={<LocationDisplay />} />
          </Routes>
        </MemoryRouter>
      );

      expect(screen.queryByTestId('public-content')).toBeNull();
      expect(screen.getByTestId('location-display').textContent).toBe('/onboarding');
    });
  });
});
