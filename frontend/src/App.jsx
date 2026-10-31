import { useEffect, useState } from 'react'
import { BrowserRouter, Navigate, Outlet, Route, Routes } from 'react-router-dom'
import api from './api/axios'
import { dashboardPath, getSession } from './auth/session'
import AppNavbar from './components/AppNavbar'
import AppSidebar from './components/AppSidebar'
import ProtectedRoute from './components/ProtectedRoute'
import { asList } from './utils/data'
import LoginPage from './pages/LoginPage'
import Dashboard from './pages/Dashboards'
import EventsPage from './pages/EventsPage'
import EventDetailPage from './pages/EventDetailPage'
import RegistrationsPage from './pages/RegistrationsPage'
import NotificationsPage from './pages/NotificationsPage'
import EventManagementPage from './pages/EventManagementPage'
import AuthenticationLandingPage from './pages/AuthenticationLandingPage'
import StudentRegistrationPage from './pages/StudentRegistrationPage'
import CoordinatorProvisioningPage from './pages/CoordinatorProvisioningPage'
import CommitteeManagementPage from './pages/CommitteeManagementPage'
import FacultyAttendancePage from './pages/FacultyAttendancePage'
import AttendanceReportPage from './pages/AttendanceReportPage'
import FacultyProvisioningPage from './pages/FacultyProvisioningPage'
import TimetableManagementPage from './pages/TimetableManagementPage'

function AuthenticatedLayout({ session, onLogout, unreadCount }) {
  return (
    <div className="app-shell">
      <AppSidebar role={session?.user?.role} unreadCount={unreadCount} />
      <div className="app-shell__main">
        <AppNavbar session={session} unreadCount={unreadCount} onLogout={onLogout} />
        <div className="app-shell__content"><Outlet /></div>
      </div>
    </div>
  )
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
    <Route path="/" element={session ? <Navigate to={dashboardPath(session.user?.role)} replace /> : <AuthenticationLandingPage />} />
    <Route path="/login" element={session ? <Navigate to={dashboardPath(session.user?.role)} replace /> : <AuthenticationLandingPage />} />
    <Route path="/login/student" element={session ? <Navigate to={dashboardPath(session.user?.role)} replace /> : <LoginPage selectedRole="STUDENT" onAuthenticated={setSession} />} />
    <Route path="/login/faculty" element={session ? <Navigate to={dashboardPath(session.user?.role)} replace /> : <LoginPage selectedRole="FACULTY" onAuthenticated={setSession} />} />
    <Route path="/login/coordinator" element={session ? <Navigate to={dashboardPath(session.user?.role)} replace /> : <LoginPage selectedRole="COMMITTEE_COORDINATOR" onAuthenticated={setSession} />} />
    <Route path="/login/administrator" element={session ? <Navigate to={dashboardPath(session.user?.role)} replace /> : <LoginPage selectedRole="ADMINISTRATOR" onAuthenticated={setSession} />} />
    <Route path="/register" element={session ? <Navigate to={dashboardPath(session.user?.role)} replace /> : <StudentRegistrationPage />} />
    <Route element={<ProtectedRoute session={session}><AuthenticatedLayout session={session} unreadCount={unreadCount} onLogout={() => setSession(null)} /></ProtectedRoute>}>
      <Route path="/student" element={<ProtectedRoute session={session} allowedRoles={['STUDENT']}><Dashboard kind="student" /></ProtectedRoute>} />
      <Route path="/coordinator" element={<ProtectedRoute session={session} allowedRoles={['COMMITTEE_COORDINATOR', 'FACULTY']}><Dashboard kind="coordinator" /></ProtectedRoute>} />
      <Route path="/admin" element={<ProtectedRoute session={session} allowedRoles={['ADMINISTRATOR']}><Dashboard kind="admin" /></ProtectedRoute>} />
      <Route path="/admin/coordinators" element={<ProtectedRoute session={session} allowedRoles={['ADMINISTRATOR']}><CoordinatorProvisioningPage /></ProtectedRoute>} />
      <Route path="/admin/committees" element={<ProtectedRoute session={session} allowedRoles={['ADMINISTRATOR']}><CommitteeManagementPage /></ProtectedRoute>} />
      <Route path="/admin/faculty" element={<ProtectedRoute session={session} allowedRoles={['ADMINISTRATOR']}><FacultyProvisioningPage /></ProtectedRoute>} />
      <Route path="/admin/attendance" element={<ProtectedRoute session={session} allowedRoles={['ADMINISTRATOR']}><AttendanceReportPage /></ProtectedRoute>} />
      <Route path="/admin/timetable" element={<ProtectedRoute session={session} allowedRoles={['ADMINISTRATOR']}><TimetableManagementPage /></ProtectedRoute>} />
      <Route path="/faculty" element={<ProtectedRoute session={session} allowedRoles={['FACULTY']}><Dashboard kind="faculty" /></ProtectedRoute>} />
      <Route path="/faculty/attendance" element={<ProtectedRoute session={session} allowedRoles={['FACULTY']}><FacultyAttendancePage /></ProtectedRoute>} />
      <Route path="/admin/events" element={<ProtectedRoute session={session} allowedRoles={['ADMINISTRATOR', 'COMMITTEE_COORDINATOR']}><EventManagementPage /></ProtectedRoute>} />
      <Route path="/coordinator/events" element={<ProtectedRoute session={session} allowedRoles={['COMMITTEE_COORDINATOR', 'FACULTY', 'STUDENT']}><EventManagementPage /></ProtectedRoute>} />
      <Route path="/events" element={<ProtectedRoute session={session} allowedRoles={['STUDENT']}><EventsPage /></ProtectedRoute>} />
      <Route path="/events/:id" element={<ProtectedRoute session={session} allowedRoles={['STUDENT']}><EventDetailPage /></ProtectedRoute>} />
      <Route path="/my-registrations" element={<ProtectedRoute session={session} allowedRoles={['STUDENT']}><RegistrationsPage /></ProtectedRoute>} />
      <Route path="/notifications" element={<NotificationsPage onUnreadChange={setUnreadCount} />} />
      <Route path="/" element={<Navigate to={dashboardPath(session?.user?.role)} replace />} />
      <Route path="*" element={<Navigate to={dashboardPath(session?.user?.role)} replace />} />
    </Route>
  </Routes></BrowserRouter>
}

export default App
