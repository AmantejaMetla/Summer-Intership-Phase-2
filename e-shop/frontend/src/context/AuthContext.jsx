import { createContext, useContext, useState, useCallback, useEffect } from 'react'

const AuthContext = createContext(null)

export function AuthProvider({ children }) {
  const [user, setUser] = useState(() => {
    const t = localStorage.getItem('accessToken')
    const u = localStorage.getItem('userId')
    const r = localStorage.getItem('roles')
    if (!t) return null
    return {
      userId: u ? Number(u) : null,
      accessToken: t,
      roles: r ? JSON.parse(r) : [],
    }
  })

  const login = useCallback((data) => {
    const { userId, accessToken, roles } = data
    localStorage.setItem('accessToken', accessToken)
    if (userId != null) localStorage.setItem('userId', String(userId))
    if (roles) localStorage.setItem('roles', JSON.stringify(roles))
    setUser({ userId, accessToken, roles: roles || [] })
  }, [])

  const logout = useCallback(() => {
    localStorage.removeItem('accessToken')
    localStorage.removeItem('userId')
    localStorage.removeItem('roles')
    setUser(null)
  }, [])

  return (
    <AuthContext.Provider value={{ user, login, logout }}>
      {children}
    </AuthContext.Provider>
  )
}

export function useAuth() {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth must be used inside AuthProvider')
  return ctx
}
