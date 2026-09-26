import React, { useState, useEffect, useMemo } from 'react';
import { useAuth } from '../context/AuthContext';
import { adminApi } from '../services/api';
import Card from '../components/common/Card';
import Button from '../components/common/Button';
import Input from '../components/common/Input';
import StatusBadge from '../components/common/StatusBadge';

export default function AdminPage() {
  const { token, user } = useAuth();

  // Active Tab: 'complaints' | 'requests' | 'users' | 'payments'
  const [activeTab, setActiveTab] = useState('complaints');

  // Data states
  const [complaints, setComplaints] = useState([]);
  const [requests, setRequests] = useState([]);
  const [users, setUsers] = useState([]);
  const [payments, setPayments] = useState([]);
  const [products, setProducts] = useState([]);

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

  // Payments filter & verification modal state
  const [paymentStatusFilter, setPaymentStatusFilter] = useState('ALL');
  const [paymentAdjustmentFilter, setPaymentAdjustmentFilter] = useState('ALL');
  const [paymentSearch, setPaymentSearch] = useState('');
  const [selectedPayment, setSelectedPayment] = useState(null);
  const [paymentAdminNotes, setPaymentAdminNotes] = useState('');
  const [isVerifyingPayment, setIsVerifyingPayment] = useState(false);
  const [paymentActionError, setPaymentActionError] = useState(null);
  const [paymentActionSuccess, setPaymentActionSuccess] = useState(null);

  // Products filter & modal state
  const [productSearch, setProductSearch] = useState('');
  const [productCategoryFilter, setProductCategoryFilter] = useState('ALL');
  const [productActiveFilter, setProductActiveFilter] = useState('ALL');
  const [productModalOpen, setProductModalOpen] = useState(false);
  const [editingProduct, setEditingProduct] = useState(null);
  const [isSavingProduct, setIsSavingProduct] = useState(false);
  const [productModalError, setProductModalError] = useState(null);
  const [productModalSuccess, setProductModalSuccess] = useState(null);
  const [productForm, setProductForm] = useState({
    name: '',
    category: 'FOOD',
    description: '',
    imageUrl: '',
    estimatedPrice: '',
    weightClass: 'LIGHT',
    sizeClass: 'SMALL',
    isSensitive: false,
    isActive: true,
  });

  // Load all admin data
  const loadAdminData = async (silent = false) => {
    if (!silent) setIsLoading(true);
    else setIsRefreshing(true);
    setError(null);

    try {
      const [complaintsData, requestsData, usersData, paymentsData, productsData] = await Promise.all([
        adminApi.getComplaints(token),
        adminApi.getRequests('ALL', token),
        adminApi.getUsers(token),
        adminApi.getPayments(token),
        adminApi.getProducts(token),
      ]);

      setComplaints(complaintsData || []);
      setRequests(requestsData || []);
      setUsers(usersData || []);
      setPayments(paymentsData || []);
      setProducts(productsData || []);
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

  // Open payment modal
  const handleOpenPayment = (payment) => {
    setSelectedPayment(payment);
    setPaymentAdminNotes(payment.adminNotes || '');
    setPaymentActionError(null);
    setPaymentActionSuccess(null);
  };

  // Close payment modal
  const handleClosePaymentModal = () => {
    setSelectedPayment(null);
    setPaymentAdminNotes('');
    setPaymentActionError(null);
    setPaymentActionSuccess(null);
  };

  // Verify initial payment
  const handleVerifyInitialPayment = async (statusToSet = 'VERIFIED') => {
    if (!selectedPayment) return;
    setIsVerifyingPayment(true);
    setPaymentActionError(null);
    setPaymentActionSuccess(null);

    try {
      const updated = await adminApi.verifyPayment(
        selectedPayment.id,
        {
          status: statusToSet,
          adminNotes: paymentAdminNotes.trim() || null,
        },
        token
      );

      setPayments((prev) => prev.map((p) => (p.id === updated.id ? updated : p)));
      setSelectedPayment(updated);
      setPaymentActionSuccess(`Initial payment marked as ${statusToSet}!`);
    } catch (err) {
      setPaymentActionError(err.message || 'Failed to update payment status');
    } finally {
      setIsVerifyingPayment(false);
    }
  };

  // Verify additional payment
  const handleVerifyAdditionalPayment = async (statusToSet = 'VERIFIED') => {
    if (!selectedPayment) return;
    setIsVerifyingPayment(true);
    setPaymentActionError(null);
    setPaymentActionSuccess(null);

    try {
      const updated = await adminApi.verifyPayment(
        selectedPayment.id,
        {
          additionalPaymentStatus: statusToSet,
          adminNotes: paymentAdminNotes.trim() || null,
        },
        token
      );

      setPayments((prev) => prev.map((p) => (p.id === updated.id ? updated : p)));
      setSelectedPayment(updated);
      setPaymentActionSuccess(`Price adjustment payment marked as ${statusToSet}!`);
    } catch (err) {
      setPaymentActionError(err.message || 'Failed to update additional payment status');
    } finally {
      setIsVerifyingPayment(false);
    }
  };

  // Open modal to create a new product
  const handleOpenCreateProduct = () => {
    setEditingProduct(null);
    setProductForm({
      name: '',
      category: 'FOOD',
      description: '',
      imageUrl: '',
      estimatedPrice: '',
      weightClass: 'LIGHT',
      sizeClass: 'SMALL',
      isSensitive: false,
      isActive: true,
    });
    setProductModalError(null);
    setProductModalSuccess(null);
    setProductModalOpen(true);
  };

  // Open modal to edit an existing product
  const handleOpenEditProduct = (product) => {
    setEditingProduct(product);
    setProductForm({
      name: product.name || '',
      category: product.category || 'FOOD',
      description: product.description || '',
      imageUrl: product.imageUrl || '',
      estimatedPrice: product.estimatedPrice != null ? String(product.estimatedPrice) : '',
      weightClass: product.weightClass || 'LIGHT',
      sizeClass: product.sizeClass || 'SMALL',
      isSensitive: Boolean(product.isSensitive),
      isActive: Boolean(product.isActive),
    });
    setProductModalError(null);
    setProductModalSuccess(null);
    setProductModalOpen(true);
  };

  // Save product (create or update)
  const handleSaveProduct = async (e) => {
    e.preventDefault();
    if (!productForm.name.trim()) {
      setProductModalError('Product name is required');
      return;
    }
    const priceNum = parseFloat(productForm.estimatedPrice);
    if (isNaN(priceNum) || priceNum < 0) {
      setProductModalError('Please enter a valid price (greater than or equal to 0)');
      return;
    }

    setIsSavingProduct(true);
    setProductModalError(null);
    setProductModalSuccess(null);

    const payload = {
      name: productForm.name.trim(),
      category: productForm.category,
      description: productForm.description.trim() || null,
      imageUrl: productForm.imageUrl.trim() || null,
      estimatedPrice: priceNum,
      weightClass: productForm.weightClass,
      sizeClass: productForm.sizeClass,
      isSensitive: productForm.isSensitive,
      isActive: productForm.isActive,
    };

    try {
      if (editingProduct) {
        const updated = await adminApi.updateProduct(editingProduct.id, payload, token);
        setProducts((prev) => prev.map((p) => (p.id === updated.id ? updated : p)));
        setProductModalSuccess(`Product "${updated.name}" updated successfully!`);
      } else {
        const created = await adminApi.createProduct(payload, token);
        setProducts((prev) => [created, ...prev]);
        setProductModalSuccess(`Product "${created.name}" created successfully!`);
      }
      setTimeout(() => {
        setProductModalOpen(false);
      }, 1000);
    } catch (err) {
      console.error('Failed to save product:', err);
      setProductModalError(err.message || 'Failed to save product');
    } finally {
      setIsSavingProduct(false);
    }
  };

  // Toggle active state
  const handleToggleProductActive = async (product) => {
    try {
      const updated = await adminApi.toggleProductStatus(product.id, !product.isActive, token);
      setProducts((prev) => prev.map((p) => (p.id === updated.id ? updated : p)));
    } catch (err) {
      console.error('Failed to toggle product status:', err);
      alert(err.message || 'Failed to change product status');
    }
  };

  // Filtered Products
  const filteredProducts = useMemo(() => {
    return products.filter((p) => {
      const matchesCategory =
        productCategoryFilter === 'ALL' || p.category === productCategoryFilter;

      const matchesActive =
        productActiveFilter === 'ALL' ||
        (productActiveFilter === 'ACTIVE_ONLY' && p.isActive) ||
        (productActiveFilter === 'INACTIVE_ONLY' && !p.isActive);

      const q = productSearch.toLowerCase().trim();
      const matchesSearch =
        !q ||
        p.name?.toLowerCase().includes(q) ||
        p.description?.toLowerCase().includes(q) ||
        String(p.id).includes(q);

      return matchesCategory && matchesActive && matchesSearch;
    });
  }, [products, productCategoryFilter, productActiveFilter, productSearch]);

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

  // Filtered Payments
  const filteredPayments = useMemo(() => {
    return payments.filter((p) => {
      const matchesStatus =
        paymentStatusFilter === 'ALL' || p.status === paymentStatusFilter;

      const matchesAdjustment =
        paymentAdjustmentFilter === 'ALL' ||
        (paymentAdjustmentFilter === 'HAS_ADJUSTMENT' && p.additionalPaymentStatus !== 'NONE') ||
        (paymentAdjustmentFilter === 'PENDING_SUBMISSION' && p.additionalPaymentStatus === 'REQUESTED') ||
        (paymentAdjustmentFilter === 'SUBMITTED' && p.additionalPaymentStatus === 'SUBMITTED') ||
        (paymentAdjustmentFilter === 'VERIFIED' && p.additionalPaymentStatus === 'VERIFIED');

      const q = paymentSearch.toLowerCase().trim();
      const matchesSearch =
        !q ||
        p.trxId?.toLowerCase().includes(q) ||
        p.additionalTrxId?.toLowerCase().includes(q) ||
        p.senderPhone?.toLowerCase().includes(q) ||
        p.customerName?.toLowerCase().includes(q) ||
        p.customerEmail?.toLowerCase().includes(q) ||
        p.productName?.toLowerCase().includes(q) ||
        String(p.requestId).includes(q) ||
        String(p.id).includes(q);

      return matchesStatus && matchesAdjustment && matchesSearch;
    });
  }, [payments, paymentStatusFilter, paymentAdjustmentFilter, paymentSearch]);

  // Counts for alert badges
  const openComplaintsCount = complaints.filter(
    (c) => c.status === 'OPEN' || c.status === 'IN_REVIEW'
  ).length;

  const unverifiedPaymentsCount = payments.filter(
    (p) => p.status === 'SUBMITTED' || p.additionalPaymentStatus === 'SUBMITTED'
  ).length;

  const formatDate = (dateStr) => {
    if (!dateStr) return 'N/A';
    try {
      return new Date(dateStr).toLocaleString();
    } catch {
      return dateStr;
    }
  };

  return (
    <div className="admin-page">
      {/* Top Banner & Header */}
      <div className="admin-header-card">
        <div className="admin-header-main">
          <div>
            <div className="admin-badge-row">
              <span className="admin-security-badge">KUET Admin Panel</span>
              <span className="admin-user-tag">{user?.fullName || 'Administrator'}</span>
            </div>
            <h1 className="admin-title">Platform Operations Center</h1>
            <p className="admin-subtitle">
              Monitor orders, manage dispute complaints, verify MFS payments, and review campus users
            </p>
          </div>
          <div className="admin-header-actions">
            <Button
              variant="outline"
              size="sm"
              onClick={() => loadAdminData(true)}
              isLoading={isRefreshing}
            >
              ↻ Refresh Data
            </Button>
          </div>
        </div>

        {/* Global Summary Stats */}
        <div className="admin-stats-strip">
          <div className="admin-stat-item">
            <span className="admin-stat-label">Total Users</span>
            <span className="admin-stat-value">{users.length}</span>
          </div>
          <div className="admin-stat-item">
            <span className="admin-stat-label">Total Orders</span>
            <span className="admin-stat-value">{requests.length}</span>
          </div>
          <div className="admin-stat-item">
            <span className="admin-stat-label">Total Payments</span>
            <span className="admin-stat-value">{payments.length}</span>
          </div>
          <div className="admin-stat-item">
            <span className="admin-stat-label">Unverified Payments</span>
            <span className={`admin-stat-value ${unverifiedPaymentsCount > 0 ? 'text-danger' : ''}`}>
              {unverifiedPaymentsCount}
            </span>
          </div>
          <div className="admin-stat-item">
            <span className="admin-stat-label">Catalog Products</span>
            <span className="admin-stat-value">{products.length}</span>
          </div>
          <div className="admin-stat-item">
            <span className="admin-stat-label">Open Complaints</span>
            <span className={`admin-stat-value ${openComplaintsCount > 0 ? 'text-danger' : ''}`}>
              {openComplaintsCount}
            </span>
          </div>
        </div>
      </div>

      {error && (
        <div className="ui-alert ui-alert--error mt-3" role="alert">
          <strong>Error loading data: </strong> {error}
        </div>
      )}

      {/* Navigation Tabs */}
      <div className="admin-tabs" role="tablist">
        <button
          type="button"
          role="tab"
          aria-selected={activeTab === 'products'}
          className={`admin-tab ${activeTab === 'products' ? 'admin-tab--active' : ''}`}
          onClick={() => setActiveTab('products')}
        >
          <span>📦 Catalog Products</span>
          <span className="admin-tab-count">{products.length}</span>
        </button>

        <button
          type="button"
          role="tab"
          aria-selected={activeTab === 'payments'}
          className={`admin-tab ${activeTab === 'payments' ? 'admin-tab--active' : ''}`}
          onClick={() => setActiveTab('payments')}
        >
          <span>💳 Payments & MFS</span>
          {unverifiedPaymentsCount > 0 ? (
            <span className="admin-tab-count admin-tab-count--alert">
              {unverifiedPaymentsCount}
            </span>
          ) : (
            <span className="admin-tab-count">{payments.length}</span>
          )}
        </button>

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
          {/* TAB 1: PAYMENTS & MFS VERIFICATION                            */}
          {/* ============================================================ */}
          {activeTab === 'payments' && (
            <div className="payments-tab">
              {/* Payments Filter Toolbar */}
              <div className="admin-toolbar" style={{ display: 'flex', flexWrap: 'wrap', gap: '1rem', alignItems: 'center' }}>
                <div className="admin-search-box" style={{ flex: '1 1 300px' }}>
                  <Input
                    name="paymentSearch"
                    placeholder="Search by TrxID, Customer, Partner, Phone, Request ID..."
                    value={paymentSearch}
                    onChange={(e) => setPaymentSearch(e.target.value)}
                  />
                </div>

                <div className="admin-filter-group">
                  <span className="admin-filter-label">Initial Status:</span>
                  <select
                    className="ui-input ui-select ui-select--compact"
                    value={paymentStatusFilter}
                    onChange={(e) => setPaymentStatusFilter(e.target.value)}
                  >
                    <option value="ALL">All Statuses</option>
                    <option value="SUBMITTED">SUBMITTED (Awaiting Verification)</option>
                    <option value="VERIFIED">VERIFIED</option>
                    <option value="PENDING">PENDING</option>
                    <option value="SIMULATED_PAID">SIMULATED PAID</option>
                  </select>
                </div>

                <div className="admin-filter-group">
                  <span className="admin-filter-label">Price Adjustment:</span>
                  <select
                    className="ui-input ui-select ui-select--compact"
                    value={paymentAdjustmentFilter}
                    onChange={(e) => setPaymentAdjustmentFilter(e.target.value)}
                  >
                    <option value="ALL">All Orders</option>
                    <option value="HAS_ADJUSTMENT">Has Price Adjustment</option>
                    <option value="PENDING_SUBMISSION">Adjustment Requested (Unpaid)</option>
                    <option value="SUBMITTED">Adjustment Submitted (Awaiting Verification)</option>
                    <option value="VERIFIED">Adjustment Verified</option>
                  </select>
                </div>
              </div>

              {filteredPayments.length === 0 ? (
                <Card className="empty-state-card">
                  <div className="empty-state">
                    <span className="empty-state__icon">💳</span>
                    <h3>No payments found</h3>
                    <p>Try modifying your search or filter options.</p>
                  </div>
                </Card>
              ) : (
                <div className="admin-table-container">
                  <table className="admin-table">
                    <thead>
                      <tr>
                        <th>Req #</th>
                        <th>Product & Customer</th>
                        <th>Method</th>
                        <th>Initial Total</th>
                        <th>TrxID</th>
                        <th>Initial Status</th>
                        <th>Need More Extra</th>
                        <th>Adjustment Status</th>
                        <th>Action</th>
                      </tr>
                    </thead>
                    <tbody>
                      {filteredPayments.map((p) => {
                        const hasAdjustment = p.additionalPaymentStatus !== 'NONE';
                        return (
                          <tr key={p.id}>
                            <td className="table-cell-bold">#{p.requestId || p.id}</td>
                            <td>
                              <div style={{ fontWeight: 600 }}>{p.productName || 'Order'}</div>
                              <div style={{ fontSize: '0.8rem', color: 'var(--color-text-muted)' }}>
                                {p.customerName} ({p.customerEmail})
                              </div>
                            </td>
                            <td>
                              <span style={{
                                padding: '0.2rem 0.5rem',
                                borderRadius: '4px',
                                fontSize: '0.8rem',
                                fontWeight: 700,
                                background: p.paymentMethod === 'BKASH' ? '#fdf2f8' : p.paymentMethod === 'NAGAD' ? '#fff7ed' : '#f1f5f9',
                                color: p.paymentMethod === 'BKASH' ? '#be185d' : p.paymentMethod === 'NAGAD' ? '#c2410c' : '#475569',
                                border: p.paymentMethod === 'BKASH' ? '1px solid #fbcfe8' : p.paymentMethod === 'NAGAD' ? '1px solid #fed7aa' : '1px solid #cbd5e1'
                              }}>
                                {p.paymentMethod || 'DIRECT'}
                              </span>
                            </td>
                            <td>
                              <div style={{ fontWeight: 700 }}>৳{parseFloat(p.total).toFixed(2)}</div>
                              <div style={{ fontSize: '0.75rem', color: 'var(--color-text-muted)' }}>
                                Item: ৳{parseFloat(p.productCost).toFixed(2)} + Fee: ৳{parseFloat(p.gatewayFee || 0).toFixed(2)} + Del: ৳{parseFloat(p.deliveryFee).toFixed(2)}
                              </div>
                            </td>
                            <td>
                              {p.trxId ? (
                                <code style={{ fontSize: '0.85rem' }}>{p.trxId}</code>
                              ) : (
                                <span className="text-muted">—</span>
                              )}
                              {p.senderPhone && (
                                <div style={{ fontSize: '0.75rem', color: 'var(--color-text-muted)' }}>
                                  Ph: {p.senderPhone}
                                </div>
                              )}
                            </td>
                            <td>
                              <span style={{
                                padding: '0.2rem 0.5rem',
                                borderRadius: '4px',
                                fontSize: '0.75rem',
                                fontWeight: 700,
                                background: p.status === 'VERIFIED' ? '#dcfce7' : p.status === 'SUBMITTED' ? '#dbeafe' : '#f1f5f9',
                                color: p.status === 'VERIFIED' ? '#15803d' : p.status === 'SUBMITTED' ? '#1d4ed8' : '#475569'
                              }}>
                                {p.status}
                              </span>
                            </td>
                            <td>
                              {hasAdjustment ? (
                                <div>
                                  <div style={{ fontWeight: 600, color: '#b45309' }}>
                                    +৳{parseFloat(p.additionalTotal).toFixed(2)}
                                  </div>
                                  <div style={{ fontSize: '0.75rem', color: 'var(--color-text-muted)' }}>
                                    (৳{parseFloat(p.additionalAmount).toFixed(2)} + fee)
                                  </div>
                                </div>
                              ) : (
                                <span className="text-muted">None</span>
                              )}
                            </td>
                            <td>
                              {hasAdjustment ? (
                                <span style={{
                                  padding: '0.2rem 0.5rem',
                                  borderRadius: '4px',
                                  fontSize: '0.75rem',
                                  fontWeight: 700,
                                  background: p.additionalPaymentStatus === 'VERIFIED' ? '#dcfce7' : p.additionalPaymentStatus === 'SUBMITTED' ? '#dbeafe' : '#fef3c7',
                                  color: p.additionalPaymentStatus === 'VERIFIED' ? '#15803d' : p.additionalPaymentStatus === 'SUBMITTED' ? '#1d4ed8' : '#b45309'
                                }}>
                                  {p.additionalPaymentStatus}
                                </span>
                              ) : (
                                <span className="text-muted">—</span>
                              )}
                            </td>
                            <td>
                              <Button
                                variant="outline"
                                size="sm"
                                onClick={() => handleOpenPayment(p)}
                              >
                                Review & Verify
                              </Button>
                            </td>
                          </tr>
                        );
                      })}
                    </tbody>
                  </table>
                </div>
              )}
            </div>
          )}

          {/* ============================================================ */}
          {/* TAB 2: COMPLAINTS QUEUE                                       */}
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
                          Manage Dispute
                        </Button>
                      </div>
                    </Card>
                  ))}
                </div>
              )}
            </div>
          )}

          {/* ============================================================ */}
          {/* TAB 3: REQUESTS REGISTRY                                      */}
          {/* ============================================================ */}
          {activeTab === 'requests' && (
            <div className="requests-tab">
              {/* Requests Toolbar */}
              <div className="admin-toolbar">
                <div className="admin-search-box">
                  <Input
                    name="requestSearch"
                    placeholder="Search by product, customer name, pickup zone, ID..."
                    value={requestSearch}
                    onChange={(e) => setRequestSearch(e.target.value)}
                  />
                </div>

                <div className="admin-filter-group">
                  <span className="admin-filter-label">Filter Status:</span>
                  <select
                    className="ui-input ui-select ui-select--compact"
                    value={requestStatusFilter}
                    onChange={(e) => setRequestStatusFilter(e.target.value)}
                  >
                    <option value="ALL">All Statuses</option>
                    <option value="REQUESTED">REQUESTED</option>
                    <option value="ACCEPTED">ACCEPTED</option>
                    <option value="COLLECTED">COLLECTED</option>
                    <option value="RETURNING">RETURNING</option>
                    <option value="READY_FOR_DELIVERY">READY FOR DELIVERY</option>
                    <option value="DELIVERED">DELIVERED</option>
                    <option value="CANCELLED">CANCELLED</option>
                  </select>
                </div>
              </div>

              {filteredRequests.length === 0 ? (
                <Card className="empty-state-card">
                  <div className="empty-state">
                    <span className="empty-state__icon">🔍</span>
                    <h3>No matching requests found</h3>
                    <p>Try modifying your search or filter.</p>
                  </div>
                </Card>
              ) : (
                <div className="admin-table-container">
                  <table className="admin-table">
                    <thead>
                      <tr>
                        <th>ID</th>
                        <th>Product Name</th>
                        <th>Customer</th>
                        <th>Pickup Zone</th>
                        <th>Budget</th>
                        <th>Payment Total</th>
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
                            {req.customerName}
                            <span className="table-cell-sub"> (ID: {req.customerId})</span>
                          </td>
                          <td>{req.pickupArea?.replace(/_/g, ' ')}</td>
                          <td>
                            {req.budget != null ? `৳${parseFloat(req.budget).toFixed(2)}` : '—'}
                          </td>
                          <td>
                            {req.payment ? (
                              <span style={{ fontWeight: 600 }}>৳{parseFloat(req.payment.total).toFixed(2)}</span>
                            ) : (
                              '—'
                            )}
                          </td>
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
          {/* TAB 4: USERS DIRECTORY                                        */}
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

          {/* ============================================================ */}
          {/* TAB 5: PRODUCTS CATALOG MANAGEMENT                           */}
          {/* ============================================================ */}
          {activeTab === 'products' && (
            <div className="products-tab">
              {/* Products Toolbar */}
              <div className="admin-toolbar" style={{ display: 'flex', flexWrap: 'wrap', gap: '1rem', alignItems: 'center', justifyContent: 'space-between' }}>
                <div style={{ display: 'flex', flexWrap: 'wrap', gap: '1rem', flex: '1 1 500px', alignItems: 'center' }}>
                  <div className="admin-search-box" style={{ flex: '1 1 250px' }}>
                    <Input
                      name="productSearch"
                      placeholder="Search products by name, description, ID..."
                      value={productSearch}
                      onChange={(e) => setProductSearch(e.target.value)}
                    />
                  </div>

                  <div className="admin-filter-group">
                    <span className="admin-filter-label">Category:</span>
                    <select
                      className="ui-input ui-select ui-select--compact"
                      value={productCategoryFilter}
                      onChange={(e) => setProductCategoryFilter(e.target.value)}
                    >
                      <option value="ALL">All Categories</option>
                      <option value="FOOD">Food</option>
                      <option value="ELECTRONICS">Electronics</option>
                      <option value="OTHERS">Others</option>
                    </select>
                  </div>

                  <div className="admin-filter-group">
                    <span className="admin-filter-label">Visibility:</span>
                    <select
                      className="ui-input ui-select ui-select--compact"
                      value={productActiveFilter}
                      onChange={(e) => setProductActiveFilter(e.target.value)}
                    >
                      <option value="ALL">All Items</option>
                      <option value="ACTIVE_ONLY">Active Only (Visible in Shop)</option>
                      <option value="INACTIVE_ONLY">Inactive Only (Hidden)</option>
                    </select>
                  </div>
                </div>

                <div>
                  <Button variant="primary" onClick={handleOpenCreateProduct}>
                    + Add New Product
                  </Button>
                </div>
              </div>

              {filteredProducts.length === 0 ? (
                <Card className="empty-state-card">
                  <div className="empty-state">
                    <span className="empty-state__icon">📦</span>
                    <h3>No products found</h3>
                    <p>Try modifying your search or filter, or create a new product above.</p>
                  </div>
                </Card>
              ) : (
                <div className="admin-table-container">
                  <table className="admin-table">
                    <thead>
                      <tr>
                        <th>ID</th>
                        <th>Product & Details</th>
                        <th>Category</th>
                        <th>Price</th>
                        <th>Weight & Size</th>
                        <th>Handling</th>
                        <th>Status</th>
                        <th>Actions</th>
                      </tr>
                    </thead>
                    <tbody>
                      {filteredProducts.map((p) => (
                        <tr key={p.id} style={{ opacity: p.isActive ? 1 : 0.65 }}>
                          <td className="table-cell-bold">#{p.id}</td>
                          <td>
                            <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
                              <img
                                src={p.imageUrl || 'https://picsum.photos/seed/carry/60/60'}
                                alt={p.name}
                                style={{ width: '44px', height: '44px', objectFit: 'cover', borderRadius: '4px', border: '1px solid var(--color-border)' }}
                                onError={(e) => {
                                  e.target.onerror = null;
                                  e.target.src = 'https://picsum.photos/seed/placeholder/60/60';
                                }}
                              />
                              <div>
                                <div style={{ fontWeight: 600 }}>{p.name}</div>
                                {p.description && (
                                  <div style={{ fontSize: '0.75rem', color: 'var(--color-text-muted)', maxWidth: '280px', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                                    {p.description}
                                  </div>
                                )}
                              </div>
                            </div>
                          </td>
                          <td>
                            <span className="cart-badge cart-badge--category">{p.category}</span>
                          </td>
                          <td style={{ fontWeight: 700 }}>৳{parseFloat(p.estimatedPrice).toFixed(2)}</td>
                          <td>
                            <div style={{ fontSize: '0.8rem' }}>
                              <span>Weight: <strong>{p.weightClass}</strong></span><br />
                              <span>Size: <strong>{p.sizeClass}</strong></span>
                            </div>
                          </td>
                          <td>
                            {p.isSensitive ? (
                              <span style={{ fontSize: '0.75rem', padding: '0.2rem 0.5rem', background: '#fef3c7', color: '#b45309', borderRadius: '4px', fontWeight: 600 }}>
                                ⚠️ Fragile (+৳20)
                              </span>
                            ) : (
                              <span style={{ fontSize: '0.75rem', color: 'var(--color-text-muted)' }}>Standard</span>
                            )}
                          </td>
                          <td>
                            <span style={{
                              padding: '0.25rem 0.5rem',
                              borderRadius: '4px',
                              fontSize: '0.75rem',
                              fontWeight: 700,
                              background: p.isActive ? '#dcfce7' : '#fee2e2',
                              color: p.isActive ? '#15803d' : '#b91c1c'
                            }}>
                              {p.isActive ? 'Active' : 'Inactive'}
                            </span>
                          </td>
                          <td>
                            <div style={{ display: 'flex', gap: '0.5rem' }}>
                              <Button
                                variant="outline"
                                size="sm"
                                onClick={() => handleOpenEditProduct(p)}
                              >
                                Edit
                              </Button>
                              <Button
                                variant={p.isActive ? 'outline' : 'secondary'}
                                size="sm"
                                onClick={() => handleToggleProductActive(p)}
                                style={p.isActive ? { borderColor: '#fca5a5', color: '#b91c1c' } : {}}
                              >
                                {p.isActive ? 'Deactivate' : 'Activate'}
                              </Button>
                            </div>
                          </td>
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
      {/* MODAL 1: PAYMENT REVIEW & VERIFICATION MODAL                  */}
      {/* ============================================================ */}
      {selectedPayment && (
        <div className="admin-modal-backdrop" onClick={handleClosePaymentModal}>
          <div
            className="admin-modal-content"
            onClick={(e) => e.stopPropagation()}
            role="dialog"
            aria-modal="true"
            aria-labelledby="payment-modal-title"
            style={{ maxWidth: '640px' }}
          >
            <div className="admin-modal-header">
              <div>
                <span className="complaint-id-tag">#PAY-{selectedPayment.id} (Order #{selectedPayment.requestId})</span>
                <h2 id="payment-modal-title" className="admin-modal-title">
                  MFS Payment Verification
                </h2>
              </div>
              <button
                type="button"
                className="admin-modal-close"
                onClick={handleClosePaymentModal}
                aria-label="Close modal"
              >
                ✕
              </button>
            </div>

            <div className="admin-modal-body">
              {paymentActionError && (
                <div className="ui-alert ui-alert--error" role="alert">
                  {paymentActionError}
                </div>
              )}

              {paymentActionSuccess && (
                <div className="ui-alert ui-alert--success" role="alert">
                  {paymentActionSuccess}
                </div>
              )}

              {/* Order & Parties Info */}
              <div className="modal-meta-grid">
                <div>
                  <span className="meta-label">Product Name</span>
                  <span className="meta-val" style={{ fontWeight: 600 }}>{selectedPayment.productName || 'Order'}</span>
                </div>
                <div>
                  <span className="meta-label">Customer</span>
                  <span className="meta-val">{selectedPayment.customerName} ({selectedPayment.customerEmail})</span>
                </div>
                <div>
                  <span className="meta-label">Assigned Partner</span>
                  <span className="meta-val">{selectedPayment.partnerName || 'Not yet assigned'}</span>
                </div>
                <div>
                  <span className="meta-label">Created At</span>
                  <span className="meta-val">{formatDate(selectedPayment.createdAt)}</span>
                </div>
              </div>

              {/* Initial Payment Box */}
              <div style={{
                marginTop: '1rem',
                padding: '1rem',
                background: '#f8fafc',
                border: '1px solid var(--color-border)',
                borderRadius: 'var(--radius-sm)'
              }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '0.5rem' }}>
                  <h4 style={{ fontWeight: 600 }}>1. Upfront Payment Details</h4>
                  <span style={{
                    padding: '0.2rem 0.5rem',
                    borderRadius: '4px',
                    fontSize: '0.75rem',
                    fontWeight: 700,
                    background: selectedPayment.status === 'VERIFIED' ? '#dcfce7' : '#dbeafe',
                    color: selectedPayment.status === 'VERIFIED' ? '#15803d' : '#1d4ed8'
                  }}>
                    {selectedPayment.status}
                  </span>
                </div>
                <div style={{ fontSize: '0.875rem', display: 'grid', gridTemplateColumns: 'repeat(2, 1fr)', gap: '0.5rem' }}>
                  <div><strong>Method:</strong> {selectedPayment.paymentMethod || 'Direct'}</div>
                  <div><strong>Sender Phone:</strong> {selectedPayment.senderPhone || '—'}</div>
                  <div><strong>TrxID:</strong> <code>{selectedPayment.trxId || '—'}</code></div>
                  <div><strong>Product Cost:</strong> ৳{parseFloat(selectedPayment.productCost).toFixed(2)}</div>
                  <div><strong>Delivery Fee:</strong> ৳{parseFloat(selectedPayment.deliveryFee).toFixed(2)}</div>
                  <div><strong>MFS Fee (1.39%):</strong> ৳{parseFloat(selectedPayment.gatewayFee || 0).toFixed(2)}</div>
                  <div style={{ gridColumn: 'span 2', fontSize: '1rem', fontWeight: 700, color: 'var(--color-primary)' }}>
                    Total Billed: ৳{parseFloat(selectedPayment.total).toFixed(2)}
                  </div>
                </div>

                <div style={{ marginTop: '0.75rem', display: 'flex', gap: '0.5rem' }}>
                  {selectedPayment.status !== 'VERIFIED' && (
                    <Button
                      variant="primary"
                      size="sm"
                      isLoading={isVerifyingPayment}
                      onClick={() => handleVerifyInitialPayment('VERIFIED')}
                    >
                      ✓ Mark Initial Payment as VERIFIED
                    </Button>
                  )}
                  {selectedPayment.status === 'VERIFIED' && (
                    <span style={{ fontSize: '0.85rem', color: '#15803d', fontWeight: 600 }}>
                      ✓ Initial payment is verified
                    </span>
                  )}
                </div>
              </div>

              {/* Price Adjustment Box */}
              {selectedPayment.additionalPaymentStatus !== 'NONE' && (
                <div style={{
                  marginTop: '1rem',
                  padding: '1rem',
                  background: '#fffbeb',
                  border: '1px solid #fde68a',
                  borderRadius: 'var(--radius-sm)'
                }}>
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '0.5rem' }}>
                    <h4 style={{ fontWeight: 600, color: '#92400e' }}>2. Price Adjustment (Need More)</h4>
                    <span style={{
                      padding: '0.2rem 0.5rem',
                      borderRadius: '4px',
                      fontSize: '0.75rem',
                      fontWeight: 700,
                      background: selectedPayment.additionalPaymentStatus === 'VERIFIED' ? '#dcfce7' : '#fef3c7',
                      color: selectedPayment.additionalPaymentStatus === 'VERIFIED' ? '#15803d' : '#b45309'
                    }}>
                      {selectedPayment.additionalPaymentStatus}
                    </span>
                  </div>
                  <div style={{ fontSize: '0.875rem', display: 'grid', gridTemplateColumns: 'repeat(2, 1fr)', gap: '0.5rem' }}>
                    <div><strong>Extra Product Cost:</strong> ৳{parseFloat(selectedPayment.additionalAmount).toFixed(2)}</div>
                    <div><strong>Extra MFS Fee:</strong> ৳{parseFloat(selectedPayment.additionalFee).toFixed(2)}</div>
                    <div style={{ gridColumn: 'span 2', fontWeight: 700, color: '#b45309' }}>
                      Total Extra Billed: ৳{parseFloat(selectedPayment.additionalTotal).toFixed(2)}
                    </div>
                    <div style={{ gridColumn: 'span 2' }}>
                      <strong>Reason:</strong> "{selectedPayment.needMoreReason}"
                    </div>
                    <div><strong>Method:</strong> {selectedPayment.additionalPaymentMethod || '—'}</div>
                    <div><strong>Sender Phone:</strong> {selectedPayment.additionalSenderPhone || '—'}</div>
                    <div style={{ gridColumn: 'span 2' }}>
                      <strong>Additional TrxID:</strong> <code>{selectedPayment.additionalTrxId || '—'}</code>
                    </div>
                  </div>

                  <div style={{ marginTop: '0.75rem', display: 'flex', gap: '0.5rem' }}>
                    {selectedPayment.additionalPaymentStatus !== 'VERIFIED' && (
                      <Button
                        variant="primary"
                        size="sm"
                        isLoading={isVerifyingPayment}
                        onClick={() => handleVerifyAdditionalPayment('VERIFIED')}
                      >
                        ✓ Mark Price Adjustment as VERIFIED
                      </Button>
                    )}
                    {selectedPayment.additionalPaymentStatus === 'VERIFIED' && (
                      <span style={{ fontSize: '0.85rem', color: '#15803d', fontWeight: 600 }}>
                        ✓ Extra payment is verified
                      </span>
                    )}
                  </div>
                </div>
              )}

              {/* Admin Notes */}
              <div style={{ marginTop: '1rem' }}>
                <label style={{ display: 'block', fontWeight: 600, fontSize: '0.875rem', marginBottom: '0.25rem' }}>
                  Admin Notes / Verification Log
                </label>
                <textarea
                  className="ui-input ui-textarea"
                  rows="2"
                  value={paymentAdminNotes}
                  onChange={(e) => setPaymentAdminNotes(e.target.value)}
                  placeholder="Notes about statement verification or transaction..."
                />
              </div>

              <div className="form-actions" style={{ marginTop: '1rem' }}>
                <Button variant="outline" size="sm" onClick={handleClosePaymentModal}>
                  Close
                </Button>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* ============================================================ */}
      {/* MODAL 2: COMPLAINT MANAGEMENT MODAL                            */}
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
                  <span className="meta-label">Associated Order</span>
                  <span className="meta-val">
                    {selectedComplaint.requestId ? `#${selectedComplaint.requestId}` : 'None'}
                  </span>
                </div>
                <div>
                  <span className="meta-label">Raised By</span>
                  <span className="meta-val">
                    {selectedComplaint.raisedByName || 'Student'} (ID: {selectedComplaint.raisedById})
                  </span>
                </div>
                <div>
                  <span className="meta-label">Against User</span>
                  <span className="meta-val">
                    {selectedComplaint.againstUserId
                      ? `${selectedComplaint.againstUserName || 'User'} (ID: ${selectedComplaint.againstUserId})`
                      : 'None specified / General'}
                  </span>
                </div>
                <div>
                  <span className="meta-label">Filed On</span>
                  <span className="meta-val">{formatDate(selectedComplaint.createdAt)}</span>
                </div>
                <div>
                  <span className="meta-label">Resolved On</span>
                  <span className="meta-val">{formatDate(selectedComplaint.resolvedAt)}</span>
                </div>
              </div>

              {/* Complaint Description */}
              <div className="modal-field-group">
                <span className="meta-label">Student Complaint Description</span>
                <div className="complaint-description-panel">
                  {selectedComplaint.description}
                </div>
              </div>

              {/* Admin Resolution Notes */}
              <div className="modal-field-group">
                <label htmlFor="admin-notes-textarea" className="meta-label">
                  Administrative Resolution Notes
                </label>
                <textarea
                  id="admin-notes-textarea"
                  rows="3"
                  className="ui-input ui-textarea"
                  placeholder="Record investigation findings, actions taken, or resolution summary..."
                  value={adminNotesInput}
                  onChange={(e) => setAdminNotesInput(e.target.value)}
                />
              </div>

              {/* Status Transition Actions */}
              <div className="modal-action-section">
                <span className="meta-label">Update Status & Save:</span>
                <div className="modal-btn-row">
                  {selectedComplaint.status !== 'IN_REVIEW' && (
                    <Button
                      variant="outline"
                      size="sm"
                      isLoading={isUpdatingComplaint}
                      onClick={() => handleUpdateComplaintStatus('IN_REVIEW')}
                    >
                      Set In Review
                    </Button>
                  )}

                  {selectedComplaint.status !== 'RESOLVED' && (
                    <Button
                      variant="primary"
                      size="sm"
                      isLoading={isUpdatingComplaint}
                      onClick={() => handleUpdateComplaintStatus('RESOLVED')}
                    >
                      ✓ Mark as Resolved
                    </Button>
                  )}

                  {selectedComplaint.status !== 'OPEN' && (
                    <Button
                      variant="ghost"
                      size="sm"
                      isLoading={isUpdatingComplaint}
                      onClick={() => handleUpdateComplaintStatus('OPEN')}
                    >
                      Re-open Complaint
                    </Button>
                  )}
                </div>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* ============================================================ */}
      {/* MODAL 3: REQUEST DETAILS INSPECTION MODAL                      */}
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
                  <span className="meta-label">Estimated Budget</span>
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

              {/* Payment Details if available */}
              {selectedRequestDetails.payment && (
                <div style={{
                  marginTop: '1rem',
                  padding: '1rem',
                  background: '#f8fafc',
                  border: '1px solid var(--color-border)',
                  borderRadius: 'var(--radius-sm)'
                }}>
                  <h4 style={{ fontWeight: 600, marginBottom: '0.5rem' }}>Financial & Payment Details:</h4>
                  <div style={{ fontSize: '0.875rem' }}>
                    <div>Total Initial Billed: <strong>৳{parseFloat(selectedRequestDetails.payment.total).toFixed(2)}</strong> ({selectedPayment?.paymentMethod || selectedRequestDetails.payment.paymentMethod || 'Direct'}, TrxID: <code>{selectedRequestDetails.payment.trxId || '—'}</code>)</div>
                    <div>Payment Status: <strong>{selectedRequestDetails.payment.status}</strong></div>
                    {selectedRequestDetails.payment.additionalPaymentStatus !== 'NONE' && (
                      <div style={{ marginTop: '0.5rem', color: '#b45309' }}>
                        Price Adjustment: <strong>+৳{parseFloat(selectedRequestDetails.payment.additionalTotal).toFixed(2)}</strong> (Status: {selectedRequestDetails.payment.additionalPaymentStatus}, TrxID: <code>{selectedRequestDetails.payment.additionalTrxId || '—'}</code>)
                      </div>
                    )}
                  </div>
                </div>
              )}

              <div className="form-actions" style={{ marginTop: '1rem' }}>
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

      {/* ============================================================ */}
      {/* MODAL 4: CREATE / EDIT PRODUCT MODAL                          */}
      {/* ============================================================ */}
      {productModalOpen && (
        <div className="admin-modal-backdrop" onClick={() => setProductModalOpen(false)}>
          <div
            className="admin-modal-content"
            onClick={(e) => e.stopPropagation()}
            role="dialog"
            aria-modal="true"
            aria-labelledby="product-modal-title"
            style={{ maxWidth: '640px' }}
          >
            <div className="admin-modal-header">
              <div>
                <span className="complaint-id-tag">
                  {editingProduct ? `#PROD-${editingProduct.id}` : 'New Catalog Item'}
                </span>
                <h2 id="product-modal-title" className="admin-modal-title">
                  {editingProduct ? `Edit: ${editingProduct.name}` : 'Add New Product to Store'}
                </h2>
              </div>
              <button
                type="button"
                className="admin-modal-close"
                onClick={() => setProductModalOpen(false)}
                aria-label="Close modal"
              >
                ✕
              </button>
            </div>

            <form onSubmit={handleSaveProduct} className="admin-modal-body">
              {productModalError && (
                <div className="ui-alert ui-alert--error" role="alert">
                  {productModalError}
                </div>
              )}

              {productModalSuccess && (
                <div className="ui-alert ui-alert--success" role="alert">
                  {productModalSuccess}
                </div>
              )}

              <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
                <div>
                  <label className="meta-label">Product Name *</label>
                  <Input
                    name="name"
                    placeholder="e.g. Hyderabadi Kacchi Biryani"
                    value={productForm.name}
                    onChange={(e) => setProductForm((prev) => ({ ...prev, name: e.target.value }))}
                    required
                  />
                </div>

                <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1rem' }}>
                  <div>
                    <label className="meta-label">Category *</label>
                    <select
                      className="ui-input ui-select"
                      value={productForm.category}
                      onChange={(e) => setProductForm((prev) => ({ ...prev, category: e.target.value }))}
                    >
                      <option value="FOOD">FOOD</option>
                      <option value="ELECTRONICS">ELECTRONICS</option>
                      <option value="OTHERS">OTHERS</option>
                    </select>
                  </div>

                  <div>
                    <label className="meta-label">Estimated Price (৳) *</label>
                    <Input
                      name="estimatedPrice"
                      type="number"
                      step="0.01"
                      min="0"
                      placeholder="e.g. 350.00"
                      value={productForm.estimatedPrice}
                      onChange={(e) => setProductForm((prev) => ({ ...prev, estimatedPrice: e.target.value }))}
                      required
                    />
                  </div>
                </div>

                <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1rem' }}>
                  <div>
                    <label className="meta-label">Weight Class *</label>
                    <select
                      className="ui-input ui-select"
                      value={productForm.weightClass}
                      onChange={(e) => setProductForm((prev) => ({ ...prev, weightClass: e.target.value }))}
                    >
                      <option value="LIGHT">LIGHT (+৳0)</option>
                      <option value="MEDIUM">MEDIUM (+৳15)</option>
                      <option value="HEAVY">HEAVY (+৳30)</option>
                    </select>
                  </div>

                  <div>
                    <label className="meta-label">Size Class *</label>
                    <select
                      className="ui-input ui-select"
                      value={productForm.sizeClass}
                      onChange={(e) => setProductForm((prev) => ({ ...prev, sizeClass: e.target.value }))}
                    >
                      <option value="SMALL">SMALL (+৳0)</option>
                      <option value="MEDIUM">MEDIUM (+৳10)</option>
                      <option value="LARGE">LARGE (+৳25)</option>
                    </select>
                  </div>
                </div>

                <div style={{ display: 'flex', gap: '2rem', padding: '0.5rem 0', flexWrap: 'wrap' }}>
                  <label style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', cursor: 'pointer', fontSize: '0.9rem' }}>
                    <input
                      type="checkbox"
                      checked={productForm.isSensitive}
                      onChange={(e) => setProductForm((prev) => ({ ...prev, isSensitive: e.target.checked }))}
                    />
                    <span>⚠️ Fragile / Sensitive handling (+৳20)</span>
                  </label>

                  <label style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', cursor: 'pointer', fontSize: '0.9rem' }}>
                    <input
                      type="checkbox"
                      checked={productForm.isActive}
                      onChange={(e) => setProductForm((prev) => ({ ...prev, isActive: e.target.checked }))}
                    />
                    <span>✓ Active (visible in public shop)</span>
                  </label>
                </div>

                <div>
                  <label className="meta-label">Image URL (plain text URL)</label>
                  <Input
                    name="imageUrl"
                    placeholder="https://picsum.photos/seed/.../400/300"
                    value={productForm.imageUrl}
                    onChange={(e) => setProductForm((prev) => ({ ...prev, imageUrl: e.target.value }))}
                  />
                  {productForm.imageUrl && (
                    <div style={{ marginTop: '0.5rem', display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
                      <span style={{ fontSize: '0.75rem', color: 'var(--color-text-muted)' }}>Preview:</span>
                      <img
                        src={productForm.imageUrl}
                        alt="Preview"
                        style={{ width: '60px', height: '45px', objectFit: 'cover', borderRadius: '4px', border: '1px solid var(--color-border)' }}
                        onError={(e) => {
                          e.target.style.display = 'none';
                        }}
                      />
                    </div>
                  )}
                </div>

                <div>
                  <label className="meta-label">Description</label>
                  <textarea
                    className="ui-input"
                    rows={3}
                    placeholder="Detailed item description, specs, or package notes..."
                    value={productForm.description}
                    onChange={(e) => setProductForm((prev) => ({ ...prev, description: e.target.value }))}
                    style={{ width: '100%', resize: 'vertical' }}
                  />
                </div>
              </div>

              <div className="modal-btn-row" style={{ marginTop: '1.5rem', display: 'flex', justifyContent: 'flex-end', gap: '0.75rem' }}>
                <Button
                  type="button"
                  variant="outline"
                  onClick={() => setProductModalOpen(false)}
                >
                  Cancel
                </Button>
                <Button
                  type="submit"
                  variant="primary"
                  isLoading={isSavingProduct}
                >
                  {editingProduct ? 'Save Changes' : 'Create Product'}
                </Button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
