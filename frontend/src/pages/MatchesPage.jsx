import React, { useState, useEffect, useCallback } from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { discoveryApi } from '../api/discovery';
import { StudentCard } from '../components/discovery/StudentCard';
import { ConnectModal } from '../components/discovery/ConnectModal';
import { ReportModal } from '../components/safety/ReportModal';
import { BlockConfirmModal } from '../components/safety/BlockConfirmModal';
import { Alert } from '../components/common/Alert';
import { LoadingSpinner } from '../components/common/LoadingSpinner';

export function MatchesPage() {
  const { user } = useAuth();
  const [matches, setMatches] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [actionSuccess, setActionSuccess] = useState('');

  // Modals state
  const [connectStudent, setConnectStudent] = useState(null);
  const [reportStudent, setReportStudent] = useState(null);
  const [blockStudent, setBlockStudent] = useState(null);

  const fetchMatches = useCallback(async () => {
    try {
      setLoading(true);
      setError('');
      const data = await discoveryApi.getMatches();
      setMatches(Array.isArray(data) ? data : []);
    } catch (err) {
      setError(err.message || 'Unable to load purpose-based matches. Please try again.');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchMatches();
  }, [fetchMatches]);

  const handleStudentBlocked = (targetUserId) => {
    setMatches((prev) => prev.filter((m) => (m.userId || m.id) !== targetUserId));
    setActionSuccess('Student blocked successfully. Both profiles are now mutually excluded.');
  };

  const handleConnectSuccess = () => {
    setActionSuccess('Connection request sent successfully!');
  };

  const handleReportSuccess = () => {
    setActionSuccess('Report submitted successfully.');
  };

  return (
    <div className="container page-enter" style={{ paddingTop: '2.5rem', paddingBottom: '4rem' }}>
      {/* Page Header */}
      <div className="card-header" style={{ marginBottom: '1.5rem' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '0.6rem', marginBottom: '0.5rem' }}>
          <span className="badge badge-dark">Purpose Matching</span>
          {user?.collegeName && <span className="badge">{user.collegeName}</span>}
        </div>
        <h1 style={{ fontSize: '2.25rem', letterSpacing: '-0.03em' }}>
          Top Purpose Matches
        </h1>
        <p className="card-subtitle" style={{ fontSize: '1rem', marginTop: '0.35rem' }}>
          Recommended peers ranked deterministically based on shared goals, overlapping skills, academic year, and schedule compatibility.
        </p>
      </div>

      {actionSuccess && <Alert type="success" message={actionSuccess} />}
      {error && <Alert type="error" message={error} onRetry={fetchMatches} />}

      {/* Explanatory Banner */}
      <div className="card" style={{ marginBottom: '2rem', backgroundColor: 'var(--bg-subtle)' }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '1rem' }}>
          <p style={{ margin: 0, fontSize: '0.9rem', color: 'var(--text-secondary)' }}>
            Matching evaluates <strong>active networking goals (40 pts)</strong>, <strong>shared skills & interests (30 pts)</strong>, <strong>department/year affinity (20 pts)</strong>, and <strong>schedule compatibility (10 pts)</strong>.
          </p>
          <div style={{ display: 'flex', gap: '0.5rem' }}>
            <Link to="/profile" className="btn btn-sm btn-subtle">
              Manage Goals & Skills &rarr;
            </Link>
          </div>
        </div>
      </div>

      {/* Loading State */}
      {loading && (
        <div style={{ padding: '4rem 0', textAlign: 'center' }}>
          <LoadingSpinner size="lg" text="Calculating deterministic recommendations..." />
        </div>
      )}

      {/* Empty State */}
      {!loading && !error && matches.length === 0 && (
        <div className="card text-center" style={{ padding: '3.5rem 1.5rem' }}>
          <div style={{ marginBottom: '1rem' }}>
            <span className="badge badge-dark">No Matches Found</span>
          </div>
          <h2 style={{ fontSize: '1.3rem' }}>No Compatible Purpose Matches Yet</h2>
          <p style={{ maxWidth: '520px', margin: '0.75rem auto 1.5rem', color: 'var(--text-secondary)', fontSize: '0.925rem' }}>
            Ensure you have added at least one active <strong>Networking Goal</strong> (e.g. Hackathon Team, Project Collaboration) and relevant skills in your profile to find aligned peers on campus.
          </p>
          <div style={{ display: 'flex', justifyContent: 'center', gap: '0.75rem', flexWrap: 'wrap' }}>
            <Link to="/profile" className="btn btn-primary btn-sm">
              Update Profile Goals &rarr;
            </Link>
            <button
              type="button"
              onClick={fetchMatches}
              className="btn btn-secondary btn-sm"
            >
              Refresh Recommendations
            </button>
          </div>
        </div>
      )}

      {/* Matches Grid */}
      {!loading && !error && matches.length > 0 && (
        <div
          style={{
            display: 'grid',
            gridTemplateColumns: 'repeat(auto-fill, minmax(320px, 1fr))',
            gap: '1.5rem',
          }}
        >
          {matches.map((match) => (
            <StudentCard
              key={match.userId}
              student={match}
              isMatch={true}
              onConnect={(s) => setConnectStudent(s)}
              onBlock={(s) => setBlockStudent(s)}
              onReport={(s) => setReportStudent(s)}
            />
          ))}
        </div>
      )}

      {/* Modals */}
      {connectStudent && (
        <ConnectModal
          student={connectStudent}
          onClose={() => setConnectStudent(null)}
          onSuccess={handleConnectSuccess}
        />
      )}

      {reportStudent && (
        <ReportModal
          student={reportStudent}
          onClose={() => setReportStudent(null)}
          onSuccess={handleReportSuccess}
        />
      )}

      {blockStudent && (
        <BlockConfirmModal
          student={blockStudent}
          onClose={() => setBlockStudent(null)}
          onSuccess={() => handleStudentBlocked(blockStudent.userId || blockStudent.id)}
        />
      )}
    </div>
  );
}
