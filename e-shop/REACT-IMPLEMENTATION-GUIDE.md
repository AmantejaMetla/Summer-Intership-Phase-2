# React Implementation Guide – Learning Walkthrough

This document explains how the E-Shop React frontend is built so you can follow the code and reinforce basic React concepts. Read it after (or while) browsing the `frontend/` folder.

---

## 1. Project structure

```
frontend/
  index.html          → Entry HTML; loads main.jsx
  package.json        → Dependencies (react, react-router-dom, vite)
  vite.config.js      → Vite config; proxy /api to backend
  src/
    main.jsx          → Renders App inside Router + AuthProvider
    App.jsx           → Defines all routes (paths → components)
    index.css         → Global CSS variables (colors, font)
    api/
      client.js       → fetch wrapper; adds Authorization header from localStorage
    context/
      AuthContext.jsx → Holds user/token; login/logout
    components/
      Header.jsx      → Logo, nav links, cart, login/logout
      Layout.jsx      → Header + outlet for current page
      ShopFilters.jsx → Left sidebar: category, price, in stock
      ProductGrid.jsx → Grid of product cards
    pages/
      Home.jsx        → Hero + floating animation
      Shop.jsx       → Filters + product list + sort
      ProductDetail.jsx → Single product
      Login.jsx      → Login form
      Register.jsx    → Register form
      About.jsx, Contact.jsx, Profile.jsx, Cart.jsx → Placeholders
```

**Concepts:** Single-page app (SPA), one `index.html`; React renders into `#root`. Routes map URLs to components. Shared state (auth) lives in context.

---

## 2. How the app boots (main.jsx)

- `ReactDOM.createRoot(document.getElementById('root')).render(...)` mounts the app.
- `BrowserRouter` lets `react-router-dom` use the URL (e.g. `/shop`).
- `AuthProvider` wraps the app so any component can call `useAuth()` to get `user`, `login`, `logout`.

**Concepts:** Component tree, providers, routing.

---

## 3. Routes (App.jsx)

- `Routes` / `Route`: path like `"/shop"` → component `<Shop />`.
- `Layout` wraps most pages: every route under `element={<Layout />}` shows the header and then the child route’s component in `<Outlet />`.
- Login and Register are **outside** Layout so they are full-page forms.

**Concepts:** Declarative routing, layout routes, nested routes, `Outlet`.

---

## 4. Auth (context + API)

- **AuthContext** (`context/AuthContext.jsx`):
  - `user` = `{ userId, accessToken, roles }` or `null`.
  - On load, it reads `localStorage` for `accessToken`, `userId`, `roles` so refresh keeps you “logged in”.
  - `login(data)` saves those to `localStorage` and `setUser`.
  - `logout()` clears `localStorage` and sets `user` to `null`.

- **API client** (`api/client.js`):
  - `apiFetch(path, options)` and `apiJson(path, options)` call `fetch(API_BASE + path)`.
  - They add `Authorization: Bearer <token>` from `localStorage` when present.
  - So every authenticated request automatically sends the token.

- **Login page** (`pages/Login.jsx`):
  - On submit, calls `apiJson('/api/auth/login', { method: 'POST', body: JSON.stringify({ email, password }) })`.
  - On success, calls `login(data)` from context and redirects to `/`.

**Concepts:** Context API, `useContext`, `useState`, `useCallback`; keeping token in localStorage and sending it on requests.

---

## 5. Home page – hero and animation (Home.jsx + Home.css)

- **Layout:** A full-width hero section with a large background word (“Shop”), centered content, and a “Shop now” button that links to `/shop`.
- **Animation:** Several small circles (`.float`) are positioned with CSS and animated with a single `@keyframes float` rule. The animation changes `transform` (translate, scale) and `opacity` over time so the circles drift gently (similar in spirit to floating elements in product hero designs).
- No JavaScript is used for the animation; it’s all CSS. This keeps the code simple and performant.

**Concepts:** Semantic HTML, CSS variables, keyframe animations, links with `react-router-dom`’s `Link`.

---

## 6. Shop page – filters and product grid (Shop.jsx)

