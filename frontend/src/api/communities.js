import { apiClient } from './client';

export const communitiesApi = {
  getCommunities(page = 0, size = 20) {
    return apiClient.get(`/api/communities?page=${page}&size=${size}`);
  },

  createCommunity(data) {
    return apiClient.post('/api/communities', data);
  },

  getCommunityDetail(id) {
    return apiClient.get(`/api/communities/${id}`);
  },

  joinCommunity(id) {
    return apiClient.post(`/api/communities/${id}/join`);
  },

  leaveCommunity(id) {
    return apiClient.post(`/api/communities/${id}/leave`);
  },

  getDiscussions(communityId, page = 0, size = 30) {
    return apiClient.get(`/api/communities/${communityId}/discussions?page=${page}&size=${size}`);
  },

  createDiscussion(communityId, data) {
    return apiClient.post(`/api/communities/${communityId}/discussions`, data);
  },

  deleteDiscussion(communityId, discussionId) {
    return apiClient.delete(`/api/communities/${communityId}/discussions/${discussionId}`);
  },
};
