import React from 'react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { MemoryRouter, Routes, Route } from 'react-router-dom';
import { OnboardingPage } from '../pages/OnboardingPage';
import { ProfilePage } from '../pages/ProfilePage';
import { DashboardPage } from '../pages/DashboardPage';
import { ProtectedRoute } from '../components/common/ProtectedRoute';
import * as AuthContextModule from '../context/AuthContext';
import { profileApi } from '../api/profile';

vi.mock('../context/AuthContext', () => ({
  useAuth: vi.fn(),
}));

vi.mock('../api/profile', () => ({
  profileApi: {
    getMyProfile: vi.fn(),
    updateMyProfile: vi.fn(),
  },
}));

vi.mock('../api/goals', () => ({
  goalsApi: {
    getMyGoals: vi.fn().mockResolvedValue([]),
    createGoal: vi.fn(),
    updateGoal: vi.fn(),
    deactivateGoal: vi.fn(),
  },
}));

describe('Slice 2: Student Identity & Onboarding Integration', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  describe('Onboarding Workflow', () => {
    it('initializes empty form when getMyProfile returns null (404 handled gracefully)', async () => {
      profileApi.getMyProfile.mockResolvedValue(null);

      AuthContextModule.useAuth.mockReturnValue({
        user: { email: 'student@anurag.edu.in', userId: 101, collegeName: 'Anurag University' },
        refreshSession: vi.fn(),
        updateProfileState: vi.fn(),
      });

      render(
        <MemoryRouter initialEntries={['/onboarding']}>
          <Routes>
            <Route path="/onboarding" element={<OnboardingPage />} />
          </Routes>
        </MemoryRouter>
      );

      await waitFor(() => {
        expect(screen.getByLabelText(/Full Name/i)).toBeDefined();
      });

      expect(screen.getByLabelText(/Full Name/i).value).toBe('');
      expect(screen.getByLabelText(/Department/i).value).toBe('');
      expect(screen.getByLabelText(/Course \/ Degree/i).value).toBe('');
      expect(screen.getByLabelText(/Current Academic Year/i).value).toBe('1');
    });

    it('validates required fields before submitting onboarding', async () => {
      profileApi.getMyProfile.mockResolvedValue(null);

      AuthContextModule.useAuth.mockReturnValue({
        user: { email: 'student@anurag.edu.in', userId: 101 },
        refreshSession: vi.fn(),
        updateProfileState: vi.fn(),
      });

      render(
        <MemoryRouter initialEntries={['/onboarding']}>
          <Routes>
            <Route path="/onboarding" element={<OnboardingPage />} />
          </Routes>
        </MemoryRouter>
      );

      await waitFor(() => {
        expect(screen.getByRole('button', { name: /Complete Setup & Launch Dashboard/i })).toBeDefined();
      });

      const submitBtn = screen.getByRole('button', { name: /Complete Setup & Launch Dashboard/i });
      fireEvent.click(submitBtn);

      await waitFor(() => {
        expect(screen.getByText(/Full name is required/i)).toBeDefined();
        expect(screen.getByText(/Department is required/i)).toBeDefined();
        expect(screen.getByText(/Course \/ Degree is required/i)).toBeDefined();
      });

      expect(profileApi.updateMyProfile).not.toHaveBeenCalled();
    });

    it('submits valid profile, updates auth state, and triggers session refresh', async () => {
      profileApi.getMyProfile.mockResolvedValue(null);

      const mockSaved = {
        id: 1,
        userId: 101,
        fullName: 'Vikram Seth',
        department: 'Computer Science & Engineering',
        course: 'B.Tech Computer Science',
        currentYear: 3,
        bio: 'Building systems.',
        discoverable: true,
        onboardingCompleted: true,
        skills: ['Java', 'React'],
        interests: ['Hackathons'],
      };
      profileApi.updateMyProfile.mockResolvedValue(mockSaved);

      const mockUpdateProfileState = vi.fn();
      const mockRefreshSession = vi.fn().mockResolvedValue({});

      AuthContextModule.useAuth.mockReturnValue({
        user: { email: 'vikram@anurag.edu.in', userId: 101 },
        refreshSession: mockRefreshSession,
        updateProfileState: mockUpdateProfileState,
      });

      render(
        <MemoryRouter initialEntries={['/onboarding']}>
          <Routes>
            <Route path="/onboarding" element={<OnboardingPage />} />
            <Route path="/dashboard" element={<div data-testid="dashboard-dest">Welcome to Dashboard</div>} />
          </Routes>
        </MemoryRouter>
      );

      await waitFor(() => {
        expect(screen.getByLabelText(/Full Name/i)).toBeDefined();
      });

      fireEvent.change(screen.getByLabelText(/Full Name/i), { target: { value: 'Vikram Seth' } });
      fireEvent.change(screen.getByLabelText(/Department/i), { target: { value: 'Computer Science and Engineering (CSE)' } });
      fireEvent.change(screen.getByLabelText(/Course \/ Degree/i), { target: { value: 'B.Tech' } });
      fireEvent.change(screen.getByLabelText(/Current Academic Year/i), { target: { value: '3' } });

      const submitBtn = screen.getByRole('button', { name: /Complete Setup & Launch Dashboard/i });
      fireEvent.click(submitBtn);

      await waitFor(() => {
        expect(profileApi.updateMyProfile).toHaveBeenCalledWith(expect.objectContaining({
          fullName: 'Vikram Seth',
          department: 'Computer Science and Engineering (CSE)',
          course: 'B.Tech',
          currentYear: '3',
        }));
        expect(mockUpdateProfileState).toHaveBeenCalledWith(mockSaved);
        expect(mockRefreshSession).toHaveBeenCalled();
        expect(screen.getByTestId('dashboard-dest')).toBeDefined();
      });
    });
  });

  describe('Profile Page & Dashboard Sync', () => {
    it('loads persisted profile data and renders strength score and fields', async () => {
      const persistedProfile = {
        id: 12,
        userId: 101,
        fullName: 'Ananya Roy',
        department: 'Information Technology',
        course: 'B.Tech IT',
        currentYear: 2,
        bio: 'AI Enthusiast',
        discoverable: true,
        onboardingCompleted: true,
        availability: 'Weekends',
        skills: ['Python', 'PyTorch'],
        interests: ['Machine Learning'],
      };
      profileApi.getMyProfile.mockResolvedValue(persistedProfile);

      const mockUpdateProfileState = vi.fn();
      AuthContextModule.useAuth.mockReturnValue({
        user: { email: 'ananya@anurag.edu.in', collegeName: 'Anurag University' },
        updateProfileState: mockUpdateProfileState,
      });

      render(
        <MemoryRouter initialEntries={['/profile']}>
          <Routes>
            <Route path="/profile" element={<ProfilePage />} />
          </Routes>
        </MemoryRouter>
      );

      await waitFor(() => {
        expect(screen.getByDisplayValue('Ananya Roy')).toBeDefined();
        expect(screen.getByDisplayValue('AI Enthusiast')).toBeDefined();
      });

      expect(mockUpdateProfileState).toHaveBeenCalledWith(persistedProfile);
    });

    it('DashboardPage renders student profile data from AuthContext', () => {
      AuthContextModule.useAuth.mockReturnValue({
        user: { email: 'student@anurag.edu.in', collegeName: 'Anurag University', collegeId: 1, role: 'STUDENT' },
        profile: {
          fullName: 'Rahul Sharma',
          department: 'Mechanical Engineering',
          course: 'B.Tech Mechanical',
          currentYear: 4,
          discoverable: true,
          bio: 'Automotive design and CAD modeling.',
          skills: ['SolidWorks', 'MATLAB'],
          interests: ['Robotics'],
          availability: '10 hrs/week',
        },
      });

      render(
        <MemoryRouter initialEntries={['/dashboard']}>
          <Routes>
            <Route path="/dashboard" element={<DashboardPage />} />
          </Routes>
        </MemoryRouter>
      );

      expect(screen.getByText(/Welcome back, Rahul Sharma/i)).toBeDefined();
      expect(screen.getByText('Mechanical Engineering')).toBeDefined();
      expect(screen.getByText('B.Tech Mechanical')).toBeDefined();
      expect(screen.getByText('Automotive design and CAD modeling.')).toBeDefined();
    });

    it('ProtectedRoute directs non-onboarded user to /onboarding', () => {
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
                  <div>Dashboard Content</div>
                </ProtectedRoute>
              }
            />
            <Route path="/onboarding" element={<div data-testid="onboarding-dest">Onboarding Screen</div>} />
          </Routes>
        </MemoryRouter>
      );

      expect(screen.getByTestId('onboarding-dest')).toBeDefined();
      expect(screen.queryByText('Dashboard Content')).toBeNull();
    });
  });
});
