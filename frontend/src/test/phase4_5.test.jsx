import { describe, it, expect, vi, beforeEach } from 'vitest';
import React from 'react';
import { render, screen, waitFor, fireEvent } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';

import { FeedPage } from '../pages/FeedPage';
import { MessagesPage } from '../pages/MessagesPage';
import { CommunitiesPage } from '../pages/CommunitiesPage';
import { NotificationsPage } from '../pages/NotificationsPage';
import { StudentCard } from '../components/discovery/StudentCard';

import { postsApi } from '../api/posts';
import { messagesApi } from '../api/messages';
import { communitiesApi } from '../api/communities';
import { notificationsApi } from '../api/notifications';
import { filesApi } from '../api/files';

vi.mock('../api/files', () => ({
  filesApi: {
    uploadFile: vi.fn(),
    getDownloadUrl: vi.fn((id) => `/api/files/${id}/download`),
  },
}));

vi.mock('../api/posts', () => ({
  postsApi: {
    getFeed: vi.fn(),
    createPost: vi.fn(),
    deletePost: vi.fn(),
    updatePost: vi.fn(),
  },
}));

vi.mock('../api/messages', () => ({
  messagesApi: {
    getConversations: vi.fn(),
    getConversationHistory: vi.fn(),
    sendMessage: vi.fn(),
    markConversationRead: vi.fn(),
  },
}));

vi.mock('../api/communities', () => ({
  communitiesApi: {
    getCommunities: vi.fn(),
    createCommunity: vi.fn(),
    getCommunityDetail: vi.fn(),
    joinCommunity: vi.fn(),
    leaveCommunity: vi.fn(),
    getDiscussions: vi.fn(),
    createDiscussion: vi.fn(),
    deleteDiscussion: vi.fn(),
  },
}));

vi.mock('../api/notifications', () => ({
  notificationsApi: {
    getNotifications: vi.fn(),
    getUnreadCount: vi.fn(),
    markRead: vi.fn(),
    markAllRead: vi.fn(),
  },
}));

