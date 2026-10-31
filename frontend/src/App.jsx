import { lazy, Suspense, useCallback, useEffect, useState } from 'react'
import { BrowserRouter, Navigate, Outlet, Route, Routes } from 'react-router-dom'
import api from './api/axios'
import { dashboardPath, getSession } from './auth/session'
import AppNavbar from './components/AppNavbar'
import AppSidebar from './components/AppSidebar'
import ProtectedRoute from './components/ProtectedRoute'
import { asList } from './utils/data'
import { LoadingSpinner } from './components/Feedback'
import LoginPage from './pages/LoginPage'
import AuthenticationLandingPage from './pages/AuthenticationLandingPage'
import StudentRegistrationPage from './pages/StudentRegistrationPage'
const Dashboard = lazy(() => import('./pages/Dashboards'))
const EventsPage = lazy(() => import('./pages/EventsPage'))
const EventDetailPage = lazy(() => import('./pages/EventDetailPage'))
const RegistrationsPage = lazy(() => import('./pages/RegistrationsPage'))
const NotificationsPage = lazy(() => import('./pages/NotificationsPage'))
const EventManagementPage = lazy(() => import('./pages/EventManagementPage'))
const CoordinatorProvisioningPage = lazy(() => import('./pages/CoordinatorProvisioningPage'))
const CommitteeManagementPage = lazy(() => import('./pages/CommitteeManagementPage'))
const FacultyAttendancePage = lazy(() => import('./pages/FacultyAttendancePage'))
const AttendanceReportPage = lazy(() => import('./pages/AttendanceReportPage'))
const FacultyProvisioningPage = lazy(() => import('./pages/FacultyProvisioningPage'))
const TimetableManagementPage = lazy(() => import('./pages/TimetableManagementPage'))
const FacultyTimetablePage = lazy(() => import('./pages/FacultyTimetablePage'))

function AuthenticatedLayout({ session, onLogout, unreadCount }) {
  return (
    <div className="app-shell">
      <AppSidebar role={session?.user?.role} unreadCount={unreadCount} />
      <div className="app-shell__main">
        <AppNavbar session={session} unreadCount={session?.token ? unreadCount : 0} onLogout={onLogout} />
        <div className="app-shell__content"><Outlet /></div>
      </div>
    </div>
  )
}

function App() {
  const [session, setSession] = useState(getSession)
  const [unreadCount, setUnreadCount] = useState(0)
  const expireSession = useCallback(() => setSession(null), [])

  useEffect(() => {
    window.addEventListener('campussync:session-expired', expireSession)
    return () => window.removeEventListener('campussync:session-expired', expireSession)
  }, [expireSession])

  useEffect(() => {
    if (!session?.token) return undefined
    let active = true
    api.get('/notifications', { params: { unread: true } })
      .then((response) => { if (active) setUnreadCount(asList(response.data).filter((item) => !item.read && !item.isRead).length) })
      .catch(() => { if (active) setUnreadCount(0) })
    return () => { active = false }
  }, [session])

  return <BrowserRouter><Suspense fallback={<LoadingSpinner label="Loading CampusSync..." />}><Routes>
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
      <Route path="/coordinator/events" element={<ProtectedRoute session={session} allowedRoles={['COMMITTEE_COORDINATOR']}><EventManagementPage /></ProtectedRoute>} />
      <Route path="/faculty/timetable" element={<ProtectedRoute session={session} allowedRoles={['FACULTY']}><FacultyTimetablePage /></ProtectedRoute>} />
      <Route path="/events" element={<ProtectedRoute session={session} allowedRoles={['STUDENT']}><EventsPage /></ProtectedRoute>} />
      <Route path="/events/:id" element={<ProtectedRoute session={session} allowedRoles={['STUDENT']}><EventDetailPage /></ProtectedRoute>} />
      <Route path="/my-registrations" element={<ProtectedRoute session={session} allowedRoles={['STUDENT']}><RegistrationsPage /></ProtectedRoute>} />
      <Route path="/notifications" element={<NotificationsPage onUnreadChange={setUnreadCount} />} />
      <Route path="/" element={<Navigate to={dashboardPath(session?.user?.role)} replace />} />
      <Route path="*" element={<Navigate to={dashboardPath(session?.user?.role)} replace />} />
    </Route>
  </Routes></Suspense></BrowserRouter>
}

export default App
