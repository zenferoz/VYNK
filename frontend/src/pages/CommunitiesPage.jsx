import React, { useState, useEffect, useCallback, useRef } from 'react';
import { communitiesApi } from '../api/communities';
import { filesApi } from '../api/files';
import { LoadingSpinner } from '../components/common/LoadingSpinner';

const MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB
const ALLOWED_MIME_TYPES = ['image/jpeg', 'image/png', 'application/pdf', 'text/plain'];

export function CommunitiesPage() {
  const [communities, setCommunities] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [selectedCommunity, setSelectedCommunity] = useState(null);
  const [discussions, setDiscussions] = useState([]);
  const [loadingDiscussions, setLoadingDiscussions] = useState(false);

  // Modals & forms
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [createName, setCreateName] = useState('');
  const [createDesc, setCreateDesc] = useState('');
  const [createPurpose, setCreatePurpose] = useState('PROJECT_COLLABORATION');
  const [creating, setCreating] = useState(false);

  const [discTitle, setDiscTitle] = useState('');
  const [discContent, setDiscContent] = useState('');
  const [postingDisc, setPostingDisc] = useState(false);
  const [actionLoadingId, setActionLoadingId] = useState(null);

  // Attachments for discussions
  const [selectedFile, setSelectedFile] = useState(null);
  const [uploadingAttachment, setUploadingAttachment] = useState(false);
  const fileInputRef = useRef(null);

  const loadCommunities = useCallback(async () => {
    try {
      setError('');
      const data = await communitiesApi.getCommunities(0, 30);
      setCommunities(data?.content || []);
    } catch (err) {
      setError(err.message || 'Failed to load communities.');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    loadCommunities();
  }, [loadCommunities]);

  const loadCommunityDiscussions = useCallback(async (communityId) => {
    try {
      setLoadingDiscussions(true);
      const data = await communitiesApi.getDiscussions(communityId, 0, 30);
      setDiscussions(data?.content || []);
    } catch (err) {
      setError(err.message || 'Failed to load discussions.');
    } finally {
      setLoadingDiscussions(false);
    }
  }, []);

  const handleSelectCommunity = async (comm) => {
    setSelectedCommunity(comm);
    setSelectedFile(null);
    if (fileInputRef.current) fileInputRef.current.value = '';
    if (comm.member) {
      loadCommunityDiscussions(comm.id);
    } else {
      setDiscussions([]);
    }
  };

  const handleFileSelect = (e) => {
    const file = e.target.files?.[0];
    if (!file) return;

    if (file.size > MAX_FILE_SIZE) {
      setError('Selected file exceeds maximum limit of 5MB.');
      return;
    }

    if (!ALLOWED_MIME_TYPES.includes(file.type)) {
      setError('File type not permitted. Allowed types: JPEG, PNG, PDF, TXT.');
      return;
    }

    setError('');
    setSelectedFile(file);
  };

  const handleJoin = async (commId) => {
    try {
      setActionLoadingId(commId);
      setError('');
      await communitiesApi.joinCommunity(commId);
      setCommunities((prev) =>
        prev.map((c) => (c.id === commId ? { ...c, member: true, memberCount: c.memberCount + 1 } : c))
      );
      if (selectedCommunity?.id === commId) {
        setSelectedCommunity((prev) => ({ ...prev, member: true, memberCount: prev.memberCount + 1 }));
        loadCommunityDiscussions(commId);
      }
    } catch (err) {
      setError(err.message || 'Failed to join community.');
    } finally {
      setActionLoadingId(null);
    }
  };

  const handleLeave = async (commId) => {
    try {
      setActionLoadingId(commId);
      setError('');
      await communitiesApi.leaveCommunity(commId);
      setCommunities((prev) =>
        prev.map((c) => (c.id === commId ? { ...c, member: false, memberCount: Math.max(0, c.memberCount - 1) } : c))
      );
      if (selectedCommunity?.id === commId) {
        setSelectedCommunity((prev) => ({ ...prev, member: false, memberCount: Math.max(0, prev.memberCount - 1) }));
        setDiscussions([]);
      }
    } catch (err) {
      setError(err.message || 'Failed to leave community.');
    } finally {
      setActionLoadingId(null);
    }
  };

  const handleCreateCommunity = async (e) => {
    e.preventDefault();
    if (!createName.trim() || !createDesc.trim()) return;

    try {
      setCreating(true);
      setError('');
      const newComm = await communitiesApi.createCommunity({
        name: createName.trim(),
        description: createDesc.trim(),
        purpose: createPurpose,
      });
      setShowCreateModal(false);
      setCreateName('');
      setCreateDesc('');
      await loadCommunities();
      setSelectedCommunity(newComm);
    } catch (err) {
      setError(err.message || 'Failed to create community.');
    } finally {
      setCreating(false);
    }
  };

  const handlePostDiscussion = async (e) => {
    e.preventDefault();
    if (!discTitle.trim() || !discContent.trim() || !selectedCommunity) return;

    try {
      setPostingDisc(true);
      setError('');

      let finalContent = discContent.trim();
      if (selectedFile) {
        setUploadingAttachment(true);
        try {
          const uploadRes = await filesApi.uploadFile(selectedFile, 'COMMUNITY', selectedCommunity.id);
          const attachmentTag = `[Attachment: ${uploadRes.originalFilename || selectedFile.name}](${filesApi.getDownloadUrl(uploadRes.id)})`;
          finalContent = `${finalContent}\n${attachmentTag}`;
        } catch (uploadErr) {
          setError(uploadErr.message || 'Failed to upload attachment. Discussion was not posted.');
          setPostingDisc(false);
          setUploadingAttachment(false);
          return;
        }
      }

      await communitiesApi.createDiscussion(selectedCommunity.id, {
        title: discTitle.trim(),
        content: finalContent,
      });
      setDiscTitle('');
      setDiscContent('');
      setSelectedFile(null);
      if (fileInputRef.current) fileInputRef.current.value = '';
      await loadCommunityDiscussions(selectedCommunity.id);
    } catch (err) {
      setError(err.message || 'Failed to post discussion.');
    } finally {
      setPostingDisc(false);
      setUploadingAttachment(false);
    }
  };

  const handleDeleteDiscussion = async (discId) => {
    if (!window.confirm('Delete this discussion thread?')) return;
    try {
      setError('');
      await communitiesApi.deleteDiscussion(selectedCommunity.id, discId);
      setDiscussions((prev) => prev.filter((d) => d.id !== discId));
    } catch (err) {
      setError(err.message || 'Failed to delete discussion.');
    }
  };

  return (
    <div className="container" style={{ padding: '2rem 1rem', maxWidth: '1100px', margin: '0 auto' }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '2rem', flexWrap: 'wrap', gap: '1rem' }}>
        <div>
          <h1 style={{ fontSize: '2rem', fontWeight: 800, margin: 0 }}>Campus Communities</h1>
          <p style={{ color: 'var(--text-muted)', marginTop: '0.5rem', fontSize: '1rem' }}>
            Join purpose-driven student groups across projects, hackathons, and placement prep.
          </p>
        </div>
        <button
          type="button"
          onClick={() => setShowCreateModal(true)}
          className="btn btn-primary"
        >
          + Create Community
        </button>
      </div>

      {error && (
        <div className="alert alert-danger" style={{ marginBottom: '1.5rem' }}>
          {error}
        </div>
      )}

      {/* Main Grid: Directory & Selected Detail */}
      <div style={{ display: 'grid', gridTemplateColumns: selectedCommunity ? '1fr 1fr' : '1fr', gap: '2rem' }}>
        {/* Communities List */}
        <div>
          <h2 style={{ fontSize: '1.25rem', fontWeight: 700, marginBottom: '1rem' }}>Available Communities</h2>
          {loading ? (
            <div style={{ padding: '3rem 0', textAlign: 'center' }}>
              <LoadingSpinner text="Loading communities..." />
            </div>
          ) : communities.length === 0 ? (
            <div className="card" style={{ textAlign: 'center', padding: '3rem 1rem' }}>
              <p style={{ color: 'var(--text-muted)', margin: 0 }}>
                No communities created yet for your campus. Start one today!
              </p>
            </div>
          ) : (
            <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
              {communities.map((c) => {
                const isSelected = selectedCommunity?.id === c.id;
                return (
                  <div
                    key={c.id}
                    className="card"
                    style={{
                      borderLeft: isSelected ? '4px solid var(--accent, #d4ff00)' : '1px solid var(--border-color)',
                      cursor: 'pointer',
                    }}
                    onClick={() => handleSelectCommunity(c)}
                  >
                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', gap: '0.5rem' }}>
                      <div>
                        <strong style={{ fontSize: '1.1rem' }}>{c.name}</strong>
                        <div style={{ display: 'flex', gap: '0.5rem', marginTop: '0.3rem', alignItems: 'center' }}>
                          <span className="badge" style={{ fontSize: '0.75rem' }}>{c.purpose}</span>
                          <span style={{ fontSize: '0.8rem', color: 'var(--text-muted)' }}>
                            {c.memberCount} {c.memberCount === 1 ? 'member' : 'members'}
                          </span>
                        </div>
                      </div>
                      {c.member ? (
                        <span className="badge badge-accent" style={{ fontSize: '0.75rem' }}>Joined</span>
                      ) : (
                        <button
                          type="button"
                          onClick={(e) => {
                            e.stopPropagation();
                            handleJoin(c.id);
                          }}
                          disabled={actionLoadingId === c.id}
                          className="btn btn-sm btn-secondary"
                        >
                          {actionLoadingId === c.id ? 'Joining...' : 'Join'}
                        </button>
                      )}
                    </div>
                    <p style={{ marginTop: '0.75rem', marginBottom: 0, fontSize: '0.9rem', color: 'var(--text-muted)', lineHeight: '1.4' }}>
                      {c.description}
                    </p>
                  </div>
                );
              })}
            </div>
          )}
        </div>

        {/* Selected Community Detail & Discussions */}
        {selectedCommunity && (
          <div>
            <div className="card" style={{ marginBottom: '1.5rem' }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
                <div>
                  <h2 style={{ fontSize: '1.3rem', margin: 0 }}>{selectedCommunity.name}</h2>
                  <p style={{ color: 'var(--text-muted)', fontSize: '0.85rem', marginTop: '0.2rem' }}>
                    Purpose: {selectedCommunity.purpose} • {selectedCommunity.memberCount} members
                  </p>
                </div>
                {selectedCommunity.member ? (
                  <button
                    type="button"
                    onClick={() => handleLeave(selectedCommunity.id)}
                    disabled={actionLoadingId === selectedCommunity.id}
                    className="btn btn-sm btn-subtle"
                    style={{ color: '#ff4d4d' }}
                  >
                    {actionLoadingId === selectedCommunity.id ? 'Leaving...' : 'Leave'}
                  </button>
                ) : (
                  <button
                    type="button"
                    onClick={() => handleJoin(selectedCommunity.id)}
                    disabled={actionLoadingId === selectedCommunity.id}
                    className="btn btn-sm btn-primary"
                  >
                    {actionLoadingId === selectedCommunity.id ? 'Joining...' : 'Join Community'}
                  </button>
                )}
              </div>
              <p style={{ marginTop: '0.75rem', marginBottom: 0, fontSize: '0.95rem' }}>
                {selectedCommunity.description}
              </p>
            </div>

            {/* Discussions Section */}
            <div>
              <h3 style={{ fontSize: '1.15rem', marginBottom: '1rem' }}>Discussions</h3>
              {!selectedCommunity.member ? (
                <div className="card" style={{ textAlign: 'center', padding: '2rem 1rem' }}>
                  <p style={{ color: 'var(--text-muted)', margin: 0 }}>
                    Join this community to view and participate in discussions.
                  </p>
                </div>
              ) : (
                <>
                  {/* Discussion Composer */}
                  <div className="card" style={{ marginBottom: '1.5rem' }}>
                    <form onSubmit={handlePostDiscussion}>
                      <input
                        type="text"
                        className="input-field"
                        placeholder="Discussion Title"
                        value={discTitle}
                        onChange={(e) => setDiscTitle(e.target.value)}
                        disabled={postingDisc || uploadingAttachment}
                        maxLength={200}
                        style={{ width: '100%', marginBottom: '0.5rem' }}
                      />
                      <textarea
                        className="input-field"
                        rows={3}
                        placeholder="Write your discussion post..."
                        value={discContent}
                        onChange={(e) => setDiscContent(e.target.value)}
                        disabled={postingDisc || uploadingAttachment}
                        maxLength={4000}
                        style={{ width: '100%', resize: 'vertical' }}
                      />

                      {/* Selected Attachment Chip */}
                      {selectedFile && (
                        <div
                          style={{
                            display: 'flex',
                            alignItems: 'center',
                            justifyContent: 'space-between',
                            padding: '0.4rem 0.75rem',
                            marginTop: '0.5rem',
                            backgroundColor: 'var(--bg-subtle, rgba(255,255,255,0.03))',
                            borderRadius: '0.5rem',
                            border: '1px solid var(--border-color)',
                            fontSize: '0.85rem',
                          }}
                        >
                          <span style={{ overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                            📎 <strong>{selectedFile.name}</strong> ({(selectedFile.size / 1024).toFixed(1)} KB)
                          </span>
                          <button
                            type="button"
                            onClick={() => {
                              setSelectedFile(null);
                              if (fileInputRef.current) fileInputRef.current.value = '';
                            }}
                            style={{ background: 'transparent', border: 'none', color: '#ff4d4d', cursor: 'pointer', fontWeight: 'bold', padding: '0 0.5rem' }}
                            title="Remove attachment"
                          >
                            ✕
                          </button>
                        </div>
                      )}

                      <input
                        type="file"
                        ref={fileInputRef}
                        style={{ display: 'none' }}
                        accept="image/jpeg,image/png,application/pdf,text/plain"
                        onChange={handleFileSelect}
                      />

                      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginTop: '0.5rem' }}>
                        <button
                          type="button"
                          onClick={() => fileInputRef.current?.click()}
                          disabled={postingDisc || uploadingAttachment}
                          className="btn btn-sm btn-subtle"
                          title="Attach file (PDF, PNG, JPG, TXT up to 5MB)"
                          style={{ display: 'inline-flex', alignItems: 'center', gap: '0.35rem' }}
                        >
                          📎 Attach File
                        </button>
                        <button
                          type="submit"
                          disabled={postingDisc || uploadingAttachment || !discTitle.trim() || !discContent.trim()}
                          className="btn btn-sm btn-primary"
                        >
                          {uploadingAttachment ? 'Uploading...' : postingDisc ? 'Posting...' : 'Post Discussion'}
                        </button>
                      </div>
                    </form>
                  </div>

                  {/* Discussion Thread List */}
                  {loadingDiscussions ? (
                    <div style={{ textAlign: 'center', padding: '2rem 0' }}>
                      <LoadingSpinner text="Loading threads..." />
                    </div>
                  ) : discussions.length === 0 ? (
                    <div className="card" style={{ textAlign: 'center', padding: '2rem' }}>
                      <p style={{ color: 'var(--text-muted)', margin: 0 }}>No discussion threads yet. Start one!</p>
                    </div>
                  ) : (
                    <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
                      {discussions.map((d) => {
                        const match = d.content?.match(/\[Attachment:\s*(.+?)\]\((.+?)\)/);
                        const attachmentName = match ? match[1] : null;
                        const attachmentUrl = match ? match[2] : null;
                        const textBefore = match ? d.content.replace(match[0], '').trim() : d.content;

                        return (
                          <div key={d.id} className="card" style={{ display: 'flex', flexDirection: 'column', gap: '0.5rem' }}>
                            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
                              <strong style={{ fontSize: '1rem' }}>{d.title}</strong>
                              {d.mine && (
                                <button
                                  type="button"
                                  onClick={() => handleDeleteDiscussion(d.id)}
                                  className="btn btn-sm btn-subtle"
                                  style={{ color: '#ff4d4d', padding: '0.1rem 0.5rem' }}
                                >
                                  Delete
                                </button>
                              )}
                            </div>
                            <div style={{ fontSize: '0.8rem', color: 'var(--text-muted)' }}>
                              By {d.authorName} • {d.createdAt ? new Date(d.createdAt).toLocaleDateString() : ''}
                            </div>
                            {textBefore && (
                              <p style={{ margin: 0, fontSize: '0.9rem', lineHeight: '1.4', whiteSpace: 'pre-wrap' }}>
                                {textBefore}
                              </p>
                            )}
                            {attachmentUrl && (
                              <div style={{ marginTop: '0.25rem' }}>
                                <a
                                  href={attachmentUrl}
                                  download={attachmentName}
                                  target="_blank"
                                  rel="noopener noreferrer"
                                  className="btn btn-sm btn-subtle"
                                  style={{
                                    display: 'inline-flex',
                                    alignItems: 'center',
                                    gap: '0.4rem',
                                    backgroundColor: 'rgba(255,255,255,0.06)',
                                    textDecoration: 'none',
                                    color: 'inherit',
                                    padding: '0.35rem 0.65rem',
                                    borderRadius: '0.4rem',
                                    fontSize: '0.85rem',
                                    fontWeight: 600,
                                  }}
                                >
                                  📎 {attachmentName} ⬇
                                </a>
                              </div>
                            )}
                          </div>
                        );
                      })}
                    </div>
                  )}
                </>
              )}
            </div>
          </div>
        )}
      </div>

      {/* Create Community Modal */}
      {showCreateModal && (
        <div
          role="dialog"
          aria-modal="true"
          style={{
            position: 'fixed',
            inset: 0,
            backgroundColor: 'rgba(0,0,0,0.7)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            zIndex: 1000,
            padding: '1rem',
          }}
        >
          <div className="card" style={{ maxWidth: '500px', width: '100%', padding: '1.75rem' }}>
            <h2 style={{ fontSize: '1.3rem', margin: 0, marginBottom: '1rem' }}>Create Campus Community</h2>
            <form onSubmit={handleCreateCommunity}>
              <div style={{ marginBottom: '1rem' }}>
                <label style={{ display: 'block', fontWeight: 600, marginBottom: '0.3rem' }}>Community Name</label>
                <input
                  type="text"
                  className="input-field"
                  placeholder="e.g. AI & ML Builders"
                  value={createName}
                  onChange={(e) => setCreateName(e.target.value)}
                  required
                  maxLength={100}
                  style={{ width: '100%' }}
                />
              </div>

              <div style={{ marginBottom: '1rem' }}>
                <label style={{ display: 'block', fontWeight: 600, marginBottom: '0.3rem' }}>Purpose</label>
                <select
                  className="input-field"
                  value={createPurpose}
                  onChange={(e) => setCreatePurpose(e.target.value)}
                  style={{ width: '100%' }}
                >
                  <option value="PROJECT_COLLABORATION">Project Collaboration</option>
                  <option value="HACKATHON_TEAM">Hackathon Team</option>
                  <option value="PLACEMENT_PREP">Placement Prep</option>
                  <option value="STUDY_PARTNER">Study Partner</option>
                  <option value="RESEARCH">Research</option>
                  <option value="CASUAL_NETWORKING">Casual Networking</option>
                </select>
              </div>

              <div style={{ marginBottom: '1.5rem' }}>
                <label style={{ display: 'block', fontWeight: 600, marginBottom: '0.3rem' }}>Description</label>
                <textarea
                  className="input-field"
                  rows={3}
                  placeholder="Explain what this group is about and what members do..."
                  value={createDesc}
                  onChange={(e) => setCreateDesc(e.target.value)}
                  required
                  maxLength={1000}
                  style={{ width: '100%', resize: 'vertical' }}
                />
              </div>

              <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '0.75rem' }}>
                <button
                  type="button"
                  onClick={() => setShowCreateModal(false)}
                  className="btn btn-secondary"
                  disabled={creating}
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={creating || !createName.trim() || !createDesc.trim()}
                  className="btn btn-primary"
                >
                  {creating ? 'Creating...' : 'Create'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
