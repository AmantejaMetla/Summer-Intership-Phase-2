import { useEffect, useMemo, useState } from 'react'
import { apiJson } from '../api/client'
import { useAuth } from '../context/AuthContext'
import './InfoPages.css'

const ALL_STATUSES = ['OPEN', 'IN_PROGRESS', 'RESOLVED', 'CLOSED']

export function SupportAdmin() {
  const { user } = useAuth()
  const [tickets, setTickets] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [statusFilter, setStatusFilter] = useState('ALL')
  const [activeTicketId, setActiveTicketId] = useState(null)
  const [messages, setMessages] = useState([])
  const [reply, setReply] = useState('')
  const [sending, setSending] = useState(false)

  const isAdmin = useMemo(() => (user?.roles || []).some((r) => String(r).toLowerCase() === 'admin'), [user?.roles])

  async function loadTickets() {
    setError('')
    try {
      const data = await apiJson('/api/support/tickets/admin')
      setTickets(Array.isArray(data) ? data : [])
    } catch (e) {
      setError(e.message || 'Failed to load support tickets.')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    if (!isAdmin) {
      setLoading(false)
      return
    }
    loadTickets()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [isAdmin])

  async function updateStatus(ticketId, status) {
    try {
      await apiJson(`/api/support/tickets/${ticketId}`, {
        method: 'PATCH',
        body: JSON.stringify({ status }),
      })
      await loadTickets()
    } catch (e) {
      setError(e.message || 'Failed to update ticket status.')
    }
  }

  async function loadMessages(ticketId) {
    if (!ticketId) return
    try {
      const data = await apiJson(`/api/support/tickets/${ticketId}/messages`)
      setMessages(Array.isArray(data) ? data : [])
    } catch (e) {
      setError(e.message || 'Failed to load ticket messages.')
    }
  }

  useEffect(() => {
    if (!activeTicketId) return undefined
    loadMessages(activeTicketId)
    const interval = setInterval(() => loadMessages(activeTicketId), 4000)
    return () => clearInterval(interval)
  }, [activeTicketId])

  async function sendReply(e) {
    e.preventDefault()
    if (!activeTicketId || !reply.trim()) return
    setSending(true)
    try {
      await apiJson(`/api/support/tickets/${activeTicketId}/messages`, {
        method: 'POST',
        body: JSON.stringify({ body: reply.trim() }),
      })
      setReply('')
      await loadMessages(activeTicketId)
    } catch (e2) {
      setError(e2.message || 'Failed to send reply.')
    } finally {
      setSending(false)
    }
  }

  if (!user) {
    return (
      <section className="info-page">
        <h1>Support Admin</h1>
        <p className="info-muted">Please login as admin.</p>
      </section>
    )
  }

  if (!isAdmin) {
    return (
      <section className="info-page">
        <h1>Support Admin</h1>
        <p className="info-error">Access denied. Admin role required.</p>
      </section>
    )
  }

  const visible = statusFilter === 'ALL' ? tickets : tickets.filter((t) => t.status === statusFilter)

  return (
    <section className="info-page">
      <h1>Support Admin Dashboard</h1>
      <p className="info-muted">Manage support tickets submitted from Contact page.</p>
      <div className="info-actions">
        <label className="info-muted">
          Status filter:{' '}
          <select value={statusFilter} onChange={(e) => setStatusFilter(e.target.value)}>
            <option value="ALL">All</option>
            {ALL_STATUSES.map((s) => <option key={s} value={s}>{s}</option>)}
          </select>
        </label>
        <button type="button" className="btn-shop btn-secondary" onClick={loadTickets}>Refresh</button>
      </div>

      {loading ? <p className="info-muted">Loading tickets...</p> : null}
      {error ? <p className="info-error">{error}</p> : null}

      {!loading && visible.length === 0 ? (
        <p className="info-muted">No tickets found.</p>
      ) : (
        <div className="info-card" style={{ marginTop: '1rem' }}>
          {visible.map((t) => (
            <div key={t.id} className="ticket-admin-row">
              <div className="ticket-admin-main">
                <div>
                  <strong>#{t.id}</strong> - {t.subject}
                </div>
                <p className="info-muted">
                  User: {t.userId} | Email: {t.email} | Order: {t.orderId || 'N/A'}
                </p>
                <p>{t.message}</p>
                {t.screenshotName ? <p className="info-muted">Attachment: {t.screenshotName}</p> : null}
                <button
                  type="button"
                  className="btn-shop btn-secondary"
                  onClick={() => setActiveTicketId(t.id)}
                >
                  {activeTicketId === t.id ? 'Chat Open' : 'Open Chat'}
                </button>
              </div>
              <div className="ticket-admin-actions">
                <span className={`ticket-status ticket-status--${String(t.status || '').toLowerCase()}`}>{t.status}</span>
                <select value={t.status} onChange={(e) => updateStatus(t.id, e.target.value)}>
                  {ALL_STATUSES.map((s) => <option key={s} value={s}>{s}</option>)}
                </select>
              </div>
            </div>
          ))}
        </div>
      )}

      {activeTicketId && (
        <div className="info-card" style={{ marginTop: '1rem' }}>
          <h3>Ticket Chat #{activeTicketId}</h3>
          <div className="chat-thread">
            {messages.length === 0 ? (
              <p className="info-muted">No chat messages yet.</p>
            ) : (
              messages.map((m) => (
                <div
                  key={m.id}
                  className={`chat-bubble ${m.senderType === 'ADMIN' ? 'chat-bubble--user' : 'chat-bubble--support'}`}
                >
                  <p>{m.body}</p>
                  <small className="info-muted">{m.senderType} via {m.channel}</small>
                </div>
              ))
            )}
          </div>
          <form className="info-form" onSubmit={sendReply}>
            <label>
              Reply to customer
              <textarea
                rows={3}
                value={reply}
                onChange={(e) => setReply(e.target.value)}
                placeholder="Type support response..."
                required
              />
            </label>
            <div className="info-actions">
              <button type="submit" className="btn-shop" disabled={sending}>
                {sending ? 'Sending...' : 'Send Reply'}
              </button>
            </div>
          </form>
        </div>
      )}
    </section>
  )
}
