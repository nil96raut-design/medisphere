import React, { createContext, useContext, useState, useCallback, useRef, useEffect } from 'react'
import { api, setAccessToken, clearAccessToken } from '../api/client'

export const AuthContext = createContext(null)

let refreshPromise = null

function loadStoredUser() {
  const raw = localStorage.getItem('ht_user')
  return raw ? JSON.parse(raw) : null
}

function getStoredRefreshToken() {
  return localStorage.getItem('ht_refresh_token')
}

function storeRefreshToken(token) {
  if (token) {
    localStorage.setItem('ht_refresh_token', token)
  } else {
    localStorage.removeItem('ht_refresh_token')
  }
}

export async function refreshAccessToken() {
  if (refreshPromise) return refreshPromise

  const refreshToken = getStoredRefreshToken()
  if (!refreshToken) {
    clearAccessToken()
    return null
  }

  refreshPromise = (async () => {
    try {
      const baseUrl = import.meta.env.VITE_API_URL || import.meta.env.VITE_API_BASE_URL || '/api'
      const res = await fetch(`${baseUrl}/auth/refresh`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        credentials: 'include',
        body: JSON.stringify({ refreshToken }),
      })
      if (!res.ok) throw new Error('Refresh failed')
      const data = await res.json()
      setAccessToken(data.token)
      storeRefreshToken(data.refreshToken)
      return data.token
    } catch {
      clearAccessToken()
      storeRefreshToken(null)
      return null
    } finally {
      refreshPromise = null
    }
  })()

  return refreshPromise
}

export function AuthProvider({ children }) {
  const [user, setUser] = useState(loadStoredUser)
  const [accessToken, setAccessTokenState] = useState(null)

  const persist = useCallback((authResponse) => {
    const { token, refreshToken, user: userData } = authResponse
    setAccessToken(token)
    setAccessTokenState(token)
    storeRefreshToken(refreshToken)
    localStorage.setItem('ht_user', JSON.stringify(userData))
    setUser(userData)
  }, [])

  const login = useCallback(async (email, password) => {
    const res = await api.login(email, password)
    persist(res)
    return res
  }, [persist])

  const register = useCallback(async (payload) => {
    const res = await api.register(payload)
    persist(res)
    return res
  }, [persist])

  const hospitalSignup = useCallback(async (payload) => {
    const res = await api.hospitalSignup(payload)
    persist(res)
    return res
  }, [persist])

  const logout = useCallback(() => {
    setAccessToken(null)
    setAccessTokenState(null)
    clearAccessToken()
    storeRefreshToken(null)
    localStorage.removeItem('ht_user')
    setUser(null)
  }, [])

  const hasRole = useCallback((...roles) => {
    return user && roles.includes(user.role)
  }, [user])

  const isAdmin = user?.role === 'ADMIN'
  const isDoctor = user?.role === 'DOCTOR'
  const isReceptionist = user?.role === 'RECEPTIONIST'
  const isNurse = user?.role === 'NURSE'
  const isPharmacist = user?.role === 'PHARMACIST'
  const isLabTech = user?.role === 'LAB_TECH'
  const isPatient = user?.role === 'PATIENT'

  useEffect(() => {
    const handleUnauthorized = () => {
      setAccessToken(null)
      setAccessTokenState(null)
      clearAccessToken()
      storeRefreshToken(null)
      localStorage.removeItem('ht_user')
      setUser(null)
    }
    window.addEventListener('ht:unauthorized', handleUnauthorized)
    return () => window.removeEventListener('ht:unauthorized', handleUnauthorized)
  }, [])

  return (
    <AuthContext.Provider value={{
      user,
      getAccessToken: () => accessToken,
      login,
      register,
      hospitalSignup,
      logout,
      hasRole,
      isAdmin,
      isDoctor,
      isReceptionist,
      isNurse,
      isPharmacist,
      isLabTech,
      isPatient,
    }}>
      {children}
    </AuthContext.Provider>
  )
}

export { useAuth } from './useAuth'
