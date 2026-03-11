import { useEffect, useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import { apiJson } from '../api/client'
import { useAuth } from '../context/AuthContext'
import './InfoPages.css'

function isIndianMobile(phone) {
  const normalized = String(phone || '').replace(/\s+/g, '')
  return /^(\+91|91)?[6-9]\d{9}$/.test(normalized)
}

export function Profile() {
  const { user } = useAuth()
  const [form, setForm] = useState({ fullName: '', phone: '', address: '' })
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState('')
  const [success, setSuccess] = useState('')

  useEffect(() => {
    let cancelled = false
    async function loadProfile() {
      if (!user) {
        setLoading(false)
        return
      }
      try {
        const data = await apiJson('/api/users/me')
        if (!cancelled) {
          setForm({
            fullName: data.fullName || '',
            phone: data.phone || '',
            address: data.address || '',
          })
        }
      } catch (e) {
        if (!cancelled) setError(e.message || 'Failed to load profile.')
      } finally {
        if (!cancelled) setLoading(false)
      }
    }
    loadProfile()
    return () => { cancelled = true }
  }, [user])

  const smsEligible = useMemo(() => isIndianMobile(form.phone), [form.phone])

  async function saveProfile(e) {
    e.preventDefault()
    setSaving(true)
    setError('')
    setSuccess('')
    try {
      const updated = await apiJson('/api/users/me', {
        method: 'PUT',
        body: JSON.stringify(form),
      })
      setForm({
        fullName: updated.fullName || '',
        phone: updated.phone || '',
        address: updated.address || '',
      })
      setSuccess('Profile updated successfully.')
    } catch (err) {
      setError(err.message || 'Failed to save profile.')
    } finally {
      setSaving(false)
    }
  }

  if (!user) {
    return (
      <section className="info-page">
        <h1>Profile</h1>
        <p className="info-muted">Please log in to view and edit your profile.</p>
        <Link to="/login" className="btn-shop">Go to Login</Link>
      </section>
    )
  }

  if (loading) {
    return (
      <section className="info-page">
        <h1>Profile</h1>
        <p className="info-muted">Loading profile...</p>
      </section>
    )
  }

  return (
    <section className="info-page">
      <h1>Profile</h1>
      <p className="info-muted">User ID: {user.userId} | Roles: {user.roles?.join(', ') || 'customer'}</p>
      {error && <p className="info-error">{error}</p>}
      {success && <p className="info-success">{success}</p>}

      <form className="info-form" onSubmit={saveProfile}>
        <label>
          Full Name
          <input
            type="text"
            value={form.fullName}
            onChange={(e) => setForm((f) => ({ ...f, fullName: e.target.value }))}
            placeholder="Your full name"
          />
        </label>
        <label>
          Phone
          <input
            type="tel"
            value={form.phone}
            onChange={(e) => setForm((f) => ({ ...f, phone: e.target.value }))}
            placeholder="+91XXXXXXXXXX"
          />
        </label>
        <label>
          Address
          <textarea
            rows={3}
            value={form.address}
            onChange={(e) => setForm((f) => ({ ...f, address: e.target.value }))}
            placeholder="Delivery address"
          />
        </label>
        <button type="submit" className="btn-shop" disabled={saving}>
          {saving ? 'Saving...' : 'Save Profile'}
        </button>
      </form>

      <p className="info-note">
        SMS eligible: {smsEligible ? 'Yes (Indian mobile detected)' : 'No (enter a valid Indian number to enable SMS updates)'}
      </p>
    </section>
  )
}
