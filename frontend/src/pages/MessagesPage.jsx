import React, { useState, useEffect, useCallback, useRef } from 'react';
import { useSearchParams } from 'react-router-dom';
import { messagesApi } from '../api/messages';
import { filesApi } from '../api/files';
import { LoadingSpinner } from '../components/common/LoadingSpinner';

const MAX_MESSAGE_LENGTH = 2000;
const MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB
const ALLOWED_MIME_TYPES = ['image/jpeg', 'image/png', 'application/pdf', 'text/plain'];

export function MessagesPage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const [conversations, setConversations] = useState([]);
  const [selectedPeerId, setSelectedPeerId] = useState(
    searchParams.get('peer') ? Number(searchParams.get('peer')) : null
  );
  const [messages, setMessages] = useState([]);
  const [loadingConversations, setLoadingConversations] = useState(true);
  const [loadingMessages, setLoadingMessages] = useState(false);
  const [sending, setSending] = useState(false);
  const [inputText, setInputText] = useState('');
  const [error, setError] = useState('');
  const [selectedFile, setSelectedFile] = useState(null);
  const [uploadingAttachment, setUploadingAttachment] = useState(false);

  const chatEndRef = useRef(null);
  const scrollContainerRef = useRef(null);
  const userScrolledUpRef = useRef(false);
  const fileInputRef = useRef(null);

  const loadConversations = useCallback(async () => {
    try {
      const data = await messagesApi.getConversations();
      const list = data || [];
      setConversations(list);
      setSelectedPeerId((prev) => (prev ? prev : (list[0]?.peerId ?? null)));
    } catch (err) {
      setError(err.message || 'Failed to load conversations.');
    } finally {
      setLoadingConversations(false);
    }
  }, []);

  const loadMessages = useCallback(async (peerId) => {
    if (!peerId) return;
    try {
      setLoadingMessages(true);
      userScrolledUpRef.current = false;
      const data = await messagesApi.getConversationHistory(peerId, 0, 50);
      const history = (data?.content || []).reverse();
      setMessages(history);
      await messagesApi.markConversationRead(peerId);
      // Update local unread badge
      setConversations((prev) =>
        prev.map((c) => (c.peerId === peerId ? { ...c, unreadCount: 0 } : c))
      );
    } catch (err) {
      setError(err.message || 'Failed to load messages.');
    } finally {
      setLoadingMessages(false);
    }
  }, []);

  useEffect(() => {
    loadConversations();
  }, [loadConversations]);

  useEffect(() => {
    if (selectedPeerId) {
      userScrolledUpRef.current = false;
      loadMessages(selectedPeerId);
      setSearchParams({ peer: selectedPeerId });
    }
  }, [selectedPeerId, loadMessages, setSearchParams]);

  // Handle scroll detection to know if user scrolled up to read older messages
  const handleScroll = () => {
    if (!scrollContainerRef.current) return;
    const { scrollTop, scrollHeight, clientHeight } = scrollContainerRef.current;
    // If distance from bottom > 120px, user is reading history
    userScrolledUpRef.current = scrollHeight - scrollTop - clientHeight > 120;
  };

  // Polling for new messages every 4 seconds without resetting scroll if user is reading history
  useEffect(() => {
    if (!selectedPeerId) return;
    const interval = setInterval(async () => {
      try {
        const data = await messagesApi.getConversationHistory(selectedPeerId, 0, 50);
        const history = (data?.content || []).reverse();
        setMessages((prev) => {
          if (
            prev.length === history.length &&
            prev[prev.length - 1]?.id === history[history.length - 1]?.id
          ) {
            return prev; // Same messages, do not trigger re-render or re-scroll
          }
          return history;
        });
      } catch {
        // silent polling failure
      }
    }, 4000);
    return () => clearInterval(interval);
  }, [selectedPeerId]);

  useEffect(() => {
    if (!userScrolledUpRef.current && typeof chatEndRef.current?.scrollIntoView === 'function') {
      chatEndRef.current.scrollIntoView({ behavior: 'smooth' });
    }
  }, [messages]);

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

  const handleSendMessage = async (e) => {
    e.preventDefault();
    const trimmed = inputText.trim();
    if ((!trimmed && !selectedFile) || !selectedPeerId) return;

    try {
      setSending(true);
      setError('');
      userScrolledUpRef.current = false; // Always scroll to bottom on explicit send

      let finalContent = trimmed;

      if (selectedFile) {
        setUploadingAttachment(true);
        const uploadRes = await filesApi.uploadFile(selectedFile, 'MESSAGE', selectedPeerId);
        const attachmentTag = `[Attachment: ${uploadRes.originalFilename || selectedFile.name}](${filesApi.getDownloadUrl(uploadRes.id)})`;
        finalContent = finalContent ? `${finalContent}\n${attachmentTag}` : attachmentTag;
        setSelectedFile(null);
        if (fileInputRef.current) fileInputRef.current.value = '';
      }

      const newMsg = await messagesApi.sendMessage(selectedPeerId, finalContent);
      setMessages((prev) => [...prev, newMsg]);
      setInputText('');
      // update conversation snippet
      setConversations((prev) =>
        prev.map((c) =>
          c.peerId === selectedPeerId
            ? { ...c, lastMessage: finalContent, lastMessageAt: new Date().toISOString() }
            : c
        )
      );
    } catch (err) {
      setError(err.message || 'Failed to send message.');
    } finally {
      setSending(false);
      setUploadingAttachment(false);
    }
  };

  const activePeer = conversations.find((c) => c.peerId === selectedPeerId);

  return (
    <div className="container" style={{ padding: '2rem 1rem', maxWidth: '1100px', margin: '0 auto', flex: 1, display: 'flex', flexDirection: 'column' }}>
      <div style={{ marginBottom: '1.5rem' }}>
        <h1 style={{ fontSize: '2rem', fontWeight: 800, margin: 0 }}>Direct Messages</h1>
        <p style={{ color: 'var(--text-muted)', marginTop: '0.5rem', fontSize: '1rem' }}>
          Real-time conversations with your accepted campus connections.
        </p>
      </div>

      {error && (
        <div className="alert alert-danger" style={{ marginBottom: '1rem' }}>
          {error}
        </div>
      )}

      <div
        className="card"
        style={{
          display: 'grid',
          gridTemplateColumns: 'minmax(260px, 320px) 1fr',
          minHeight: '600px',
          padding: 0,
          overflow: 'hidden',
          border: '1px solid var(--border-color)',
        }}
      >
        {/* Conversation List Sidebar */}
        <aside
          style={{
            borderRight: '1px solid var(--border-color)',
            display: 'flex',
            flexDirection: 'column',
            backgroundColor: 'var(--bg-subtle, rgba(255,255,255,0.02))',
          }}
        >
          <div style={{ padding: '1rem', borderBottom: '1px solid var(--border-color)' }}>
            <strong style={{ fontSize: '1rem' }}>Conversations</strong>
          </div>

          <div style={{ flex: 1, overflowY: 'auto' }}>
            {loadingConversations ? (
              <div style={{ padding: '2rem', textAlign: 'center' }}>
                <LoadingSpinner text="Loading..." />
              </div>
            ) : conversations.length === 0 ? (
              <div style={{ padding: '2rem 1rem', textAlign: 'center', color: 'var(--text-muted)' }}>
                <p style={{ margin: 0, fontSize: '0.9rem' }}>No connections yet.</p>
                <p style={{ margin: '0.5rem 0 0', fontSize: '0.8rem' }}>
                  Connect with students in the Directory or Matches tab to start chatting!
                </p>
              </div>
            ) : (
              conversations.map((c) => {
                const isSelected = c.peerId === selectedPeerId;
                return (
                  <button
                    key={c.peerId}
                    type="button"
                    onClick={() => setSelectedPeerId(c.peerId)}
                    style={{
                      width: '100%',
                      textAlign: 'left',
                      padding: '1rem',
                      background: isSelected ? 'var(--bg-card)' : 'transparent',
                      border: 'none',
                      borderBottom: '1px solid var(--border-color)',
                      borderLeft: isSelected ? '4px solid var(--accent, #d4ff00)' : '4px solid transparent',
                      cursor: 'pointer',
                      display: 'flex',
                      flexDirection: 'column',
                      gap: '0.25rem',
                      color: 'inherit',
                    }}
                  >
                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                      <strong style={{ fontSize: '0.95rem' }}>{c.peerName}</strong>
                      {c.unreadCount > 0 && (
                        <span className="badge badge-accent" style={{ fontSize: '0.75rem', padding: '0.1rem 0.5rem' }}>
                          {c.unreadCount}
                        </span>
                      )}
                    </div>
                    {c.peerDepartment && (
                      <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>{c.peerDepartment}</span>
                    )}
                    <p
                      style={{
                        margin: 0,
                        fontSize: '0.8rem',
                        color: 'var(--text-muted)',
                        whiteSpace: 'nowrap',
                        overflow: 'hidden',
                        textOverflow: 'ellipsis',
                      }}
                    >
                      {c.lastMessage || 'Start conversation...'}
                    </p>
                  </button>
                );
              })
            )}
          </div>
        </aside>

        {/* Message Thread Area */}
        <section style={{ display: 'flex', flexDirection: 'column', minHeight: '600px' }}>
          {selectedPeerId ? (
            <>
              {/* Header */}
              <div
                style={{
                  padding: '1rem',
                  borderBottom: '1px solid var(--border-color)',
                  display: 'flex',
                  alignItems: 'center',
                  gap: '0.75rem',
                  backgroundColor: 'var(--bg-subtle, rgba(255,255,255,0.01))',
                }}
              >
                <div>
                  <strong style={{ fontSize: '1.1rem' }}>{activePeer?.peerName || 'Conversation'}</strong>
                  {activePeer?.peerDepartment && (
                    <span style={{ marginLeft: '0.75rem', fontSize: '0.8rem', color: 'var(--text-muted)' }}>
                      {activePeer.peerDepartment}
                    </span>
                  )}
                </div>
              </div>

              {/* Message History */}
              <div
                ref={scrollContainerRef}
                onScroll={handleScroll}
                style={{
                  flex: 1,
                  padding: '1.5rem',
                  overflowY: 'auto',
                  display: 'flex',
                  flexDirection: 'column',
                  gap: '0.75rem',
                }}
              >
                {loadingMessages ? (
                  <div style={{ textAlign: 'center', margin: 'auto' }}>
                    <LoadingSpinner text="Loading messages..." />
                  </div>
                ) : messages.length === 0 ? (
                  <div style={{ margin: 'auto', textAlign: 'center', color: 'var(--text-muted)' }}>
                    <p style={{ margin: 0 }}>Say hello! Break the ice with your new connection.</p>
                  </div>
                ) : (
                  messages.map((m) => {
                    const match = m.content?.match(/\[Attachment:\s*(.+?)\]\((.+?)\)/);
                    const attachmentName = match ? match[1] : (m.attachmentFileName || null);
                    const attachmentUrl = match ? match[2] : (m.attachmentDownloadUrl || (m.attachmentId ? `/api/files/${m.attachmentId}/download` : null));
                    const textBefore = match ? m.content.replace(match[0], '').trim() : m.content;

                    return (
                      <div
                        key={m.id}
                        style={{
                          alignSelf: m.mine ? 'flex-end' : 'flex-start',
                          maxWidth: '75%',
                          padding: '0.75rem 1rem',
                          borderRadius: '0.75rem',
                          backgroundColor: m.mine ? 'var(--accent, #d4ff00)' : 'var(--bg-subtle, #1a1a1a)',
                          color: m.mine ? '#000000' : 'var(--text-primary)',
                          border: m.mine ? 'none' : '1px solid var(--border-color)',
                          wordBreak: 'break-word',
                        }}
                      >
                        {textBefore && (
                          <p style={{ margin: 0, fontSize: '0.95rem', lineHeight: '1.4' }}>{textBefore}</p>
                        )}
                        {attachmentUrl && (
                          <div style={{ marginTop: textBefore ? '0.5rem' : 0 }}>
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
                                backgroundColor: m.mine ? 'rgba(0,0,0,0.1)' : 'rgba(255,255,255,0.08)',
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
                        <span
                          style={{
                            display: 'block',
                            fontSize: '0.7rem',
                            marginTop: '0.25rem',
                            textAlign: m.mine ? 'right' : 'left',
                            opacity: 0.7,
                          }}
                        >
                          {m.createdAt ? new Date(m.createdAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }) : ''}
                        </span>
                      </div>
                    );
                  })
                )}
                <div ref={chatEndRef} />
              </div>

              {/* Message Input Box & Attachment Form */}
              <div style={{ padding: '0.75rem 1rem', borderTop: '1px solid var(--border-color)', display: 'flex', flexDirection: 'column', gap: '0.5rem' }}>
                {selectedFile && (
                  <div
                    style={{
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'space-between',
                      padding: '0.4rem 0.75rem',
                      backgroundColor: 'var(--bg-card)',
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

                <form
                  onSubmit={handleSendMessage}
                  style={{
                    display: 'flex',
                    gap: '0.75rem',
                    alignItems: 'center',
                  }}
                >
                  <input
                    type="file"
                    ref={fileInputRef}
                    style={{ display: 'none' }}
                    accept="image/jpeg,image/png,application/pdf,text/plain"
                    onChange={handleFileSelect}
                  />
                  <button
                    type="button"
                    onClick={() => fileInputRef.current?.click()}
                    disabled={sending || uploadingAttachment}
                    className="btn btn-subtle"
                    title="Attach file (PDF, PNG, JPG, TXT up to 5MB)"
                    style={{ padding: '0.75rem', fontSize: '1.1rem', lineHeight: 1 }}
                  >
                    📎
                  </button>

                  <input
                    type="text"
                    className="input-field"
                    placeholder={selectedFile ? 'Add an optional note with your file...' : 'Type a message...'}
                    value={inputText}
                    onChange={(e) => setInputText(e.target.value)}
                    disabled={sending || uploadingAttachment}
                    maxLength={MAX_MESSAGE_LENGTH}
                    style={{ flex: 1, padding: '0.75rem 1rem' }}
                  />

                  <button
                    type="submit"
                    disabled={sending || uploadingAttachment || (!inputText.trim() && !selectedFile)}
                    className="btn btn-primary"
                    style={{ padding: '0.75rem 1.5rem' }}
                  >
                    {uploadingAttachment ? 'Uploading...' : sending ? 'Sending...' : 'Send'}
                  </button>
                </form>
              </div>
            </>
          ) : (
            <div style={{ margin: 'auto', textAlign: 'center', color: 'var(--text-muted)' }}>
              <p>Select a conversation from the sidebar to start messaging.</p>
            </div>
          )}
        </section>
      </div>
    </div>
  );
}
