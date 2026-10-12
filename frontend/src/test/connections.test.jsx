import React from 'react';
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { ConnectionsPage } from '../pages/ConnectionsPage';
import { useAuth } from '../context/AuthContext';
import { connectionsApi } from '../api/connections';

vi.mock('../context/AuthContext', () => ({
  useAuth: vi.fn(),
}));

vi.mock('../api/connections', () => ({
  connectionsApi: {
    getAccepted: vi.fn(),
    getPending: vi.fn(),
    acceptRequest: vi.fn(),
    rejectRequest: vi.fn(),
    cancelRequest: vi.fn(),
  },
}));

vi.mock('../api/safety', () => ({
  safetyApi: {
    getBlockedUsers: vi.fn(),
    unblockUser: vi.fn(),
  },
}));

const mockAuthUser = {
  id: 100,
  email: 'current@college.edu',
  collegeName: 'Campus Engineering College',
  onboardingCompleted: true,
};

const renderWithRouter = (ui) => {
  return render(<MemoryRouter>{ui}</MemoryRouter>);
};

describe('Slice 4: Connection Requests & Mutual Introductions', () => {
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

  describe('ConnectionsPage — Accepted Connections', () => {
    it('loads and renders accepted connections with details and purpose note', async () => {
      const mockAccepted = [
        {
          id: 1,
          userId: 201,
          fullName: 'Aarav Sharma',
          department: 'Computer Science and Engineering (CSE)',
          currentYear: 3,
          status: 'ACCEPTED',
          purposeNote: 'Looking to collaborate on the upcoming college hackathon',
          direction: 'OUTGOING',
        },
      ];

      connectionsApi.getAccepted.mockResolvedValue(mockAccepted);
      connectionsApi.getPending.mockResolvedValue({ incoming: [], outgoing: [] });

      renderWithRouter(<ConnectionsPage />);

      await waitFor(() => {
        expect(screen.getByText('Aarav Sharma')).toBeDefined();
        expect(screen.getByText(/Connected/i)).toBeDefined();
        expect(screen.getByText(/Looking to collaborate on the upcoming college hackathon/i)).toBeDefined();
        expect(screen.getByText(/Computer Science and Engineering/i)).toBeDefined();
      });
    });

    it('renders empty state when there are no accepted connections', async () => {
      connectionsApi.getAccepted.mockResolvedValue([]);
      connectionsApi.getPending.mockResolvedValue({ incoming: [], outgoing: [] });

      renderWithRouter(<ConnectionsPage />);

      await waitFor(() => {
        expect(screen.getByText(/No Connected Peers Yet/i)).toBeDefined();
      });
    });
  });

  describe('ConnectionsPage — Pending Requests Lifecycle', () => {
    it('displays incoming requests, accepts request, and updates state', async () => {
      const mockIncoming = [
        {
          id: 10,
          userId: 301,
          fullName: 'Meera Patel',
          department: 'Information Technology (IT)',
          currentYear: 2,
          status: 'PENDING',
          purposeNote: 'DSA study partner',
          direction: 'INCOMING',
        },
      ];

      connectionsApi.getAccepted.mockResolvedValue([]);
      connectionsApi.getPending.mockResolvedValue({ incoming: mockIncoming, outgoing: [] });
      connectionsApi.acceptRequest.mockResolvedValue({
        id: 10,
        status: 'ACCEPTED',
      });

      renderWithRouter(<ConnectionsPage />);

      // Switch to Pending Requests tab
      await waitFor(() => {
        expect(screen.getByRole('button', { name: /Pending Requests/i })).toBeDefined();
      });

      fireEvent.click(screen.getByRole('button', { name: /Pending Requests/i }));

      // Verify incoming request rendered
      await waitFor(() => {
        expect(screen.getByText('Meera Patel')).toBeDefined();
        expect(screen.getByText(/DSA study partner/i)).toBeDefined();
      });

      // Click Accept
      const acceptBtn = screen.getByRole('button', { name: /^Accept$/i });
      fireEvent.click(acceptBtn);

      await waitFor(() => {
        expect(connectionsApi.acceptRequest).toHaveBeenCalledWith(10);
        expect(screen.getByText(/Connection accepted successfully!/i)).toBeDefined();
      });
    });

    it('displays incoming requests, rejects request, and updates state', async () => {
      const mockIncoming = [
        {
          id: 11,
          userId: 302,
          fullName: 'Devansh Roy',
          department: 'ECE',
          currentYear: 1,
          status: 'PENDING',
          purposeNote: 'Hardware project',
          direction: 'INCOMING',
        },
      ];

      connectionsApi.getAccepted.mockResolvedValue([]);
      connectionsApi.getPending.mockResolvedValue({ incoming: mockIncoming, outgoing: [] });
      connectionsApi.rejectRequest.mockResolvedValue({
        id: 11,
        status: 'REJECTED',
      });

      renderWithRouter(<ConnectionsPage />);

      // Switch to Pending Requests tab
      await waitFor(() => {
        expect(screen.getByRole('button', { name: /Pending Requests/i })).toBeDefined();
      });

      fireEvent.click(screen.getByRole('button', { name: /Pending Requests/i }));

      await waitFor(() => {
        expect(screen.getByText('Devansh Roy')).toBeDefined();
      });

      // Click Reject
      const rejectBtn = screen.getByRole('button', { name: /^Reject$/i });
      fireEvent.click(rejectBtn);

      await waitFor(() => {
        expect(connectionsApi.rejectRequest).toHaveBeenCalledWith(11);
        expect(screen.getByText(/Connection request rejected/i)).toBeDefined();
      });
    });

    it('switches to sent sub-tab, cancels outgoing request, and updates state', async () => {
      const mockOutgoing = [
        {
          id: 25,
          userId: 401,
          fullName: 'Siddharth Sen',
          department: 'Mechanical Engineering',
          currentYear: 4,
          status: 'PENDING',
          purposeNote: 'Robotics project collab',
          direction: 'OUTGOING',
        },
      ];

      connectionsApi.getAccepted.mockResolvedValue([]);
      connectionsApi.getPending.mockResolvedValue({ incoming: [], outgoing: mockOutgoing });
      connectionsApi.cancelRequest.mockResolvedValue({
        id: 25,
        status: 'CANCELLED',
      });

      renderWithRouter(<ConnectionsPage />);

      // Switch to Pending Requests tab
      await waitFor(() => {
        expect(screen.getByRole('button', { name: /Pending Requests/i })).toBeDefined();
      });

      fireEvent.click(screen.getByRole('button', { name: /Pending Requests/i }));

      // Switch to Sent Requests sub-tab
      await waitFor(() => {
        expect(screen.getByRole('button', { name: /Sent Requests/i })).toBeDefined();
      });

      fireEvent.click(screen.getByRole('button', { name: /Sent Requests/i }));

      // Verify outgoing request rendered
      await waitFor(() => {
        expect(screen.getByText('Siddharth Sen')).toBeDefined();
        expect(screen.getByText(/Pending Response/i)).toBeDefined();
      });

      // Click Cancel Request
      const cancelBtn = screen.getByRole('button', { name: /Cancel Request/i });
      fireEvent.click(cancelBtn);

      await waitFor(() => {
        expect(connectionsApi.cancelRequest).toHaveBeenCalledWith(25);
        expect(screen.getByText(/Connection request cancelled/i)).toBeDefined();
      });
    });

    it('renders error alert when accept request fails', async () => {
      const mockIncoming = [
        {
          id: 30,
          userId: 501,
          fullName: 'Pooja Hegde',
          department: 'CSE',
          currentYear: 3,
          status: 'PENDING',
          direction: 'INCOMING',
        },
      ];

      connectionsApi.getAccepted.mockResolvedValue([]);
      connectionsApi.getPending.mockResolvedValue({ incoming: mockIncoming, outgoing: [] });
      connectionsApi.acceptRequest.mockRejectedValue(new Error('Network error or already handled'));

      renderWithRouter(<ConnectionsPage />);

      await waitFor(() => {
        expect(screen.getByRole('button', { name: /Pending Requests/i })).toBeDefined();
      });

      fireEvent.click(screen.getByRole('button', { name: /Pending Requests/i }));

      await waitFor(() => {
        expect(screen.getByText('Pooja Hegde')).toBeDefined();
      });

      const acceptBtn = screen.getByRole('button', { name: /^Accept$/i });
      fireEvent.click(acceptBtn);

      await waitFor(() => {
        expect(screen.getByText(/Network error or already handled/i)).toBeDefined();
      });
    });
  });
});
