import React from 'react';
import { Navigate, useLocation } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

/**
 * AdminRoute Guard
 * 
 * Verifies that the current user is authenticated and has isAdmin === true.
 * Redirects non-admins (or unauthenticated visitors) BEFORE rendering
 * any admin component or triggering admin network calls.
 */
export default function AdminRoute({ children }) {
  const { isAuthenticated, isAdmin, isLoading } = useAuth();
  const location = useLocation();

  if (isLoading) {
    return (
      <div className="ui-loading-card">
        <span className="ui-btn__spinner" aria-hidden="true"></span>
        <span>Verifying administrative authorization...</span>
      </div>
    );
  }

  if (!isAuthenticated) {
    return <Navigate to="/login" state={{ from: location }} replace />;
  }

  if (!isAdmin) {
    // Strictly redirect non-admins away before mounting any admin child components
    return <Navigate to="/" replace />;
  }

  return children;
}
