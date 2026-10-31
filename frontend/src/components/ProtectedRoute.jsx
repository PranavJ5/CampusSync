import { Navigate, useLocation } from 'react-router-dom'
import { dashboardPath } from '../auth/session'

export default function ProtectedRoute({ session, allowedRoles, children }) {
  const location = useLocation()
  if (!session?.token) return <Navigate to="/login" replace state={{ from: location }} />

  const role = String(session.user?.role || '').toUpperCase()
  if (allowedRoles && !allowedRoles.includes(role)) {
    return <Navigate to={dashboardPath(role)} replace />
  }
  return children
}
