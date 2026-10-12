import { apiClient } from './client';

export const goalsApi = {
  /**
   * Fetches all goals created by the current user.
   */
  getMyGoals() {
    return apiClient.get('/api/goals/me');
  },

  /**
   * Creates a new networking goal.
   * @param {{
   *   purpose: string,
   *   description?: string,
   *   skills?: string[],
   *   targetYear?: number,
   *   active?: boolean,
   *   expiresAt?: string
   * }} payload
   */
  createGoal(payload) {
    return apiClient.post('/api/goals', payload);
  },

  /**
   * Updates an existing networking goal.
   */
  updateGoal(goalId, payload) {
    return apiClient.put(`/api/goals/${goalId}`, payload);
  },

  /**
   * Deactivates a networking goal.
   */
  deactivateGoal(goalId) {
    return apiClient.delete(`/api/goals/${goalId}`);
  },
};
