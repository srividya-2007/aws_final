import React, { createContext, useContext, useState, useEffect, useCallback } from 'react';
import { authAPI } from '../services/api';

const AuthContext = createContext(null);

function parseJwt(token) {
  try {
    const base64 = token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/');
    return JSON.parse(atob(base64));
  } catch {
    return null;
  }
}

export function AuthProvider({ children }) {
  const [user, setUser]     = useState(null);
  const [loading, setLoading] = useState(true);

  // Restore session on mount
  useEffect(() => {
    const token    = localStorage.getItem('token');
    const username = localStorage.getItem('username');
    const role     = localStorage.getItem('role');
    if (token && username && role) {
      const payload = parseJwt(token);
      // Check expiry
      if (payload && payload.exp * 1000 > Date.now()) {
        setUser({ token, username, role });
      } else {
        localStorage.clear();
      }
    }
    setLoading(false);
  }, []);

  const login = useCallback(async (username, password) => {
    const res = await authAPI.login({ username, password });
    if (res.data.otpRequired) {
      return res.data;
    }
    const { token, refreshToken, username: uname, role } = res.data;
    localStorage.setItem('token', token);
    if (refreshToken) localStorage.setItem('refreshToken', refreshToken);
    localStorage.setItem('username', uname);
    localStorage.setItem('role', role);
    setUser({ token, username: uname, role });
    return role;
  }, []);

  const verifyLoginOtp = useCallback(async ({ email, otpCode, username }) => {
    const res = await authAPI.verifyLoginOtp({ email, otpCode, username });
    const { token, refreshToken, username: uname, role } = res.data;
    localStorage.setItem('token', token);
    if (refreshToken) localStorage.setItem('refreshToken', refreshToken);
    localStorage.setItem('username', uname);
    localStorage.setItem('role', role);
    setUser({ token, username: uname, role });
    return role;
  }, []);

  const signup = useCallback(async (payload) => {
    const res = await authAPI.signup(payload);
    return res.data;
  }, []);

  const verifySignupOtp = useCallback(async ({ email, otpCode }) => {
    const res = await authAPI.verifySignupOtp({ email, otpCode });
    return res.data;
  }, []);

  const resendOtp = useCallback(async ({ email, purpose }) => {
    const res = await authAPI.resendOtp({ email, purpose });
    return res.data;
  }, []);

  const register = useCallback(async (payload) => {
    const res = await authAPI.register(payload);
    return res.data;
  }, []);

  const logout = useCallback(() => {
    localStorage.clear();
    setUser(null);
  }, []);

  const isAuthenticated = !!user;
  const hasRole = (r) => user?.role === r;

  return (
    <AuthContext.Provider value={{
      user,
      loading,
      isAuthenticated,
      login,
      verifyLoginOtp,
      signup,
      verifySignupOtp,
      resendOtp,
      register,
      logout,
      hasRole
    }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  return useContext(AuthContext);
}
