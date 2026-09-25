import React from 'react';

/**
 * Toast / Alert / ErrorMessage Component
 * Reusable feedback message component for error, success, warning, and info notifications.
 *
 * @param {'error'|'success'|'warning'|'info'} variant - Style variant
 * @param {string} [title] - Optional heading
 * @param {React.ReactNode} children - Message content or body
 * @param {string} [message] - Message string shorthand if children not passed
 * @param {() => void} [onClose] - Optional close/dismiss handler
 * @param {React.ReactNode} [action] - Optional action button (e.g., Retry)
 * @param {string} [className] - Additional CSS class names
 */
export default function Toast({
  variant = 'error',
  title,
  children,
  message,
  onClose,
  action,
  className = '',
  ...props
}) {
  const content = children || message;
  if (!content && !title) return null;

  // Accessible role based on severity
  const role = variant === 'error' || variant === 'warning' ? 'alert' : 'status';

  // Distinct icons for accessible visual distinction (never color alone)
  const icons = {
    error: '⚠️',
    success: '✓',
    warning: '⚡',
    info: 'ℹ',
  };

  return (
    <div
      role={role}
      className={`ui-alert ui-alert--${variant} ${className}`.trim()}
      {...props}
    >
      <div className="ui-alert__inner" style={{ display: 'flex', alignItems: 'flex-start', gap: '0.75rem' }}>
        <span className="ui-alert__icon" aria-hidden="true" style={{ fontSize: '1.1rem', flexShrink: 0, lineHeight: 1.2 }}>
          {icons[variant] || 'ℹ'}
        </span>

        <div className="ui-alert__content" style={{ flex: 1, minWidth: 0 }}>
          {title && (
            <h4 className="ui-alert__title" style={{ margin: '0 0 0.25rem 0', fontWeight: 600, fontSize: '0.95rem' }}>
              {title}
            </h4>
          )}
          <div className="ui-alert__body" style={{ margin: 0, wordBreak: 'break-word' }}>
            {content}
          </div>
          {action && (
            <div className="ui-alert__action" style={{ marginTop: '0.5rem' }}>
              {action}
            </div>
          )}
        </div>

        {onClose && (
          <button
            type="button"
            onClick={onClose}
            aria-label="Dismiss alert"
            className="ui-alert__close"
            style={{
              background: 'transparent',
              border: 'none',
              cursor: 'pointer',
              fontSize: '1rem',
              lineHeight: 1,
              padding: '0.125rem 0.25rem',
              color: 'inherit',
              opacity: 0.7,
              marginLeft: '0.5rem',
              flexShrink: 0,
            }}
          >
            ✕
          </button>
        )}
      </div>
    </div>
  );
}
