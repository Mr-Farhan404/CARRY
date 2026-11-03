import React, { createContext, useContext, useState, useCallback } from 'react';
import { authApi } from '../services/api';

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  // CRITICAL REQUIREMENT: JWT and user are stored in memory (React state) ONLY, NOT localStorage.
  const [token, setToken] = useState(null);
  const [user, setUser] = useState(null);
  const [isLoading, setIsLoading] = useState(false);

  const login = useCallback(async (email, password) => {
    setIsLoading(true);
    try {
      const loginResponse = await authApi.login({ email, password });
      const authToken = loginResponse.token;
      
      // Fetch authenticated user profile
      const userProfile = await authApi.getMe(authToken);
      
      setToken(authToken);
      setUser(userProfile);
      return userProfile;
    } finally {
      setIsLoading(false);
    }
  }, []);

  const register = useCallback(async (userData) => {
    setIsLoading(true);
    try {
      await authApi.register(userData);
    } finally {
      setIsLoading(false);
    }
  }, []);

  const logout = useCallback(() => {
    setToken(null);
    setUser(null);
  }, []);

  const refreshProfile = useCallback(async () => {
    if (!token) return null;
    try {
      const userProfile = await authApi.getMe(token);
      setUser(userProfile);
      return userProfile;
    } catch (e) {
      console.error('Failed to refresh user profile', e);
      return null;
    }
  }, [token]);

  const role = user?.role || (user?.isAdmin ? 'ADMIN' : 'STUDENT');

  const value = {
    token,
    user,
    role,
    isAuthenticated: Boolean(token && user),
    isAdmin: Boolean(user?.isAdmin || role === 'ADMIN'),
    isModerator: role === 'MODERATOR',
    isDeliveryPartner: role === 'DELIVERY_PARTNER',
    isTeacher: role === 'TEACHER',
    isStudent: role === 'STUDENT',
    isLoading,
    login,
    register,
    refreshProfile,
    logout,
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
