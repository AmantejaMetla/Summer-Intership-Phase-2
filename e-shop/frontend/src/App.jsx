import { Routes, Route } from 'react-router-dom'
import { Layout } from './components/Layout'
import { Home } from './pages/Home'
import { Shop } from './pages/Shop'
import { ProductDetail } from './pages/ProductDetail'
import { Login } from './pages/Login'
import { Register } from './pages/Register'
import { About } from './pages/About'
import { Contact } from './pages/Contact'
import { Profile } from './pages/Profile'
import { Cart } from './pages/Cart'
import { SupportAdmin } from './pages/SupportAdmin'
import { ForgotPassword } from './pages/ForgotPassword'
import { RoleApplications } from './pages/RoleApplications'
import { RoleApplicationsAdmin } from './pages/RoleApplicationsAdmin'
import { DeliveryDashboard } from './pages/DeliveryDashboard'

function App() {
  return (
    <Routes>
      <Route path="/" element={<Layout />}>
        <Route index element={<Home />} />
        <Route path="shop" element={<Shop />} />
        <Route path="shop/:id" element={<ProductDetail />} />
        <Route path="about" element={<About />} />
        <Route path="contact" element={<Contact />} />
        <Route path="profile" element={<Profile />} />
        <Route path="cart" element={<Cart />} />
        <Route path="apply-role" element={<RoleApplications />} />
        <Route path="delivery" element={<DeliveryDashboard />} />
        <Route path="admin/roles" element={<RoleApplicationsAdmin />} />
        <Route path="admin/support" element={<SupportAdmin />} />
      </Route>
      <Route path="/login" element={<Login />} />
      <Route path="/register" element={<Register />} />
      <Route path="/forgot-password" element={<ForgotPassword />} />
    </Routes>
  )
}

export default App
