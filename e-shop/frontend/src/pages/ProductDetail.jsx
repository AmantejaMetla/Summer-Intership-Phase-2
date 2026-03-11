import { useState, useEffect } from 'react'
import { useParams, Link } from 'react-router-dom'
import { apiJson } from '../api/client'
import { useAuth } from '../context/AuthContext'
import './ProductDetail.css'

export function ProductDetail() {
  const { user } = useAuth()
  const { id } = useParams()
  const [product, setProduct] = useState(null)
  const [loading, setLoading] = useState(true)
  const [adding, setAdding] = useState(false)
  const [message, setMessage] = useState('')
  const [error, setError] = useState('')

  useEffect(() => {
    let cancelled = false
    apiJson(`/api/products/${id}`)
      .then((p) => { if (!cancelled) setProduct(p) })
      .catch(() => { if (!cancelled) setProduct(null) })
      .finally(() => { if (!cancelled) setLoading(false) })
    return () => { cancelled = true }
  }, [id])

  if (loading) return <div className="product-detail-loading">Loading…</div>
  if (!product) return <div className="product-detail-loading">Product not found.</div>

  async function addToCart() {
    if (!user) return
    setAdding(true)
    setError('')
    setMessage('')
    try {
      await apiJson('/api/cart/items', {
        method: 'POST',
        body: JSON.stringify({
          productId: product.id,
          productName: product.name,
          quantity: 1,
          unitPrice: Number(product.price || 0),
        }),
      })
      setMessage('Added to cart.')
    } catch (e) {
      setError(e.message || 'Failed to add item to cart.')
    } finally {
      setAdding(false)
    }
  }

  return (
    <div className="product-detail">
      <Link to="/shop" className="back-link">← Back to Products</Link>
      <div className="product-detail-grid">
        <div className="product-detail-image">
          {product.imageUrl ? (
            <img src={product.imageUrl} alt={product.name} />
          ) : (
            <div className="product-detail-placeholder" />
          )}
        </div>
        <div className="product-detail-info">
          <span className="product-detail-category">
            {product.categoryRef?.categoryName || product.category}
          </span>
          <h1>{product.name}</h1>
          <p className="product-detail-price">₹{Number(product.price).toFixed(2)}</p>
          {product.description && <p className="product-detail-desc">{product.description}</p>}
          <p className="product-detail-stock">
            {product.stockQuantity > 0 ? `In stock (${product.stockQuantity})` : 'Out of stock'}
          </p>
          <div className="product-detail-actions">
            {user ? (
              <button
                type="button"
                className="btn-shop"
                onClick={addToCart}
                disabled={adding || product.stockQuantity <= 0}
              >
                {adding ? 'Adding...' : 'Add to Cart'}
              </button>
            ) : (
              <Link to="/login" className="btn-shop">Login to add to cart</Link>
            )}
            <Link to="/cart" className="btn-shop btn-secondary">View Cart</Link>
          </div>
          {message && <p className="product-detail-success">{message}</p>}
          {error && <p className="product-detail-error">{error}</p>}
        </div>
      </div>
    </div>
  )
}
