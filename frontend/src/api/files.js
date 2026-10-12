import { apiClient } from './client';

export const filesApi = {
  uploadFile(file, associationType = null, associationId = null) {
    const formData = new FormData();
    formData.append('file', file);
    if (associationType) formData.append('associationType', associationType);
    if (associationId) formData.append('associationId', associationId);

    return apiClient.post('/api/files/upload', formData);
  },

  getDownloadUrl(fileId) {
    return `/api/files/${fileId}/download`;
  },
};
