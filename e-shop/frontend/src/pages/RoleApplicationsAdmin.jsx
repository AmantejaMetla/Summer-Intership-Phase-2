import { useEffect, useMemo, useState } from 'react'
import { useAuth } from '../context/AuthContext'
import { apiJson } from '../api/client'
import './InfoPages.css'

const STATUS_FILTERS = ['ALL', 'SUBMITTED', 'UNDER_REVIEW', 'APPROVED', 'REJECTED']

function pretty(value) {
  return String(value || '')
    .toLowerCase()
    .split('_')
    .map((w) => w.charAt(0).toUpperCase() + w.slice(1))
    .join(' ')
}

export function RoleApplicationsAdmin() {
  const { user } = useAuth()
  const [items, setItems] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [statusFilter, setStatusFilter] = useState('ALL')
  const [reviewNotes, setReviewNotes] = useState({})

  const isAdmin = useMemo(
    () => (user?.roles || []).some((r) => String(r).toLowerCase() === 'admin'),
    [user?.roles]
  )

  async function loadAll() {
    try {
      setError('')
      const data = await apiJson('/api/users/role-applications/admin')
      setItems(Array.isArray(data) ? data : [])
    } catch (e) {
      setError(e.message || 'Failed to load role applications.')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    if (!isAdmin) {
      setLoading(false)
      return
    }
    loadAll()
  }, [isAdmin])

  async function review(id, action) {
    try {
      setError('')
      await apiJson(`/api/users/role-applications/admin/${id}`, {
        method: 'PATCH',
        body: JSON.stringify({
          action,
          notes: reviewNotes[id] || '',
        }),
      })
      await loadAll()
    } catch (e) {
      setError(e.message || 'Failed to review application.')
    }
  }

  if (!user) {
    return (
      <section className="info-page">
        <h1>Role Applications Admin</h1>
        <p className="info-muted">Please login as admin.</p>
      </section>
    )
  }

  if (!isAdmin) {
    return (
      <section className="info-page">
        <h1>Role Applications Admin</h1>
        <p className="info-error">Access denied. Admin role required.</p>
      </section>
    )
  }

  const visible = statusFilter === 'ALL'
    ? items
    : items.filter((i) => i.status === statusFilter)

  return (
    <section className="info-page">
      <h1>Role Applications Admin</h1>
      <p className="info-muted">Review merchant and delivery agent applications.</p>
      {error && <p className="info-error">{error}</p>}
      <div className="info-actions">
        <label className="info-muted">
          Status filter:{' '}
          <select value={statusFilter} onChange={(e) => setStatusFilter(e.target.value)}>
            {STATUS_FILTERS.map((s) => <option key={s} value={s}>{pretty(s)}</option>)}
          </select>
        </label>
        <button type="button" className="btn-shop btn-secondary" onClick={loadAll}>Refresh</button>
      </div>

      {loading ? <p className="info-muted">Loading applications...</p> : null}
      {!loading && visible.length === 0 ? <p className="info-muted">No applications found.</p> : null}

      {!loading && visible.length > 0 && (
        <div className="info-card" style={{ marginTop: '1rem' }}>
          {visible.map((item) => (
            <div key={item.id} className="ticket-admin-row">
              <div className="ticket-admin-main">
                <div>
                  <strong>#{item.id}</strong> - {pretty(item.requestedRole)} / User {item.userId}
                </div>
                <p className="info-muted">
                  {item.fullName} | {item.email} | {item.phone}
                </p>
                <p className="info-muted">Gov ID: {item.governmentId || 'N/A'}</p>
                {item.shopName ? <p className="info-muted">Shop: {item.shopName}</p> : null}
                {item.drivingLicense ? <p className="info-muted">DL: {item.drivingLicense}</p> : null}
                <p className="info-muted">Experience: {item.yearsExperience || 0} years</p>
                <p className="info-muted">Check: {item.credentialsSummary || 'N/A'}</p>
                <label>
                  <span className="info-muted">Admin notes</span>
                  <textarea
                    rows={2}
                    value={reviewNotes[item.id] ?? ''}
                    onChange={(e) => setReviewNotes((s) => ({ ...s, [item.id]: e.target.value }))}
                    placeholder="Optional notes"
                  />
                </label>
              </div>
              <div className="ticket-admin-actions">
                <span className={`ticket-status ticket-status--${String(item.status || '').toLowerCase()}`}>{pretty(item.status)}</span>
                <button type="button" className="btn-shop" onClick={() => review(item.id, 'APPROVE')}>Approve</button>
                <button type="button" className="btn-shop btn-secondary" onClick={() => review(item.id, 'REVIEW')}>Review</button>
                <button type="button" className="btn-shop btn-secondary" onClick={() => review(item.id, 'REJECT')}>Reject</button>
              </div>
            </div>
          ))}
        </div>
      )}
    </section>
  )
}
