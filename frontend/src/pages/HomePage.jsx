import React, { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { Button, Card, StatusBadge, STATUS_CONFIG } from '../components/common';
import { systemApi, API_BASE_URL } from '../services/api';

export default function HomePage() {
  const { user, isAuthenticated, isAdmin } = useAuth();
  const [healthStatus, setHealthStatus] = useState(null);
  const [isLoadingHealth, setIsLoadingHealth] = useState(true);
  const [healthError, setHealthError] = useState(null);

  const fetchHealth = async () => {
    setIsLoadingHealth(true);
    setHealthError(null);
    try {
      const data = await systemApi.getHealth();
      setHealthStatus(data.status);
    } catch (err) {
      setHealthError(err.message || 'Unable to connect to backend server');
    } finally {
      setIsLoadingHealth(false);
    }
  };

  useEffect(() => {
    fetchHealth();
  }, []);

  return (
    <div className="home-page">
      {/* Hero Section */}
      <section className="hero">
        <h1 className="hero__title">
          Campus Logistics & <span className="hero__highlight">Carpooling</span>
        </h1>
        <p className="hero__subtitle">
          CARRY connects students heading to Khulna city with campus peers who need items
          purchased and brought back to KUET.
        </p>

        {!isAuthenticated ? (
          <div className="hero__actions">
            <Link to="/login">
              <Button variant="primary" size="lg">
                Sign In
              </Button>
            </Link>
            <Link to="/register">
              <Button variant="outline" size="lg">
                Create Account
              </Button>
            </Link>
          </div>
        ) : (
          <div className="hero__user-welcome">
            <p className="hero__welcome-text">
              Signed in as <strong>{user?.fullName}</strong> ({user?.email})
            </p>
            {isAdmin && (
              <span className="hero__admin-tag">
                Administrator Account
              </span>
            )}
          </div>
        )}
      </section>

      <div className="home-grid">
        {/* Design System StatusBadge Preview Section */}
        <Card
          title="Order Lifecycle States"
          subtitle="All 7 delivery states with distinct visual cues & accessible text labels"
          className="home-section-card"
        >
          <div className="status-grid">
            {Object.keys(STATUS_CONFIG).map((statusKey) => (
              <div key={statusKey} className="status-card-item">
                <StatusBadge status={statusKey} size="md" />
                <span className="status-desc">
                  {STATUS_CONFIG[statusKey].description}
                </span>
              </div>
            ))}
          </div>
        </Card>

        {/* System Backend Health Status */}
        <Card
          title="Backend System Status"
          subtitle={`Connected to ${API_BASE_URL}`}
          className="home-section-card"
        >
          {isLoadingHealth && (
            <div className="status-box loading" role="status">
              <span className="spinner" aria-hidden="true"></span>
              <span>Checking backend connection...</span>
            </div>
          )}

          {!isLoadingHealth && healthError && (
            <div className="status-box error" role="alert">
              <p>
                <strong>Connection Error:</strong> {healthError}
              </p>
              <Button
                variant="outline"
                size="sm"
                onClick={fetchHealth}
                className="mt-2"
              >
                Retry Health Check
              </Button>
            </div>
          )}

          {!isLoadingHealth && !healthError && (
            <div className="status-box success">
              <span className="badge">Status: {healthStatus}</span>
              <p className="success-text">
                All backend services operational and ready.
              </p>
            </div>
          )}
        </Card>
      </div>
    </div>
  );
}
