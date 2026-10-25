export function LoadingSpinner({ label = 'Loading...' }) {
  return (
    <div className="d-flex flex-column align-items-center justify-content-center gap-2 py-5" role="status">
      <div className="spinner-border text-primary" aria-hidden="true" />
      <span className="text-secondary">{label}</span>
    </div>
  )
}

export function EmptyState({ title, detail, action }) {
  return (
    <section className="empty-state text-center p-4 p-md-5">
      <div className="empty-state__icon" aria-hidden="true">◇</div>
      <h2 className="h5 mb-2">{title}</h2>
      {detail && <p className="text-secondary mb-3">{detail}</p>}
      {action}
    </section>
  )
}

export function ErrorAlert({ message, onRetry }) {
  return (
    <div className="alert alert-danger d-flex flex-wrap align-items-center justify-content-between gap-2" role="alert">
      <span>{message}</span>
      {onRetry && <button className="btn btn-sm btn-outline-danger" onClick={onRetry}>Try again</button>}
    </div>
  )
}
