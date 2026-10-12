import React, { useState, useEffect, useCallback } from 'react';
import { postsApi } from '../api/posts';
import { LoadingSpinner } from '../components/common/LoadingSpinner';

const MAX_POST_LENGTH = 2000;

export function FeedPage() {
  const [posts, setPosts] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [postContent, setPostContent] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [deletingId, setDeletingId] = useState(null);
  const [editingId, setEditingId] = useState(null);
  const [editContent, setEditContent] = useState('');
  const [savingEdit, setSavingEdit] = useState(false);

  const loadFeed = useCallback(async () => {
    try {
      setError('');
      const data = await postsApi.getFeed(0, 30);
      setPosts(data?.content || []);
    } catch (err) {
      setError(err.message || 'Failed to load campus feed.');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    loadFeed();
  }, [loadFeed]);

  const handleCreatePost = async (e) => {
    e.preventDefault();
    const trimmed = postContent.trim();
    if (!trimmed) return;
    if (trimmed.length > MAX_POST_LENGTH) {
      setError(`Post exceeds maximum length of ${MAX_POST_LENGTH} characters.`);
      return;
    }

    try {
      setSubmitting(true);
      setError('');
      await postsApi.createPost(trimmed);
      setPostContent('');
      await loadFeed();
    } catch (err) {
      setError(err.message || 'Failed to publish post.');
    } finally {
      setSubmitting(false);
    }
  };

  const handleDeletePost = async (postId) => {
    if (!window.confirm('Are you sure you want to delete this post?')) return;
    try {
      setDeletingId(postId);
      setError('');
      await postsApi.deletePost(postId);
      setPosts((prev) => prev.filter((p) => p.id !== postId));
    } catch (err) {
      setError(err.message || 'Failed to delete post.');
    } finally {
      setDeletingId(null);
    }
  };

  const handleStartEdit = (post) => {
    setEditingId(post.id);
    setEditContent(post.content);
    setError('');
  };

  const handleCancelEdit = () => {
    setEditingId(null);
    setEditContent('');
  };

  const handleSaveEdit = async (postId) => {
    const trimmed = editContent.trim();
    if (!trimmed) return;
    if (trimmed.length > MAX_POST_LENGTH) {
      setError(`Post exceeds maximum length of ${MAX_POST_LENGTH} characters.`);
      return;
    }

    try {
      setSavingEdit(true);
      setError('');
      const updated = await postsApi.updatePost(postId, trimmed);
      setPosts((prev) => prev.map((p) => (p.id === postId ? { ...p, content: updated.content || trimmed } : p)));
      setEditingId(null);
      setEditContent('');
    } catch (err) {
      setError(err.message || 'Failed to update post.');
    } finally {
      setSavingEdit(false);
    }
  };

  return (
    <div className="container" style={{ padding: '2rem 1rem', maxWidth: '800px', margin: '0 auto' }}>
      <div style={{ marginBottom: '2rem' }}>
        <h1 style={{ fontSize: '2rem', fontWeight: 800, margin: 0 }}>Campus Feed</h1>
        <p style={{ color: 'var(--text-muted)', marginTop: '0.5rem', fontSize: '1rem' }}>
          Discover projects, collaborations, and announcements from students at your college.
        </p>
      </div>

      {error && (
        <div className="alert alert-danger" style={{ marginBottom: '1.5rem' }}>
          {error}
        </div>
      )}

      {/* Post Composer */}
      <div className="card" style={{ marginBottom: '2rem' }}>
        <form onSubmit={handleCreatePost}>
          <label htmlFor="feed-post-content" style={{ fontWeight: 600, display: 'block', marginBottom: '0.5rem' }}>
            Share what you're working on
          </label>
          <textarea
            id="feed-post-content"
            className="input-field"
            rows={3}
            placeholder="Share a project idea, hackathon search, or questions with your peers..."
            value={postContent}
            onChange={(e) => setPostContent(e.target.value)}
            disabled={submitting}
            maxLength={MAX_POST_LENGTH}
            style={{ width: '100%', resize: 'vertical', minHeight: '80px' }}
          />
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginTop: '0.75rem' }}>
            <span style={{ fontSize: '0.8rem', color: postContent.length > MAX_POST_LENGTH * 0.9 ? '#ff4d4d' : 'var(--text-muted)' }}>
              {postContent.length} / {MAX_POST_LENGTH}
            </span>
            <button
              type="submit"
              disabled={submitting || !postContent.trim()}
              className="btn btn-primary"
            >
              {submitting ? <LoadingSpinner text="Posting..." /> : 'Publish Post'}
            </button>
          </div>
        </form>
      </div>

      {/* Feed Stream */}
      {loading ? (
        <div style={{ textAlign: 'center', padding: '3rem 0' }}>
          <LoadingSpinner text="Loading campus feed..." />
        </div>
      ) : posts.length === 0 ? (
        <div className="card" style={{ textAlign: 'center', padding: '3rem 1rem' }}>
          <p style={{ fontSize: '1.1rem', color: 'var(--text-muted)', margin: 0 }}>
            No posts yet in your campus feed. Be the first to share an update!
          </p>
        </div>
      ) : (
        <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
          {posts.map((post) => (
            <article key={post.id} className="card" style={{ display: 'flex', flexDirection: 'column', gap: '0.75rem' }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
                <div>
                  <strong style={{ fontSize: '1.05rem', color: 'var(--text-primary)' }}>{post.authorName}</strong>
                  <div style={{ display: 'flex', gap: '0.5rem', alignItems: 'center', marginTop: '0.2rem', flexWrap: 'wrap' }}>
                    {post.authorDepartment && (
                      <span className="badge" style={{ fontSize: '0.75rem' }}>{post.authorDepartment}</span>
                    )}
                    {post.authorYear && (
                      <span className="badge" style={{ fontSize: '0.75rem' }}>Year {post.authorYear}</span>
                    )}
                    <span style={{ fontSize: '0.8rem', color: 'var(--text-muted)' }}>
                      {post.createdAt ? new Date(post.createdAt).toLocaleDateString() : ''}
                    </span>
                  </div>
                </div>

                {post.mine && (
                  <div style={{ display: 'flex', gap: '0.5rem', alignItems: 'center' }}>
                    {editingId !== post.id && (
                      <button
                        type="button"
                        onClick={() => handleStartEdit(post)}
                        className="btn btn-sm btn-subtle"
                        style={{ color: 'var(--brand-primary, #6366f1)' }}
                        title="Edit your post"
                      >
                        Edit
                      </button>
                    )}
                    <button
                      type="button"
                      onClick={() => handleDeletePost(post.id)}
                      disabled={deletingId === post.id}
                      className="btn btn-sm btn-subtle"
                      style={{ color: '#ff4d4d' }}
                      title="Delete your post"
                    >
                      {deletingId === post.id ? 'Deleting...' : 'Delete'}
                    </button>
                  </div>
                )}
              </div>

              {editingId === post.id ? (
                <div style={{ display: 'flex', flexDirection: 'column', gap: '0.5rem', marginTop: '0.5rem' }}>
                  <textarea
                    className="input-field"
                    rows={3}
                    value={editContent}
                    onChange={(e) => setEditContent(e.target.value)}
                    disabled={savingEdit}
                    maxLength={MAX_POST_LENGTH}
                    style={{ width: '100%', resize: 'vertical' }}
                  />
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                    <span style={{ fontSize: '0.8rem', color: editContent.length > MAX_POST_LENGTH * 0.9 ? '#ff4d4d' : 'var(--text-muted)' }}>
                      {editContent.length} / {MAX_POST_LENGTH}
                    </span>
                    <div style={{ display: 'flex', gap: '0.5rem' }}>
                      <button
                        type="button"
                        onClick={handleCancelEdit}
                        disabled={savingEdit}
                        className="btn btn-sm btn-subtle"
                      >
                        Cancel
                      </button>
                      <button
                        type="button"
                        onClick={() => handleSaveEdit(post.id)}
                        disabled={savingEdit || !editContent.trim()}
                        className="btn btn-sm btn-primary"
                      >
                        {savingEdit ? <LoadingSpinner text="Saving..." /> : 'Save Changes'}
                      </button>
                    </div>
                  </div>
                </div>
              ) : (
                <p style={{ margin: 0, fontSize: '0.95rem', lineHeight: '1.5', whiteSpace: 'pre-wrap', color: 'var(--text-primary)' }}>
                  {post.content}
                </p>
              )}
            </article>
          ))}
        </div>
      )}
    </div>
  );
}
