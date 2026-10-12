import { apiClient } from './client';

export const connectionsApi = {
  /**
   * Sends a connection request to a peer.
   * @param {{ receiverId: number, purposeNote?: string }} payload
   */
  sendRequest(payload) {
    return apiClient.post('/api/connections/request', payload);
  },

  /**
   * Fetches pending incoming and outgoing connection requests.
   * @returns {Promise<{ incoming: Array, outgoing: Array }>}
   */
  getPending() {
    return apiClient.get('/api/connections/pending');
  },

  /**
   * Fetches accepted connections.
   */
  getAccepted() {
    return apiClient.get('/api/connections');
  },

  /**
   * Accepts a pending incoming connection request.
   */
  acceptRequest(id) {
    return apiClient.post(`/api/connections/${id}/accept`);
  },

  /**
   * Rejects a pending incoming connection request.
   */
  rejectRequest(id) {
    return apiClient.post(`/api/connections/${id}/reject`);
  },

  /**
   * Cancels a pending outgoing connection request.
   */
  cancelRequest(id) {
    return apiClient.post(`/api/connections/${id}/cancel`);
  },
};
