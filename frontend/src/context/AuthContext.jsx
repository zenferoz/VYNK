import React, { createContext, useContext, useState, useEffect, useCallback } from 'react';
import { authApi } from '../api/auth';
import { profileApi } from '../api/profile';
import { ApiError, setUnauthorizedHandler } from '../api/client';

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null); // { email, userId, role, collegeId, collegeName, onboardingCompleted }
  const [profile, setProfile] = useState(null);
  const [loading, setLoading] = useState(true);
  const [networkError, setNetworkError] = useState(null);

  /**
   * Restores authenticated session from backend cookie.
   */
  const refreshSession = useCallback(async () => {
    try {
      setNetworkError(null);
      const sessionData = await authApi.getSession();
      setUser(sessionData);

      // If user has onboardingCompleted, also fetch student profile
      if (sessionData && sessionData.onboardingCompleted) {
        try {
          const profileData = await profileApi.getMyProfile();
          setProfile(profileData);
        } catch (profileErr) {
          console.warn('Unable to load profile data alongside session:', profileErr);
        }
      } else {
        setProfile(null);
      }
      return sessionData;
    } catch (err) {
      if (err instanceof ApiError && err.status === 401) {
        // Normal unauthenticated state; no active session cookie
        setUser(null);
        setProfile(null);
      } else if (err instanceof ApiError && err.status === 0) {
        // Network failure
        setNetworkError('Unable to connect to VYNK backend server.');
        setUser(null);
        setProfile(null);
      } else {
        setUser(null);
        setProfile(null);
      }
      return null;
    }
  }, []);

  // Initial session restoration on application mount
  useEffect(() => {
    let mounted = true;
    (async () => {
      await refreshSession();
      if (mounted) {
        setLoading(false);
      }
    })();
    return () => {
      mounted = false;
    };
  }, [refreshSession]);

  // Global 401 session eviction listener
  useEffect(() => {
    setUnauthorizedHandler(() => {
      setUser(null);
      setProfile(null);
    });
    return () => {
      setUnauthorizedHandler(null);
    };
  }, []);

  /**
   * Submits credentials, initiates session, and loads user profile.
   */
  const login = async (credentials) => {
    const loginResult = await authApi.login(credentials);
    const sessionData = await refreshSession();
    return { loginResult, sessionData };
  };

  /**
   * Terminates session on the backend and clears client memory state.
   */
  const logout = async () => {
    try {
      await authApi.logout();
    } finally {
      setUser(null);
      setProfile(null);
    }
  };

  /**
   * Refreshes profile state after an onboarding or profile update.
   */
  const updateProfileState = useCallback((newProfile) => {
    setProfile(newProfile);
    if (newProfile && newProfile.onboardingCompleted) {
      setUser((prev) => (prev ? { ...prev, onboardingCompleted: true } : null));
    }
  }, []);

  const value = {
    user,
    profile,
    loading,
    networkError,
    isAuthenticated: Boolean(user),
    isOnboarded: Boolean(user?.onboardingCompleted),
    login,
    logout,
    refreshSession,
    updateProfileState,
  };

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
}
