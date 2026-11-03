import React, { useState, useEffect } from 'react';
import { useAuth } from '../../context/AuthContext';
import { usersApi } from '../../services/api';
import Button from '../common/Button';
import Input from '../common/Input';

export default function ProfileModal({ isOpen, onClose }) {
  const { user, role, token, refreshProfile } = useAuth();
  const [applications, setApplications] = useState([]);
  const [isLoadingApps, setIsLoadingApps] = useState(false);

  // Application form state
  const [showApplyForm, setShowApplyForm] = useState(false);
  const [requestedRole, setRequestedRole] = useState('DELIVERY_PARTNER');

  // Form fields
  const [altPhone, setAltPhone] = useState('');
  const [bkashNumber, setBkashNumber] = useState('');
  const [estimatedReturnTime, setEstimatedReturnTime] = useState('');
  const [activeHonestCommitment, setActiveHonestCommitment] = useState(true);
  const [reason, setReason] = useState('');
  const [agreedToRules, setAgreedToRules] = useState(false);

  const [isSubmitting, setIsSubmitting] = useState(false);
  const [applyError, setApplyError] = useState(null);
  const [applySuccess, setApplySuccess] = useState(null);

  const loadApplications = async () => {
    if (!token) return;
    setIsLoadingApps(true);
    try {
      const data = await usersApi.getMyApplications(token);
      setApplications(data || []);
    } catch (err) {
      console.error('Failed to load my applications:', err);
    } finally {
      setIsLoadingApps(false);
    }
  };

  useEffect(() => {
    if (isOpen) {
      loadApplications();
      refreshProfile?.();
      setShowApplyForm(false);
      resetForm();
      setApplyError(null);
      setApplySuccess(null);
    }
  }, [isOpen]);

  const resetForm = () => {
    setAltPhone('');
    setBkashNumber('');
    setEstimatedReturnTime('');
    setActiveHonestCommitment(true);
    setReason('');
    setAgreedToRules(false);
  };

  if (!isOpen) return null;

  const handleApply = async (e) => {
    e.preventDefault();
    setApplyError(null);

    if (!altPhone.trim()) {
      setApplyError('Alternative phone number is required.');
      return;
    }
    if (!bkashNumber.trim()) {
      setApplyError('bKash number is required for receiving salary / delivery fees.');
      return;
    }
    if (requestedRole === 'DELIVERY_PARTNER' && !estimatedReturnTime.trim()) {
      setApplyError('Please provide your estimated return time from Khulna.');
      return;
    }
    if (!reason.trim()) {
      setApplyError(`Please explain why you want to be a ${requestedRole === 'DELIVERY_PARTNER' ? 'Delivery Partner' : 'Moderator'}.`);
      return;
    }
    if (!agreedToRules) {
      setApplyError('You must read and agree to all rules before submitting.');
      return;
    }

    setIsSubmitting(true);
    setApplySuccess(null);

    try {
      const payload = {
        requestedRole,
        alternativePhone: altPhone.trim(),
        bkashNumber: bkashNumber.trim(),
        estimatedReturnTime: requestedRole === 'DELIVERY_PARTNER' ? estimatedReturnTime.trim() : null,
        activeHonestCommitment: requestedRole === 'MODERATOR' ? Boolean(activeHonestCommitment) : null,
        reason: reason.trim(),
        agreedToRules: true,
      };

      await usersApi.applyRole(payload, token);
      setApplySuccess(`Your application for ${requestedRole === 'DELIVERY_PARTNER' ? 'Delivery Partner' : 'Moderator'} has been submitted!`);
      resetForm();
      setShowApplyForm(false);
      loadApplications();
    } catch (err) {
      setApplyError(err.message || 'Failed to submit application');
    } finally {
      setIsSubmitting(false);
    }
  };

  const getRoleBadge = (r) => {
    switch (r) {
      case 'ADMIN':
        return <span style={{ background: '#dc2626', color: '#fff', padding: '3px 10px', borderRadius: '12px', fontWeight: 700, fontSize: '0.8rem' }}>👑 Admin</span>;
      case 'MODERATOR':
        return <span style={{ background: '#7c3aed', color: '#fff', padding: '3px 10px', borderRadius: '12px', fontWeight: 700, fontSize: '0.8rem' }}>🛡️ Moderator</span>;
      case 'DELIVERY_PARTNER':
        return <span style={{ background: '#0284c7', color: '#fff', padding: '3px 10px', borderRadius: '12px', fontWeight: 700, fontSize: '0.8rem' }}>🚗 Delivery Partner</span>;
      case 'TEACHER':
        return <span style={{ background: '#059669', color: '#fff', padding: '3px 10px', borderRadius: '12px', fontWeight: 700, fontSize: '0.8rem' }}>👨‍🏫 Teacher</span>;
      default:
        return <span style={{ background: '#e2e8f0', color: '#334155', padding: '3px 10px', borderRadius: '12px', fontWeight: 700, fontSize: '0.8rem' }}>🎓 Student</span>;
    }
  };

  const hasPendingPartner = applications.some((a) => a.requestedRole === 'DELIVERY_PARTNER' && a.status === 'PENDING');
  const hasPendingMod = applications.some((a) => a.requestedRole === 'MODERATOR' && a.status === 'PENDING');

  return (
    <div style={{
      position: 'fixed', top: 0, left: 0, right: 0, bottom: 0,
      backgroundColor: 'rgba(0,0,0,0.5)', display: 'flex',
      alignItems: 'center', justifyContent: 'center', zIndex: 1100
    }}>
      <div style={{
        background: '#fff', padding: '24px', borderRadius: '16px',
        width: '600px', maxWidth: '95%', maxHeight: '90vh', overflowY: 'auto',
        boxShadow: '0 20px 25px -5px rgba(0, 0, 0, 0.1), 0 10px 10px -5px rgba(0, 0, 0, 0.04)'
      }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: '16px' }}>
          <div>
            <h2 style={{ margin: '0 0 4px 0', fontSize: '1.4rem', fontWeight: 800, color: '#1e293b' }}>
              My Profile & Access
            </h2>
            <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginTop: '6px' }}>
              {getRoleBadge(role)}
              <span style={{ fontSize: '0.85rem', color: '#64748b' }}>Account Type</span>
            </div>
          </div>
          <button
            onClick={onClose}
            style={{ background: 'none', border: 'none', fontSize: '1.5rem', cursor: 'pointer', color: '#94a3b8' }}
          >
            ✕
          </button>
        </div>

        {/* User Info Card */}
        <div style={{ background: '#f8fafc', border: '1px solid #e2e8f0', borderRadius: '10px', padding: '14px', marginBottom: '16px' }}>
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '10px', fontSize: '0.9rem' }}>
            <div>
              <span style={{ color: '#64748b', display: 'block', fontSize: '0.75rem', textTransform: 'uppercase' }}>Full Name</span>
              <strong>{user?.fullName}</strong>
            </div>
            <div>
              <span style={{ color: '#64748b', display: 'block', fontSize: '0.75rem', textTransform: 'uppercase' }}>ID Number</span>
              <strong>{user?.studentId}</strong>
            </div>
            <div>
              <span style={{ color: '#64748b', display: 'block', fontSize: '0.75rem', textTransform: 'uppercase' }}>Email</span>
              <span>{user?.email}</span>
            </div>
            <div>
              <span style={{ color: '#64748b', display: 'block', fontSize: '0.75rem', textTransform: 'uppercase' }}>Phone</span>
              <span>{user?.phone || '—'}</span>
            </div>
          </div>
        </div>

        {/* Role Specific Actions & Applications */}
        {role === 'ADMIN' ? (
          <div style={{ background: '#fef2f2', border: '1px solid #fecaca', borderRadius: '8px', padding: '12px', color: '#991b1b', fontSize: '0.9rem', marginBottom: '16px' }}>
            👑 <strong>Admin Account:</strong> You have unlimited control across CARRY. You can manage products, view moderator reports, delete accounts, and verify orders.
          </div>
        ) : role === 'TEACHER' ? (
          <div style={{ background: '#f0fdf4', border: '1px solid #bbf7d0', borderRadius: '8px', padding: '12px', color: '#166534', fontSize: '0.9rem', marginBottom: '16px' }}>
            👨‍🏫 <strong>Teacher / Faculty Account:</strong> You can place catalog and custom orders directly. Faculty accounts are restricted from applying for delivery partner or moderator duties.
          </div>
        ) : (
          <div>
            {applySuccess && (
              <div className="ui-alert ui-alert--success" style={{ marginBottom: '16px' }}>
                {applySuccess}
              </div>
            )}
            {applyError && (
              <div className="ui-alert ui-alert--error" style={{ marginBottom: '16px' }}>
                {applyError}
              </div>
            )}

            {/* Application Section */}
            {!showApplyForm ? (
              <div style={{ display: 'flex', gap: '10px', marginBottom: '20px' }}>
                {role !== 'DELIVERY_PARTNER' && (
                  <Button
                    variant="primary"
                    disabled={hasPendingPartner}
                    onClick={() => {
                      setRequestedRole('DELIVERY_PARTNER');
                      resetForm();
                      setShowApplyForm(true);
                    }}
                    style={{ flex: 1, fontSize: '0.85rem' }}
                  >
                    {hasPendingPartner ? '⏳ Partner Applied' : '🚗 Apply as Delivery Partner'}
                  </Button>
                )}

                {role !== 'MODERATOR' && (
                  <Button
                    variant="outline"
                    disabled={hasPendingMod}
                    onClick={() => {
                      setRequestedRole('MODERATOR');
                      resetForm();
                      setShowApplyForm(true);
                    }}
                    style={{ flex: 1, fontSize: '0.85rem', borderColor: '#7c3aed', color: '#7c3aed' }}
                  >
                    {hasPendingMod ? '⏳ Moderator Applied' : '🛡️ Apply as Moderator'}
                  </Button>
                )}
              </div>
            ) : (
              <div style={{
                background: requestedRole === 'DELIVERY_PARTNER' ? '#eff6ff' : '#faf5ff',
                border: `1px solid ${requestedRole === 'DELIVERY_PARTNER' ? '#bfdbfe' : '#e9d5ff'}`,
                borderRadius: '12px',
                padding: '18px',
                marginBottom: '20px'
              }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '12px' }}>
                  <h4 style={{ margin: 0, fontSize: '1.1rem', color: requestedRole === 'DELIVERY_PARTNER' ? '#1e40af' : '#6b21a8' }}>
                    {requestedRole === 'DELIVERY_PARTNER' ? '🚗 Delivery Partner Application Form' : '🛡️ Moderator Application Form'}
                  </h4>
                  <button
                    type="button"
                    onClick={() => setShowApplyForm(false)}
                    style={{ background: 'none', border: 'none', cursor: 'pointer', fontSize: '1.1rem', color: '#64748b' }}
                  >
                    ✕
                  </button>
                </div>

                <form onSubmit={handleApply}>
                  {/* Common Fields: Alternative Phone & bKash */}
                  <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '12px', marginBottom: '12px' }}>
                    <div>
                      <label style={{ display: 'block', fontSize: '0.8rem', fontWeight: 600, marginBottom: '4px', color: '#334155' }}>
                        Alternative Phone Number: *
                      </label>
                      <Input
                        type="tel"
                        value={altPhone}
                        onChange={(e) => setAltPhone(e.target.value)}
                        placeholder="01XXXXXXXXX"
                        required
                      />
                    </div>
                    <div>
                      <label style={{ display: 'block', fontSize: '0.8rem', fontWeight: 600, marginBottom: '4px', color: '#334155' }}>
                        bKash Number: *
                      </label>
                      <Input
                        type="tel"
                        value={bkashNumber}
                        onChange={(e) => setBkashNumber(e.target.value)}
                        placeholder="01XXXXXXXXX (Personal)"
                        required
                      />
                    </div>
                  </div>

                  {/* Delivery Partner Specific: Estimated Return Time */}
                  {requestedRole === 'DELIVERY_PARTNER' && (
                    <div style={{ marginBottom: '12px' }}>
                      <label style={{ display: 'block', fontSize: '0.8rem', fontWeight: 600, marginBottom: '4px', color: '#334155' }}>
                        Estimate time when you usually come back: *
                      </label>
                      <Input
                        type="text"
                        value={estimatedReturnTime}
                        onChange={(e) => setEstimatedReturnTime(e.target.value)}
                        placeholder="e.g. Daily by 6:00 PM / Sundays & Tuesdays 4:30 PM"
                        required
                      />
                    </div>
                  )}

                  {/* Moderator Specific: Active & Honest Commitment */}
                  {requestedRole === 'MODERATOR' && (
                    <div style={{ marginBottom: '12px', padding: '10px 12px', background: '#fff', border: '1px solid #ddd6fe', borderRadius: '8px' }}>
                      <label style={{ display: 'block', fontSize: '0.85rem', fontWeight: 600, marginBottom: '6px', color: '#4c1d95' }}>
                        Can you work actively and honestly? *
                      </label>
                      <div style={{ display: 'flex', gap: '16px' }}>
                        <label style={{ display: 'flex', alignItems: 'center', gap: '6px', fontSize: '0.85rem', cursor: 'pointer' }}>
                          <input
                            type="radio"
                            name="honestCommitment"
                            checked={activeHonestCommitment === true}
                            onChange={() => setActiveHonestCommitment(true)}
                          />
                          Yes, I will work actively and honestly
                        </label>
                        <label style={{ display: 'flex', alignItems: 'center', gap: '6px', fontSize: '0.85rem', cursor: 'pointer' }}>
                          <input
                            type="radio"
                            name="honestCommitment"
                            checked={activeHonestCommitment === false}
                            onChange={() => setActiveHonestCommitment(false)}
                          />
                          No
                        </label>
                      </div>
                    </div>
                  )}

                  {/* Why you want to be ... */}
                  <div style={{ marginBottom: '12px' }}>
                    <label style={{ display: 'block', fontSize: '0.8rem', fontWeight: 600, marginBottom: '4px', color: '#334155' }}>
                      {requestedRole === 'DELIVERY_PARTNER' ? 'Why you want to be a delivery partner? *' : 'Why you want to be a moderator? *'}
                    </label>
                    <textarea
                      className="ui-input"
                      rows="3"
                      value={reason}
                      onChange={(e) => setReason(e.target.value)}
                      placeholder={requestedRole === 'DELIVERY_PARTNER' ? "Explain your travel schedule, vehicle or bike, willingness to deliver..." : "Explain your platform management experience, catalog handling, availability..."}
                      style={{ width: '100%', padding: '8px', fontSize: '0.85rem' }}
                      required
                    />
                  </div>

                  {/* Rules Card & Checkbox */}
                  <div style={{
                    marginBottom: '14px',
                    padding: '12px 14px',
                    background: '#fff',
                    borderRadius: '8px',
                    border: '1px solid #e2e8f0'
                  }}>
                    <strong style={{ display: 'block', fontSize: '0.85rem', color: '#0f172a', marginBottom: '6px' }}>
                      📜 {requestedRole === 'DELIVERY_PARTNER' ? 'Delivery Partner Rules:' : 'Moderator Rules:'}
                    </strong>
                    {requestedRole === 'DELIVERY_PARTNER' ? (
                      <ol style={{ margin: '0 0 10px 18px', padding: 0, fontSize: '0.8rem', color: '#475569', lineHeight: 1.5 }}>
                        <li>Can't misbehave with order holder student.</li>
                        <li>Cannot charge extra amount; if needed contact with authority.</li>
                        <li>Delivery salary will be paid through bKash within 24 hours according to delivery %.</li>
                      </ol>
                    ) : (
                      <ol style={{ margin: '0 0 10px 18px', padding: 0, fontSize: '0.8rem', color: '#475569', lineHeight: 1.5 }}>
                        <li>Work honestly.</li>
                        <li>Salary will be paid through bKash within 7 days.</li>
                        <li>Must be present when authority needs you.</li>
                      </ol>
                    )}

                    <label style={{ display: 'flex', alignItems: 'center', gap: '8px', fontSize: '0.85rem', fontWeight: 600, color: '#1e293b', cursor: 'pointer', borderTop: '1px solid #f1f5f9', paddingTop: '8px' }}>
                      <input
                        type="checkbox"
                        checked={agreedToRules}
                        onChange={(e) => setAgreedToRules(e.target.checked)}
                        required
                      />
                      I have read and agree to all the rules.
                    </label>
                  </div>

                  <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '8px' }}>
                    <Button type="button" variant="outline" size="sm" onClick={() => setShowApplyForm(false)}>
                      Cancel
                    </Button>
                    <Button type="submit" variant="primary" size="sm" disabled={isSubmitting || !agreedToRules}>
                      {isSubmitting ? 'Submitting...' : 'Submit Application'}
                    </Button>
                  </div>
                </form>
              </div>
            )}

            {/* Applications History */}
            <h4 style={{ margin: '0 0 8px 0', fontSize: '0.95rem', color: '#1e293b' }}>
              My Applications History
            </h4>
            {isLoadingApps ? (
              <div style={{ fontSize: '0.85rem', color: '#64748b' }}>Loading applications...</div>
            ) : applications.length === 0 ? (
              <div style={{ fontSize: '0.85rem', color: '#94a3b8', fontStyle: 'italic', marginBottom: '16px' }}>
                You have not submitted any role applications yet.
              </div>
            ) : (
              <div style={{ display: 'flex', flexDirection: 'column', gap: '8px', marginBottom: '16px' }}>
                {applications.map((app) => (
                  <div key={app.id} style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: '10px 14px', background: '#f8fafc', border: '1px solid #e2e8f0', borderRadius: '8px', fontSize: '0.85rem' }}>
                    <div>
                      <strong>{app.requestedRole === 'DELIVERY_PARTNER' ? '🚗 Delivery Partner' : '🛡️ Moderator'}</strong>
                      <div style={{ fontSize: '0.75rem', color: '#64748b', marginTop: '2px' }}>
                        bKash: {app.bkashNumber || 'N/A'} &middot; Alt: {app.alternativePhone || 'N/A'}
                        {app.estimatedReturnTime && <span> &middot; Return: {app.estimatedReturnTime}</span>}
                      </div>
                      <div style={{ fontSize: '0.75rem', color: '#475569', fontStyle: 'italic', marginTop: '2px' }}>
                        "{app.reason || ''}"
                      </div>
                    </div>
                    <div>
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
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>
        )}

        <div style={{ display: 'flex', justifyContent: 'flex-end', marginTop: '16px' }}>
          <Button variant="secondary" onClick={onClose}>
            Close
          </Button>
        </div>
      </div>
    </div>
  );
}
