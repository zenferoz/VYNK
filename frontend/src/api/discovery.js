import { apiClient } from './client';

export const discoveryApi = {
  /**
   * Fetches discoverable students from the authenticated student's college.
   * @returns {Promise<Array<{
   *   id: number,
   *   userId: number,
   *   fullName: string,
   *   department: string,
   *   course: string,
   *   currentYear: number,
   *   bio: string,
   *   collegeName: string
   * }>>}
   */
  /**
   * Fetches discoverable students from the authenticated student's college.
   */
  getStudents() {
    return apiClient.get('/api/students');
  },

  /**
   * Fetches top purpose-based matches with explainable scores.
   */
  getMatches() {
    return apiClient.get('/api/students/matches');
  },
};
