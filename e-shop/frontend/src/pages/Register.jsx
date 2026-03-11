import { useState } from 'react'
import { useNavigate, Link } from 'react-router-dom'
import { QRCodeSVG } from 'qrcode.react'
import { apiJson } from '../api/client'
import './Login.css'

export function Register() {
  const navigate = useNavigate()
  const [step, setStep] = useState('REGISTER') // REGISTER -> VERIFY_EMAIL -> SETUP_TOTP -> CONFIRM_TOTP
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [emailOtp, setEmailOtp] = useState('')
  const [otpChannel, setOtpChannel] = useState('EMAIL')
  const [phoneNumber, setPhoneNumber] = useState('')
  const [totpSecret, setTotpSecret] = useState('')
  const [otpAuthUri, setOtpAuthUri] = useState('')
  const [totpCode, setTotpCode] = useState('')
  const [error, setError] = useState('')
  const [success, setSuccess] = useState('')
  const [loading, setLoading] = useState(false)

  const handleRegister = async (e) => {
    e.preventDefault()
    setError('')
    setSuccess('')
    setLoading(true)
    try {
      await apiJson('/api/auth/register', {
        method: 'POST',
        body: JSON.stringify({
          email,
          password,
          otpChannel,
          phoneNumber: otpChannel === 'SMS' ? phoneNumber : null,
        }),
      })
      setStep('VERIFY_EMAIL')
      setSuccess(`Registration started. Check your ${otpChannel === 'SMS' ? 'SMS' : 'email'} for OTP.`)
    } catch (err) {
      setError(err.message || 'Registration failed')
    } finally {
      setLoading(false)
    }
  }

  const handleVerifyEmail = async (e) => {
    e.preventDefault()
    setError('')
    setSuccess('')
    setLoading(true)
    try {
      await apiJson('/api/auth/verify-email', {
        method: 'POST',
        body: JSON.stringify({ email, otp: emailOtp }),
      })
      setStep('SETUP_TOTP')
      setSuccess('Email verified. Setup Google Authenticator.')
    } catch (err) {
      setError(err.message || 'Email verification failed')
    } finally {
      setLoading(false)
    }
  }

  const handleSetupTotp = async (e) => {
    e.preventDefault()
    setError('')
    setSuccess('')
    setLoading(true)
    try {
      const data = await apiJson('/api/auth/totp/setup', {
        method: 'POST',
        body: JSON.stringify({ email, password }),
      })
      setTotpSecret(data.secret || '')
      setOtpAuthUri(data.otpAuthUri || '')
      setStep('CONFIRM_TOTP')
      setSuccess('Scan the key in Google Authenticator, then enter current code.')
    } catch (err) {
      setError(err.message || 'TOTP setup failed')
    } finally {
      setLoading(false)
    }
  }

  const handleConfirmTotp = async (e) => {
    e.preventDefault()
    setError('')
    setSuccess('')
    setLoading(true)
    try {
      await apiJson('/api/auth/totp/confirm', {
        method: 'POST',
        body: JSON.stringify({ email, otpCode: totpCode }),
      })
      setSuccess('2FA enabled. Please login with email, password, and 2FA code.')
      navigate('/login')
    } catch (err) {
      setError(err.message || 'TOTP verification failed')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="login-page">
      <form className="login-form" onSubmit={
        step === 'REGISTER' ? handleRegister
          : step === 'VERIFY_EMAIL' ? handleVerifyEmail
            : step === 'SETUP_TOTP' ? handleSetupTotp
              : handleConfirmTotp
      }>
        <h1>Register</h1>
        <p className="login-footer">Step: {step.replace('_', ' ')}</p>
        {error && <p className="form-error">{error}</p>}
        {success && <p className="form-success">{success}</p>}
        <label>
          Email
          <input
            type="email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            required
            autoComplete="email"
            disabled={step !== 'REGISTER'}
          />
        </label>
        <label>
          Password
          <input
            type="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            required
            autoComplete="new-password"
            disabled={step !== 'REGISTER'}
          />
        </label>
        {step === 'REGISTER' && (
          <>
            <label>
              OTP Delivery
              <select value={otpChannel} onChange={(e) => setOtpChannel(e.target.value)}>
                <option value="EMAIL">Email</option>
                <option value="SMS">SMS (India)</option>
              </select>
            </label>
            {otpChannel === 'SMS' && (
              <label>
                Indian Mobile Number
                <input
                  type="tel"
                  value={phoneNumber}
                  onChange={(e) => setPhoneNumber(e.target.value)}
                  required
                  placeholder="+91XXXXXXXXXX or 10-digit"
                />
              </label>
            )}
          </>
        )}
        {step === 'VERIFY_EMAIL' && (
          <label>
            OTP
            <input
              type="text"
              value={emailOtp}
              onChange={(e) => setEmailOtp(e.target.value)}
              required
              inputMode="numeric"
              pattern="\d{6}"
              placeholder="Enter email OTP"
            />
          </label>
        )}
        {step === 'CONFIRM_TOTP' && (
          <>
            {otpAuthUri && (
              <div className="totp-qr-wrap">
                <p className="login-footer">Scan this QR in Google Authenticator</p>
                <QRCodeSVG
                  value={otpAuthUri}
                  size={180}
                  level="M"
                  includeMargin
                  bgColor="#ffffff"
                  fgColor="#111111"
                />
              </div>
            )}
            {totpSecret && (
              <label>
                TOTP Secret (manual setup)
                <input type="text" value={totpSecret} readOnly />
              </label>
            )}
            {otpAuthUri && (
              <p className="login-footer" style={{ wordBreak: 'break-all' }}>
                otpauth URI: {otpAuthUri}
              </p>
            )}
            <label>
              Google Authenticator Code
              <input
                type="text"
                value={totpCode}
                onChange={(e) => setTotpCode(e.target.value)}
                required
                inputMode="numeric"
                pattern="\d{6}"
                placeholder="6-digit code"
              />
            </label>
          </>
        )}
        <button type="submit" disabled={loading}>
          {loading ? 'Please wait…' : (
            step === 'REGISTER' ? 'Start Registration'
              : step === 'VERIFY_EMAIL' ? 'Verify Email OTP'
                : step === 'SETUP_TOTP' ? 'Setup Google Authenticator'
                  : 'Confirm 2FA'
          )}
        </button>
        <p className="login-footer">
          Already have an account? <Link to="/login">Login</Link>
        </p>
      </form>
    </div>
  )
}
