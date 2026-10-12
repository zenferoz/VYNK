import { apiClient } from './client';

export const collegesApi = {
  /**
   * Fetches active registered colleges.
   * @returns {Promise<Array<{ id: number, name: string, emailDomain: string, active: boolean }>>}
   */
  getColleges() {
    return apiClient.get('/api/colleges');
  },
};
