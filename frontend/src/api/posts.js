import { apiClient } from './client';

export const postsApi = {
  getFeed(page = 0, size = 20) {
    return apiClient.get(`/api/posts?page=${page}&size=${size}`);
  },

  createPost(content) {
    return apiClient.post('/api/posts', { content });
  },

  deletePost(id) {
    return apiClient.delete(`/api/posts/${id}`);
  },

  updatePost(id, content) {
    return apiClient.put(`/api/posts/${id}`, { content });
  },
};
