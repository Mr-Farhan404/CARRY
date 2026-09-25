import React from 'react';
import { Link, NavLink, useNavigate } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import Button from '../common/Button';

export default function Navbar() {
  const { user, isAuthenticated, isAdmin, logout } = useAuth();
  const navigate = useNavigate();

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  return (
    <header className="navbar">
      <div className="navbar__container">
        <Link to="/" className="navbar__brand">
          <span className="navbar__logo-box">C</span>
          <span className="navbar__logo-text">CARRY</span>
        </Link>

        <nav className="navbar__nav" aria-label="Main Navigation">
          <NavLink
            to="/"
            end
            className={({ isActive }) =>
              `navbar__link ${isActive ? 'navbar__link--active' : ''}`
            }
          >
            Home
          </NavLink>

          {isAuthenticated ? (
            <>
              <NavLink
                to="/requests"
                end
                className={({ isActive }) =>
                  `navbar__link ${isActive ? 'navbar__link--active' : ''}`
                }
              >
                My Requests
              </NavLink>

              <NavLink
                to="/requests/new"
                className={({ isActive }) =>
                  `navbar__link ${isActive ? 'navbar__link--active' : ''}`
                }
              >
                + Request
              </NavLink>

              <NavLink
                to="/requests/available"
                className={({ isActive }) =>
                  `navbar__link ${isActive ? 'navbar__link--active' : ''}`
                }
              >
                Available Orders
              </NavLink>

              
              <NavLink
                to="/trips"
                className={({ isActive }) =>
                  `navbar__link ${isActive ? 'navbar__link--active' : ''}`
                }
              >
                My Trips
              </NavLink>

              <NavLink
                to="/deliveries"
                className={({ isActive }) =>
                  `navbar__link ${isActive ? 'navbar__link--active' : ''}`
                }
              >
                My Deliveries
              </NavLink>

              <NavLink
                to="/trips/new"
                className={({ isActive }) =>
                  `navbar__link ${isActive ? 'navbar__link--active' : ''}`
                }
              >
                + Post Trip
              </NavLink>

              {/* Show Admin link ONLY when user is an admin */}
              {isAdmin && (
                <NavLink
                  to="/admin"
                  className={({ isActive }) =>
                    `navbar__link navbar__link--admin ${isActive ? 'navbar__link--active' : ''}`
                  }
                >
                  <span className="navbar__admin-badge">Admin</span>
                </NavLink>
              )}

              <div className="navbar__user-section">
                <div className="navbar__user-info" title={user?.email}>
                  <span className="navbar__user-name">{user?.fullName || 'Student'}</span>
                  {user?.department && (
                    <span className="navbar__user-dept">{user.department}</span>
                  )}
                </div>

                <Button
                  variant="outline"
                  size="sm"
                  onClick={handleLogout}
                  className="navbar__logout-btn"
                >
                  Logout
                </Button>
              </div>
            </>
          ) : (
            <div className="navbar__auth-links">
              <NavLink
                to="/login"
                className={({ isActive }) =>
                  `navbar__link ${isActive ? 'navbar__link--active' : ''}`
                }
              >
                Login
              </NavLink>
              <NavLink
                to="/register"
                className={({ isActive }) =>
                  `navbar__link navbar__link--cta ${isActive ? 'navbar__link--active' : ''}`
                }
              >
                Register
              </NavLink>
            </div>
          )}
        </nav>
      </div>
    </header>
  );
}
