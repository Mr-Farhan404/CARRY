/**
 * CARRY Design System - Design Tokens & Constants
 * Central source of truth for colors, typography, spacing, radii, and status badge configurations.
 */

export const COLORS = {
  // Brand & Accent
  primary: '#2563eb',
  primaryHover: '#1d4ed8',
  primaryLight: '#eff6ff',
  primaryDark: '#1e40af',

  // Surfaces & Backgrounds
  background: '#f8fafc',
  surface: '#ffffff',
  surfaceAlt: '#f1f5f9',

  // Text Hierarchy
  textPrimary: '#0f172a',
  textSecondary: '#475569',
  textMuted: '#64748b',
  textSubtle: '#94a3b8',
  textInverse: '#ffffff',

  // Borders & Dividers
  border: '#e2e8f0',
  borderStrong: '#cbd5e1',
  borderFocus: '#3b82f6',

  // Feedback & Alerts
  error: '#ef4444',
  errorBg: '#fef2f2',
  errorBorder: '#fecaca',
  errorText: '#991b1b',

  success: '#10b981',
  successBg: '#f0fdf4',
  successBorder: '#bbf7d0',
  successText: '#166534',

  warning: '#f59e0b',
  warningBg: '#fffbeb',
  warningBorder: '#fde68a',
  warningText: '#92400e',

  info: '#3b82f6',
  infoBg: '#eff6ff',
  infoBorder: '#bfdbfe',
  infoText: '#1e40af',

  // 7 Core Order Lifecycle Statuses + Trip & Complaint Statuses
  status: {
    REQUESTED: {
      bg: '#eff6ff',
      text: '#1d4ed8',
      border: '#bfdbfe',
      dot: '#3b82f6',
      label: 'Requested',
      description: 'Order placed, waiting for delivery partner acceptance',
    },
    ACCEPTED: {
      bg: '#eef2ff',
      text: '#4338ca',
      border: '#c7d2fe',
      dot: '#6366f1',
      label: 'Accepted',
      description: 'Matched with delivery partner trip',
    },
    COLLECTED: {
      bg: '#fffbeb',
      text: '#b45309',
      border: '#fde68a',
      dot: '#f59e0b',
      label: 'Collected',
      description: 'Partner has collected items from shop',
    },
    RETURNING: {
      bg: '#f0fdfa',
      text: '#0f766e',
      border: '#99f6e4',
      dot: '#14b8a6',
      label: 'Returning',
      description: 'Partner is travelling back to campus',
    },
    READY_FOR_DELIVERY: {
      bg: '#fff7ed',
      text: '#c2410c',
      border: '#fed7aa',
      dot: '#f97316',
      label: 'Ready for Delivery',
      description: 'Partner is on campus, ready for handover',
    },
    DELIVERED: {
      bg: '#f0fdf4',
      text: '#15803d',
      border: '#bbf7d0',
      dot: '#22c55e',
      label: 'Delivered',
      description: 'Successfully delivered to customer',
    },
    CANCELLED: {
      bg: '#fff1f2',
      text: '#be123c',
      border: '#fecdd3',
      dot: '#f43f5e',
      label: 'Cancelled',
      description: 'Order cancelled before collection',
    },
  },
};

export const SPACING = {
  none: '0',
  xs: '0.25rem', // 4px
  sm: '0.5rem',  // 8px
  md: '1rem',    // 16px
  lg: '1.5rem',  // 24px
  xl: '2rem',    // 32px
  '2xl': '3rem', // 48px
};

export const RADII = {
  none: '0',
  sm: '6px',
  md: '10px',
  lg: '16px',
  full: '9999px',
};

export const SHADOWS = {
  sm: '0 1px 2px 0 rgba(0, 0, 0, 0.05)',
  md: '0 4px 6px -1px rgba(0, 0, 0, 0.1), 0 2px 4px -2px rgba(0, 0, 0, 0.1)',
  lg: '0 10px 15px -3px rgba(0, 0, 0, 0.1), 0 4px 6px -4px rgba(0, 0, 0, 0.1)',
};

export const TYPOGRAPHY = {
  fontFamily: "-apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Oxygen, Ubuntu, Cantarell, sans-serif",
  fontSize: {
    xs: '0.75rem',
    sm: '0.875rem',
    base: '1rem',
    lg: '1.125rem',
    xl: '1.25rem',
    '2xl': '1.5rem',
    '3xl': '1.875rem',
  },
};

export default {
  COLORS,
  SPACING,
  RADII,
  SHADOWS,
  TYPOGRAPHY,
};
