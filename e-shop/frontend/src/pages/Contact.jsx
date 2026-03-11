import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { apiJson } from '../api/client'
import { useAuth } from '../context/AuthContext'
import './InfoPages.css'

export function Contact() {
  const { user } = useAuth()
  const [apiStatus, setApiStatus] = useState('Checking...')
  const [submitted, setSubmitted] = useState(false)
  const [submitting, setSubmitting] = useState(false)
  const [submitError, setSubmitError] = useState('')
  const [tickets, setTickets] = useState([])
  const [activeTicketId, setActiveTicketId] = useState(null)
  const [messages, setMessages] = useState([])
  const [chatText, setChatText] = useState('')
  const [chatSending, setChatSending] = useState(false)
  const [form, setForm] = useState({
    fullName: '',
    email: '',
    phone: '',
    orderId: '',
    subject: '',
    message: '',
    screenshot: null,
  })

  useEffect(() => {
    let cancelled = false
    async function check() {
      try {
        await apiJson('/api/products/categories')
        if (!cancelled) setApiStatus('Online')
      } catch (_) {
        if (!cancelled) setApiStatus('Offline')
      }
    }
    check()
    return () => { cancelled = true }
  }, [])

  function clearForm() {
    setForm({
      fullName: '',
      email: '',
      phone: '',
      orderId: '',
      subject: '',
      message: '',
      screenshot: null,
    })
    setSubmitted(false)
    setSubmitError('')
  }

  async function loadMyTickets() {
    if (!user) return
    try {
      const data = await apiJson('/api/support/tickets/me')
      setTickets(Array.isArray(data) ? data : [])
    } catch (_) {
      // non-blocking
    }
  }

  useEffect(() => {
    loadMyTickets()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [user?.userId])

  async function submitForm(e) {
    e.preventDefault()
    setSubmitted(false)
    setSubmitError('')
    if (!user) {
      setSubmitError('Please login to submit a support ticket.')
      return
    }
    setSubmitting(true)
    try {
      await apiJson('/api/support/tickets', {
        method: 'POST',
        body: JSON.stringify({
          fullName: form.fullName,
          email: form.email,
          phone: form.phone || null,
          orderId: form.orderId || null,
          subject: form.subject,
          message: form.message,
          screenshotName: form.screenshot ? form.screenshot.name : null,
        }),
      })
      await loadMyTickets()
      clearForm()
      setSubmitted(true)
    } catch (err) {
      setSubmitError(err.message || 'Failed to submit ticket.')
    } finally {
      setSubmitting(false)
    }
  }

  async function loadMessages(ticketId) {
    if (!ticketId) return
    try {
      const data = await apiJson(`/api/support/tickets/${ticketId}/messages`)
      setMessages(Array.isArray(data) ? data : [])
    } catch (_) {
      // best effort
    }
  }

  useEffect(() => {
    if (!activeTicketId) return undefined
    loadMessages(activeTicketId)
    const interval = setInterval(() => loadMessages(activeTicketId), 4000)
    return () => clearInterval(interval)
  }, [activeTicketId])

  async function sendChatMessage(e) {
    e.preventDefault()
    if (!activeTicketId || !chatText.trim()) return
    setChatSending(true)
    try {
      await apiJson(`/api/support/tickets/${activeTicketId}/messages`, {
        method: 'POST',
        body: JSON.stringify({ body: chatText.trim() }),
      })
      setChatText('')
      await loadMessages(activeTicketId)
    } catch (err) {
      setSubmitError(err.message || 'Failed to send chat message.')
    } finally {
      setChatSending(false)
    }
  }

  return (
    <section className="info-page">
      <h1>Contact</h1>
      <p className="info-muted">
        Need help with orders, payments, or account setup? Reach out to support.
      </p>

      <div className="info-grid">
        <article className="info-card">
          <h3>Support channels</h3>
          <p><strong>Business:</strong> E-Shop</p>
          <p><strong>Email:</strong> amantejametla@gmail.com</p>
          <p><strong>Phone:</strong> +91 70211 77291</p>
          <p><strong>Address:</strong> Any place in Mumbai</p>
          <p><strong>Hours:</strong> Mon-Sun, 8:00 AM - 6:00 PM</p>
        </article>
        <article className="info-card">
          <h3>System status</h3>
          <p>Gateway/API status: <strong>{apiStatus}</strong></p>
          <p className="info-muted">Status is checked against live backend endpoints.</p>
        </article>
        <article className="info-card">
          <h3>Quick help</h3>
          <ul>
            <li><a href="/shop">Order Issues</a></li>
            <li><a href="/cart">Payment Problems</a></li>
            <li><a href="/profile">Refund Requests</a></li>
            <li><a href="/shop">Delivery Issues</a></li>
            <li><a href="/profile">Account Support</a></li>
          </ul>
        </article>
        <article className="info-card">
          <h3>Social links</h3>
          <p><a href="https://x.com/AmantejaMetla" target="_blank" rel="noreferrer">Twitter / X</a></p>
          <p><a href="https://www.linkedin.com/in/amanteja-metla-285b73276/" target="_blank" rel="noreferrer">LinkedIn</a></p>
          <p>
            <a
              href="https://wa.me/917021177291?text=Hi%20E-Shop%20Support%2C%20I%20need%20help."
              target="_blank"
              rel="noreferrer"
            >
              Live chat support (WhatsApp)
            </a>
          </p>
        </article>
      </div>

      <div className="info-grid">
        <article className="info-card contact-form-card">
          <h3>Contact form</h3>
          {!user && (
            <p className="info-note">
              Login is required to create support tickets. <Link to="/login">Go to Login</Link>
            </p>
          )}
          <form className="info-form" onSubmit={submitForm}>
            <label>
              Full Name
              <input
                type="text"
                required
                value={form.fullName}
                onChange={(e) => setForm((f) => ({ ...f, fullName: e.target.value }))}
              />
            </label>
            <label>
              Email
              <input
                type="email"
                required
                value={form.email}
                onChange={(e) => setForm((f) => ({ ...f, email: e.target.value }))}
              />
            </label>
            <label>
              Phone Number (optional)
              <input
                type="tel"
                value={form.phone}
                onChange={(e) => setForm((f) => ({ ...f, phone: e.target.value }))}
              />
            </label>
            <label>
              Order ID (optional)
              <input
                type="text"
                value={form.orderId}
                onChange={(e) => setForm((f) => ({ ...f, orderId: e.target.value }))}
              />
            </label>
            <label>
              Subject
              <input
                type="text"
                required
                value={form.subject}
                onChange={(e) => setForm((f) => ({ ...f, subject: e.target.value }))}
              />
            </label>
            <label>
              Message
              <textarea
                rows={4}
                required
                value={form.message}
                onChange={(e) => setForm((f) => ({ ...f, message: e.target.value }))}
              />
            </label>
            <label>
              Attach screenshot (optional)
              <input
                type="file"
                accept="image/*"
                onChange={(e) => setForm((f) => ({ ...f, screenshot: e.target.files?.[0] || null }))}
              />
            </label>
            <div className="info-actions">
              <button type="submit" className="btn-shop" disabled={submitting}>
                {submitting ? 'Submitting...' : 'Submit'}
              </button>
              <button type="button" className="btn-shop btn-secondary" onClick={clearForm}>Clear</button>
            </div>
          </form>
          {submitError && <p className="info-error">{submitError}</p>}
          {submitted && (
            <p className="info-success">
              Thank you for contacting us. Our support team will get back to you within 24 hours.
            </p>
          )}
        </article>

        <article className="info-card">
          <h3>Map location</h3>
          <div className="contact-map-wrap">
            <iframe
              title="E-Shop Mumbai location"
              src="https://www.google.com/maps?q=Mumbai,India&output=embed"
              loading="lazy"
              referrerPolicy="no-referrer-when-downgrade"
            />
          </div>
          <p className="info-muted">Map is approximate for support and delivery coverage reference.</p>
        </article>
      </div>

      {user && (
        <div className="info-grid">
          <article className="info-card">
            <h3>My support tickets</h3>
            {tickets.length === 0 ? (
              <p className="info-muted">No tickets yet.</p>
            ) : (
              tickets.slice(0, 6).map((t) => (
                <div key={t.id} className="ticket-row">
                  <div>
                    <strong>#{t.id}</strong> - {t.subject}
                    <p className="info-muted">{t.orderId ? `Order: ${t.orderId}` : 'General query'}</p>
                    <button
                      type="button"
                      className="btn-shop btn-secondary"
                      onClick={() => setActiveTicketId(t.id)}
                    >
                      {activeTicketId === t.id ? 'Chat Open' : 'Open Chat'}
                    </button>
                  </div>
                  <span className={`ticket-status ticket-status--${String(t.status || '').toLowerCase()}`}>
                    {t.status}
                  </span>
                </div>
              ))
            )}
          </article>
        </div>
      )}

      {user && activeTicketId && (
        <div className="info-grid">
          <article className="info-card contact-form-card">
            <h3>Live Support Chat - Ticket #{activeTicketId}</h3>
            <div className="chat-thread">
              {messages.length === 0 ? (
                <p className="info-muted">No messages yet.</p>
              ) : (
                messages.map((m) => (
                  <div
                    key={m.id}
                    className={`chat-bubble ${m.senderType === 'USER' ? 'chat-bubble--user' : 'chat-bubble--support'}`}
                  >
                    <p>{m.body}</p>
                    <small className="info-muted">{m.senderType} via {m.channel}</small>
                  </div>
                ))
              )}
            </div>
            <form className="info-form" onSubmit={sendChatMessage}>
              <label>
                Message
                <textarea
                  rows={3}
                  value={chatText}
                  onChange={(e) => setChatText(e.target.value)}
                  placeholder="Type your message..."
                  required
                />
              </label>
              <div className="info-actions">
                <button type="submit" className="btn-shop" disabled={chatSending}>
                  {chatSending ? 'Sending...' : 'Send'}
                </button>
              </div>
            </form>
          </article>
        </div>
      )}
    </section>
  )
}
