import { useCallback, useEffect, useMemo, useState } from 'react'
import api, { getErrorMessage } from '../api/axios'
import { EmptyState, ErrorAlert, LoadingSpinner } from '../components/Feedback'
import { asList, displayDate } from '../utils/data'

export default function NotificationsPage({ onUnreadChange }) {
  const [notifications, setNotifications] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [filter, setFilter] = useState('all')

  const load = useCallback(async () => {
    setLoading(true); setError('')
    try {
      const items = asList((await api.get('/notifications')).data).sort((a, b) => new Date(b.createdAt || b.sentAt || 0) - new Date(a.createdAt || a.sentAt || 0))
      setNotifications(items); onUnreadChange?.(items.filter((item) => !item.read && !item.isRead).length)
    } catch (requestError) { setError(getErrorMessage(requestError, 'Notifications could not be loaded.')) }
    finally { setLoading(false) }
  }, [onUnreadChange])
  useEffect(() => { load() }, [load])

  const visible = useMemo(() => notifications.filter((item) => filter === 'all' || (filter === 'unread' ? !(item.read || item.isRead) : (item.category || item.type) === filter)), [notifications, filter])
  const categories = useMemo(() => [...new Set(notifications.map((item) => item.category || item.type).filter(Boolean))], [notifications])
  const markRead = async (id) => { try { await api.patch(`/notifications/${id}/read`); load() } catch (requestError) { setError(getErrorMessage(requestError, 'Notification could not be marked as read.')) } }
  const markAll = async () => { try { await api.patch('/notifications/read-all'); load() } catch (requestError) { setError(getErrorMessage(requestError, 'Notifications could not be marked as read.')) } }

  return <main className="container page-content"><div className="d-flex flex-column flex-md-row justify-content-between gap-3 mb-4"><div><h1 className="h2 mb-1">Notifications</h1><p className="text-secondary mb-0">Your latest CampusSync updates, newest first.</p></div><button className="btn btn-outline-primary align-self-md-start" onClick={markAll} disabled={!notifications.some((item) => !(item.read || item.isRead))}>Mark all as read</button></div>
    <div className="mb-3"><label className="visually-hidden" htmlFor="notification-filter">Filter notifications</label><select id="notification-filter" className="form-select w-auto" value={filter} onChange={(e) => setFilter(e.target.value)}><option value="all">All notifications</option><option value="unread">Unread only</option>{categories.map((category) => <option key={category} value={category}>{category}</option>)}</select></div>
    {loading ? <LoadingSpinner label="Loading notifications..." /> : error ? <ErrorAlert message={error} onRetry={load} /> : !visible.length ? <EmptyState title={filter === 'unread' ? 'No unread notifications' : 'No notifications found'} detail="Updates about your events and registrations will appear here." /> : <div className="list-group shadow-sm">{visible.map((item, index) => { const read = item.read || item.isRead; return <article className={`list-group-item p-3 p-md-4 ${read ? '' : 'notification--unread'}`} key={item.id || item.notificationId || index}><div className="d-flex gap-3 justify-content-between"><div><div className="d-flex flex-wrap align-items-center gap-2 mb-1"><h2 className="h6 mb-0">{item.title || 'CampusSync update'}</h2><span className="badge text-bg-info">{item.category || item.type || 'General'}</span></div><p className="mb-1">{item.message || item.content}</p><time className="small text-secondary">{displayDate(item.createdAt || item.sentAt)}</time></div>{!read && <button className="btn btn-sm btn-outline-primary align-self-start flex-shrink-0" onClick={() => markRead(item.id || item.notificationId)}>Mark as read</button>}</div></article>})}</div>}
  </main>
}