describe('Phases 4 & 5 Frontend Integration Tests', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  describe('FeedPage Component', () => {
    it('loads and renders campus feed posts', async () => {
      postsApi.getFeed.mockResolvedValueOnce({
        content: [
          {
            id: 1,
            authorName: 'Alex Rivera',
            authorDepartment: 'CSE',
            authorYear: 3,
            content: 'Anyone interested in teaming up for the upcoming hackathon?',
            mine: false,
            createdAt: '2026-10-10T10:00:00',
          },
        ],
      });

      render(
        <MemoryRouter>
          <FeedPage />
        </MemoryRouter>
      );

      expect(await screen.findByText('Campus Feed')).toBeDefined();
      expect(await screen.findByText('Alex Rivera')).toBeDefined();
      expect(screen.getByText('Anyone interested in teaming up for the upcoming hackathon?')).toBeDefined();
      expect(screen.getByText('CSE')).toBeDefined();
    });

    it('creates a new post and refreshes the feed', async () => {
      postsApi.getFeed.mockResolvedValue({ content: [] });
      postsApi.createPost.mockResolvedValueOnce({ id: 2, content: 'Excited for semester project!' });

      render(
        <MemoryRouter>
          <FeedPage />
        </MemoryRouter>
      );

      const textarea = await screen.findByPlaceholderText(/Share a project idea/i);
      fireEvent.change(textarea, { target: { value: 'Excited for semester project!' } });

      const submitBtn = screen.getByRole('button', { name: /Publish Post/i });
      fireEvent.click(submitBtn);

      await waitFor(() => {
        expect(postsApi.createPost).toHaveBeenCalledWith('Excited for semester project!');
      });
    });

    it('allows author to edit their own post', async () => {
      postsApi.getFeed.mockResolvedValueOnce({
        content: [
          {
            id: 15,
            authorName: 'Alex Rivera',
            authorDepartment: 'CSE',
            authorYear: 3,
            content: 'Original post content',
            mine: true,
            createdAt: '2026-10-10T10:00:00',
          },
        ],
      });
      postsApi.updatePost.mockResolvedValueOnce({
        id: 15,
        content: 'Updated post content',
      });

      render(
        <MemoryRouter>
          <FeedPage />
        </MemoryRouter>
      );

      expect(await screen.findByText('Original post content')).toBeDefined();
      const editBtn = screen.getByTitle('Edit your post');
      fireEvent.click(editBtn);

      const editInput = screen.getByDisplayValue('Original post content');
      fireEvent.change(editInput, { target: { value: 'Updated post content' } });

      const saveBtn = screen.getByRole('button', { name: /Save Changes/i });
      fireEvent.click(saveBtn);

      await waitFor(() => {
        expect(postsApi.updatePost).toHaveBeenCalledWith(15, 'Updated post content');
      });
      expect(await screen.findByText('Updated post content')).toBeDefined();
    });
  });

  describe('MessagesPage Component', () => {
    it('renders conversation list and message history with attachment pill', async () => {
      messagesApi.getConversations.mockResolvedValue([
        {
          peerId: 42,
          peerName: 'Samira Khan',
          peerDepartment: 'ECE',
          lastMessage: 'Check the attached notes.',
          lastMessageAt: '2026-10-10T12:00:00',
          unreadCount: 1,
        },
      ]);
      messagesApi.getConversationHistory.mockResolvedValue({
        content: [
          {
            id: 101,
            senderId: 42,
            recipientId: 10,
            content: 'Check the attached notes.',
            mine: false,
            createdAt: '2026-10-10T12:00:00',
            attachmentFileName: 'lecture_notes.pdf',
            attachmentDownloadUrl: '/api/files/88/download',
          },
        ],
      });
      messagesApi.markConversationRead.mockResolvedValue({});

      render(
        <MemoryRouter>
          <MessagesPage />
        </MemoryRouter>
      );

      expect(await screen.findByText('Direct Messages')).toBeDefined();
      const peerElements = await screen.findAllByText('Samira Khan');
      expect(peerElements.length).toBeGreaterThan(0);
      expect((await screen.findAllByText('Check the attached notes.')).length).toBeGreaterThan(0);
      expect(await screen.findByText(/lecture_notes\.pdf/i)).toBeDefined();
    });
  });

  describe('CommunitiesPage Component', () => {
    it('renders communities and allows joining', async () => {
      communitiesApi.getCommunities.mockResolvedValueOnce({
        content: [
          {
            id: 10,
            name: 'Web Dev Enthusiasts',
            description: 'Building modern fullstack apps together.',
            purpose: 'PROJECT_COLLABORATION',
            memberCount: 5,
            member: false,
          },
        ],
      });
      communitiesApi.joinCommunity.mockResolvedValueOnce({});

      render(
        <MemoryRouter>
          <CommunitiesPage />
        </MemoryRouter>
      );

      expect(await screen.findByText('Web Dev Enthusiasts')).toBeDefined();
      expect(screen.getByText('Building modern fullstack apps together.')).toBeDefined();

      const joinBtn = screen.getByRole('button', { name: /Join/i });
      fireEvent.click(joinBtn);

      await waitFor(() => {
        expect(communitiesApi.joinCommunity).toHaveBeenCalledWith(10);
      });
    });

    it('renders discussions with attachment pill', async () => {
      communitiesApi.getCommunities.mockResolvedValueOnce({
        content: [
          {
            id: 10,
            name: 'Web Dev Enthusiasts',
            description: 'Building modern fullstack apps together.',
            purpose: 'PROJECT_COLLABORATION',
            memberCount: 5,
            member: true,
          },
        ],
      });
      communitiesApi.getDiscussions.mockResolvedValueOnce({
        content: [
          {
            id: 1,
            communityId: 10,
            title: 'Fullstack Architecture Diagram',
            content: 'Here is our draft architecture.\n[Attachment: architecture_spec.pdf](/api/files/88/download)',
            authorName: 'Alex Rivera',
            createdAt: '2026-10-10T12:00:00',
            mine: false,
          },
        ],
      });

      render(
        <MemoryRouter>
          <CommunitiesPage />
        </MemoryRouter>
      );

      // Select community card
      const commCard = await screen.findByText('Web Dev Enthusiasts');
      fireEvent.click(commCard);

      expect(await screen.findByText('Fullstack Architecture Diagram')).toBeDefined();
      expect(screen.getByText('Here is our draft architecture.')).toBeDefined();
      const pill = await screen.findByText(/architecture_spec\.pdf/i);
      expect(pill).toBeDefined();
    });

    it('uploads an attachment and posts discussion', async () => {
      communitiesApi.getCommunities.mockResolvedValueOnce({
        content: [
          {
            id: 10,
            name: 'Web Dev Enthusiasts',
            description: 'Building modern fullstack apps together.',
            purpose: 'PROJECT_COLLABORATION',
            memberCount: 5,
            member: true,
          },
        ],
      });
      communitiesApi.getDiscussions.mockResolvedValue({ content: [] });
      filesApi.uploadFile.mockResolvedValueOnce({
        id: 77,
        originalFilename: 'diagram.png',
      });
      communitiesApi.createDiscussion.mockResolvedValueOnce({
        id: 2,
        title: 'Project Kickoff',
        content: 'Check attached diagram\n[Attachment: diagram.png](/api/files/77/download)',
      });

      render(
        <MemoryRouter>
          <CommunitiesPage />
        </MemoryRouter>
      );

      const commCard = await screen.findByText('Web Dev Enthusiasts');
      fireEvent.click(commCard);

      const titleInput = await screen.findByPlaceholderText('Discussion Title');
      const contentInput = screen.getByPlaceholderText('Write your discussion post...');
      fireEvent.change(titleInput, { target: { value: 'Project Kickoff' } });
      fireEvent.change(contentInput, { target: { value: 'Check attached diagram' } });

      const file = new File(['dummy png content'], 'diagram.png', { type: 'image/png' });
      const attachBtn = screen.getByRole('button', { name: /Attach File/i });
      expect(attachBtn).toBeDefined();

      const fileInput = document.querySelector('input[type="file"]');
      fireEvent.change(fileInput, { target: { files: [file] } });

      expect(await screen.findByText(/diagram\.png/)).toBeDefined();

      const submitBtn = screen.getByRole('button', { name: /Post Discussion/i });
      fireEvent.click(submitBtn);

      await waitFor(() => {
        expect(filesApi.uploadFile).toHaveBeenCalledWith(file, 'COMMUNITY', 10);
      });
      await waitFor(() => {
        expect(communitiesApi.createDiscussion).toHaveBeenCalledWith(10, {
          title: 'Project Kickoff',
          content: 'Check attached diagram\n[Attachment: diagram.png](/api/files/77/download)',
        });
      });
    });

    it('validates and rejects file exceeding 5MB limit', async () => {
      communitiesApi.getCommunities.mockResolvedValueOnce({
        content: [
          {
            id: 10,
            name: 'Web Dev Enthusiasts',
            description: 'Building modern fullstack apps together.',
            purpose: 'PROJECT_COLLABORATION',
            memberCount: 5,
            member: true,
          },
        ],
      });
      communitiesApi.getDiscussions.mockResolvedValue({ content: [] });

      render(
        <MemoryRouter>
          <CommunitiesPage />
        </MemoryRouter>
      );

      const commCard = await screen.findByText('Web Dev Enthusiasts');
      fireEvent.click(commCard);

      // Create a 6MB file
      const largeFile = new File(['x'.repeat(100)], 'huge.pdf', { type: 'application/pdf' });
      Object.defineProperty(largeFile, 'size', { value: 6 * 1024 * 1024 });

      const fileInput = document.querySelector('input[type="file"]');
      fireEvent.change(fileInput, { target: { files: [largeFile] } });

      expect(await screen.findByText(/Selected file exceeds maximum limit of 5MB/i)).toBeDefined();
      expect(filesApi.uploadFile).not.toHaveBeenCalled();
    });
  });

  describe('NotificationsPage Component', () => {
    it('renders notifications and supports mark all read', async () => {
      notificationsApi.getNotifications.mockResolvedValueOnce({
        content: [
          {
            id: 99,
            type: 'CONNECTION_REQUEST',
            title: 'New Connection Request',
            message: 'Rohan Sharma sent you a connection request.',
            read: false,
            createdAt: '2026-10-10T09:00:00',
          },
        ],
      });
      notificationsApi.markAllRead.mockResolvedValueOnce({});

      render(
        <MemoryRouter>
          <NotificationsPage />
        </MemoryRouter>
      );

      expect(await screen.findByText('Notifications')).toBeDefined();
      expect(await screen.findByText('New Connection Request')).toBeDefined();
      expect(screen.getByText('Rohan Sharma sent you a connection request.')).toBeDefined();

      const markAllBtn = screen.getByRole('button', { name: /Mark all as read/i });
      fireEvent.click(markAllBtn);

      await waitFor(() => {
        expect(notificationsApi.markAllRead).toHaveBeenCalled();
      });
    });
  });

  describe('AI Matching StudentCard Component', () => {
    it('renders AI Enhanced badge when semanticAssisted is true', () => {
      const student = {
        userId: 77,
        fullName: 'Maya Lin',
        department: 'CSE',
        currentYear: 4,
        matchScore: 92,
        semanticAssisted: true,
        matchReasons: ['Shared purpose: Project Collaboration', 'Semantic synergy: 85% intent match'],
      };

      render(
        <MemoryRouter>
          <StudentCard student={student} isMatch={true} />
        </MemoryRouter>
      );

      expect(screen.getByText('92% Match')).toBeDefined();
      expect(screen.getByText('AI Enhanced')).toBeDefined();
    });

    it('does not render AI Enhanced badge when semanticAssisted is false', () => {
      const student = {
        userId: 88,
        fullName: 'Dev Patel',
        department: 'ECE',
        currentYear: 2,
        matchScore: 70,
        semanticAssisted: false,
      };

      render(
        <MemoryRouter>
          <StudentCard student={student} isMatch={true} />
        </MemoryRouter>
      );

      expect(screen.getByText('70% Match')).toBeDefined();
      expect(screen.queryByText('AI Enhanced')).toBeNull();
    });
  });
});
