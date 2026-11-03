import React, { useState, useEffect, useMemo } from 'react';
import { useAuth } from '../context/AuthContext';
import { moderatorApi } from '../services/api';
import Card from '../components/common/Card';
import Button from '../components/common/Button';
import Input from '../components/common/Input';

export default function ModeratorPage() {
  const { token, user } = useAuth();

  // Active Tab: 'partner-apps' | 'products' | 'orders'
  const [activeTab, setActiveTab] = useState('partner-apps');

  // Data
  const [partnerApps, setPartnerApps] = useState([]);
  const [products, setProducts] = useState([]);
  const [orders, setOrders] = useState([]);

  // Loading & error
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState(null);

  // Partner Apps Filter
  const [partnerAppStatusFilter, setPartnerAppStatusFilter] = useState('ALL');

  // Product Modal State
  const [productModalOpen, setProductModalOpen] = useState(false);
  const [editingProduct, setEditingProduct] = useState(null);
  const [isSavingProduct, setIsSavingProduct] = useState(false);
  const [productModalError, setProductModalError] = useState(null);
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

  // Orders search
  const [orderSearch, setOrderSearch] = useState('');

  const loadData = async () => {
    setIsLoading(true);
    setError(null);
    try {
      const [appsData, productsData, ordersData] = await Promise.all([
        moderatorApi.getAllPartnerApplications(token).catch(() => []),
        moderatorApi.getProducts(token).catch(() => []),
        moderatorApi.getOrders(null, token).catch(() => []),
      ]);
      setPartnerApps(appsData || []);
      setProducts(productsData || []);
      setOrders(ordersData || []);
    } catch (err) {
      console.error('Failed to load moderator data:', err);
      setError(err.message || 'Failed to fetch moderator data');
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    loadData();
  }, [token]);

  // Review Delivery Partner Application
  const handleReviewPartnerApp = async (appId, approve) => {
    const notes = prompt(`Enter ${approve ? 'approval' : 'rejection'} notes (optional):`);
    if (notes === null) return;

    try {
      const updated = approve
        ? await moderatorApi.approvePartnerApplication(appId, notes, token)
        : await moderatorApi.rejectPartnerApplication(appId, notes, token);

      setPartnerApps((prev) => prev.map((a) => (a.id === appId ? updated : a)));
      alert(`Delivery partner application #${appId} was ${approve ? 'APPROVED' : 'REJECTED'}. A report was logged for Admin.`);
    } catch (err) {
      alert(`Error reviewing application: ${err.message}`);
    }
  };

  // Open Product Modal
  const handleOpenAddProduct = () => {
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
    setProductModalOpen(true);
  };

  const handleOpenEditProduct = (prod) => {
    setEditingProduct(prod);
    setProductForm({
      name: prod.name || '',
      category: prod.category || 'FOOD',
      description: prod.description || '',
      imageUrl: prod.imageUrl || '',
      estimatedPrice: prod.estimatedPrice != null ? String(prod.estimatedPrice) : '',
      weightClass: prod.weightClass || 'LIGHT',
      sizeClass: prod.sizeClass || 'SMALL',
      isSensitive: Boolean(prod.isSensitive),
      isActive: Boolean(prod.isActive),
    });
    setProductModalError(null);
    setProductModalOpen(true);
  };

  const handleSaveProduct = async (e) => {
    e.preventDefault();
    if (!productForm.name.trim() || !productForm.estimatedPrice) {
      setProductModalError('Product name and price are required.');
      return;
    }

    setIsSavingProduct(true);
    setProductModalError(null);
    try {
      const payload = {
        name: productForm.name.trim(),
        category: productForm.category,
        description: productForm.description.trim() || null,
        imageUrl: productForm.imageUrl.trim() || null,
        estimatedPrice: parseFloat(productForm.estimatedPrice),
        weightClass: productForm.weightClass,
        sizeClass: productForm.sizeClass,
        isSensitive: productForm.isSensitive,
        isActive: productForm.isActive,
      };

      if (editingProduct) {
        const updated = await moderatorApi.updateProduct(editingProduct.id, payload, token);
        setProducts((prev) => prev.map((p) => (p.id === updated.id ? updated : p)));
        alert(`Product "${updated.name}" updated! Report logged for Admin.`);
      } else {
        const created = await moderatorApi.createProduct(payload, token);
        setProducts((prev) => [created, ...prev]);
        alert(`Product "${created.name}" created! Report logged for Admin.`);
      }
      setProductModalOpen(false);
    } catch (err) {
      setProductModalError(err.message || 'Failed to save product');
    } finally {
      setIsSavingProduct(false);
    }
  };

  const handleToggleProductActive = async (prod) => {
    try {
      const updated = await moderatorApi.toggleProductStatus(prod.id, !prod.isActive, token);
      setProducts((prev) => prev.map((p) => (p.id === updated.id ? updated : p)));
      alert(`Product "${prod.name}" ${!prod.isActive ? 'activated' : 'deactivated'}. Report logged.`);
    } catch (err) {
      alert(`Error toggling product status: ${err.message}`);
    }
  };

  const pendingPartnerAppsCount = partnerApps.filter((a) => a.status === 'PENDING').length;

  const filteredOrders = useMemo(() => {
    const q = orderSearch.toLowerCase().trim();
    if (!q) return orders;
    return orders.filter(
      (o) =>
        o.productName?.toLowerCase().includes(q) ||
        o.customerName?.toLowerCase().includes(q) ||
        String(o.id).includes(q)
    );
  }, [orders, orderSearch]);

  const formatDate = (dateStr) => {
    if (!dateStr) return 'N/A';
    try {
      return new Date(dateStr).toLocaleString();
    } catch {
      return dateStr;
    }
  };

  return (
    <div className="admin-page" style={{ maxWidth: '1200px', margin: '0 auto', padding: '1.5rem' }}>
      {/* Header */}
      <div className="admin-header-card" style={{ marginBottom: '1.5rem' }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <div>
            <div style={{ display: 'flex', gap: '8px', alignItems: 'center', marginBottom: '8px' }}>
              <span style={{ background: '#7c3aed', color: '#fff', padding: '3px 10px', borderRadius: '12px', fontSize: '0.8rem', fontWeight: 700 }}>
                🛡️ Moderator Control Panel
              </span>
              <span style={{ fontSize: '0.9rem', color: '#64748b' }}>{user?.fullName}</span>
            </div>
            <h1 style={{ fontSize: '1.75rem', fontWeight: 800, color: '#1e293b', margin: '0 0 6px 0' }}>
              Operations & Catalog Desk
            </h1>
            <p style={{ color: '#64748b', fontSize: '0.9rem', margin: 0 }}>
              Manage student delivery partner applications, maintain campus shop catalog products, and inspect orders. Note: All moderator activities are automatically sent to Admin reports.
            </p>
          </div>
          <Button variant="outline" size="sm" onClick={loadData}>
            🔄 Refresh
          </Button>
        </div>
      </div>

      {error && (
        <div className="ui-alert ui-alert--error" style={{ marginBottom: '1.5rem' }}>
          {error}
        </div>
      )}

      {/* Tabs */}
      <div className="admin-tabs" style={{ display: 'flex', gap: '8px', marginBottom: '1.5rem', borderBottom: '2px solid #e2e8f0' }}>
        <button
          type="button"
          className={`admin-tab ${activeTab === 'partner-apps' ? 'admin-tab--active' : ''}`}
          onClick={() => setActiveTab('partner-apps')}
          style={{ padding: '10px 16px', fontWeight: 600, background: 'none', border: 'none', cursor: 'pointer', borderBottom: activeTab === 'partner-apps' ? '3px solid #7c3aed' : 'none', color: activeTab === 'partner-apps' ? '#7c3aed' : '#64748b' }}
        >
          🚗 Partner Applications {pendingPartnerAppsCount > 0 && <span style={{ background: '#ef4444', color: '#fff', padding: '2px 6px', borderRadius: '10px', fontSize: '0.75rem', marginLeft: '6px' }}>{pendingPartnerAppsCount}</span>}
        </button>

        <button
          type="button"
          className={`admin-tab ${activeTab === 'products' ? 'admin-tab--active' : ''}`}
          onClick={() => setActiveTab('products')}
          style={{ padding: '10px 16px', fontWeight: 600, background: 'none', border: 'none', cursor: 'pointer', borderBottom: activeTab === 'products' ? '3px solid #7c3aed' : 'none', color: activeTab === 'products' ? '#7c3aed' : '#64748b' }}
        >
          📦 Catalog Products ({products.length})
        </button>

        <button
          type="button"
          className={`admin-tab ${activeTab === 'orders' ? 'admin-tab--active' : ''}`}
          onClick={() => setActiveTab('orders')}
          style={{ padding: '10px 16px', fontWeight: 600, background: 'none', border: 'none', cursor: 'pointer', borderBottom: activeTab === 'orders' ? '3px solid #7c3aed' : 'none', color: activeTab === 'orders' ? '#7c3aed' : '#64748b' }}
        >
          📋 Order Details ({orders.length})
        </button>
      </div>

      {isLoading ? (
        <div style={{ textAlign: 'center', padding: '3rem', color: '#64748b' }}>
          Loading moderator data...
        </div>
      ) : (
        <div>
          {/* TAB 1: DELIVERY PARTNER APPLICATIONS */}
          {activeTab === 'partner-apps' && (
            <div>
              <div style={{ marginBottom: '1rem', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <p style={{ margin: 0, color: '#475569', fontSize: '0.9rem' }}>
                  Review student requests to become campus delivery partners. Your approval grants them access to post trips and accept Khulna delivery orders.
                </p>
                <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                  <span style={{ fontSize: '0.85rem', color: '#64748b' }}>Filter:</span>
                  <select
                    className="ui-input ui-select ui-select--compact"
                    value={partnerAppStatusFilter}
                    onChange={(e) => setPartnerAppStatusFilter(e.target.value)}
                  >
                    <option value="ALL">All Statuses</option>
                    <option value="PENDING">Pending Only</option>
                    <option value="APPROVED">Approved Only</option>
                    <option value="REJECTED">Rejected Only</option>
                  </select>
                </div>
              </div>

              {partnerApps.filter((a) => partnerAppStatusFilter === 'ALL' || a.status === partnerAppStatusFilter).length === 0 ? (
                <Card className="empty-state-card">
                  <div style={{ textAlign: 'center', padding: '2rem' }}>
                    <h3>No partner applications found</h3>
                    <p style={{ color: '#64748b' }}>No students have pending delivery partner requests under this filter.</p>
                  </div>
                </Card>
              ) : (
                <div className="admin-table-container">
                  <table className="admin-table" style={{ width: '100%', borderCollapse: 'collapse' }}>
                    <thead>
                      <tr style={{ background: '#f8fafc', borderBottom: '1px solid #e2e8f0', textAlign: 'left' }}>
                        <th style={{ padding: '10px' }}>App ID</th>
                        <th style={{ padding: '10px' }}>Student</th>
                        <th style={{ padding: '10px' }}>Student ID</th>
                        <th style={{ padding: '10px' }}>Contact &amp; bKash</th>
                        <th style={{ padding: '10px' }}>Return Time</th>
                        <th style={{ padding: '10px' }}>Reason</th>
                        <th style={{ padding: '10px' }}>Rules Agreed</th>
                        <th style={{ padding: '10px' }}>Status</th>
                        <th style={{ padding: '10px' }}>Date</th>
                        <th style={{ padding: '10px' }}>Action</th>
                      </tr>
                    </thead>
                    <tbody>
                      {partnerApps
                        .filter((a) => partnerAppStatusFilter === 'ALL' || a.status === partnerAppStatusFilter)
                        .map((app) => (
                          <tr key={app.id} style={{ borderBottom: '1px solid #f1f5f9' }}>
                            <td style={{ padding: '10px', fontWeight: 600 }}>#{app.id}</td>
                            <td style={{ padding: '10px' }}>
                              <strong>{app.userName}</strong>
                              <div style={{ fontSize: '0.75rem', color: '#64748b' }}>{app.userEmail}</div>
                            </td>
                            <td style={{ padding: '10px' }}><span className="student-id-badge">{app.studentId || '—'}</span></td>
                            <td style={{ padding: '10px', fontSize: '0.8rem' }}>
                              <div><strong>bKash:</strong> {app.bkashNumber || '—'}</div>
                              <div><strong>Alt:</strong> {app.alternativePhone || '—'}</div>
                            </td>
                            <td style={{ padding: '10px', fontSize: '0.85rem' }}>
                              {app.estimatedReturnTime || '—'}
                            </td>
                            <td style={{ padding: '10px', maxWidth: '200px', fontSize: '0.85rem' }}>{app.reason || 'None'}</td>
                            <td style={{ padding: '10px' }}>
                              <span style={{
                                padding: '2px 6px',
                                borderRadius: '4px',
                                fontSize: '0.75rem',
                                fontWeight: 700,
                                background: '#dcfce7',
                                color: '#15803d'
                              }}>
                                ✓ Agreed
                              </span>
                            </td>
                            <td style={{ padding: '10px' }}>
                              <span style={{
                                padding: '2px 8px',
                                borderRadius: '4px',
                                fontSize: '0.75rem',
                                fontWeight: 700,
                                background: app.status === 'APPROVED' ? '#dcfce7' : app.status === 'REJECTED' ? '#fee2e2' : '#fef3c7',
                                color: app.status === 'APPROVED' ? '#15803d' : app.status === 'REJECTED' ? '#b91c1c' : '#b45309',
                              }}>
                                {app.status}
                              </span>
                            </td>
                            <td style={{ padding: '10px', fontSize: '0.8rem', color: '#64748b' }}>{formatDate(app.createdAt)}</td>
                            <td style={{ padding: '10px' }}>
                              {app.status === 'PENDING' ? (
                                <div style={{ display: 'flex', gap: '6px' }}>
                                  <Button
                                    variant="primary"
                                    size="sm"
                                    style={{ padding: '3px 8px', fontSize: '0.75rem' }}
                                    onClick={() => handleReviewPartnerApp(app.id, true)}
                                  >
                                    ✓ Approve
                                  </Button>
                                  <Button
                                    variant="outline"
                                    size="sm"
                                    style={{ padding: '3px 8px', fontSize: '0.75rem', borderColor: '#fca5a5', color: '#b91c1c' }}
                                    onClick={() => handleReviewPartnerApp(app.id, false)}
                                  >
                                    ✕ Reject
                                  </Button>
                                </div>
                              ) : (
                                <span style={{ fontSize: '0.8rem', color: '#64748b' }}>
                                  {app.status === 'APPROVED' ? 'Approved' : 'Rejected'} ({app.reviewedByName || 'Moderator'})
                                </span>
                              )}
                            </td>
                          </tr>
                        ))}
                    </tbody>
                  </table>
                </div>
              )}
            </div>
          )}

          {/* TAB 2: PRODUCTS CATALOG MANAGEMENT */}
          {activeTab === 'products' && (
            <div>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1rem' }}>
                <p style={{ margin: 0, color: '#475569', fontSize: '0.9rem' }}>
                  Add products, adjust prices, and update pictures. Every modification is logged to Admin reports.
                </p>
                <Button variant="primary" size="sm" onClick={handleOpenAddProduct}>
                  + Add New Product
                </Button>
              </div>

              <div className="admin-table-container">
                <table className="admin-table" style={{ width: '100%', borderCollapse: 'collapse' }}>
                  <thead>
                    <tr style={{ background: '#f8fafc', borderBottom: '1px solid #e2e8f0', textAlign: 'left' }}>
                      <th style={{ padding: '10px' }}>Image</th>
                      <th style={{ padding: '10px' }}>Product Name</th>
                      <th style={{ padding: '10px' }}>Category</th>
                      <th style={{ padding: '10px' }}>Price</th>
                      <th style={{ padding: '10px' }}>Status</th>
                      <th style={{ padding: '10px' }}>Actions</th>
                    </tr>
                  </thead>
                  <tbody>
                    {products.map((p) => (
                      <tr key={p.id} style={{ borderBottom: '1px solid #f1f5f9' }}>
                        <td style={{ padding: '10px' }}>
                          {p.imageUrl ? (
                            <img src={p.imageUrl} alt={p.name} style={{ width: '40px', height: '40px', objectFit: 'cover', borderRadius: '4px' }} />
                          ) : (
                            <span style={{ fontSize: '1.5rem' }}>📦</span>
                          )}
                        </td>
                        <td style={{ padding: '10px', fontWeight: 600 }}>{p.name}</td>
                        <td style={{ padding: '10px' }}>{p.category}</td>
                        <td style={{ padding: '10px', fontWeight: 700, color: '#16a34a' }}>৳{p.estimatedPrice}</td>
                        <td style={{ padding: '10px' }}>
                          <span style={{
                            padding: '2px 8px',
                            borderRadius: '4px',
                            fontSize: '0.75rem',
                            fontWeight: 700,
                            background: p.isActive ? '#dcfce7' : '#fee2e2',
                            color: p.isActive ? '#15803d' : '#b91c1c',
                          }}>
                            {p.isActive ? 'Active' : 'Inactive'}
                          </span>
                        </td>
                        <td style={{ padding: '10px' }}>
                          <div style={{ display: 'flex', gap: '6px' }}>
                            <Button variant="outline" size="sm" onClick={() => handleOpenEditProduct(p)}>
                              Edit Price / Picture
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
            </div>
          )}

          {/* TAB 3: ORDER DETAILS */}
          {activeTab === 'orders' && (
            <div>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1rem' }}>
                <p style={{ margin: 0, color: '#475569', fontSize: '0.9rem' }}>
                  Inspect campus customer requests, items, and delivery status.
                </p>
                <div style={{ width: '300px' }}>
                  <Input
                    placeholder="Search by product, customer..."
                    value={orderSearch}
                    onChange={(e) => setOrderSearch(e.target.value)}
                  />
                </div>
              </div>

              <div className="admin-table-container">
                <table className="admin-table" style={{ width: '100%', borderCollapse: 'collapse' }}>
                  <thead>
                    <tr style={{ background: '#f8fafc', borderBottom: '1px solid #e2e8f0', textAlign: 'left' }}>
                      <th style={{ padding: '10px' }}>Order ID</th>
                      <th style={{ padding: '10px' }}>Item Name</th>
                      <th style={{ padding: '10px' }}>Customer</th>
                      <th style={{ padding: '10px' }}>Pickup Area</th>
                      <th style={{ padding: '10px' }}>Qty / Budget</th>
                      <th style={{ padding: '10px' }}>Status</th>
                      <th style={{ padding: '10px' }}>Date</th>
                    </tr>
                  </thead>
                  <tbody>
                    {filteredOrders.map((ord) => (
                      <tr key={ord.id} style={{ borderBottom: '1px solid #f1f5f9' }}>
                        <td style={{ padding: '10px', fontWeight: 600 }}>#{ord.id}</td>
                        <td style={{ padding: '10px', fontWeight: 600 }}>{ord.productName}</td>
                        <td style={{ padding: '10px' }}>
                          <div>{ord.customerName}</div>
                          <div style={{ fontSize: '0.75rem', color: '#64748b' }}>{ord.customerEmail}</div>
                        </td>
                        <td style={{ padding: '10px' }}>{ord.pickupArea}</td>
                        <td style={{ padding: '10px' }}>
                          Qty: {ord.quantity} | {ord.budget != null ? `৳${ord.budget}` : 'Catalog'}
                        </td>
                        <td style={{ padding: '10px' }}>
                          <span style={{
                            padding: '2px 8px',
                            borderRadius: '4px',
                            fontSize: '0.75rem',
                            fontWeight: 700,
                            background: '#e0e7ff',
                            color: '#3730a3',
                          }}>
                            {ord.status}
                          </span>
                        </td>
                        <td style={{ padding: '10px', fontSize: '0.8rem', color: '#64748b' }}>{formatDate(ord.createdAt)}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>
          )}
        </div>
      )}

      {/* Product Add / Edit Modal */}
      {productModalOpen && (
        <div style={{
          position: 'fixed', top: 0, left: 0, right: 0, bottom: 0,
          backgroundColor: 'rgba(0,0,0,0.5)', display: 'flex',
          alignItems: 'center', justifyContent: 'center', zIndex: 1000
        }}>
          <div style={{ background: '#fff', padding: '24px', borderRadius: '12px', width: '500px', maxWidth: '90%' }}>
            <h2 style={{ margin: '0 0 16px 0', fontSize: '1.25rem' }}>
              {editingProduct ? 'Edit Catalog Product & Price' : 'Add New Catalog Product'}
            </h2>

            {productModalError && (
              <div className="ui-alert ui-alert--error" style={{ marginBottom: '12px' }}>
                {productModalError}
              </div>
            )}

            <form onSubmit={handleSaveProduct} style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
              <div>
                <label style={{ display: 'block', fontWeight: 600, fontSize: '0.85rem', marginBottom: '4px' }}>Product Name</label>
                <Input
                  value={productForm.name}
                  onChange={(e) => setProductForm({ ...productForm, name: e.target.value })}
                  placeholder="e.g. Pran Mango Juice"
                  required
                />
              </div>

              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '12px' }}>
                <div>
                  <label style={{ display: 'block', fontWeight: 600, fontSize: '0.85rem', marginBottom: '4px' }}>Estimated Price (৳)</label>
                  <Input
                    type="number"
                    step="0.01"
                    value={productForm.estimatedPrice}
                    onChange={(e) => setProductForm({ ...productForm, estimatedPrice: e.target.value })}
                    placeholder="e.g. 50"
                    required
                  />
                </div>
                <div>
                  <label style={{ display: 'block', fontWeight: 600, fontSize: '0.85rem', marginBottom: '4px' }}>Category</label>
                  <select
                    className="ui-input ui-select"
                    value={productForm.category}
                    onChange={(e) => setProductForm({ ...productForm, category: e.target.value })}
                    style={{ width: '100%', height: '42px' }}
                  >
                    <option value="FOOD">Food & Beverage</option>
                    <option value="STATIONERY">Stationery</option>
                    <option value="ELECTRONICS">Electronics</option>
                    <option value="MEDICINE">Medicine</option>
                    <option value="GROCERY">Grocery</option>
                    <option value="OTHER">Other</option>
                  </select>
                </div>
              </div>

              <div>
                <label style={{ display: 'block', fontWeight: 600, fontSize: '0.85rem', marginBottom: '4px' }}>Image URL</label>
                <Input
                  value={productForm.imageUrl}
                  onChange={(e) => setProductForm({ ...productForm, imageUrl: e.target.value })}
                  placeholder="https://... (direct image link)"
                />
              </div>

              <div>
                <label style={{ display: 'block', fontWeight: 600, fontSize: '0.85rem', marginBottom: '4px' }}>Description</label>
                <textarea
                  className="ui-input"
                  rows="2"
                  value={productForm.description}
                  onChange={(e) => setProductForm({ ...productForm, description: e.target.value })}
                  placeholder="Short description..."
                  style={{ width: '100%', padding: '8px' }}
                />
              </div>

              <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '8px', marginTop: '12px' }}>
                <Button type="button" variant="outline" onClick={() => setProductModalOpen(false)}>
                  Cancel
                </Button>
                <Button type="submit" variant="primary" disabled={isSavingProduct}>
                  {isSavingProduct ? 'Saving...' : editingProduct ? 'Save Changes' : 'Create Product'}
                </Button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
