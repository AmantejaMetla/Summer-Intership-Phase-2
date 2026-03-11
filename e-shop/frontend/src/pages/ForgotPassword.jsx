import { useState } from 'react'
import { Link } from 'react-router-dom'
import { apiJson } from '../api/client'
import './Login.css'

export function ForgotPassword() {
  const [step, setStep] = useState('REQUEST_OTP')
  const [email, setEmail] = useState('')
  const [otp, setOtp] = useState('')
  const [otpChannel, setOtpChannel] = useState('EMAIL')
  const [phoneNumber, setPhoneNumber] = useState('')
  const [resetToken, setResetToken] = useState('')
  const [newPassword, setNewPassword] = useState('')
  const [totpCode, setTotpCode] = useState('')
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const [success, setSuccess] = useState('')

  async function requestOtp(e) {
    e.preventDefault()
    setError('')
    setSuccess('')
    setLoading(true)
    try {
      await apiJson('/api/auth/forgot-password/request', {
        method: 'POST',
        body: JSON.stringify({
          email,
          otpChannel,
          phoneNumber: otpChannel === 'SMS' ? phoneNumber : null,
        }),
      })
      setStep('VERIFY_OTP')
      setSuccess(`OTP sent to your ${otpChannel === 'SMS' ? 'SMS' : 'email'}.`)
    } catch (err) {
      setError(err.message || 'Failed to request OTP.')
    } finally {
      setLoading(false)
    }
  }

  async function verifyOtp(e) {
    e.preventDefault()
    setError('')
    setSuccess('')
    setLoading(true)
    try {
      const data = await apiJson('/api/auth/forgot-password/verify-otp', {
        method: 'POST',
        body: JSON.stringify({ email, otp }),
      })
      setResetToken(data.resetToken || '')
      setStep('RESET_PASSWORD')
      setSuccess('OTP verified. Now set a new password with your 2FA code.')
    } catch (err) {
      setError(err.message || 'OTP verification failed.')
    } finally {
      setLoading(false)
    }
  }

  async function resetPassword(e) {
    e.preventDefault()
    setError('')
    setSuccess('')
    setLoading(true)
    try {
      await apiJson('/api/auth/forgot-password/reset', {
        method: 'POST',
        body: JSON.stringify({ email, resetToken, newPassword, totpCode }),
      })
      setSuccess('Password reset complete. You can now login.')
      setStep('DONE')
    } catch (err) {
      setError(err.message || 'Password reset failed.')
    } finally {
      setLoading(false)
    }
  }

  const onSubmit = step === 'REQUEST_OTP' ? requestOtp : step === 'VERIFY_OTP' ? verifyOtp : resetPassword

  return (
    <div className="login-page">
      <form className="login-form" onSubmit={onSubmit}>
        <h1>Forgot Password</h1>
        {error && <p className="form-error">{error}</p>}
        {success && <p className="form-success">{success}</p>}
        <label>
          Email
          <input
            type="email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            required
            disabled={step !== 'REQUEST_OTP'}
          />
        </label>
        {step === 'REQUEST_OTP' && (
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
                Registered Indian Mobile Number
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
        {step === 'VERIFY_OTP' && (
          <label>
            OTP
            <input
              type="text"
              value={otp}
              onChange={(e) => setOtp(e.target.value)}
              required
              pattern="\d{6}"
            />
          </label>
        )}
        {step === 'RESET_PASSWORD' && (
          <>
            <label>
              Reset Token
              <input type="text" value={resetToken} readOnly />
            </label>
            <label>
              New Password
              <input
                type="password"
                value={newPassword}
                onChange={(e) => setNewPassword(e.target.value)}
                required
              />
            </label>
            <label>
              Google Authenticator Code
              <input
                type="text"
                value={totpCode}
                onChange={(e) => setTotpCode(e.target.value)}
                required
                pattern="\d{6}"
              />
            </label>
          </>
        )}
        {step !== 'DONE' && (
          <button type="submit" disabled={loading}>
            {loading ? 'Please wait…' : step === 'REQUEST_OTP' ? 'Send OTP' : step === 'VERIFY_OTP' ? 'Verify OTP' : 'Reset Password'}
          </button>
        )}
        {step !== 'REQUEST_OTP' && step !== 'DONE' && (
          <button
            type="button"
            className="totp-dev-btn"
            onClick={() => {
              setStep('REQUEST_OTP')
              setOtp('')
              setResetToken('')
              setNewPassword('')
              setTotpCode('')
              setError('')
              setSuccess('')
            }}
          >
            Back
          </button>
        )}
        <p className="login-footer">
          Don&apos;t have an account? <Link to="/register">Register</Link>
        </p>
        <p className="login-footer">
          Already have an account? <Link to="/login">Back to Login</Link>
        </p>
      </form>
    </div>
  )
}
