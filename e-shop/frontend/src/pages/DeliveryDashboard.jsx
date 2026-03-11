import { useEffect, useMemo, useState } from 'react'
import { apiJson } from '../api/client'
import { useAuth } from '../context/AuthContext'
import './InfoPages.css'

const MAP_START = { x: 12, y: 70 }
const MAP_END = { x: 88, y: 24 }

function pretty(value) {
  return String(value || '')
    .toLowerCase()
    .split('_')
    .map((w) => w.charAt(0).toUpperCase() + w.slice(1))
    .join(' ')
}

export function DeliveryDashboard() {
  const { user } = useAuth()
  const [orders, setOrders] = useState([])
  const [loading, setLoading] = useState(true)
  const [busyId, setBusyId] = useState(null)
  const [error, setError] = useState('')
  const [success, setSuccess] = useState('')

  const isDeliveryOrAdmin = useMemo(
    () => (user?.roles || []).some((r) => ['delivery_agent', 'admin'].includes(String(r).toLowerCase())),
    [user?.roles]
  )

  async function loadBoard() {
    try {
      setError('')
      const data = await apiJson('/api/orders/delivery/board')
      setOrders(Array.isArray(data) ? data : [])
    } catch (e) {
      setError(e.message || 'Failed to load delivery board.')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    if (!isDeliveryOrAdmin) {
      setLoading(false)
      return
    }
    loadBoard()
    const interval = setInterval(loadBoard, 7000)
    return () => clearInterval(interval)
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [isDeliveryOrAdmin])

  async function claim(orderId) {
    setBusyId(orderId)
    setError('')
    setSuccess('')
    try {
      await apiJson(`/api/orders/${orderId}/delivery/claim`, { method: 'POST' })
      setSuccess(`Order #${orderId} claimed.`)
      await loadBoard()
    } catch (e) {
      setError(e.message || 'Failed to claim order.')
    } finally {
      setBusyId(null)
    }
  }

  async function advance(orderId) {
    setBusyId(orderId)
    setError('')
    setSuccess('')
    try {
      await apiJson(`/api/orders/${orderId}/delivery/advance`, { method: 'POST' })
      setSuccess(`Order #${orderId} advanced to next stage.`)
      await loadBoard()
    } catch (e) {
      setError(e.message || 'Failed to advance order status.')
    } finally {
      setBusyId(null)
    }
  }

  if (!user) {
    return (
      <section className="info-page">
        <h1>Delivery Dashboard</h1>
        <p className="info-muted">Please login first.</p>
      </section>
    )
  }

  if (!isDeliveryOrAdmin) {
    return (
      <section className="info-page">
        <h1>Delivery Dashboard</h1>
        <p className="info-error">Access denied. Delivery agent or admin role required.</p>
      </section>
    )
  }

  return (
    <section className="info-page">
      <h1>Delivery Dashboard</h1>
      <p className="info-muted">Claim orders, move status, and monitor simulation progress.</p>
      {error && <p className="info-error">{error}</p>}
      {success && <p className="info-success">{success}</p>}
      {loading ? <p className="info-muted">Loading delivery board...</p> : null}

      {!loading && orders.length === 0 ? (
        <p className="info-muted">No active delivery orders right now.</p>
      ) : (
        <div className="info-card" style={{ marginTop: '1rem' }}>
          {orders.map((order) => (
            <div key={order.id} className="ticket-admin-row">
              <div className="ticket-admin-main">
                <div>
                  <strong>Order #{order.id}</strong> - User {order.userId}
                </div>
                <p className="info-muted">Status: {pretty(order.status)} | Progress: {Number(order.deliveryProgressPercent || 0)}%</p>
                <p className="info-muted">
                  Agent: {order.deliveryAgentName || 'Unassigned'}
                </p>
                <div className="delivery-progress-wrap">
                  <div className="delivery-progress-bar" style={{ width: `${Math.max(0, Math.min(100, Number(order.deliveryProgressPercent || 0)))}%` }} />
                </div>
                <div className="delivery-map" style={{ marginTop: '0.55rem', height: '100px' }}>
                  <div className="delivery-map-road" />
                  <div className="delivery-map-pin delivery-map-pin--shop">Kitchen</div>
                  <div className="delivery-map-pin delivery-map-pin--customer">Customer</div>
                  <div
                    className="delivery-map-rider"
                    style={{
                      left: `${MAP_START.x + ((MAP_END.x - MAP_START.x) * Math.max(0, Math.min(100, Number(order.deliveryProgressPercent || 0)))) / 100}%`,
                      top: `${MAP_START.y + ((MAP_END.y - MAP_START.y) * Math.max(0, Math.min(100, Number(order.deliveryProgressPercent || 0)))) / 100}%`,
                    }}
                    title="Simulated delivery rider"
                  >
                    🚴
                  </div>
                </div>
              </div>
              <div className="ticket-admin-actions">
                <button
                  type="button"
                  className="btn-shop btn-secondary"
                  onClick={() => claim(order.id)}
                  disabled={busyId === order.id}
                >
                  Claim
                </button>
                <button
                  type="button"
                  className="btn-shop"
                  onClick={() => advance(order.id)}
                  disabled={busyId === order.id}
                >
                  Advance
                </button>
              </div>
            </div>
          ))}
        </div>
      )}
    </section>
  )
}
