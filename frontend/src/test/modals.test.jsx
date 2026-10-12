import React from 'react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor, act } from '@testing-library/react';
import { ConnectModal } from '../components/discovery/ConnectModal';
import { BlockConfirmModal } from '../components/safety/BlockConfirmModal';
import { ReportModal } from '../components/safety/ReportModal';
import { connectionsApi } from '../api/connections';
import { safetyApi } from '../api/safety';

// Mock API modules
vi.mock('../api/connections', () => ({
  connectionsApi: {
    sendRequest: vi.fn(),
  },
}));

vi.mock('../api/safety', () => ({
  safetyApi: {
    blockUser: vi.fn(),
    reportUser: vi.fn(),
  },
}));

const mockStudent = {
  userId: 'student-uuid-123',
  id: 'student-uuid-123',
  fullName: 'Alex Morgan',
  department: 'Computer Science',
  currentYear: 3,
};

describe('Modal Integration Tests', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  describe('ConnectModal', () => {
    it('renders with accessible dialog role, aria-modal, and labeled heading', () => {
      render(<ConnectModal student={mockStudent} onClose={vi.fn()} />);

      const dialog = screen.getByRole('dialog');
      expect(dialog).toBeDefined();
      expect(dialog.getAttribute('aria-modal')).toBe('true');
      expect(dialog.getAttribute('aria-labelledby')).toBe('connect-modal-title');

      const title = screen.getByText('Connect with Alex Morgan');
      expect(title.id).toBe('connect-modal-title');
    });

    it('renders nothing when student is null or undefined', () => {
      const { container } = render(<ConnectModal student={null} onClose={vi.fn()} />);
      expect(container.firstChild).toBeNull();
    });

    it('closes on Escape key press without sending a connection request', () => {
      const onClose = vi.fn();
      render(<ConnectModal student={mockStudent} onClose={onClose} />);

      fireEvent.keyDown(document, { key: 'Escape' });

      expect(onClose).toHaveBeenCalledTimes(1);
      expect(connectionsApi.sendRequest).not.toHaveBeenCalled();
    });

    it('closes on backdrop/overlay click', () => {
      const onClose = vi.fn();
      render(<ConnectModal student={mockStudent} onClose={onClose} />);

      const dialogOverlay = screen.getByRole('dialog');
      fireEvent.click(dialogOverlay);

      expect(onClose).toHaveBeenCalledTimes(1);
      expect(connectionsApi.sendRequest).not.toHaveBeenCalled();
    });

    it('does NOT close when clicking inside the modal dialog card', () => {
      const onClose = vi.fn();
      render(<ConnectModal student={mockStudent} onClose={onClose} />);

      const textarea = screen.getByLabelText(/Purpose Note/i);
      fireEvent.click(textarea);

      expect(onClose).not.toHaveBeenCalled();
    });

    it('submits connection request successfully and triggers callbacks', async () => {
      const onClose = vi.fn();
      const onSuccess = vi.fn();
      connectionsApi.sendRequest.mockResolvedValueOnce({ id: 'req-1' });

      render(<ConnectModal student={mockStudent} onClose={onClose} onSuccess={onSuccess} />);

      const textarea = screen.getByLabelText(/Purpose Note/i);
      fireEvent.change(textarea, { target: { value: 'Looking for a study group partner.' } });

      const submitButton = screen.getByRole('button', { name: /Send Request/i });
      fireEvent.click(submitButton);

      await waitFor(() => {
        expect(connectionsApi.sendRequest).toHaveBeenCalledWith({
          receiverId: 'student-uuid-123',
          purposeNote: 'Looking for a study group partner.',
        });
        expect(onSuccess).toHaveBeenCalledTimes(1);
        expect(onClose).toHaveBeenCalledTimes(1);
      });
    });

    it('displays error alert on failure and does not close modal', async () => {
      const onClose = vi.fn();
      connectionsApi.sendRequest.mockRejectedValueOnce(new Error('Connection limit reached'));

      render(<ConnectModal student={mockStudent} onClose={onClose} />);

      const submitButton = screen.getByRole('button', { name: /Send Request/i });
      fireEvent.click(submitButton);

      await waitFor(() => {
        expect(screen.getByText('Connection limit reached')).toBeDefined();
        expect(onClose).not.toHaveBeenCalled();
      });
    });

    it('displays and updates character counter as user types', () => {
      render(<ConnectModal student={mockStudent} onClose={vi.fn()} />);

      expect(screen.getByText('0/300')).toBeDefined();

      const textarea = screen.getByLabelText(/Purpose Note/i);
      const text120 = 'A'.repeat(120);
      fireEvent.change(textarea, { target: { value: text120 } });

      expect(screen.getByText('120/300')).toBeDefined();
    });

    it('prevents typing or pasting from exceeding the 300-character maximum', () => {
      render(<ConnectModal student={mockStudent} onClose={vi.fn()} />);

      const textarea = screen.getByLabelText(/Purpose Note/i);
      const text350 = 'B'.repeat(350);
      fireEvent.change(textarea, { target: { value: text350 } });

      expect(textarea.value.length).toBe(300);
      expect(textarea.value).toBe('B'.repeat(300));
      expect(screen.getByText('300/300')).toBeDefined();
    });

    it('submits a valid note exactly at the 300-character boundary', async () => {
      const onClose = vi.fn();
      const onSuccess = vi.fn();
      connectionsApi.sendRequest.mockResolvedValueOnce({ id: 'req-300' });

      render(<ConnectModal student={mockStudent} onClose={onClose} onSuccess={onSuccess} />);

      const textarea = screen.getByLabelText(/Purpose Note/i);
      const text300 = 'C'.repeat(300);
      fireEvent.change(textarea, { target: { value: text300 } });

      const submitButton = screen.getByRole('button', { name: /Send Request/i });
      fireEvent.click(submitButton);

      await waitFor(() => {
        expect(connectionsApi.sendRequest).toHaveBeenCalledWith({
          receiverId: 'student-uuid-123',
          purposeNote: text300,
        });
        expect(onSuccess).toHaveBeenCalledTimes(1);
        expect(onClose).toHaveBeenCalledTimes(1);
      });
    });
  });

  describe('BlockConfirmModal', () => {
    it('renders with accessible dialog role, aria-modal, and labeled title', () => {
      render(<BlockConfirmModal student={mockStudent} onClose={vi.fn()} />);

      const dialog = screen.getByRole('dialog');
      expect(dialog).toBeDefined();
      expect(dialog.getAttribute('aria-modal')).toBe('true');
      expect(dialog.getAttribute('aria-labelledby')).toBe('block-modal-title');

      const title = screen.getByText('Block Alex Morgan?');
      expect(title.id).toBe('block-modal-title');
    });

    it('renders nothing when student is null or undefined', () => {
      const { container } = render(<BlockConfirmModal student={null} onClose={vi.fn()} />);
      expect(container.firstChild).toBeNull();
    });

    it('Escape closes the dialog and does NOT trigger destructive block action', () => {
      const onClose = vi.fn();
      render(<BlockConfirmModal student={mockStudent} onClose={onClose} />);

      fireEvent.keyDown(document, { key: 'Escape' });

      expect(onClose).toHaveBeenCalledTimes(1);
      expect(safetyApi.blockUser).not.toHaveBeenCalled();
    });

    it('backdrop click closes dialog without blocking', () => {
      const onClose = vi.fn();
      render(<BlockConfirmModal student={mockStudent} onClose={onClose} />);

      const overlay = screen.getByRole('dialog');
      fireEvent.click(overlay);

      expect(onClose).toHaveBeenCalledTimes(1);
      expect(safetyApi.blockUser).not.toHaveBeenCalled();
    });

    it('clicking inside modal content does not dismiss dialog', () => {
      const onClose = vi.fn();
      render(<BlockConfirmModal student={mockStudent} onClose={onClose} />);

      const warningText = screen.getByText(/Blocking will permanently prevent/i);
      fireEvent.click(warningText);

      expect(onClose).not.toHaveBeenCalled();
    });

    it('calls safetyApi.blockUser when clicking Block Student button and triggers onSuccess', async () => {
      const onClose = vi.fn();
      const onSuccess = vi.fn();
      safetyApi.blockUser.mockResolvedValueOnce({ success: true });

      render(<BlockConfirmModal student={mockStudent} onClose={onClose} onSuccess={onSuccess} />);

      const blockButton = screen.getByRole('button', { name: /Block Student/i });
      fireEvent.click(blockButton);

      await waitFor(() => {
        expect(safetyApi.blockUser).toHaveBeenCalledWith('student-uuid-123');
        expect(onSuccess).toHaveBeenCalledTimes(1);
        expect(onClose).toHaveBeenCalledTimes(1);
      });
    });

    it('displays error message when blocking fails', async () => {
      const onClose = vi.fn();
      safetyApi.blockUser.mockRejectedValueOnce(new Error('Cannot block this user'));

      render(<BlockConfirmModal student={mockStudent} onClose={onClose} />);

      const blockButton = screen.getByRole('button', { name: /Block Student/i });
      fireEvent.click(blockButton);

      await waitFor(() => {
        expect(screen.getByText('Cannot block this user')).toBeDefined();
        expect(onClose).not.toHaveBeenCalled();
      });
    });
  });

  describe('ReportModal', () => {
    it('renders with accessible dialog role, aria-modal, and labeled title', () => {
      render(<ReportModal student={mockStudent} onClose={vi.fn()} />);

      const dialog = screen.getByRole('dialog');
      expect(dialog).toBeDefined();
      expect(dialog.getAttribute('aria-modal')).toBe('true');
      expect(dialog.getAttribute('aria-labelledby')).toBe('report-modal-title');

      const title = screen.getByText('Report Alex Morgan');
      expect(title.id).toBe('report-modal-title');
    });

    it('renders nothing when student is null or undefined', () => {
      const { container } = render(<ReportModal student={null} onClose={vi.fn()} />);
      expect(container.firstChild).toBeNull();
    });

    it('Escape key closes modal and does not submit report', () => {
      const onClose = vi.fn();
      render(<ReportModal student={mockStudent} onClose={onClose} />);

      fireEvent.keyDown(document, { key: 'Escape' });

      expect(onClose).toHaveBeenCalledTimes(1);
      expect(safetyApi.reportUser).not.toHaveBeenCalled();
    });

    it('backdrop click closes modal and inner click does not dismiss', () => {
      const onClose = vi.fn();
      render(<ReportModal student={mockStudent} onClose={onClose} />);

      // Inner click
      const reasonSelect = screen.getByLabelText(/Reason for Report/i);
      fireEvent.click(reasonSelect);
      expect(onClose).not.toHaveBeenCalled();

      // Backdrop click
      const dialog = screen.getByRole('dialog');
      fireEvent.click(dialog);
      expect(onClose).toHaveBeenCalledTimes(1);
    });

    it('submits report with selected reason and description, showing success confirmation', async () => {
      const setTimeoutSpy = vi.spyOn(window, 'setTimeout');
      const onClose = vi.fn();
      const onSuccess = vi.fn();
      safetyApi.reportUser.mockResolvedValueOnce({ id: 'report-1' });

      render(<ReportModal student={mockStudent} onClose={onClose} onSuccess={onSuccess} />);

      const reasonSelect = screen.getByLabelText(/Reason for Report/i);
      fireEvent.change(reasonSelect, { target: { value: 'HARASSMENT' } });

      const descriptionInput = screen.getByLabelText(/Details/i);
      fireEvent.change(descriptionInput, { target: { value: 'Inappropriate messages sent.' } });

      const submitButton = screen.getByRole('button', { name: /Submit Report/i });
      fireEvent.click(submitButton);

      await waitFor(() => {
        expect(safetyApi.reportUser).toHaveBeenCalledWith('student-uuid-123', {
          reason: 'HARASSMENT',
          description: 'Inappropriate messages sent.',
        });
      });

      expect(screen.getByText(/Report submitted safely/i)).toBeDefined();

      const autoCloseCall = setTimeoutSpy.mock.calls.find((call) => call[1] === 1500);
      expect(autoCloseCall).toBeDefined();

      act(() => {
        autoCloseCall[0]();
      });

      expect(onSuccess).toHaveBeenCalledTimes(1);
      expect(onClose).toHaveBeenCalledTimes(1);

      setTimeoutSpy.mockRestore();
    });

    it('shows error alert when reporting fails', async () => {
      const onClose = vi.fn();
      safetyApi.reportUser.mockRejectedValueOnce(new Error('Service temporarily unavailable'));

      render(<ReportModal student={mockStudent} onClose={onClose} />);

      const submitButton = screen.getByRole('button', { name: /Submit Report/i });
      fireEvent.click(submitButton);

      await waitFor(() => {
        expect(screen.getByText('Service temporarily unavailable')).toBeDefined();
        expect(onClose).not.toHaveBeenCalled();
      });
    });
  });
});
