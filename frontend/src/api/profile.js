import { apiClient, ApiError } from './client';

export const profileApi = {
  /**
   * Fetches the current student's profile.
   * If the profile does not exist yet (404), returns null rather than throwing,
   * signifying that onboarding is required.
   */
  async getMyProfile() {
    try {
      return await apiClient.get('/api/profile/me');
    } catch (err) {
      if (err instanceof ApiError && err.status === 404) {
        return null;
      }
      throw err;
    }
  },

  /**
   * Creates or updates the student profile.
   * Supported fields: fullName, department, course, currentYear, bio, discoverable,
   * skills, interests, availability.
   * @param {{
   *   fullName: string,
   *   department: string,
   *   course: string,
   *   currentYear: number,
   *   bio?: string,
   *   discoverable?: boolean,
   *   skills?: string[],
   *   interests?: string[],
   *   availability?: string
   * }} payload
   */
  updateMyProfile(payload) {
    const body = {
      fullName: payload.fullName.trim(),
      department: payload.department.trim(),
      course: payload.course.trim(),
      currentYear: parseInt(payload.currentYear, 10),
      bio: payload.bio !== undefined && payload.bio !== null ? payload.bio.trim() : '',
      discoverable: payload.discoverable !== undefined ? Boolean(payload.discoverable) : true,
      skills: Array.isArray(payload.skills) ? payload.skills : [],
      interests: Array.isArray(payload.interests) ? payload.interests : [],
      availability: payload.availability !== undefined && payload.availability !== null ? payload.availability.trim() : null,
    };
    return apiClient.put('/api/profile/me', body);
  },
};
