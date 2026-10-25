export const asList = (data) => {
  if (Array.isArray(data)) return data
  if (Array.isArray(data?.content)) return data.content
  if (Array.isArray(data?.items)) return data.items
  if (Array.isArray(data?.data)) return data.data
  return []
}

export const displayDate = (value, options = { dateStyle: 'medium', timeStyle: 'short' }) => {
  if (!value) return 'Not specified'
  const date = new Date(value)
  return Number.isNaN(date.getTime()) ? value : new Intl.DateTimeFormat(undefined, options).format(date)
}

export const eventId = (event) => event?.id || event?.eventId
export const eventTitle = (event) => event?.title || event?.eventTitle || 'Untitled event'
export const eventStart = (event) => event?.startsAt || event?.startDateTime || event?.startTime
export const eventDeadline = (event) => event?.registrationDeadline || event?.registrationClosesAt || event?.deadline
export const eventCommittee = (event) => event?.committeeName || event?.committee?.committeeName || event?.committee?.name || 'Campus committee'
export const seatsRemaining = (event) => event?.availableSeats ?? event?.seatsRemaining ?? (event?.capacity != null ? Math.max(event.capacity - (event.registeredCount || event.registrationCount || 0), 0) : null)

export const isPast = (date) => date && new Date(date).getTime() < Date.now()
