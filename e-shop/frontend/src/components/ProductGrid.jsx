import { Link } from 'react-router-dom'
import './ProductGrid.css'

export function ProductGrid({ products, promotion }) {
  if (!products.length) {
    return <p className="product-grid-empty">No products match your filters.</p>
  }

  return (
    <div className="product-grid">
      {products.map((p) => (
        <article key={p.id} className="product-card">
          <Link to={`/shop/${p.id}`} className="product-card-link">
            <div className="product-card-image">
              {promotion?.productId === p.id ? (
                <span className="product-card-promo-badge">
                  Coffee of the Day -{promotion.discountPercent}%
                </span>
              ) : null}
              {p.imageUrl ? (
                <img src={p.imageUrl} alt={p.name} />
              ) : (
                <div className="product-card-placeholder" />
              )}
            </div>
            <div className="product-card-body">
              <span className="product-card-category">
                {p.categoryRef?.categoryName || p.category || 'Product'}
              </span>
              <h3 className="product-card-name">{p.name}</h3>
              <p className="product-card-price">
                {promotion?.productId === p.id ? (
                  <>
                    <span className="product-card-price-old">₹{Number(p.price).toFixed(2)}</span>{' '}
                    ₹{(Number(p.price) * (100 - Number(promotion.discountPercent || 0)) / 100).toFixed(2)}
                  </>
                ) : (
                  <>₹{Number(p.price).toFixed(2)}</>
                )}
              </p>
            </div>
          </Link>
        </article>
      ))}
    </div>
  )
}
