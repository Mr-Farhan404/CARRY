import React, { useState } from 'react';
import { Link, NavLink, useNavigate } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import { useCart } from '../../context/CartContext';
import Button from '../common/Button';
import ProfileModal from '../profile/ProfileModal';

export default function Navbar() {
  const { user, role, isAuthenticated, isAdmin, isModerator, logout } = useAuth();
  const { openCart, totalCount } = useCart();
  const [profileModalOpen, setProfileModalOpen] = useState(false);
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

          <NavLink
            to="/shop"
            className={({ isActive }) =>
              `navbar__link ${isActive ? 'navbar__link--active' : ''}`
            }
          >
            🛍️ Shop
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

              {/* Show Moderator link when user is a moderator */}
              {isModerator && (
                <NavLink
                  to="/moderator"
                  className={({ isActive }) =>
                    `navbar__link ${isActive ? 'navbar__link--active' : ''}`
                  }
                  style={{ background: '#f5f3ff', color: '#7c3aed', fontWeight: 700 }}
                >
                  <span>🛡️ Moderator</span>
                </NavLink>
              )}

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

              <button
                type="button"
                className="navbar__cart-btn"
                onClick={openCart}
                title="View Shopping Cart"
                aria-label="View Shopping Cart"
              >
                <span className="navbar__cart-icon">🛒</span>
                {totalCount > 0 && <span className="navbar__cart-badge">{totalCount}</span>}
              </button>

              <div className="navbar__user-section">
                <div
                  className="navbar__user-info"
                  title="Click to view Profile & Role Applications"
                  onClick={() => setProfileModalOpen(true)}
                  style={{ cursor: 'pointer', display: 'flex', flexDirection: 'column' }}
                >
                  <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
                    <span className="navbar__user-name">{user?.fullName || 'User'}</span>
                    <span style={{
                      fontSize: '0.7rem',
                      fontWeight: 700,
                      padding: '1px 6px',
                      borderRadius: '8px',
                      background: role === 'ADMIN' ? '#dc2626' : role === 'MODERATOR' ? '#7c3aed' : role === 'DELIVERY_PARTNER' ? '#0284c7' : role === 'TEACHER' ? '#059669' : '#64748b',
                      color: '#fff',
                    }}>
                      {role === 'ADMIN' ? '👑 Admin' : role === 'MODERATOR' ? '🛡️ Mod' : role === 'DELIVERY_PARTNER' ? '🚗 Partner' : role === 'TEACHER' ? '👨‍🏫 Faculty' : '🎓 Student'}
                    </span>
                  </div>
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

              <ProfileModal
                isOpen={profileModalOpen}
                onClose={() => setProfileModalOpen(false)}
              />
            </>
          ) : (
            <div className="navbar__auth-links">
              <button
                type="button"
                className="navbar__cart-btn"
                onClick={openCart}
                title="View Shopping Cart"
                aria-label="View Shopping Cart"
              >
                <span className="navbar__cart-icon">🛒</span>
                {totalCount > 0 && <span className="navbar__cart-badge">{totalCount}</span>}
              </button>

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
