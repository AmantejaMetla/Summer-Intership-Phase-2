import { Link } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'

export function Header() {
  const { user, logout } = useAuth()
  const isAdmin = (user?.roles || []).some((r) => String(r).toLowerCase() === 'admin')
  const isDeliveryOrAdmin = (user?.roles || []).some((r) => ['delivery_agent', 'admin'].includes(String(r).toLowerCase()))

  return (
    <header className="header">
      <Link to="/" className="header-logo">
        <img src="/images/logo.png" alt="" className="header-logo-img" />
        <span>E-Shop</span>
      </Link>
      <nav className="header-nav">
        <Link to="/">Home</Link>
        <Link to="/shop">Products</Link>
        <Link to="/about">About</Link>
        <Link to="/contact">Contact</Link>
        <Link to="/apply-role">Apply Roles</Link>
      </nav>
      <div className="header-actions">
        {user ? (
          <>
            <Link to="/profile" className="nav-link">Profile</Link>
            {isAdmin && <Link to="/admin/support" className="nav-link">Support Admin</Link>}
            {isAdmin && <Link to="/admin/roles" className="nav-link">Role Admin</Link>}
            {isDeliveryOrAdmin && <Link to="/delivery" className="nav-link">Delivery</Link>}
            <Link to="/cart" className="cart-icon" aria-label="Cart">
              🛒
            </Link>
            <button type="button" className="btn-text" onClick={logout}>Logout</button>
          </>
        ) : (
          <Link to="/login" className="btn-text">Login</Link>
        )}
      </div>
    </header>
  )
}
