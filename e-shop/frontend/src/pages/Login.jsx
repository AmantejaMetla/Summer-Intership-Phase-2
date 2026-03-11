import { useState } from 'react'
import { useNavigate, Link } from 'react-router-dom'
import { apiJson } from '../api/client'
import { useAuth } from '../context/AuthContext'
import './Login.css'

export function Login() {
  const navigate = useNavigate()
  const { login } = useAuth()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [showPassword, setShowPassword] = useState(false)
  const [totpCode, setTotpCode] = useState('')
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  const handleSubmit = async (e) => {
    e.preventDefault()
    setError('')
    setLoading(true)
    try {
      const data = await apiJson('/api/auth/login', {
        method: 'POST',
        body: JSON.stringify({ email, password, totpCode }),
      })
      login(data)
      navigate('/')
    } catch (err) {
      setError(err.message || 'Login failed')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="login-page">
      <form className="login-form" onSubmit={handleSubmit}>
        <h1>Login</h1>
        {error && <p className="form-error">{error}</p>}
        <label>
          Email
          <input
            type="email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            required
            autoComplete="email"
          />
        </label>
        <label>
          Password
          <div className="password-input-wrap">
            <input
              type={showPassword ? 'text' : 'password'}
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              required
              autoComplete="current-password"
            />
            <button
              type="button"
              className="password-toggle-btn"
              onClick={() => setShowPassword((prev) => !prev)}
              aria-label={showPassword ? 'Hide password' : 'Show password'}
              title={showPassword ? 'Hide password' : 'Show password'}
            >
              <svg
                viewBox="0 0 24 24"
                width="18"
                height="18"
                aria-hidden="true"
                focusable="false"
              >
                <path
                  d="M12 5c5.5 0 9.2 4.6 10.3 6-.2.3-.6.8-1.2 1.4"
                  fill="none"
                  stroke="currentColor"
                  strokeWidth="1.8"
                  strokeLinecap="round"
                  strokeLinejoin="round"
                />
                <path
                  d="M4.9 8.1C6.7 6.6 9 5 12 5"
                  fill="none"
                  stroke="currentColor"
                  strokeWidth="1.8"
                  strokeLinecap="round"
                  strokeLinejoin="round"
                />
                <path
                  d="M2 12s3.8 7 10 7c5.8 0 9.5-6.2 10-7-.2-.3-.5-.8-1-1.4"
                  fill="none"
                  stroke="currentColor"
                  strokeWidth="1.8"
                  strokeLinecap="round"
                  strokeLinejoin="round"
                />
                <circle
                  cx="12"
                  cy="12"
                  r="3"
                  fill="none"
                  stroke="currentColor"
                  strokeWidth="1.8"
                />
                {!showPassword && (
                  <path
                    d="M3 3l18 18"
                    fill="none"
                    stroke="currentColor"
                    strokeWidth="1.8"
                    strokeLinecap="round"
                  />
                )}
              </svg>
            </button>
          </div>
        </label>
        <label>
          2FA Code (Google Authenticator)
          <input
            type="text"
            value={totpCode}
            onChange={(e) => setTotpCode(e.target.value)}
            required
            inputMode="numeric"
            pattern="\d{6}"
            placeholder="6-digit code"
          />
          <span className="totp-help">
            Login does not send OTP. Use your authenticator code, or use dev code below.
          </span>
          <button
            type="button"
            className="totp-dev-btn"
            onClick={() => setTotpCode('000000')}
          >
            Use dev code (000000)
          </button>
        </label>
        <button type="submit" disabled={loading}>
          {loading ? 'Logging in…' : 'Login'}
        </button>
        <p className="login-footer">
          Don't have an account? <Link to="/register">Register</Link>
        </p>
        <p className="login-footer">
          Forgot password? <Link to="/forgot-password">Reset here</Link>
        </p>
      </form>
    </div>
  )
}
