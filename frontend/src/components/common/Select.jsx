import React, { useId } from 'react';

export default function Select({
  label,
  id,
  name,
  value,
  onChange,
  options = [],
  placeholder = 'Select an option',
  error,
  helperText,
  required = false,
  disabled = false,
  className = '',
  ...props
}) {
  const generatedId = useId();
  const selectId = id || (name ? `field-${name}` : generatedId);
  const errorId = `${selectId}-error`;
  const helperId = `${selectId}-helper`;

  const ariaDescribedBy = [
    error ? errorId : null,
    helperText ? helperId : null,
  ].filter(Boolean).join(' ') || undefined;

  return (
    <div className={`ui-input-group ${error ? 'ui-input-group--error' : ''} ${className}`.trim()}>
      {label && (
        <label htmlFor={selectId} className="ui-input-label">
          {label}
          {required && <span className="ui-input-required" aria-hidden="true"> *</span>}
        </label>
      )}

      <select
        id={selectId}
        name={name}
        value={value}
        onChange={onChange}
        required={required}
        disabled={disabled}
        aria-invalid={Boolean(error)}
        aria-describedby={ariaDescribedBy}
        className={`ui-input ui-select ${error ? 'ui-input--invalid' : ''}`}
        {...props}
      >
        {placeholder && (
          <option value="" disabled>
            {placeholder}
          </option>
        )}
        {options.map((opt) => {
          const optValue = typeof opt === 'object' ? opt.value : opt;
          const optLabel = typeof opt === 'object' ? opt.label : opt.replace(/_/g, ' ');
          return (
            <option key={optValue} value={optValue}>
              {optLabel}
            </option>
          );
        })}
      </select>

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
