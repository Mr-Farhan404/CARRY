import React from 'react';

export default function Card({
  title,
  subtitle,
  headerAction,
  children,
  footer,
  className = '',
  as: Component = 'div',
  ...props
}) {
  const hasHeader = Boolean(title || subtitle || headerAction);

  return (
    <Component className={`ui-card ${className}`.trim()} {...props}>
      {hasHeader && (
        <header className="ui-card__header">
          <div className="ui-card__titles">
            {title && <h2 className="ui-card__title">{title}</h2>}
            {subtitle && <p className="ui-card__subtitle">{subtitle}</p>}
          </div>
          {headerAction && <div className="ui-card__action">{headerAction}</div>}
        </header>
      )}

      <div className="ui-card__body">{children}</div>

      {footer && <footer className="ui-card__footer">{footer}</footer>}
    </Component>
  );
}
