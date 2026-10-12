import { apiClient } from './client';

export const safetyApi = {
  /**
   * Blocks a user.
   */
  blockUser(userId) {
    return apiClient.post(`/api/users/${userId}/block`);
  },

  /**
   * Unblocks a user.
   */
  unblockUser(userId) {
    return apiClient.delete(`/api/users/${userId}/block`);
  },

  /**
   * Fetches list of users blocked by the current user.
   */
  getBlockedUsers() {
    return apiClient.get('/api/users/blocked');
  },

  /**
   * Submits a safety / abuse report for a user.
   * @param {number} userId
   * @param {{ reason: string, description?: string }} payload
   */
  reportUser(userId, payload) {
    return apiClient.post(`/api/users/${userId}/report`, payload);
  },
};
