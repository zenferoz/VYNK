import React from 'react';
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { GoalManager } from '../components/profile/GoalManager';
import { MatchesPage } from '../pages/MatchesPage';
import { useAuth } from '../context/AuthContext';
import { goalsApi } from '../api/goals';
import { discoveryApi } from '../api/discovery';

vi.mock('../context/AuthContext', () => ({
  useAuth: vi.fn(),
}));

// Mock API clients
vi.mock('../api/goals', () => ({
  goalsApi: {
    getMyGoals: vi.fn(),
    createGoal: vi.fn(),
    updateGoal: vi.fn(),
    deactivateGoal: vi.fn(),
  },
}));

vi.mock('../api/discovery', () => ({
  discoveryApi: {
    getStudents: vi.fn(),
    getMatches: vi.fn(),
  },
}));

const mockAuthUser = {
  id: 101,
  email: 'student@college.edu',
  collegeName: 'Campus Engineering College',
  onboardingCompleted: true,
};

const renderWithRouter = (ui) => {
  return render(<MemoryRouter>{ui}</MemoryRouter>);
};

describe('Slice 3: Purpose Goals & Deterministic Matching Integration', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    useAuth.mockReturnValue({
      user: mockAuthUser,
      profile: null,
      refreshSession: vi.fn(),
      updateProfileState: vi.fn(),
    });
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  describe('GoalManager Component', () => {
    it('loads and renders active and inactive goals with badges and skills', async () => {
      const mockGoals = [
        {
          id: 1,
          userId: 101,
          purpose: 'HACKATHON_TEAM',
          purposeDisplayName: 'Hackathon Team',
          description: 'Building an AI tool for hackathon',
          skills: ['React', 'Python'],
          targetYear: 3,
          active: true,
        },
        {
          id: 2,
          userId: 101,
          purpose: 'STUDY_PARTNER',
          purposeDisplayName: 'Study Partner',
          description: 'DSA practice',
          skills: ['Java'],
          targetYear: 2,
          active: false,
        },
      ];

      goalsApi.getMyGoals.mockResolvedValue(mockGoals);

      renderWithRouter(<GoalManager />);

      await waitFor(() => {
        expect(screen.getByText('Hackathon Team')).toBeDefined();
        expect(screen.getByText('Study Partner')).toBeDefined();
      });

      // Verify active & inactive badges
      expect(screen.getByText('Active')).toBeDefined();
      expect(screen.getByText('Inactive')).toBeDefined();

      // Verify descriptions and details
      expect(screen.getByText('Building an AI tool for hackathon')).toBeDefined();
      expect(screen.getByText('Target Year 3')).toBeDefined();
      expect(screen.getByText('React')).toBeDefined();
      expect(screen.getByText('Python')).toBeDefined();

      // Deactivate button only on active goal
      const deactivateButtons = screen.getAllByRole('button', { name: /Deactivate/i });
      expect(deactivateButtons.length).toBe(1);
    });

    it('renders empty state when no goals exist', async () => {
      goalsApi.getMyGoals.mockResolvedValue([]);

      renderWithRouter(<GoalManager />);

      await waitFor(() => {
        expect(
          screen.getByText(/No networking goals added yet/i)
        ).toBeDefined();
      });
    });

    it('creates a new goal, calls createGoal API, and refreshes goals list', async () => {
      goalsApi.getMyGoals
        .mockResolvedValueOnce([])
        .mockResolvedValueOnce([
          {
            id: 10,
            userId: 101,
            purpose: 'PROJECT_COLLABORATION',
            purposeDisplayName: 'Project Collaboration',
            description: 'Open source web platform',
            skills: ['TypeScript'],
            targetYear: 3,
            active: true,
          },
        ]);

      goalsApi.createGoal.mockResolvedValue({
        id: 10,
        purpose: 'PROJECT_COLLABORATION',
        active: true,
      });

      renderWithRouter(<GoalManager />);

      await waitFor(() => {
        expect(screen.getByRole('button', { name: /\+ Add New Goal/i })).toBeDefined();
      });

      fireEvent.click(screen.getByRole('button', { name: /\+ Add New Goal/i }));

      // Form appears
      expect(screen.getByText('Create Networking Goal')).toBeDefined();

      // Fill in description & target year
      const descInput = screen.getByLabelText(/Short Description/i);
      fireEvent.change(descInput, { target: { value: 'Open source web platform' } });

      const yearSelect = screen.getByLabelText(/Target Academic Year/i);
      fireEvent.change(yearSelect, { target: { value: '3' } });

      // Submit form
      const saveBtn = screen.getByRole('button', { name: /Save Goal/i });
      fireEvent.click(saveBtn);

      await waitFor(() => {
        expect(goalsApi.createGoal).toHaveBeenCalledWith(
          expect.objectContaining({
            purpose: 'PROJECT_COLLABORATION',
            description: 'Open source web platform',
            targetYear: 3,
            active: true,
          })
        );
        expect(screen.getByText(/Networking goal created successfully/i)).toBeDefined();
      });
    });

    it('deactivates an active goal upon confirmation', async () => {
      const mockGoal = {
        id: 42,
        userId: 101,
        purpose: 'RESEARCH',
        purposeDisplayName: 'Academic Research',
        description: 'NLP paper collaboration',
        active: true,
      };

      goalsApi.getMyGoals.mockResolvedValue([mockGoal]);
      goalsApi.deactivateGoal.mockResolvedValue({ message: 'Networking goal deactivated successfully.' });

      // Mock window.confirm
      const confirmSpy = vi.spyOn(window, 'confirm').mockReturnValue(true);

      renderWithRouter(<GoalManager />);

      await waitFor(() => {
        expect(screen.getByText('Academic Research')).toBeDefined();
      });

      const deactivateBtn = screen.getByRole('button', { name: /Deactivate/i });
      fireEvent.click(deactivateBtn);

      expect(confirmSpy).toHaveBeenCalled();
      await waitFor(() => {
        expect(goalsApi.deactivateGoal).toHaveBeenCalledWith(42);
        expect(screen.getByText(/Networking goal deactivated/i)).toBeDefined();
      });
    });
  });

  describe('MatchesPage Component', () => {
    it('fetches and renders deterministic match recommendations with scores and reasons', async () => {
      const mockMatches = [
        {
          userId: 201,
          fullName: 'Kavya Sharma',
          department: 'Computer Science and Engineering (CSE)',
          course: 'B.Tech',
          currentYear: 3,
          bio: 'Full stack builder looking for hackathon team.',
          skills: ['React', 'Node.js'],
          interests: ['Hackathons', 'Open Source'],
          availability: 'Weekends',
          activePurposes: ['Hackathon Team'],
          matchScore: 88,
          matchReasons: [
            'Shared purpose: Hackathon Team',
            'Same department: Computer Science and Engineering (CSE)',
            'Same academic year (3)',
          ],
        },
        {
          userId: 301,
          fullName: 'Rohan Verma',
          department: 'Information Technology (IT)',
          course: 'B.Tech',
          currentYear: 2,
          bio: 'Data structures enthusiast.',
          skills: ['Java'],
          interests: ['Algorithms'],
          availability: 'Evenings',
          activePurposes: ['Study Partner'],
          matchScore: 35,
          matchReasons: ['Shared skills/interests: Java'],
        },
      ];

      discoveryApi.getMatches.mockResolvedValue(mockMatches);

      renderWithRouter(<MatchesPage />);

      await waitFor(() => {
        expect(screen.getByText('Kavya Sharma')).toBeDefined();
        expect(screen.getByText('Rohan Verma')).toBeDefined();
      });

      // Verify deterministic score pills
      expect(screen.getByText('88% Match')).toBeDefined();
      expect(screen.getByText('35% Match')).toBeDefined();

      // Verify explainable match reasons
      expect(screen.getByText('Shared purpose: Hackathon Team')).toBeDefined();
      expect(screen.getByText('Shared skills/interests: Java')).toBeDefined();

      // Verify active purposes tags
      expect(screen.getByText('Hackathon Team')).toBeDefined();
      expect(screen.getByText('Study Partner')).toBeDefined();
    });

    it('renders empty recommendations state when no compatible matches are found', async () => {
      discoveryApi.getMatches.mockResolvedValue([]);

      renderWithRouter(<MatchesPage />);

      await waitFor(() => {
        expect(screen.getByText('No Compatible Purpose Matches Yet')).toBeDefined();
        expect(
          screen.getByText(/Ensure you have added at least one active/i)
        ).toBeDefined();
      });
    });
  });
});
