import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { apiJson } from '../api/client'
import { useAuth } from '../context/AuthContext'
import './InfoPages.css'

const ROLE_OPTIONS = [
  { value: 'MERCHANT', label: 'Merchant' },
  { value: 'DELIVERY_AGENT', label: 'Delivery Agent' },
]

function prettyStatus(value) {
  return String(value || '')
    .toLowerCase()
    .split('_')
    .map((w) => w.charAt(0).toUpperCase() + w.slice(1))
    .join(' ')
}

export function RoleApplications() {
  const { user } = useAuth()
  const [form, setForm] = useState({
    requestedRole: 'MERCHANT',
    fullName: '',
    email: '',
    phone: '',
    governmentId: '',
    drivingLicense: '',
    shopName: '',
    yearsExperience: 0,
  })
  const [items, setItems] = useState([])
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const [success, setSuccess] = useState('')

  async function loadMine() {
    if (!user) return
    try {
      const data = await apiJson('/api/users/role-applications/me')
      setItems(Array.isArray(data) ? data : [])
    } catch (e) {
      setError(e.message || 'Failed to load applications.')
    }
  }

  useEffect(() => {
    loadMine()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [user])

  async function handleSubmit(e) {
    e.preventDefault()
    setError('')
    setSuccess('')
    setLoading(true)
    try {
      const payload = {
        ...form,
        yearsExperience: Number(form.yearsExperience || 0),
      }
      const created = await apiJson('/api/users/role-applications', {
        method: 'POST',
        body: JSON.stringify(payload),
      })
      setSuccess(
        created.status === 'APPROVED'
          ? `Application approved. Role granted: ${prettyStatus(created.requestedRole)}. Please logout/login once to refresh your role token.`
          : 'Application submitted. It is under review.'
      )
      await loadMine()
    } catch (e2) {
      setError(e2.message || 'Failed to submit application.')
    } finally {
      setLoading(false)
    }
  }

  if (!user) {
    return (
      <section className="info-page">
        <h1>Apply for Merchant / Delivery Role</h1>
        <p className="info-muted">Please login first.</p>
        <Link to="/login" className="btn-shop">Go to Login</Link>
      </section>
    )
  }

  const isMerchant = form.requestedRole === 'MERCHANT'

  return (
    <section className="info-page">
      <h1>Apply for Merchant / Delivery Role</h1>
      <p className="info-muted">
        Auto-approval rules are simple for this case study:
        valid government ID, Indian mobile, and role-specific credentials.
      </p>
      {error && <p className="info-error">{error}</p>}
      {success && <p className="info-success">{success}</p>}

      <form className="info-form" onSubmit={handleSubmit}>
        <label>
          Role
          <select
            value={form.requestedRole}
            onChange={(e) => setForm((s) => ({ ...s, requestedRole: e.target.value }))}
            required
          >
            {ROLE_OPTIONS.map((opt) => (
              <option key={opt.value} value={opt.value}>{opt.label}</option>
            ))}
          </select>
        </label>

        <label>
          Full Name
          <input
            type="text"
            value={form.fullName}
            onChange={(e) => setForm((s) => ({ ...s, fullName: e.target.value }))}
            required
          />
        </label>

        <label>
          Email
          <input
            type="email"
            value={form.email}
            onChange={(e) => setForm((s) => ({ ...s, email: e.target.value }))}
            required
          />
        </label>

        <label>
          Indian Mobile Number
          <input
            type="text"
            value={form.phone}
            onChange={(e) => setForm((s) => ({ ...s, phone: e.target.value }))}
            required
            placeholder="e.g. 9876543210 or 919876543210"
          />
        </label>

        <label>
          Government ID (8-20 alphanumeric)
          <input
            type="text"
            value={form.governmentId}
            onChange={(e) => setForm((s) => ({ ...s, governmentId: e.target.value }))}
            required
          />
        </label>

        {isMerchant ? (
          <label>
            Shop Name
            <input
              type="text"
              value={form.shopName}
              onChange={(e) => setForm((s) => ({ ...s, shopName: e.target.value }))}
              required
            />
          </label>
        ) : (
          <label>
            Driving License ID
            <input
              type="text"
              value={form.drivingLicense}
              onChange={(e) => setForm((s) => ({ ...s, drivingLicense: e.target.value }))}
              required
            />
          </label>
        )}

        <label>
          Years of Experience
          <input
            type="number"
            min="0"
            value={form.yearsExperience}
            onChange={(e) => setForm((s) => ({ ...s, yearsExperience: e.target.value }))}
            required
          />
        </label>

        <button type="submit" className="btn-shop" disabled={loading}>
          {loading ? 'Submitting...' : 'Submit Application'}
        </button>
      </form>

      <div className="info-card">
        <h3>My Applications</h3>
        {!items.length ? (
          <p className="info-muted">No applications yet.</p>
        ) : (
          <div className="info-table">
            {items.map((item) => (
              <div key={item.id} className="info-row">
                <div>
                  <strong>{prettyStatus(item.requestedRole)}</strong>
                  <p className="info-muted">{item.reviewNotes || 'No notes yet.'}</p>
                </div>
                <div className="info-row-right">{prettyStatus(item.status)}</div>
              </div>
            ))}
          </div>
        )}
      </div>
    </section>
  )
}
