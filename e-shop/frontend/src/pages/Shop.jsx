import { useState, useEffect } from 'react'
import { apiJson } from '../api/client'
import { ShopFilters } from '../components/ShopFilters'
import { ProductGrid } from '../components/ProductGrid'
import './Shop.css'

export function Shop() {
  const [products, setProducts] = useState([])
  const [categories, setCategories] = useState([])
  const [promotion, setPromotion] = useState(null)
  const [loading, setLoading] = useState(true)
  const [apiError, setApiError] = useState(false)
  const [filters, setFilters] = useState({
    categoryId: null,
    minPrice: '',
    maxPrice: '',
    inStockOnly: true,
  })
  const [sort, setSort] = useState('default')

  useEffect(() => {
    let cancelled = false
    async function load() {
      setApiError(false)
      try {
        const [prods, cats, promo] = await Promise.all([
          apiJson('/api/products').catch((e) => { throw e }),
          apiJson('/api/products/categories').catch((e) => { throw e }),
          apiJson('/api/products/promotions/coffee-of-the-day').catch(() => null),
        ])
        if (!cancelled) {
          setProducts(Array.isArray(prods) ? prods : [])
          setCategories(Array.isArray(cats) ? cats : [])
          setPromotion(promo)
        }
      } catch (_) {
        if (!cancelled) {
          setProducts([])
          setCategories([])
          setPromotion(null)
          setApiError(true)
        }
      } finally {
        if (!cancelled) setLoading(false)
      }
    }
    load()
    return () => { cancelled = true }
  }, [])

  const filtered = products.filter((p) => {
    if (filters.categoryId != null && p.categoryRef?.id !== filters.categoryId && p.categoryId !== filters.categoryId) return false
    const price = Number(p.price)
    if (filters.minPrice !== '' && price < Number(filters.minPrice)) return false
    if (filters.maxPrice !== '' && price > Number(filters.maxPrice)) return false
    if (filters.inStockOnly && (p.stockQuantity == null || p.stockQuantity <= 0)) return false
    return true
  })

  const sorted = [...filtered].sort((a, b) => {
    if (sort === 'price-asc') return Number(a.price) - Number(b.price)
    if (sort === 'price-desc') return Number(b.price) - Number(a.price)
    if (sort === 'name') return (a.name || '').localeCompare(b.name || '')
    return 0
  })

  const activeFilters = []
  if (filters.categoryId != null) {
    const cat = categories.find((c) => c.id === filters.categoryId)
    activeFilters.push({ key: 'category', label: cat?.categoryName || 'Category', value: filters.categoryId })
  }
  if (filters.minPrice !== '' || filters.maxPrice !== '') {
    activeFilters.push({
      key: 'price',
      label: 'Price',
      value: `${filters.minPrice || '0'} - ${filters.maxPrice || '∞'}`,
    })
  }
  if (filters.inStockOnly) activeFilters.push({ key: 'stock', label: 'In Stock', value: true })

  const removeFilter = (key) => {
    if (key === 'category') setFilters((f) => ({ ...f, categoryId: null }))
    if (key === 'price') setFilters((f) => ({ ...f, minPrice: '', maxPrice: '' }))
    if (key === 'stock') setFilters((f) => ({ ...f, inStockOnly: false }))
  }

  const clearAllFilters = () => {
    setFilters({
      categoryId: null,
      minPrice: '',
      maxPrice: '',
      inStockOnly: true,
    })
  }

  return (
    <div className="shop-page">
      {apiError && (
        <div className="shop-api-error" role="alert">
          Can't reach the server. Start the backend (Eureka, Gateway, and services) and MySQL. See <strong>SETUP-ON-ANOTHER-LAPTOP.md</strong> or <strong>README.md</strong>.
        </div>
      )}
      <aside className="shop-sidebar">
        <h2 className="filter-title">Filter Options</h2>
        <ShopFilters
          categories={categories}
          filters={filters}
          setFilters={setFilters}
        />
      </aside>
      <div className="shop-main">
        {promotion?.productId ? (
          <div className="shop-promo-banner">
            <strong>{promotion.couponCode}</strong> - {promotion.message}
          </div>
        ) : null}
        <div className="shop-toolbar">
          <p className="shop-results">
            Showing 1–{sorted.length} of {products.length} results
          </p>
          <label className="shop-sort">
            Sort by:
            <select value={sort} onChange={(e) => setSort(e.target.value)}>
              <option value="default">Default Sorting</option>
              <option value="price-asc">Price: Low to High</option>
              <option value="price-desc">Price: High to Low</option>
              <option value="name">Name</option>
            </select>
          </label>
        </div>
        {activeFilters.length > 0 && (
          <div className="active-filters">
            <span className="active-filters-label">Active Filter</span>
            {activeFilters.map((f) => (
              <span key={f.key} className="filter-pill">
                {f.label}: {String(f.value)}
                <button type="button" onClick={() => removeFilter(f.key)} aria-label="Remove">×</button>
              </span>
            ))}
            <button type="button" className="clear-filters" onClick={clearAllFilters}>
              Clear All
            </button>
          </div>
        )}
        {loading ? (
          <p className="shop-loading">Loading products…</p>
        ) : (
          <ProductGrid products={sorted} promotion={promotion} />
        )}
      </div>
    </div>
  )
}
