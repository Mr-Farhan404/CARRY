import React, { useId } from 'react';

export default function Input({
  label,
  id,
  name,
  type = 'text',
  value,
  onChange,
  placeholder,
  error,
  helperText,
  required = false,
  disabled = false,
  className = '',
  ...props
}) {
  const generatedId = useId();
  const inputId = id || (name ? `field-${name}` : generatedId);
  const errorId = `${inputId}-error`;
  const helperId = `${inputId}-helper`;

  const ariaDescribedBy = [
    error ? errorId : null,
    helperText ? helperId : null,
  ].filter(Boolean).join(' ') || undefined;

  return (
    <div className={`ui-input-group ${error ? 'ui-input-group--error' : ''} ${className}`.trim()}>
      {label && (
        <label htmlFor={inputId} className="ui-input-label">
          {label}
          {required && <span className="ui-input-required" aria-hidden="true"> *</span>}
        </label>
      )}

      <input
        id={inputId}
        name={name}
        type={type}
        value={value}
        onChange={onChange}
        placeholder={placeholder}
        required={required}
        disabled={disabled}
        aria-invalid={Boolean(error)}
        aria-describedby={ariaDescribedBy}
        className={`ui-input ${error ? 'ui-input--invalid' : ''}`}
        {...props}
      />

      {error && (
        <p id={errorId} className="ui-input-error" role="alert">
          {error}
        </p>
      )}

      {!error && helperText && (
        <p id={helperId} className="ui-input-helper">
          {helperText}
        </p>
      )}
    </div>
  );
}
