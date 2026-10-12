import React, { useState, useEffect, useMemo, useCallback } from 'react';
import { useAuth } from '../context/AuthContext';
import { discoveryApi } from '../api/discovery';
import { StudentCard } from '../components/discovery/StudentCard';
import { ConnectModal } from '../components/discovery/ConnectModal';
import { ReportModal } from '../components/safety/ReportModal';
import { BlockConfirmModal } from '../components/safety/BlockConfirmModal';
import { InputField } from '../components/forms/InputField';
import { SelectField } from '../components/forms/SelectField';
import { Alert } from '../components/common/Alert';
import { LoadingSpinner } from '../components/common/LoadingSpinner';

export function DiscoveryPage() {
  const { user } = useAuth();
  const [activeTab, setActiveTab] = useState('directory'); // 'directory' | 'matches'

  const [students, setStudents] = useState([]);
  const [matches, setMatches] = useState([]);
  const [loading, setLoading] = useState(true);
  const [loadingMatches, setLoadingMatches] = useState(false);
  const [error, setError] = useState('');
  const [actionSuccess, setActionSuccess] = useState('');

  // Modals state
  const [connectStudent, setConnectStudent] = useState(null);
  const [reportStudent, setReportStudent] = useState(null);
  const [blockStudent, setBlockStudent] = useState(null);

  // Client-side filter states for Directory tab
  const [searchQuery, setSearchQuery] = useState('');
  const [selectedDepartment, setSelectedDepartment] = useState('');
  const [selectedYear, setSelectedYear] = useState('');

  const fetchStudents = useCallback(async () => {
    try {
      setLoading(true);
      setError('');
      const data = await discoveryApi.getStudents();
      setStudents(Array.isArray(data) ? data : []);
    } catch (err) {
      setError(err.message || 'Unable to load student directory. Please try again.');
    } finally {
      setLoading(false);
    }
  }, []);

  const fetchMatches = useCallback(async () => {
    try {
      setLoadingMatches(true);
      setError('');
      const data = await discoveryApi.getMatches();
      setMatches(Array.isArray(data) ? data : []);
    } catch (err) {
      setError(err.message || 'Unable to load purpose matches.');
    } finally {
      setLoadingMatches(false);
    }
  }, []);

  useEffect(() => {
    fetchStudents();
  }, [fetchStudents]);

  const handleTabChange = (tab) => {
    setActiveTab(tab);
    if (tab === 'matches') {
      fetchMatches();
    } else {
      fetchStudents();
    }
  };

  // Distinct departments
  const availableDepartments = useMemo(() => {
    const depts = new Set();
    students.forEach((s) => {
      if (s.department && s.department.trim()) {
        depts.add(s.department.trim());
      }
    });
    return Array.from(depts).sort();
  }, [students]);

  // Distinct academic years
  const availableYears = useMemo(() => {
    const years = new Set();
    students.forEach((s) => {
      if (s.currentYear) {
        years.add(String(s.currentYear));
      }
    });
    return Array.from(years).sort((a, b) => Number(a) - Number(b));
  }, [students]);

  // Filtered directory students
  const filteredStudents = useMemo(() => {
    return students.filter((student) => {
      if (searchQuery.trim()) {
        const query = searchQuery.toLowerCase().trim();
        const matchesName = student.fullName && student.fullName.toLowerCase().includes(query);
        const matchesBio = student.bio && student.bio.toLowerCase().includes(query);
        const matchesSkills = student.skills && student.skills.some((sk) => sk.toLowerCase().includes(query));
        if (!matchesName && !matchesBio && !matchesSkills) {
          return false;
        }
      }

      if (selectedDepartment && student.department !== selectedDepartment) {
        return false;
      }

      if (selectedYear && String(student.currentYear) !== selectedYear) {
        return false;
      }

      return true;
    });
  }, [students, searchQuery, selectedDepartment, selectedYear]);

  const hasActiveFilters = Boolean(searchQuery.trim() || selectedDepartment || selectedYear);

  const handleClearFilters = () => {
    setSearchQuery('');
    setSelectedDepartment('');
    setSelectedYear('');
  };

  const handleStudentBlocked = (targetUserId) => {
    setStudents((prev) => prev.filter((s) => (s.userId || s.id) !== targetUserId));
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
      {/* Header */}
      <div className="card-header" style={{ marginBottom: '1.5rem' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '0.6rem', marginBottom: '0.5rem' }}>
          <span className="badge badge-dark">Campus Discovery</span>
          {user?.collegeName && <span className="badge">{user.collegeName}</span>}
        </div>
        <h1 style={{ fontSize: '2.25rem', letterSpacing: '-0.03em' }}>
          Peer Discovery & Purpose Matching
        </h1>
        <p className="card-subtitle" style={{ fontSize: '1rem', marginTop: '0.35rem' }}>
          Discover verified academic peers exclusively within {user?.collegeName || 'your campus network'}.
        </p>
      </div>

      {actionSuccess && (
        <Alert
          type="success"
          message={actionSuccess}
        />
      )}

      {error && (
        <Alert
          type="error"
          message={error}
          onRetry={activeTab === 'matches' ? fetchMatches : fetchStudents}
        />
      )}

      {/* Tabs navigation */}
      <div className="segmented-nav">
        <button
          type="button"
          onClick={() => handleTabChange('directory')}
          className={`segmented-tab ${activeTab === 'directory' ? 'active' : ''}`}
        >
          <span>Campus Directory</span>
          <span className="segmented-tab-count">{students.length}</span>
        </button>
        <button
          type="button"
          onClick={() => handleTabChange('matches')}
          className={`segmented-tab ${activeTab === 'matches' ? 'active' : ''}`}
        >
          <span>Top Purpose Matches</span>
          {matches.length > 0 && <span className="segmented-tab-count">{matches.length}</span>}
        </button>
      </div>

      {/* DIRECTORY TAB CONTENT */}
      {activeTab === 'directory' && (
        <>
          {/* Filter and Search Bar */}
          {!loading && students.length > 0 && (
            <div
              className="card"
              style={{
                marginBottom: '2rem',
                padding: '1.25rem',
                backgroundColor: 'var(--bg-secondary)',
              }}
            >
              <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))', gap: '1rem', alignItems: 'flex-end' }}>
                <InputField
                  id="studentSearch"
                  name="studentSearch"
                  label="Search Students"
                  value={searchQuery}
                  onChange={(e) => setSearchQuery(e.target.value)}
                  placeholder="Search by name, bio, or skill..."
                />

                <SelectField
                  id="departmentFilter"
                  name="departmentFilter"
                  label="Filter by Department"
                  value={selectedDepartment}
                  onChange={(e) => setSelectedDepartment(e.target.value)}
                  placeholder="All Departments"
                  options={availableDepartments.map((d) => ({ value: d, label: d }))}
                />

                <SelectField
                  id="yearFilter"
                  name="yearFilter"
                  label="Filter by Year"
                  value={selectedYear}
                  onChange={(e) => setSelectedYear(e.target.value)}
                  placeholder="All Years"
                  options={availableYears.map((y) => ({ value: y, label: `Year ${y}` }))}
                />
              </div>

              {hasActiveFilters && (
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginTop: '1rem', paddingTop: '0.75rem', borderTop: '1px solid var(--border-light)' }}>
                  <span style={{ fontSize: '0.85rem', color: 'var(--text-muted)' }}>
                    Showing <strong>{filteredStudents.length}</strong> of <strong>{students.length}</strong> students
                  </span>
                  <button
                    type="button"
                    onClick={handleClearFilters}
                    className="btn btn-sm btn-subtle"
                  >
                    Clear all filters
                  </button>
                </div>
              )}
            </div>
          )}

          {/* Loading State */}
          {loading && (
            <div style={{ padding: '4rem 0', textAlign: 'center' }}>
              <LoadingSpinner size="lg" text="Loading campus directory..." />
            </div>
          )}

          {/* Empty Directory */}
          {!loading && !error && students.length === 0 && (
            <div className="card text-center" style={{ padding: '3.5rem 1.5rem' }}>
              <div style={{ marginBottom: '1rem' }}>
                <span className="badge badge-dark">Directory Empty</span>
              </div>
              <h2 style={{ fontSize: '1.4rem' }}>No Other Students Discovered Yet</h2>
              <p style={{ maxWidth: '520px', margin: '0.75rem auto 1.5rem', color: 'var(--text-secondary)', fontSize: '0.95rem' }}>
                You are the first active, discoverable student registered at <strong>{user?.collegeName || 'your college'}</strong>, or other students have not completed onboarding yet.
              </p>
              <button
                type="button"
                onClick={fetchStudents}
                className="btn btn-secondary btn-sm"
              >
                Refresh Directory
              </button>
            </div>
          )}

          {/* Filter Empty State */}
          {!loading && !error && students.length > 0 && filteredStudents.length === 0 && (
            <div className="card text-center" style={{ padding: '3rem 1.5rem' }}>
              <h2 style={{ fontSize: '1.25rem' }}>No Students Match Your Filters</h2>
              <p style={{ margin: '0.5rem 0 1.25rem', color: 'var(--text-secondary)', fontSize: '0.9rem' }}>
                No discoverable students match &ldquo;{searchQuery}&rdquo; with the selected filters.
              </p>
              <button
                type="button"
                onClick={handleClearFilters}
                className="btn btn-secondary btn-sm"
              >
                Reset Filters
              </button>
            </div>
          )}

          {/* Students Grid */}
          {!loading && !error && filteredStudents.length > 0 && (
            <div
              style={{
                display: 'grid',
                gridTemplateColumns: 'repeat(auto-fill, minmax(320px, 1fr))',
                gap: '1.5rem',
              }}
            >
              {filteredStudents.map((student) => (
                <StudentCard
                  key={student.id || student.userId}
                  student={student}
                  isMatch={false}
                  onConnect={(s) => setConnectStudent(s)}
                  onBlock={(s) => setBlockStudent(s)}
                  onReport={(s) => setReportStudent(s)}
                />
              ))}
            </div>
          )}
        </>
      )}

      {/* TOP PURPOSE MATCHES TAB CONTENT */}
      {activeTab === 'matches' && (
        <>
          <div className="card" style={{ marginBottom: '1.5rem', backgroundColor: 'var(--bg-subtle)' }}>
            <p style={{ margin: 0, fontSize: '0.9rem', color: 'var(--text-secondary)' }}>
              Matching is calculated deterministically based on your <strong>active networking goals (40 pts)</strong>, <strong>shared skills & interests (30 pts)</strong>, <strong>department/year affinity (20 pts)</strong>, and <strong>schedule compatibility (10 pts)</strong>.
            </p>
          </div>

          {loadingMatches && (
            <div style={{ padding: '4rem 0', textAlign: 'center' }}>
              <LoadingSpinner size="lg" text="Calculating deterministic recommendations..." />
            </div>
          )}

          {!loadingMatches && !error && matches.length === 0 && (
            <div className="card text-center" style={{ padding: '3.5rem 1.5rem' }}>
              <div style={{ marginBottom: '1rem' }}>
                <span className="badge badge-dark">No Matches Found</span>
              </div>
              <h2 style={{ fontSize: '1.3rem' }}>No Compatible Purpose Matches Yet</h2>
              <p style={{ maxWidth: '520px', margin: '0.75rem auto 1.5rem', color: 'var(--text-secondary)', fontSize: '0.925rem' }}>
                Make sure you have created at least one active <strong>Networking Goal</strong> and added relevant skills in your profile to find aligned peers!
              </p>
              <button
                type="button"
                onClick={fetchMatches}
                className="btn btn-secondary btn-sm"
              >
                Refresh Recommendations
              </button>
            </div>
          )}

          {!loadingMatches && !error && matches.length > 0 && (
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
        </>
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
