import React from 'react';
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { BlockConfirmModal } from '../components/safety/BlockConfirmModal';
import { ReportModal } from '../components/safety/ReportModal';
import { ConnectionsPage } from '../pages/ConnectionsPage';
import { safetyApi } from '../api/safety';
import { connectionsApi } from '../api/connections';
import { useAuth } from '../context/AuthContext';

vi.mock('../context/AuthContext', () => ({
  useAuth: vi.fn(),
}));

vi.mock('../api/safety', () => ({
  safetyApi: {
    blockUser: vi.fn(),
    unblockUser: vi.fn(),
    getBlockedUsers: vi.fn(),
    reportUser: vi.fn(),
  },
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

const mockAuthUser = {
  id: 100,
  email: 'current@college.edu',
  collegeName: 'Campus Engineering College',
  onboardingCompleted: true,
};

const renderWithRouter = (ui) => {
  return render(<MemoryRouter>{ui}</MemoryRouter>);
};

describe('Slice 5: Safety, Reporting & User Controls', () => {
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

  describe('BlockConfirmModal Component', () => {
    it('renders confirmation dialog and blocks user on submit', async () => {
      const mockStudent = { userId: 201, fullName: 'Problematic Student' };
      const onClose = vi.fn();
      const onSuccess = vi.fn();

      safetyApi.blockUser.mockResolvedValue({ message: 'User blocked successfully.' });

      renderWithRouter(
        <BlockConfirmModal student={mockStudent} onClose={onClose} onSuccess={onSuccess} />
      );

      expect(screen.getByText(/Block Problematic Student\?/i)).toBeDefined();

      const blockBtn = screen.getByRole('button', { name: /Block Student/i });
      fireEvent.click(blockBtn);

      await waitFor(() => {
        expect(safetyApi.blockUser).toHaveBeenCalledWith(201);
        expect(onSuccess).toHaveBeenCalled();
        expect(onClose).toHaveBeenCalled();
      });
    });

    it('displays error alert when blocking fails', async () => {
      const mockStudent = { userId: 201, fullName: 'Problematic Student' };
      safetyApi.blockUser.mockRejectedValue(new Error('Cannot block this user'));

      renderWithRouter(
        <BlockConfirmModal student={mockStudent} onClose={vi.fn()} onSuccess={vi.fn()} />
      );

      const blockBtn = screen.getByRole('button', { name: /Block Student/i });
      fireEvent.click(blockBtn);

      await waitFor(() => {
        expect(screen.getByText(/Cannot block this user/i)).toBeDefined();
      });
    });
  });

  describe('ReportModal Component', () => {
    it('submits safety report with selected category and description', async () => {
      const mockStudent = { userId: 305, fullName: 'Spam Student' };
      const onClose = vi.fn();
      const onSuccess = vi.fn();

      safetyApi.reportUser.mockResolvedValue({ message: 'Report submitted successfully.' });

      renderWithRouter(
        <ReportModal student={mockStudent} onClose={onClose} onSuccess={onSuccess} />
      );

      expect(screen.getByText(/Report Spam Student/i)).toBeDefined();

      // Select reason
      const reasonSelect = screen.getByLabelText(/Reason for Report/i);
      fireEvent.change(reasonSelect, { target: { value: 'SPAM' } });

      // Enter description
      const descInput = screen.getByLabelText(/Details/i);
      fireEvent.change(descInput, { target: { value: 'Promoting external spam links' } });

      // Submit
      const submitBtn = screen.getByRole('button', { name: /Submit Report/i });
      fireEvent.click(submitBtn);

      await waitFor(() => {
        expect(safetyApi.reportUser).toHaveBeenCalledWith(305, {
          reason: 'SPAM',
          description: 'Promoting external spam links',
        });
        expect(screen.getByText(/Report submitted safely/i)).toBeDefined();
      });
    });
  });

  describe('Blocked Students Tab & Contract Alignment (Slice 5 Fix Verification)', () => {
    it('renders meaningful student id and valid date using real BlockedUserResponse format', async () => {
      // Backend BlockedUserResponse contract: { id, blockedUserId, createdAt }
      const mockBlockedFromBackend = [
        {
          id: 1,
          blockedUserId: 201,
          createdAt: '2026-10-10T14:30:00',
        },
      ];

      connectionsApi.getAccepted.mockResolvedValue([]);
      connectionsApi.getPending.mockResolvedValue({ incoming: [], outgoing: [] });
      safetyApi.getBlockedUsers.mockResolvedValue(mockBlockedFromBackend);

      renderWithRouter(<ConnectionsPage />);

      // Switch to Blocked tab
      await waitFor(() => {
        expect(screen.getByRole('button', { name: /Blocked Students/i })).toBeDefined();
      });

      fireEvent.click(screen.getByRole('button', { name: /Blocked Students/i }));

      await waitFor(() => {
        // Must render meaningful student identification
        expect(screen.getByText(/Student #201/i)).toBeDefined();
        // Must NOT render "Invalid Date"
        expect(screen.queryByText(/Invalid Date/i)).toBeNull();
        expect(screen.getByText(/Blocked on/i)).toBeDefined();
        // Must have unblock button for user 201
        expect(screen.getByRole('button', { name: /Unblock/i })).toBeDefined();
      });
    });

    it('unblocks student on click and refreshes list', async () => {
      const mockBlocked = [
        {
          id: 5,
          blockedUserId: 350,
          createdAt: '2026-10-11T09:00:00',
        },
      ];

      connectionsApi.getAccepted.mockResolvedValue([]);
      connectionsApi.getPending.mockResolvedValue({ incoming: [], outgoing: [] });
      safetyApi.getBlockedUsers
        .mockResolvedValueOnce(mockBlocked)
        .mockResolvedValueOnce([]);
      safetyApi.unblockUser.mockResolvedValue({ message: 'User unblocked successfully.' });

      renderWithRouter(<ConnectionsPage />);

      await waitFor(() => {
        expect(screen.getByRole('button', { name: /Blocked Students/i })).toBeDefined();
      });

      fireEvent.click(screen.getByRole('button', { name: /Blocked Students/i }));

      await waitFor(() => {
        expect(screen.getByText(/Student #350/i)).toBeDefined();
      });

      const unblockBtn = screen.getByRole('button', { name: /Unblock/i });
      fireEvent.click(unblockBtn);

      await waitFor(() => {
        expect(safetyApi.unblockUser).toHaveBeenCalledWith(350);
        expect(screen.getByText(/Student unblocked successfully/i)).toBeDefined();
      });
    });
  });
});
