import { apiClient } from './client';

export const messagesApi = {
  getConversations() {
    return apiClient.get('/api/messages/conversations');
  },

  getConversationHistory(peerId, page = 0, size = 50) {
    return apiClient.get(`/api/messages/${peerId}?page=${page}&size=${size}`);
  },

  sendMessage(peerId, content) {
    return apiClient.post(`/api/messages/${peerId}`, { content });
  },

  markConversationRead(peerId) {
    return apiClient.put(`/api/messages/${peerId}/read`);
  },
};
