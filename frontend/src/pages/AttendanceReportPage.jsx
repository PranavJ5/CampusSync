import { useCallback, useEffect, useState } from 'react'
import api, { getErrorMessage } from '../api/axios'
import { EmptyState, ErrorAlert, LoadingSpinner } from '../components/Feedback'
import { asList } from '../utils/data'

export default function AttendanceReportPage() {
  const [report, setReport] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  const load = useCallback(async () => {
    setLoading(true)
    setError('')
    try {
      setReport((await api.get('/attendance/admin/report')).data)
    } catch (requestError) {
      setError(getErrorMessage(requestError, 'Attendance report could not be loaded.'))
    } finally {
      setLoading(false)
    }
  }, [])
  useEffect(() => { load() }, [load])

  if (loading) return <LoadingSpinner label="Loading attendance report..." />
  return <main className="container page-content">
    <h1 className="h2 mb-1">Attendance reports</h1>
    <p className="text-secondary mb-4">Department, division, subject, and faculty summaries from recorded class sessions.</p>
    {error ? <ErrorAlert message={error} onRetry={load} /> :
      !asList(report?.rows).length ? <EmptyState title="No attendance has been recorded" detail="Administrative summaries will appear after faculty submit attendance for held sessions." /> :
      <>
        <div className="alert alert-info" role="status">Shortage threshold: {report.thresholdPercent}%</div>
        <div className="table-responsive card border-0 shadow-sm">
          <table className="table table-hover align-middle mb-0">
            <thead><tr><th scope="col">Department</th><th scope="col">Division</th><th scope="col">Subject</th><th scope="col">Faculty</th><th scope="col">Held sessions</th><th scope="col">Present</th><th scope="col">Recorded</th><th scope="col">Rate</th></tr></thead>
            <tbody>{report.rows.map((row) => <tr key={`${row.departmentId}-${row.division}-${row.subjectId}-${row.facultyId}`}>
              <td>{row.departmentName}</td><td>{row.division}</td><td>{row.subjectCode} · {row.subjectName}</td><td>{row.facultyName}</td>
              <td>{row.heldClasses}</td><td>{row.attendedRecords}</td><td>{row.recordedStudents}</td>
              <td><span className={row.belowThreshold ? 'text-danger fw-semibold' : ''}>{row.percentage}%</span>{row.belowThreshold && <span className="visually-hidden"> below shortage threshold</span>}</td>
            </tr>)}</tbody>
          </table>
        </div>
      </>}
  </main>
}
