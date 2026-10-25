const STORAGE_KEY = 'campusSyncSession'

export function getSession() {
  try {
    const saved = localStorage.getItem(STORAGE_KEY) || sessionStorage.getItem(STORAGE_KEY)
    return saved ? JSON.parse(saved) : null
  } catch {
    return null
  }
}

export function getToken() {
  return getSession()?.token || localStorage.getItem('token') || sessionStorage.getItem('token')
}

export function saveSession(payload, remember = true) {
  const token = payload?.token || payload?.accessToken || payload?.jwt
  if (!token) throw new Error('The login response did not include an access token.')

  const session = {
    token,
    user: payload.user || {
      name: payload.name || payload.fullName || payload.email || 'CampusSync user',
      email: payload.email,
      role: payload.role || payload.userRole || payload.roles?.[0] || 'STUDENT',
    },
  }
  const storage = remember ? localStorage : sessionStorage
  const otherStorage = remember ? sessionStorage : localStorage
  otherStorage.removeItem(STORAGE_KEY)
  otherStorage.removeItem('token')
  storage.setItem(STORAGE_KEY, JSON.stringify(session))
  storage.setItem('token', token)
  return session
}

export function clearSession() {
  localStorage.removeItem(STORAGE_KEY)
  localStorage.removeItem('token')
  sessionStorage.removeItem(STORAGE_KEY)
  sessionStorage.removeItem('token')
}

export function dashboardPath(role) {
  const normalized = String(role || '').toUpperCase()
  if (normalized.includes('ADMIN')) return '/admin'
  if (normalized.includes('COORDINATOR') || normalized.includes('FACULTY')) return '/coordinator'
  return '/student'
}