- **Data:** `useEffect` runs once on mount and fetches products and categories with `apiJson('/api/products')` and `apiJson('/api/products/categories')`. Results are stored in `products` and `categories` state.
- **Filtering:** `filters` state holds `categoryId`, `minPrice`, `maxPrice`, `inStockOnly`. The displayed list is derived: `filtered = products.filter(...)` then `sorted` by price or name depending on `sort` state. So the list is **derived state**, not stored separately.
- **Sidebar:** `ShopFilters` receives `categories`, `filters`, and `setFilters`. When the user changes a filter, it only updates `filters`; the parent re-renders and recomputes `filtered` and `sorted`.
- **Active filters:** A list of “pills” shows what’s active (e.g. “Category: X”, “Price: 0 - 100”). Each pill has a remove button that clears that filter. “Clear all” resets `filters` to defaults.
- **Sort:** A `<select>` sets `sort`; the same `sorted` array is used to render the grid.

**Concepts:** `useState`, `useEffect`, derived state, controlled inputs, list rendering, conditional rendering.

---

## 7. Left sidebar filters (ShopFilters.jsx)

- **By category:** Buttons for “All” and each category. The active one is determined by `filters.categoryId`. Clicking sets `categoryId` (or `null` for “All”) via `setFilters`.
- **Price:** Two number inputs for min and max. Values are stored in `filters.minPrice` and `filters.maxPrice`.
- **Availability:** One checkbox “In stock only” bound to `filters.inStockOnly`.

All updates go through `setFilters` so the parent owns the single source of truth.

**Concepts:** Lifting state up, controlled components.

---

## 8. Product grid (ProductGrid.jsx)

- Receives `products` (the sorted, filtered array).
- Maps each product to a card: image (or placeholder), category, name, price. Each card is a `Link` to `/shop/:id` so clicking goes to the product detail page.
- Prices are shown in ₹ (rupees) with `Number(p.price).toFixed(2)`.

**Concepts:** Props, mapping over arrays, `Link` and route params.

---

## 9. Product detail (ProductDetail.jsx)

- `useParams()` from React Router gives the `id` from the URL (`/shop/123` → `id = "123"`).
- `useEffect` runs when `id` changes and fetches `GET /api/products/:id` via `apiJson`. Result is stored in `product` state.
- The page shows loading state, “not found” if no product, or image, name, price, description, stock.

**Concepts:** `useParams`, fetching by id, conditional rendering.

---

## 10. Styling approach

- **Global:** `index.css` defines CSS variables (e.g. `--bg-hero`, `--accent`, `--font`) and basic resets. Each component that needs specific styles has a matching `.css` file (e.g. `Home.css`, `Shop.css`). No CSS-in-JS or utility framework; plain CSS for clarity.
- **Layout:** Flexbox and Grid are used for header, hero, shop layout (sidebar + main), and product grid. The design follows the references: soft beige hero, grey sidebar, white cards, green accents.

---

## 11. What to try next (as learning)

- **Cart:** Implement Cart page: call `GET /api/cart` and show items; add “Add to cart” on product detail using `POST /api/cart/items`.
- **Profile:** Call `GET /api/users/me` and `PUT /api/users/me` to view and edit profile.
- **Merchant:** If the user has role `merchant`, show a link to “My products” and call `GET /api/products/my` and `GET /api/orders/merchant/sales`.

---

## Quick concept checklist

| Concept | Where you see it |
|--------|-------------------|
| Components | Every `.jsx` file |
| JSX | Return values like `<div className="...">` |
| Props | e.g. `ShopFilters` gets `categories`, `filters`, `setFilters` |
| State | `useState` in Shop, Login, ProductDetail |
| Effect | `useEffect` for fetching in Shop, ProductDetail |
| Context | `AuthContext`, `useAuth()` in Header, Login, Profile |
| Routing | `App.jsx` routes; `Link`, `useParams`, `useNavigate` |
| Lists | `products.map(...)` in ProductGrid; `categories.map(...)` in ShopFilters |
| Conditional UI | `loading`, `!product`, `activeFilters.length > 0` |
| Forms | Login, Register: controlled inputs + `onSubmit` |
| API calls | `apiJson` in Login, Register, Shop, ProductDetail |

Use this guide together with the code in `frontend/src` to see how each idea is used in a real, small app.
