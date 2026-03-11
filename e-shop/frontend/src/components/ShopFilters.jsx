import './ShopFilters.css'

export function ShopFilters({ categories, filters, setFilters }) {
  return (
    <div className="shop-filters">
      <div className="filter-block">
        <h3 className="filter-block-title">By Category</h3>
        <ul className="filter-list">
          <li>
            <button
              type="button"
              className={filters.categoryId == null ? 'active' : ''}
              onClick={() => setFilters((f) => ({ ...f, categoryId: null }))}
            >
              All
            </button>
          </li>
          {categories.map((c) => (
            <li key={c.id}>
              <button
                type="button"
                className={filters.categoryId === c.id ? 'active' : ''}
                onClick={() => setFilters((f) => ({ ...f, categoryId: c.id }))}
              >
                {c.categoryName}
              </button>
            </li>
          ))}
        </ul>
      </div>

      <div className="filter-block">
        <h3 className="filter-block-title">Price</h3>
        <div className="price-inputs">
          <input
            type="number"
            placeholder="Min"
            min="0"
            step="1"
            value={filters.minPrice}
            onChange={(e) => setFilters((f) => ({ ...f, minPrice: e.target.value }))}
          />
          <span>–</span>
          <input
            type="number"
            placeholder="Max"
            min="0"
            step="1"
            value={filters.maxPrice}
            onChange={(e) => setFilters((f) => ({ ...f, maxPrice: e.target.value }))}
          />
        </div>
      </div>

      <div className="filter-block">
        <h3 className="filter-block-title">Availability</h3>
        <label className="filter-checkbox">
          <input
            type="checkbox"
            checked={filters.inStockOnly}
            onChange={(e) => setFilters((f) => ({ ...f, inStockOnly: e.target.checked }))}
          />
          In stock only
        </label>
      </div>
    </div>
  )
}
