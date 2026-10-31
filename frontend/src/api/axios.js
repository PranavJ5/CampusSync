import axios from 'axios'
import { clearSession, getToken } from '../auth/session'

const api = axios.create({
  baseURL: '/api',
  headers: { 'Content-Type': 'application/json' },
})

const publicEndpoints = new Set(['/auth/login', '/auth/register', '/public/departments'])

api.interceptors.request.use((config) => {
  const token = getToken()
  const requestPath = config.url?.split('?')[0]
  if (token && !publicEndpoints.has(requestPath)) config.headers.Authorization = `Bearer ${token}`
  return config
})

api.interceptors.response.use(
  (response) => response,
  (error) => {
    const status = error.response?.status
    const requestUrl = error.config?.url || ''
    const isLoginRequest = requestUrl.includes('/auth/login')

    if ((status === 401 || status === 403) && !isLoginRequest) {
      clearSession()
      window.dispatchEvent(new Event('campussync:session-expired'))
      if (window.location.pathname !== '/login') {
        window.location.assign('/login')
      }
    }

    return Promise.reject(error)
  },
)

export const getErrorMessage = (error, fallback = 'Something went wrong. Please try again.') =>
  error.response?.data?.message || error.response?.data?.error || fallback

export default api
