import React, { useState, useEffect, useMemo } from 'react';
import { useAuth } from '../context/AuthContext';
import { adminApi } from '../services/api';
import Card from '../components/common/Card';
import Button from '../components/common/Button';
import Input from '../components/common/Input';
import StatusBadge from '../components/common/StatusBadge';

export default function AdminPage() {
  const { token, user } = useAuth();

  // Active Tab: 'complaints' | 'requests' | 'users'
  const [activeTab, setActiveTab] = useState('complaints');

  // Data states
  const [complaints, setComplaints] = useState([]);
  const [requests, setRequests] = useState([]);
  const [users, setUsers] = useState([]);

  // Loading & error states
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState(null);
  const [isRefreshing, setIsRefreshing] = useState(false);

  // Complaints filter & management modal state
  const [complaintFilter, setComplaintFilter] = useState('ALL');
  const [selectedComplaint, setSelectedComplaint] = useState(null);
  const [adminNotesInput, setAdminNotesInput] = useState('');
  const [isUpdatingComplaint, setIsUpdatingComplaint] = useState(false);
  const [complaintActionError, setComplaintActionError] = useState(null);
  const [complaintActionSuccess, setComplaintActionSuccess] = useState(null);

  // Requests filter state
  const [requestStatusFilter, setRequestStatusFilter] = useState('ALL');
  const [requestSearch, setRequestSearch] = useState('');
  const [selectedRequestDetails, setSelectedRequestDetails] = useState(null);

  // Users filter state
  const [userSearch, setUserSearch] = useState('');
  const [userRoleFilter, setUserRoleFilter] = useState('ALL');

  // Load all admin data
  const loadAdminData = async (silent = false) => {
    if (!silent) setIsLoading(true);
    else setIsRefreshing(true);
    setError(null);

    try {
      const [complaintsData, requestsData, usersData] = await Promise.all([
        adminApi.getComplaints(token),
        adminApi.getRequests('ALL', token),
        adminApi.getUsers(token),
      ]);

      setComplaints(complaintsData || []);
      setRequests(requestsData || []);
      setUsers(usersData || []);
    } catch (err) {
      console.error('Failed to load admin data:', err);
      setError(err.message || 'Failed to fetch administrative data');
    } finally {
      setIsLoading(false);
      setIsRefreshing(false);
    }
  };

  useEffect(() => {
    loadAdminData();
  }, [token]);

  // Open complaint for management
  const handleOpenComplaint = (complaint) => {
    setSelectedComplaint(complaint);
    setAdminNotesInput(complaint.adminNotes || '');
    setComplaintActionError(null);
    setComplaintActionSuccess(null);
  };

  // Close complaint modal
  const handleCloseComplaintModal = () => {
    setSelectedComplaint(null);
    setAdminNotesInput('');
    setComplaintActionError(null);
    setComplaintActionSuccess(null);
  };

  // Update complaint status & notes
  const handleUpdateComplaintStatus = async (newStatus) => {
    if (!selectedComplaint) return;

    setIsUpdatingComplaint(true);
    setComplaintActionError(null);
    setComplaintActionSuccess(null);

    try {
      const updated = await adminApi.updateComplaint(
        selectedComplaint.id,
        {
          status: newStatus,
          adminNotes: adminNotesInput.trim() || null,
        },
        token
      );

      // Update in local state
      setComplaints((prev) =>
        prev.map((c) => (c.id === updated.id ? updated : c))
      );
      setSelectedComplaint(updated);
      setComplaintActionSuccess(
        `Complaint #${updated.id} status updated to ${updated.status}`
      );
    } catch (err) {
      console.error('Failed to update complaint:', err);
      setComplaintActionError(err.message || 'Failed to update complaint');
    } finally {
      setIsUpdatingComplaint(false);
    }
  };

  // Filtered Complaints
  const filteredComplaints = useMemo(() => {
    if (complaintFilter === 'ALL') return complaints;
    return complaints.filter((c) => c.status === complaintFilter);
  }, [complaints, complaintFilter]);

  // Filtered Requests
  const filteredRequests = useMemo(() => {
    return requests.filter((r) => {
      const matchesStatus =
        requestStatusFilter === 'ALL' || r.status === requestStatusFilter;

      const q = requestSearch.toLowerCase().trim();
      const matchesSearch =
        !q ||
        r.productName?.toLowerCase().includes(q) ||
        r.customerName?.toLowerCase().includes(q) ||
        r.pickupArea?.toLowerCase().includes(q) ||
        String(r.id).includes(q);

      return matchesStatus && matchesSearch;
    });
  }, [requests, requestStatusFilter, requestSearch]);

  // Filtered Users
  const filteredUsers = useMemo(() => {
    return users.filter((u) => {
      const matchesRole =
        userRoleFilter === 'ALL' ||
        (userRoleFilter === 'ADMIN' && u.isAdmin) ||
        (userRoleFilter === 'STUDENT' && !u.isAdmin);

      const q = userSearch.toLowerCase().trim();
      const matchesSearch =
        !q ||
        u.fullName?.toLowerCase().includes(q) ||
        u.email?.toLowerCase().includes(q) ||
        u.studentId?.toLowerCase().includes(q) ||
        u.department?.toLowerCase().includes(q);

      return matchesRole && matchesSearch;
    });
  }, [users, userRoleFilter, userSearch]);

  // Overview Stats
  const openComplaintsCount = complaints.filter(
    (c) => c.status === 'OPEN' || c.status === 'IN_REVIEW'
  ).length;

  const formatDate = (isoString) => {
    if (!isoString) return '—';
    try {
      const d = new Date(isoString);
      return d.toLocaleDateString('en-US', {
        month: 'short',
        day: 'numeric',
        year: 'numeric',
        hour: '2-digit',
        minute: '2-digit',
      });
    } catch {
      return isoString;
    }
  };

  return (
    <div className="admin-page">
      {/* Admin Header */}
      <div className="admin-header-row">
        <div>
          <div className="admin-badge-label">
            <span className="admin-lock-icon">🛡️</span> Administrator Control Center
          </div>
          <h1 className="admin-page-title">Platform Oversight Panel</h1>
          <p className="admin-page-subtitle">
            Manage student users, review platform delivery requests, and resolve dispute complaints.
          </p>
        </div>

        <div className="admin-header-actions">
          <Button
            variant="outline"
            size="sm"
            onClick={() => loadAdminData(true)}
            isLoading={isRefreshing}
            disabled={isRefreshing}
          >
            ↻ Refresh Data
          </Button>
        </div>
      </div>

      {/* Global Error Banner */}
      {error && (
        <div className="ui-alert ui-alert--error" role="alert">
          {error}
        </div>
      )}

      {/* Overview Stats Bar */}
      <div className="admin-stats-grid">
        <div className="admin-stat-card">
          <span className="admin-stat-icon">👥</span>
          <div className="admin-stat-info">
            <span className="admin-stat-label">Total Users</span>
            <span className="admin-stat-value">{users.length}</span>
          </div>
        </div>

        <div className="admin-stat-card">
          <span className="admin-stat-icon">📦</span>
          <div className="admin-stat-info">
            <span className="admin-stat-label">Total Requests</span>
            <span className="admin-stat-value">{requests.length}</span>
          </div>
        </div>

        <div className="admin-stat-card admin-stat-card--attention">
          <span className="admin-stat-icon">⚠️</span>
          <div className="admin-stat-info">
            <span className="admin-stat-label">Pending Complaints</span>
            <span className="admin-stat-value">{openComplaintsCount}</span>
          </div>
        </div>

        <div className="admin-stat-card">
          <span className="admin-stat-icon">✅</span>
          <div className="admin-stat-info">
            <span className="admin-stat-label">Resolved Complaints</span>
            <span className="admin-stat-value">
              {complaints.filter((c) => c.status === 'RESOLVED').length}
            </span>
          </div>
        </div>
      </div>

      {/* Navigation Tabs */}
      <div className="admin-tabs" role="tablist">
        <button
          type="button"
          role="tab"
          aria-selected={activeTab === 'complaints'}
          className={`admin-tab ${activeTab === 'complaints' ? 'admin-tab--active' : ''}`}
          onClick={() => setActiveTab('complaints')}
        >
          <span>Complaints Queue</span>
          {openComplaintsCount > 0 ? (
            <span className="admin-tab-count admin-tab-count--alert">
              {openComplaintsCount}
            </span>
          ) : (
            <span className="admin-tab-count">{complaints.length}</span>
          )}
        </button>

        <button
          type="button"
          role="tab"
          aria-selected={activeTab === 'requests'}
          className={`admin-tab ${activeTab === 'requests' ? 'admin-tab--active' : ''}`}
          onClick={() => setActiveTab('requests')}
        >
          <span>Requests Registry</span>
          <span className="admin-tab-count">{requests.length}</span>
        </button>

        <button
          type="button"
          role="tab"
          aria-selected={activeTab === 'users'}
          className={`admin-tab ${activeTab === 'users' ? 'admin-tab--active' : ''}`}
          onClick={() => setActiveTab('users')}
        >
          <span>User Directory</span>
          <span className="admin-tab-count">{users.length}</span>
        </button>
      </div>

      {/* Loading State */}
      {isLoading ? (
        <div className="ui-loading-card">
          <span className="ui-btn__spinner" aria-hidden="true"></span>
          <span>Loading administrative data...</span>
        </div>
      ) : (
        <div className="admin-tab-content">
          {/* ============================================================ */}
          {/* TAB 1: COMPLAINTS QUEUE                                       */}
          {/* ============================================================ */}
          {activeTab === 'complaints' && (
            <div className="complaints-tab">
              {/* Complaints Filter Toolbar */}
              <div className="admin-toolbar">
                <div className="admin-filter-group">
                  <span className="admin-filter-label">Filter Status:</span>
                  <div className="admin-filter-pills">
                    {['ALL', 'OPEN', 'IN_REVIEW', 'RESOLVED'].map((st) => (
                      <button
                        key={st}
                        type="button"
                        className={`admin-filter-pill ${
                          complaintFilter === st ? 'admin-filter-pill--active' : ''
                        }`}
                        onClick={() => setComplaintFilter(st)}
                      >
                        {st === 'ALL'
                          ? `All (${complaints.length})`
                          : `${st.replace('_', ' ')} (${
                              complaints.filter((c) => c.status === st).length
                            })`}
                      </button>
                    ))}
                  </div>
                </div>
              </div>

              {filteredComplaints.length === 0 ? (
                <Card className="empty-state-card">
                  <div className="empty-state">
                    <span className="empty-state__icon">🎉</span>
                    <h3>No complaints found</h3>
                    <p>
                      {complaintFilter === 'ALL'
                        ? 'There are currently no dispute complaints in the system.'
                        : `No complaints found with status "${complaintFilter}".`}
                    </p>
                  </div>
                </Card>
              ) : (
                <div className="complaints-list">
                  {filteredComplaints.map((complaint) => (
                    <Card key={complaint.id} className="complaint-card">
                      <div className="complaint-card__header">
                        <div className="complaint-card__id-group">
                          <span className="complaint-id-tag">#CMP-{complaint.id}</span>
                          <StatusBadge status={complaint.status} size="sm" />
                          {complaint.requestId && (
                            <span className="complaint-request-tag">
                              Order #{complaint.requestId}
                            </span>
                          )}
                        </div>
                        <span className="complaint-date">
                          Filed {formatDate(complaint.createdAt)}
                        </span>
                      </div>

                      <div className="complaint-parties-grid">
                        <div className="party-box">
                          <span className="party-role">Raised By</span>
                          <span className="party-name">
                            {complaint.raisedByName || 'Student'} (ID: {complaint.raisedById})
                          </span>
                        </div>
                        <div className="party-box">
                          <span className="party-role">Against User</span>
                          <span className="party-name">
                            {complaint.againstUserId
                              ? `${complaint.againstUserName || 'User'} (ID: ${complaint.againstUserId})`
                              : 'None specified / General'}
                          </span>
                        </div>
                      </div>

                      <div className="complaint-description-box">
                        <span className="meta-label">Description</span>
                        <p className="complaint-description-text">{complaint.description}</p>
                      </div>

                      {complaint.adminNotes && (
                        <div className="complaint-notes-preview">
                          <span className="complaint-notes-badge">Admin Notes:</span>
                          <span className="complaint-notes-text">{complaint.adminNotes}</span>
                        </div>
                      )}

                      {complaint.resolvedAt && (
                        <div className="complaint-resolved-banner">
                          ✓ Resolved on {formatDate(complaint.resolvedAt)}
                        </div>
                      )}

                      <div className="complaint-card__actions">
                        <Button
                          variant="primary"
                          size="sm"
                          onClick={() => handleOpenComplaint(complaint)}
                        >
                          Review & Resolve Complaint
                        </Button>
                      </div>
                    </Card>
                  ))}
                </div>
              )}
            </div>
          )}

          {/* ============================================================ */}
          {/* TAB 2: REQUESTS REGISTRY                                      */}
          {/* ============================================================ */}
          {activeTab === 'requests' && (
            <div className="requests-tab">
              {/* Requests Toolbar */}
              <div className="admin-toolbar">
                <div className="admin-search-box">
                  <Input
                    name="requestSearch"
                    placeholder="Search by product, customer, or ID..."
                    value={requestSearch}
                    onChange={(e) => setRequestSearch(e.target.value)}
                  />
                </div>

                <div className="admin-filter-group">
                  <span className="admin-filter-label">Status:</span>
                  <select
                    className="ui-input ui-select ui-select--compact"
                    value={requestStatusFilter}
                    onChange={(e) => setRequestStatusFilter(e.target.value)}
                  >
                    <option value="ALL">All Statuses</option>
                    <option value="REQUESTED">Requested</option>
                    <option value="ACCEPTED">Accepted</option>
                    <option value="COLLECTED">Collected</option>
                    <option value="RETURNING">Returning</option>
                    <option value="READY_FOR_DELIVERY">Ready for Delivery</option>
                    <option value="DELIVERED">Delivered</option>
                    <option value="CANCELLED">Cancelled</option>
                  </select>
                </div>
              </div>

              {filteredRequests.length === 0 ? (
                <Card className="empty-state-card">
                  <div className="empty-state">
                    <span className="empty-state__icon">🔍</span>
                    <h3>No matching requests</h3>
                    <p>Try adjusting your search query or status filter.</p>
                  </div>
                </Card>
              ) : (
                <div className="admin-table-container">
                  <table className="admin-table">
                    <thead>
                      <tr>
                        <th>ID</th>
                        <th>Product</th>
                        <th>Category</th>
                        <th>Qty</th>
                        <th>Pickup Area</th>
                        <th>Budget</th>
                        <th>Customer</th>
                        <th>Matched Trip</th>
                        <th>Status</th>
                        <th>Created</th>
                        <th>Action</th>
                      </tr>
                    </thead>
                    <tbody>
                      {filteredRequests.map((req) => (
                        <tr key={req.id}>
                          <td className="table-cell-bold">#{req.id}</td>
                          <td className="table-cell-title">{req.productName}</td>
                          <td>
                            <span className="category-pill">{req.category || 'General'}</span>
                          </td>
                          <td>{req.quantity}</td>
                          <td>
                            <span className="area-pill">{req.pickupArea?.replace('_', ' ')}</span>
                          </td>
                          <td>{req.budget ? `৳${req.budget}` : '—'}</td>
                          <td>
                            <div className="user-table-cell">
                              <span>{req.customerName || 'Customer'}</span>
                              <span className="user-subid">ID: {req.customerId}</span>
                            </div>
                          </td>
                          <td>{req.matchedTripId ? `#${req.matchedTripId}` : '—'}</td>
                          <td>
                            <StatusBadge status={req.status} size="sm" />
                          </td>
                          <td className="table-cell-date">{formatDate(req.createdAt)}</td>
                          <td>
                            <button
                              type="button"
                              className="table-action-link"
                              onClick={() => setSelectedRequestDetails(req)}
                            >
                              Details
                            </button>
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              )}
            </div>
          )}

          {/* ============================================================ */}
          {/* TAB 3: USERS DIRECTORY                                        */}
          {/* ============================================================ */}
          {activeTab === 'users' && (
            <div className="users-tab">
              {/* Users Toolbar */}
              <div className="admin-toolbar">
                <div className="admin-search-box">
                  <Input
                    name="userSearch"
                    placeholder="Search by name, student ID, email, dept..."
                    value={userSearch}
                    onChange={(e) => setUserSearch(e.target.value)}
                  />
                </div>

                <div className="admin-filter-group">
                  <span className="admin-filter-label">Role:</span>
                  <select
                    className="ui-input ui-select ui-select--compact"
                    value={userRoleFilter}
                    onChange={(e) => setUserRoleFilter(e.target.value)}
                  >
                    <option value="ALL">All Roles</option>
                    <option value="ADMIN">Admins Only</option>
                    <option value="STUDENT">Students Only</option>
                  </select>
                </div>
              </div>

              {filteredUsers.length === 0 ? (
                <Card className="empty-state-card">
                  <div className="empty-state">
                    <span className="empty-state__icon">🔍</span>
                    <h3>No matching users found</h3>
                    <p>Try modifying your search or filter.</p>
                  </div>
                </Card>
              ) : (
                <div className="admin-table-container">
                  <table className="admin-table">
                    <thead>
                      <tr>
                        <th>ID</th>
                        <th>Student ID</th>
                        <th>Full Name</th>
                        <th>Email</th>
                        <th>Department</th>
                        <th>Phone</th>
                        <th>Role</th>
                        <th>Avg Rating</th>
                        <th>Joined</th>
                      </tr>
                    </thead>
                    <tbody>
                      {filteredUsers.map((u) => (
                        <tr key={u.id}>
                          <td className="table-cell-bold">#{u.id}</td>
                          <td>
                            <span className="student-id-badge">{u.studentId}</span>
                          </td>
                          <td className="table-cell-title">{u.fullName}</td>
                          <td className="table-cell-email">{u.email}</td>
                          <td>{u.department || '—'}</td>
                          <td>{u.phone || '—'}</td>
                          <td>
                            {u.isAdmin ? (
                              <span className="admin-role-badge">Admin</span>
                            ) : (
                              <span className="user-role-badge">Student</span>
                            )}
                          </td>
                          <td>
                            {u.avgRating != null ? (
                              <span className="rating-badge">★ {Number(u.avgRating).toFixed(2)}</span>
                            ) : (
                              <span className="text-muted">No ratings</span>
                            )}
                          </td>
                          <td className="table-cell-date">{formatDate(u.createdAt)}</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              )}
            </div>
          )}
        </div>
      )}

      {/* ============================================================ */}
      {/* MODAL 1: COMPLAINT MANAGEMENT MODAL                            */}
      {/* ============================================================ */}
      {selectedComplaint && (
        <div className="admin-modal-backdrop" onClick={handleCloseComplaintModal}>
          <div
            className="admin-modal-content"
            onClick={(e) => e.stopPropagation()}
            role="dialog"
            aria-modal="true"
            aria-labelledby="complaint-modal-title"
          >
            <div className="admin-modal-header">
              <div>
                <span className="complaint-id-tag">#CMP-{selectedComplaint.id}</span>
                <h2 id="complaint-modal-title" className="admin-modal-title">
                  Manage Dispute Complaint
                </h2>
              </div>
              <button
                type="button"
                className="admin-modal-close"
                onClick={handleCloseComplaintModal}
                aria-label="Close modal"
              >
                ✕
              </button>
            </div>

            <div className="admin-modal-body">
              {complaintActionError && (
                <div className="ui-alert ui-alert--error" role="alert">
                  {complaintActionError}
                </div>
              )}

              {complaintActionSuccess && (
                <div className="ui-alert ui-alert--success" role="alert">
                  {complaintActionSuccess}
                </div>
              )}

              {/* Status & Filing Metadata */}
              <div className="modal-meta-grid">
                <div>
                  <span className="meta-label">Current Status</span>
                  <div style={{ marginTop: '0.25rem' }}>
                    <StatusBadge status={selectedComplaint.status} size="md" />
                  </div>
                </div>
                <div>
                  <span className="meta-label">Date Filed</span>
                  <span className="meta-val">{formatDate(selectedComplaint.createdAt)}</span>
                </div>
                <div>
                  <span className="meta-label">Complainant</span>
                  <span className="meta-val">
                    {selectedComplaint.raisedByName || 'Student'} (ID: {selectedComplaint.raisedById})
                  </span>
                </div>
                <div>
                  <span className="meta-label">Against User</span>
                  <span className="meta-val">
                    {selectedComplaint.againstUserId
                      ? `${selectedComplaint.againstUserName || 'User'} (ID: ${selectedComplaint.againstUserId})`
                      : 'None specified'}
                  </span>
                </div>
              </div>

              {/* Full Description */}
              <div className="modal-field-group">
                <span className="meta-label">Complaint Description</span>
                <div className="complaint-description-panel">
                  {selectedComplaint.description}
                </div>
              </div>

              {/* Admin Notes Form */}
              <div className="modal-field-group">
                <label htmlFor="admin-notes-textarea" className="meta-label">
                  Administrative Notes & Action Log:
                </label>
                <textarea
                  id="admin-notes-textarea"
                  rows={4}
                  className="ui-input ui-textarea"
                  placeholder="Record investigations, partner warnings, refund details, or resolution rationale..."
                  value={adminNotesInput}
                  onChange={(e) => setAdminNotesInput(e.target.value)}
                />
              </div>

              {/* Action Buttons to set status */}
              <div className="modal-action-bar">
                <div className="modal-status-buttons">
                  <Button
                    type="button"
                    variant="outline"
                    isLoading={isUpdatingComplaint}
                    disabled={isUpdatingComplaint || selectedComplaint.status === 'IN_REVIEW'}
                    onClick={() => handleUpdateComplaintStatus('IN_REVIEW')}
                  >
                    Set Status: IN_REVIEW
                  </Button>

                  <Button
                    type="button"
                    variant="primary"
                    isLoading={isUpdatingComplaint}
                    disabled={isUpdatingComplaint || selectedComplaint.status === 'RESOLVED'}
                    onClick={() => handleUpdateComplaintStatus('RESOLVED')}
                  >
                    Set Status: RESOLVED
                  </Button>
                </div>

                <Button
                  type="button"
                  variant="outline"
                  size="sm"
                  onClick={handleCloseComplaintModal}
                >
                  Close
                </Button>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* ============================================================ */}
      {/* MODAL 2: REQUEST DETAILS MODAL                                 */}
      {/* ============================================================ */}
      {selectedRequestDetails && (
        <div
          className="admin-modal-backdrop"
          onClick={() => setSelectedRequestDetails(null)}
        >
          <div
            className="admin-modal-content"
            onClick={(e) => e.stopPropagation()}
            role="dialog"
            aria-modal="true"
            aria-labelledby="request-details-modal-title"
          >
            <div className="admin-modal-header">
              <div>
                <span className="complaint-id-tag">#REQ-{selectedRequestDetails.id}</span>
                <h2 id="request-details-modal-title" className="admin-modal-title">
                  {selectedRequestDetails.productName}
                </h2>
              </div>
              <button
                type="button"
                className="admin-modal-close"
                onClick={() => setSelectedRequestDetails(null)}
                aria-label="Close modal"
              >
                ✕
              </button>
            </div>

            <div className="admin-modal-body">
              <div className="modal-meta-grid">
                <div>
                  <span className="meta-label">Status</span>
                  <div style={{ marginTop: '0.25rem' }}>
                    <StatusBadge status={selectedRequestDetails.status} size="md" />
                  </div>
                </div>
                <div>
                  <span className="meta-label">Customer</span>
                  <span className="meta-val">
                    {selectedRequestDetails.customerName} (ID: {selectedRequestDetails.customerId})
                  </span>
                </div>
                <div>
                  <span className="meta-label">Pickup Zone</span>
                  <span className="meta-val">
                    {selectedRequestDetails.pickupArea?.replace('_', ' ')}
                  </span>
                </div>
                <div>
                  <span className="meta-label">Matched Trip ID</span>
                  <span className="meta-val">
                    {selectedRequestDetails.matchedTripId ? `#${selectedRequestDetails.matchedTripId}` : 'None'}
                  </span>
                </div>
                <div>
                  <span className="meta-label">Quantity</span>
                  <span className="meta-val">{selectedRequestDetails.quantity}</span>
                </div>
                <div>
                  <span className="meta-label">Budget</span>
                  <span className="meta-val">
                    {selectedRequestDetails.budget ? `৳${selectedRequestDetails.budget}` : 'None specified'}
                  </span>
                </div>
                <div>
                  <span className="meta-label">Preferred Shop</span>
                  <span className="meta-val">
                    {selectedRequestDetails.preferredShop || 'Any'}
                  </span>
                </div>
                <div>
                  <span className="meta-label">Optimistic Version</span>
                  <span className="meta-val">v{selectedRequestDetails.version}</span>
                </div>
              </div>

              {selectedRequestDetails.instructions && (
                <div className="modal-field-group">
                  <span className="meta-label">Customer Instructions</span>
                  <div className="complaint-description-panel">
                    {selectedRequestDetails.instructions}
                  </div>
                </div>
              )}

              <div className="form-actions">
                <Button
                  variant="outline"
                  onClick={() => setSelectedRequestDetails(null)}
                >
                  Close
                </Button>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
