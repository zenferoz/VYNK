import { apiClient } from './client';

export const notificationsApi = {
  getNotifications(page = 0, size = 20) {
    return apiClient.get(`/api/notifications?page=${page}&size=${size}`);
  },

  getUnreadCount() {
    return apiClient.get('/api/notifications/unread-count');
  },

  markRead(id) {
    return apiClient.put(`/api/notifications/${id}/read`);
  },

  markAllRead() {
    return apiClient.put('/api/notifications/read-all');
  },
};
