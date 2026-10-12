import React, { useState, useEffect, useCallback } from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { connectionsApi } from '../api/connections';
import { safetyApi } from '../api/safety';
import { Alert } from '../components/common/Alert';
import { LoadingSpinner } from '../components/common/LoadingSpinner';
import { BlockConfirmModal } from '../components/safety/BlockConfirmModal';

export function ConnectionsPage() {
  const { user } = useAuth();
  const [activeTab, setActiveTab] = useState('accepted'); // 'accepted' | 'pending' | 'blocked'
  const [pendingSubTab, setPendingSubTab] = useState('incoming'); // 'incoming' | 'outgoing'

  const [accepted, setAccepted] = useState([]);
  const [incoming, setIncoming] = useState([]);
  const [outgoing, setOutgoing] = useState([]);
  const [blockedList, setBlockedList] = useState([]);

  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [actionSuccess, setActionSuccess] = useState('');
  const [actionLoadingId, setActionLoadingId] = useState(null);

  // Block modal state
  const [blockTarget, setBlockTarget] = useState(null);

  const fetchConnections = useCallback(async () => {
    try {
      setLoading(true);
      setError('');

      const [acceptedRes, pendingRes] = await Promise.all([
        connectionsApi.getAccepted(),
        connectionsApi.getPending(),
      ]);

      setAccepted(Array.isArray(acceptedRes) ? acceptedRes : []);
      setIncoming(pendingRes && Array.isArray(pendingRes.incoming) ? pendingRes.incoming : []);
      setOutgoing(pendingRes && Array.isArray(pendingRes.outgoing) ? pendingRes.outgoing : []);
    } catch (err) {
      setError(err.message || 'Unable to load connections data.');
    } finally {
      setLoading(false);
    }
  }, []);

  const fetchBlocked = useCallback(async () => {
    try {
      setLoading(true);
      setError('');
      const data = await safetyApi.getBlockedUsers();
      setBlockedList(Array.isArray(data) ? data : []);
    } catch (err) {
      setError(err.message || 'Unable to load blocked students.');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    if (activeTab === 'blocked') {
      fetchBlocked();
    } else {
      fetchConnections();
    }
  }, [activeTab, fetchConnections, fetchBlocked]);

  const handleAccept = async (requestId) => {
    try {
      setActionLoadingId(requestId);
      setError('');
      setActionSuccess('');
      await connectionsApi.acceptRequest(requestId);
      setActionSuccess('Connection accepted successfully!');
      fetchConnections();
    } catch (err) {
      setError(err.message || 'Failed to accept connection request.');
    } finally {
      setActionLoadingId(null);
    }
  };

  const handleReject = async (requestId) => {
    try {
      setActionLoadingId(requestId);
      setError('');
      setActionSuccess('');
      await connectionsApi.rejectRequest(requestId);
      setActionSuccess('Connection request rejected.');
      fetchConnections();
    } catch (err) {
      setError(err.message || 'Failed to reject connection request.');
    } finally {
      setActionLoadingId(null);
    }
  };

  const handleCancel = async (requestId) => {
    try {
      setActionLoadingId(requestId);
      setError('');
      setActionSuccess('');
      await connectionsApi.cancelRequest(requestId);
      setActionSuccess('Connection request cancelled.');
      fetchConnections();
    } catch (err) {
      setError(err.message || 'Failed to cancel connection request.');
    } finally {
      setActionLoadingId(null);
    }
  };

  const handleUnblock = async (targetUserId) => {
    try {
      setActionLoadingId(targetUserId);
      setError('');
      setActionSuccess('');
      await safetyApi.unblockUser(targetUserId);
      setActionSuccess('Student unblocked successfully.');
      fetchBlocked();
    } catch (err) {
      setError(err.message || 'Failed to unblock student.');
    } finally {
      setActionLoadingId(null);
    }
  };

  const getInitials = (name) => {
    if (!name) return 'S';
    const parts = name.trim().split(/\s+/);
    if (parts.length >= 2) {
      return (parts[0][0] + parts[parts.length - 1][0]).toUpperCase();
    }
    return name.slice(0, 2).toUpperCase();
  };

  return (
    <div className="container page-enter" style={{ paddingTop: '2.5rem', paddingBottom: '4rem' }}>
      {/* Header */}
      <div className="card-header" style={{ marginBottom: '1.5rem' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '0.6rem', marginBottom: '0.5rem' }}>
          <span className="badge badge-dark">Campus Network</span>
          {user?.collegeName && <span className="badge">{user.collegeName}</span>}
        </div>
        <h1 style={{ fontSize: '2.25rem', letterSpacing: '-0.03em' }}>
          Connections & Requests
        </h1>
        <p className="card-subtitle" style={{ fontSize: '1rem', marginTop: '0.35rem' }}>
          Manage your accepted academic peers, pending invitations, and safety controls.
        </p>
      </div>

      {actionSuccess && <Alert type="success" message={actionSuccess} />}
      {error && <Alert type="error" message={error} />}

      {/* Main Tabs */}
      <div className="segmented-nav">
        <button
          type="button"
          onClick={() => setActiveTab('accepted')}
          className={`segmented-tab ${activeTab === 'accepted' ? 'active' : ''}`}
        >
          <span>My Connections</span>
          <span className="segmented-tab-count">{accepted.length}</span>
        </button>

        <button
          type="button"
          onClick={() => setActiveTab('pending')}
          className={`segmented-tab ${activeTab === 'pending' ? 'active' : ''}`}
        >
          <span>Pending Requests</span>
          <span className="segmented-tab-count">{incoming.length + outgoing.length}</span>
        </button>

        <button
          type="button"
          onClick={() => setActiveTab('blocked')}
          className={`segmented-tab ${activeTab === 'blocked' ? 'active' : ''}`}
        >
          <span>Blocked Students</span>
          <span className="segmented-tab-count">{blockedList.length}</span>
        </button>
      </div>

      {loading ? (
        <div style={{ padding: '4rem 0', textAlign: 'center' }}>
          <LoadingSpinner size="lg" text="Loading network..." />
        </div>
      ) : (
        <>
          {/* TAB 1: ACCEPTED CONNECTIONS */}
          {activeTab === 'accepted' && (
            <div>
              {accepted.length === 0 ? (
                <div className="card text-center" style={{ padding: '3.5rem 1.5rem' }}>
                  <h2 style={{ fontSize: '1.3rem' }}>No Connected Peers Yet</h2>
                  <p style={{ maxWidth: '480px', margin: '0.75rem auto 1.5rem', color: 'var(--text-secondary)', fontSize: '0.925rem' }}>
                    Connect with fellow students from <strong>{user?.collegeName || 'your campus'}</strong> by browsing the student directory or top purpose matches.
                  </p>
                  <Link to="/students" className="btn btn-primary btn-sm">
                    Discover Campus Peers &rarr;
                  </Link>
                </div>
              ) : (
                <div
                  style={{
                    display: 'grid',
                    gridTemplateColumns: 'repeat(auto-fill, minmax(320px, 1fr))',
                    gap: '1.5rem',
                  }}
                >
                  {accepted.map((conn) => (
                    <article key={conn.id} className="card student-card" style={{ display: 'flex', flexDirection: 'column' }}>
                      <div style={{ display: 'flex', gap: '0.85rem', alignItems: 'flex-start', marginBottom: '0.85rem' }}>
                        <div className="avatar-monogram">
                          {getInitials(conn.fullName)}
                        </div>
                        <div style={{ flexGrow: 1, minWidth: 0 }}>
                          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
                            <h2 style={{ fontSize: '1.15rem', margin: 0, fontWeight: 700, whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>
                              {conn.fullName}
                            </h2>
                            <span className="badge badge-dark" style={{ flexShrink: 0, marginLeft: '0.5rem' }}>Connected</span>
                          </div>
                          <p style={{ margin: '0.2rem 0 0', fontSize: '0.85rem', color: 'var(--text-muted)' }}>
                            {conn.department} {conn.currentYear ? `• Year ${conn.currentYear}` : ''}
                          </p>
                        </div>
                      </div>

                      {conn.purposeNote && (
                        <div style={{
                          backgroundColor: 'var(--bg-subtle)',
                          padding: '0.6rem 0.8rem',
                          borderRadius: 'var(--radius-sm)',
                          border: '1px solid var(--border-light)',
                          marginBottom: '0.85rem',
                        }}>
                          <p style={{ fontSize: '0.825rem', color: 'var(--text-secondary)', fontStyle: 'italic', margin: 0 }}>
                            &ldquo;{conn.purposeNote}&rdquo;
                          </p>
                        </div>
                      )}

                      <div style={{ flexGrow: 1 }} />

                      <div style={{ display: 'flex', justifyContent: 'flex-end', marginTop: '0.75rem', paddingTop: '0.75rem', borderTop: '1px solid var(--border-light)' }}>
                        <button
                          type="button"
                          onClick={() => setBlockTarget({ userId: conn.userId, fullName: conn.fullName })}
                          className="btn btn-sm btn-subtle"
                          style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}
                        >
                          Block Student
                        </button>
                      </div>
                    </article>
                  ))}
                </div>
              )}
            </div>
          )}

          {/* TAB 2: PENDING REQUESTS */}
          {activeTab === 'pending' && (
            <div>
              {/* Sub-tabs for incoming and outgoing */}
              <div style={{ display: 'flex', gap: '0.5rem', marginBottom: '1.5rem' }}>
                <button
                  type="button"
                  onClick={() => setPendingSubTab('incoming')}
                  className={`btn btn-sm ${pendingSubTab === 'incoming' ? 'btn-secondary' : 'btn-subtle'}`}
                  style={{ borderRadius: 'var(--radius-sm)' }}
                >
                  Received Requests ({incoming.length})
                </button>
                <button
                  type="button"
                  onClick={() => setPendingSubTab('outgoing')}
                  className={`btn btn-sm ${pendingSubTab === 'outgoing' ? 'btn-secondary' : 'btn-subtle'}`}
                  style={{ borderRadius: 'var(--radius-sm)' }}
                >
                  Sent Requests ({outgoing.length}/15)
                </button>
              </div>

              {pendingSubTab === 'incoming' ? (
                <div>
                  {incoming.length === 0 ? (
                    <div className="card text-center" style={{ padding: '3.5rem 1.5rem' }}>
                      <div style={{ marginBottom: '0.75rem' }}>
                        <span className="badge badge-dark">Inbox Zero</span>
                      </div>
                      <h2 style={{ fontSize: '1.25rem', marginBottom: '0.4rem' }}>No Incoming Requests</h2>
                      <p style={{ maxWidth: '440px', margin: '0 auto', color: 'var(--text-secondary)', fontSize: '0.9rem' }}>
                        When campus peers invite you to collaborate, their requests and purpose notes will appear here.
                      </p>
                    </div>
                  ) : (
                    <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
                      {incoming.map((req) => (
                        <div
                          key={req.id}
                          className="card student-card"
                          style={{
                            display: 'flex',
                            justifyContent: 'space-between',
                            alignItems: 'center',
                            flexWrap: 'wrap',
                            gap: '1rem',
                            padding: '1.25rem',
                          }}
                        >
                          <div style={{ display: 'flex', gap: '0.85rem', alignItems: 'center' }}>
                            <div className="avatar-monogram">
                              {getInitials(req.fullName)}
                            </div>
                            <div>
                              <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginBottom: '0.2rem', flexWrap: 'wrap' }}>
                                <strong style={{ fontSize: '1.05rem' }}>{req.fullName}</strong>
                                <span className="badge">{req.department || 'Student'}</span>
                                {req.currentYear && <span className="badge">Year {req.currentYear}</span>}
                              </div>
                              {req.purposeNote && (
                                <p style={{ margin: '0.2rem 0 0', fontSize: '0.85rem', color: 'var(--text-secondary)' }}>
                                  Note: &ldquo;{req.purposeNote}&rdquo;
                                </p>
                              )}
                            </div>
                          </div>

                          <div style={{ display: 'flex', gap: '0.5rem' }}>
                            <button
                              type="button"
                              onClick={() => handleAccept(req.id)}
                              disabled={actionLoadingId === req.id}
                              className="btn btn-sm btn-primary"
                            >
                              {actionLoadingId === req.id ? <LoadingSpinner text="Accepting..." /> : 'Accept'}
                            </button>
                            <button
                              type="button"
                              onClick={() => handleReject(req.id)}
                              disabled={actionLoadingId === req.id}
                              className="btn btn-sm btn-secondary"
                            >
                              Reject
                            </button>
                          </div>
                        </div>
                      ))}
                    </div>
                  )}
                </div>
              ) : (
                <div>
                  <div style={{ marginBottom: '1rem', fontSize: '0.85rem', color: 'var(--text-muted)' }}>
                    Active sent requests ({outgoing.length} of 15 allowed). Cancel requests you no longer wish to pursue.
                  </div>

                  {outgoing.length === 0 ? (
                    <div className="card text-center" style={{ padding: '3.5rem 1.5rem' }}>
                      <div style={{ marginBottom: '0.75rem' }}>
                        <span className="badge badge-dark">Queue Clear</span>
                      </div>
                      <h2 style={{ fontSize: '1.25rem', marginBottom: '0.4rem' }}>No Active Outgoing Requests</h2>
                      <p style={{ maxWidth: '440px', margin: '0 auto', color: 'var(--text-secondary)', fontSize: '0.9rem' }}>
                        You haven&apos;t sent any connection requests yet. Find campus collaborators in Directory or Matches!
                      </p>
                    </div>
                  ) : (
                    <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
                      {outgoing.map((req) => (
                        <div
                          key={req.id}
                          className="card student-card"
                          style={{
                            display: 'flex',
                            justifyContent: 'space-between',
                            alignItems: 'center',
                            flexWrap: 'wrap',
                            gap: '1rem',
                            padding: '1.25rem',
                          }}
                        >
                          <div style={{ display: 'flex', gap: '0.85rem', alignItems: 'center' }}>
                            <div className="avatar-monogram">
                              {getInitials(req.fullName)}
                            </div>
                            <div>
                              <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginBottom: '0.2rem', flexWrap: 'wrap' }}>
                                <strong style={{ fontSize: '1.05rem' }}>{req.fullName}</strong>
                                <span className="badge">{req.department || 'Student'}</span>
                                <span className="badge badge-dark">Pending Response</span>
                              </div>
                              {req.purposeNote && (
                                <p style={{ margin: '0.2rem 0 0', fontSize: '0.85rem', color: 'var(--text-secondary)' }}>
                                  Note: &ldquo;{req.purposeNote}&rdquo;
                                </p>
                              )}
                            </div>
                          </div>

                          <button
                            type="button"
                            onClick={() => handleCancel(req.id)}
                            disabled={actionLoadingId === req.id}
                            className="btn btn-sm btn-subtle"
                            style={{ color: 'var(--error-text)' }}
                          >
                            {actionLoadingId === req.id ? <LoadingSpinner text="Cancelling..." /> : 'Cancel Request'}
                          </button>
                        </div>
                      ))}
                    </div>
                  )}
                </div>
              )}
            </div>
          )}

          {/* TAB 3: BLOCKED STUDENTS */}
          {activeTab === 'blocked' && (
            <div>
              {blockedList.length === 0 ? (
                <div className="card text-center" style={{ padding: '3.5rem 1.5rem' }}>
                  <div style={{ marginBottom: '0.75rem' }}>
                    <span className="badge badge-dark">Safety Clear</span>
                  </div>
                  <h2 style={{ fontSize: '1.25rem', marginBottom: '0.4rem' }}>No Blocked Students</h2>
                  <p style={{ maxWidth: '440px', margin: '0 auto', color: 'var(--text-secondary)', fontSize: '0.9rem' }}>
                    You haven&apos;t blocked anyone. If needed, you can block any peer directly from their profile card.
                  </p>
                </div>
              ) : (
                <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
                  {blockedList.map((item) => (
                    <div
                      key={item.blockedUserId}
                      className="card"
                      style={{
                        display: 'flex',
                        justifyContent: 'space-between',
                        alignItems: 'center',
                        flexWrap: 'wrap',
                        gap: '1rem',
                      }}
                    >
                      <div>
                        <strong style={{ fontSize: '1rem' }}>
                          {item.fullName || (item.blockedUserId ? `Student #${item.blockedUserId}` : 'Student')}
                        </strong>
                        <p style={{ margin: '0.2rem 0 0', fontSize: '0.8rem', color: 'var(--text-muted)' }}>
                          Blocked on {item.createdAt || item.blockedAt ? new Date(item.createdAt || item.blockedAt).toLocaleDateString() : '—'}
                        </p>
                      </div>

                      <button
                        type="button"
                        onClick={() => handleUnblock(item.blockedUserId)}
                        disabled={actionLoadingId === item.blockedUserId}
                        className="btn btn-sm btn-secondary"
                      >
                        {actionLoadingId === item.blockedUserId ? <LoadingSpinner text="Unblocking..." /> : 'Unblock'}
                      </button>
                    </div>
                  ))}
                </div>
              )}
            </div>
          )}
        </>
      )}

      {/* Block Confirm Modal */}
      {blockTarget && (
        <BlockConfirmModal
          student={blockTarget}
          onClose={() => setBlockTarget(null)}
          onSuccess={() => {
            fetchConnections();
            setActionSuccess('Student blocked.');
          }}
        />
      )}
    </div>
  );
}
