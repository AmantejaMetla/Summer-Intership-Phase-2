import { useEffect, useMemo, useRef, useState } from 'react'
import { Link } from 'react-router-dom'
import { Client } from '@stomp/stompjs'
import SockJS from 'sockjs-client'
import { apiJson } from '../api/client'
import { useAuth } from '../context/AuthContext'
import './InfoPages.css'

const RAZORPAY_CHECKOUT_SRC = 'https://checkout.razorpay.com/v1/checkout.js'
const ORDER_STAGES = [
  'RECEIVED',
  'PREPARING_IN_KITCHEN',
  'PICKED_BY_DELIVERY_AGENT',
  'OUT_FOR_DELIVERY',
  'DELIVERED',
]

const MAP_START = { x: 12, y: 70 }
const MAP_END = { x: 88, y: 24 }

function prettyStatus(status) {
  return String(status || '')
    .toLowerCase()
    .split('_')
    .map((word) => word.charAt(0).toUpperCase() + word.slice(1))
    .join(' ')
}

function clampPercent(value) {
  return Math.max(0, Math.min(100, Number(value || 0)))
}

function loadRazorpayScript() {
  return new Promise((resolve) => {
    if (window.Razorpay) {
      resolve(true)
      return
    }
    const script = document.createElement('script')
    script.src = RAZORPAY_CHECKOUT_SRC
    script.onload = () => resolve(true)
    script.onerror = () => resolve(false)
    document.body.appendChild(script)
  })
}

