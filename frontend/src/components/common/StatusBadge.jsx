import React from 'react';

export const STATUS_CONFIG = {
  PLANNED: {
    label: 'Planned',
    variant: 'planned',
    description: 'Trip is planned but not yet started'
  },
  IN_CITY: {
    label: 'In City',
    variant: 'in-city',
    description: 'Partner is in the city and can accept requests'
  },
  COMPLETED: {
    label: 'Completed',
    variant: 'completed',
    description: 'Trip and all deliveries are completed'
  },

  REQUESTED: {
    label: 'Requested',
    variant: 'requested',
    description: 'Order placed, waiting for delivery partner acceptance',
  },
  ACCEPTED: {
    label: 'Accepted',
    variant: 'accepted',
    description: 'Matched with partner trip',
  },
  COLLECTED: {
    label: 'Collected',
    variant: 'collected',
    description: 'Partner has collected the items from shop',
  },
  RETURNING: {
    label: 'Returning',
    variant: 'returning',
    description: 'Partner is travelling back to campus',
  },
  READY_FOR_DELIVERY: {
    label: 'Ready for Delivery',
    variant: 'ready',
    description: 'Partner is on campus, ready for handover',
  },
  DELIVERED: {
    label: 'Delivered',
    variant: 'delivered',
    description: 'Successfully delivered to customer',
  },
  CANCELLED: {
    label: 'Cancelled',
    variant: 'cancelled',
    description: 'Order has been cancelled',
  },
  OPEN: {
    label: 'Open',
    variant: 'open',
    description: 'Complaint is open and awaiting admin review',
  },
  IN_REVIEW: {
    label: 'In Review',
    variant: 'in-review',
    description: 'Complaint is actively under administrative investigation',
  },
  RESOLVED: {
    label: 'Resolved',
    variant: 'resolved',
    description: 'Complaint has been reviewed and resolved',
  },
};

export default function StatusBadge({ status, size = 'md', className = '' }) {
  const normalizedKey = status ? String(status).toUpperCase().trim() : '';
  const config = STATUS_CONFIG[normalizedKey] || {
    label: status || 'Unknown',
    variant: 'default',
    description: 'Current status',
  };

  return (
    <span
      className={`ui-badge ui-badge--${config.variant} ui-badge--${size} ${className}`.trim()}
      title={config.description}
      role="status"
    >
      <span className="ui-badge__dot" aria-hidden="true" />
      <span className="ui-badge__text">{config.label}</span>
    </span>
  );
}
