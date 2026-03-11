import { useEffect, useState } from 'react'
import { apiJson } from '../api/client'
import './InfoPages.css'

export function About() {
  const [categories, setCategories] = useState([])
  const [error, setError] = useState('')

  useEffect(() => {
    let cancelled = false
    async function load() {
      try {
        const data = await apiJson('/api/products/categories')
        if (!cancelled) setCategories(Array.isArray(data) ? data : [])
      } catch (e) {
        if (!cancelled) setError(e.message || 'Unable to load live category data.')
      }
    }
    load()
    return () => { cancelled = true }
  }, [])

  return (
    <section className="info-page">
      <h1>Why I built E-Shop</h1>
      <p className="info-muted">
        I chose this project to learn how a real commerce platform works end-to-end: authentication, catalog, cart,
        orders, payments, and role-based control across multiple services. This project matters to me because it
        combines both engineering depth (microservices, gateway, queues) and real product behavior users can feel.
      </p>

      <div className="info-grid">
        <article className="info-card">
          <h3>Why this architecture</h3>
          <ul>
            <li>Microservices for clear ownership and easier scaling later</li>
            <li>Eureka + API Gateway for service discovery and unified routing</li>
            <li>JWT security so roles and identity are enforced at API level</li>
            <li>Razorpay integration in test mode for safe payment testing</li>
          </ul>
        </article>
        <article className="info-card">
          <h3>Live catalog data</h3>
          {error ? (
            <p className="info-error">{error}</p>
          ) : (
            <>
              <p>Categories from backend: <strong>{categories.length}</strong></p>
              {categories.length > 0 && (
                <p className="info-muted">
                  {categories
                    .slice(0, 6)
                    .map((c) => (typeof c === 'string' ? c : c?.categoryName || c?.name || 'Category'))
                    .join(' • ')}
                </p>
              )}
            </>
          )}
        </article>
        <article className="info-card">
          <h3>Core APIs and why they exist</h3>
          <ul>
            <li><code>/api/auth</code>: login/register and JWT issuance</li>
            <li><code>/api/auth/verify-email</code>: email OTP verification before account activation</li>
            <li><code>/api/auth/totp/*</code>: Google Authenticator setup + 2FA confirmation</li>
            <li><code>/api/auth/forgot-password/*</code>: OTP + 2FA secured password reset flow</li>
            <li><code>/api/products</code>: catalog browsing and product details</li>
            <li><code>/api/cart</code>: user cart state before checkout</li>
            <li><code>/api/orders</code>: order creation and lifecycle</li>
            <li><code>/api/payments</code>: Razorpay order creation + verification</li>
            <li><code>/api/users/me</code>: profile and delivery contact info</li>
            <li><code>/api/support/tickets</code>: create support ticket from contact form</li>
            <li><code>/api/support/tickets/me</code>: logged-in user's submitted tickets</li>
            <li><code>/api/support/tickets/admin</code>: admin queue for ticket operations</li>
            <li><code>/api/support/tickets/{'{id}'}/messages</code>: two-way ticket chat messages</li>
            <li><code>/api/support/whatsapp/webhook</code>: Meta webhook bridge for inbound WhatsApp replies</li>
          </ul>
        </article>
      </div>
    </section>
  )
}