export function Cart() {
  const { user } = useAuth()
  const [cart, setCart] = useState({ items: [] })
  const [profile, setProfile] = useState({ fullName: '', phone: '' })
  const [activeOrder, setActiveOrder] = useState(null)
  const [loading, setLoading] = useState(true)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  const [success, setSuccess] = useState('')
  const [wsConnected, setWsConnected] = useState(false)
  const [promoQuote, setPromoQuote] = useState(null)
  const wsClientRef = useRef(null)

  const subtotal = useMemo(
    () => (cart.items || []).reduce((sum, item) => sum + Number(item.unitPrice || 0) * Number(item.quantity || 0), 0),
    [cart.items]
  )
  const discountAmount = Number(promoQuote?.discountAmount || 0)
  const payableTotal = Number(promoQuote?.finalTotal || subtotal)
  const deliveryPercent = clampPercent(activeOrder?.deliveryProgressPercent)
  const dotX = MAP_START.x + ((MAP_END.x - MAP_START.x) * deliveryPercent) / 100
  const dotY = MAP_START.y + ((MAP_END.y - MAP_START.y) * deliveryPercent) / 100
  const distanceLeftKm = (6 * (100 - deliveryPercent)) / 100

  useEffect(() => {
    let cancelled = false
    async function loadData() {
      if (!user) {
        setLoading(false)
        return
      }
      try {
        const [cartData, profileData] = await Promise.all([
          apiJson('/api/cart'),
          apiJson('/api/users/me').catch(() => null),
        ])
        if (!cancelled) {
          setCart(cartData || { items: [] })
          if (profileData) {
            setProfile({
              fullName: profileData.fullName || '',
              phone: profileData.phone || '',
            })
          }
        }
      } catch (e) {
        if (!cancelled) setError(e.message || 'Failed to load cart.')
      } finally {
        if (!cancelled) setLoading(false)
      }
    }
    loadData()
    return () => { cancelled = true }
  }, [user])

  useEffect(() => {
    if (!activeOrder?.id) return undefined
    const interval = setInterval(async () => {
      try {
        const latest = await apiJson(`/api/orders/${activeOrder.id}`)
        setActiveOrder(latest)
      } catch (_) {
        // Keep current state; polling is best-effort
      }
    }, 5000)
    return () => clearInterval(interval)
  }, [activeOrder?.id])

  useEffect(() => {
    let cancelled = false
    async function quote() {
      if (!cart.items?.length) {
        setPromoQuote(null)
        return
      }
      try {
        const data = await apiJson('/api/products/promotions/quote', {
          method: 'POST',
          body: JSON.stringify({
            items: cart.items.map((i) => ({
              productId: i.productId,
              quantity: i.quantity,
              unitPrice: Number(i.unitPrice || 0),
            })),
          }),
        })
        if (!cancelled) setPromoQuote(data)
      } catch (_) {
        if (!cancelled) setPromoQuote(null)
      }
    }
    quote()
    return () => { cancelled = true }
  }, [cart.items])

  useEffect(() => {
    if (wsClientRef.current) {
      wsClientRef.current.deactivate()
      wsClientRef.current = null
    }
    if (!activeOrder?.id) {
      setWsConnected(false)
      return undefined
    }

    const wsUrl = import.meta.env.VITE_ORDER_WS_URL || '/ws-orders'
    const client = new Client({
      webSocketFactory: () => new SockJS(wsUrl),
      reconnectDelay: 5000,
      onConnect: () => {
        setWsConnected(true)
        client.subscribe(`/topic/orders/${activeOrder.id}`, (message) => {
          try {
            const updatedOrder = JSON.parse(message.body)
            setActiveOrder(updatedOrder)
          } catch (_) {
            // ignore malformed payloads
          }
        })
      },
      onWebSocketClose: () => setWsConnected(false),
      onStompError: () => setWsConnected(false),
    })

    wsClientRef.current = client
    client.activate()
    return () => {
      setWsConnected(false)
      client.deactivate()
    }
  }, [activeOrder?.id])

  async function refreshCart() {
    const updated = await apiJson('/api/cart')
    setCart(updated || { items: [] })
  }

  async function clearCart() {
    setBusy(true)
    setError('')
    setSuccess('')
    try {
      await apiJson('/api/cart', { method: 'DELETE' })
      await refreshCart()
      setSuccess('Cart cleared.')
    } catch (e) {
      setError(e.message || 'Failed to clear cart.')
    } finally {
      setBusy(false)
    }
  }

  async function checkout() {
    if (!cart.items?.length) return
    setBusy(true)
    setError('')
    setSuccess('')
    try {
      const order = await apiJson('/api/orders', {
        method: 'POST',
        body: JSON.stringify({ totalAmount: Number(payableTotal.toFixed(2)) }),
      })

      for (const item of cart.items) {
        await apiJson(`/api/orders/${order.id}/items`, {
          method: 'POST',
          body: JSON.stringify({
            productId: item.productId,
            quantity: item.quantity,
            price: Number(item.unitPrice || 0),
          }),
        })
      }

      const paymentOrder = await apiJson('/api/payments/create-order', {
        method: 'POST',
        body: JSON.stringify({
          orderId: order.id,
          amount: Number(payableTotal.toFixed(2)),
        }),
      })

      const loaded = await loadRazorpayScript()
      if (!loaded || !window.Razorpay) {
        throw new Error('Razorpay checkout script failed to load.')
      }

      const options = {
        key: paymentOrder.razorpayKeyId,
        amount: Math.round(payableTotal * 100),
        currency: 'INR',
        name: 'E-Shop',
        description: `Order #${order.id}`,
        order_id: paymentOrder.razorpayOrderId,
        prefill: {
          name: profile.fullName || '',
          contact: profile.phone || '',
        },
        handler: async (response) => {
          try {
            await apiJson('/api/payments/verify', {
              method: 'POST',
              body: JSON.stringify({
                paymentId: paymentOrder.paymentId,
                razorpayOrderId: response.razorpay_order_id,
                razorpayPaymentId: response.razorpay_payment_id,
                razorpaySignature: response.razorpay_signature,
              }),
            })
            await apiJson('/api/cart', { method: 'DELETE' })
            await refreshCart()
            await apiJson(`/api/orders/${order.id}/delivery/simulate/start`, {
              method: 'POST',
            }).catch(() => null)
            setActiveOrder(order)
            setSuccess('Payment successful and order placed.')
          } catch (verifyError) {
            setError(verifyError.message || 'Payment verification failed.')
          } finally {
            setBusy(false)
          }
        },
        modal: {
          ondismiss: () => {
            setBusy(false)
          },
        },
      }

      const rz = new window.Razorpay(options)
      rz.on('payment.failed', () => {
        setError('Payment failed. Please try again.')
        setBusy(false)
      })
      rz.open()
    } catch (e) {
      setError(e.message || 'Checkout failed.')
      setBusy(false)
    }
  }

  if (!user) {
    return (
      <section className="info-page">
        <h1>Cart</h1>
        <p className="info-muted">Please login to view your cart and checkout.</p>
        <Link to="/login" className="btn-shop">Go to Login</Link>
      </section>
    )
  }

  if (loading) {
    return (
      <section className="info-page">
        <h1>Cart</h1>
        <p className="info-muted">Loading cart...</p>
      </section>
    )
  }

  return (
    <section className="info-page">
      <h1>Cart</h1>
      {error && <p className="info-error">{error}</p>}
      {success && <p className="info-success">{success}</p>}

      {cart.items?.length ? (
        <>
          <div className="info-table">
            {(cart.items || []).map((item) => (
              <div key={item.productId} className="info-row">
                <div>
                  <strong>{item.productName}</strong>
                  <p className="info-muted">Qty: {item.quantity}</p>
                </div>
                <div className="info-row-right">₹{(Number(item.unitPrice) * Number(item.quantity)).toFixed(2)}</div>
              </div>
            ))}
            <div className="info-row info-total">
              <strong>Total</strong>
              <strong className="info-row-right">₹{subtotal.toFixed(2)}</strong>
            </div>
            {discountAmount > 0 && (
              <div className="info-row">
                <div>
                  <strong>Coupon Discount</strong>
                  <p className="info-muted">{promoQuote?.couponCode} - {promoQuote?.message}</p>
                </div>
                <div className="info-row-right">-₹{discountAmount.toFixed(2)}</div>
              </div>
            )}
            <div className="info-row info-total">
              <strong>Payable</strong>
              <strong className="info-row-right">₹{payableTotal.toFixed(2)}</strong>
            </div>
          </div>
          <div className="info-actions">
            <button type="button" className="btn-shop btn-secondary" onClick={clearCart} disabled={busy}>
              Clear Cart
            </button>
            <button type="button" className="btn-shop" onClick={checkout} disabled={busy}>
              {busy ? 'Processing...' : 'Pay with Razorpay'}
            </button>
          </div>
        </>
      ) : (
        <p className="info-muted">Your cart is empty. Go to Products to add items.</p>
      )}

      {activeOrder && (
        <div className="info-card">
          <h3>Latest Order</h3>
          <p>Order ID: {activeOrder.id}</p>
          <p>Status: <strong>{prettyStatus(activeOrder.status)}</strong></p>
          <p>Total: ₹{Number(activeOrder.totalAmount || 0).toFixed(2)}</p>
          {activeOrder.deliveryAgentName && (
            <p>Delivery Agent: <strong>{activeOrder.deliveryAgentName}</strong></p>
          )}
          <div className="delivery-progress-wrap">
            <div
              className="delivery-progress-bar"
              style={{ width: `${deliveryPercent}%` }}
            />
          </div>
          <p className="info-muted">Progress: {deliveryPercent}%</p>
          <div className="delivery-map">
            <div className="delivery-map-road" />
            <div className="delivery-map-pin delivery-map-pin--shop">Kitchen</div>
            <div className="delivery-map-pin delivery-map-pin--customer">Customer</div>
            <div
              className="delivery-map-rider"
              style={{
                left: `${dotX}%`,
                top: `${dotY}%`,
              }}
              title="Simulated delivery rider"
            >
              🚴
            </div>
          </div>
          <p className="info-muted">Simulated distance left: {distanceLeftKm.toFixed(1)} km</p>
          <div className="delivery-stage-list">
            {ORDER_STAGES.map((stage) => {
              const done = ORDER_STAGES.indexOf(stage) <= ORDER_STAGES.indexOf(activeOrder.status)
              return (
                <span key={stage} className={`delivery-stage-chip ${done ? 'done' : ''}`}>
                  {prettyStatus(stage)}
                </span>
              )
            })}
          </div>
          <p className="info-muted">
            {wsConnected ? 'Live updates connected.' : 'Live updates reconnecting... polling fallback every 5 seconds.'}
          </p>
        </div>
      )}
    </section>
  )
}
