import React from 'react';

export default function Button({
  children,
  type = 'button',
  variant = 'primary',
  size = 'md',
  isLoading = false,
  disabled = false,
  className = '',
  onClick,
  ...props
}) {
  const baseClasses = 'ui-btn';
  const variantClass = `ui-btn--${variant}`;
  const sizeClass = `ui-btn--${size}`;
  const loadingClass = isLoading ? 'ui-btn--loading' : '';

  return (
    <button
      type={type}
      disabled={disabled || isLoading}
      aria-busy={isLoading}
      onClick={onClick}
      className={`${baseClasses} ${variantClass} ${sizeClass} ${loadingClass} ${className}`.trim()}
      {...props}
    >
      {isLoading && (
        <span className="ui-btn__spinner" aria-hidden="true"></span>
      )}
      <span className="ui-btn__content">{children}</span>
    </button>
  );
}
