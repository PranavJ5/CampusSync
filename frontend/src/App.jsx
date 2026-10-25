import { useEffect, useState } from 'react'
import { BrowserRouter, Navigate, Outlet, Route, Routes } from 'react-router-dom'
import api from './api/axios'
import { dashboardPath, getSession } from './auth/session'
import AppNavbar from './components/AppNavbar'
import ProtectedRoute from './components/ProtectedRoute'
import { asList } from './utils/data'
import LoginPage from './pages/LoginPage'
import Dashboard from './pages/Dashboards'
import EventsPage from './pages/EventsPage'
import EventDetailPage from './pages/EventDetailPage'
import RegistrationsPage from './pages/RegistrationsPage'
import NotificationsPage from './pages/NotificationsPage'

function AuthenticatedLayout({ session, onLogout, unreadCount }) {
  return <><AppNavbar session={session} unreadCount={unreadCount} onLogout={onLogout} /><Outlet /></>
}

function App() {
  const [session, setSession] = useState(getSession)
  const [unreadCount, setUnreadCount] = useState(0)

  useEffect(() => {
    const expireSession = () => setSession(null)
    window.addEventListener('campussync:session-expired', expireSession)
    return () => window.removeEventListener('campussync:session-expired', expireSession)
  }, [])

  useEffect(() => {
    if (!session?.token) { setUnreadCount(0); return undefined }
    let active = true
    api.get('/notifications', { params: { unread: true } })
      .then((response) => { if (active) setUnreadCount(asList(response.data).filter((item) => !item.read && !item.isRead).length) })
      .catch(() => { if (active) setUnreadCount(0) })
    return () => { active = false }
  }, [session])

  return <BrowserRouter><Routes>
    <Route path="/login" element={session ? <Navigate to={dashboardPath(session.user?.role)} replace /> : <LoginPage onAuthenticated={setSession} />} />
    <Route element={<ProtectedRoute session={session}><AuthenticatedLayout session={session} unreadCount={unreadCount} onLogout={() => setSession(null)} /></ProtectedRoute>}>
      <Route path="/student" element={<ProtectedRoute session={session} allowedRoles={['STUDENT']}><Dashboard kind="student" /></ProtectedRoute>} />
      <Route path="/coordinator" element={<ProtectedRoute session={session} allowedRoles={['COORDINATOR', 'FACULTY']}><Dashboard kind="coordinator" /></ProtectedRoute>} />
      <Route path="/admin" element={<ProtectedRoute session={session} allowedRoles={['ADMIN']}><Dashboard kind="admin" /></ProtectedRoute>} />
      <Route path="/events" element={<EventsPage />} />
      <Route path="/events/:id" element={<EventDetailPage />} />
      <Route path="/my-registrations" element={<ProtectedRoute session={session} allowedRoles={['STUDENT']}><RegistrationsPage /></ProtectedRoute>} />
      <Route path="/notifications" element={<NotificationsPage onUnreadChange={setUnreadCount} />} />
      <Route path="/" element={<Navigate to={dashboardPath(session?.user?.role)} replace />} />
      <Route path="*" element={<Navigate to={dashboardPath(session?.user?.role)} replace />} />
    </Route>
  </Routes></BrowserRouter>
}

export default App
