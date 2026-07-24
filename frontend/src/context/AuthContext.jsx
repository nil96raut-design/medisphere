import React, { createContext, useContext, useState, useCallback, useEffect } from 'react'
import { api } from '../api/client'

const AuthContext = createContext(null)

function loadStoredUser() {
  const raw = localStorage.getItem('ht_user')
  return raw ? JSON.parse(raw) : null
}

export function AuthProvider({ children }) {
  const [user, setUser] = useState(loadStoredUser)

  const persist = (authResponse) => {
    const { token, user: userData } = authResponse
    localStorage.setItem('ht_token', token)
    localStorage.setItem('ht_user', JSON.stringify(userData))
    setUser(userData)
  }

  const login = useCallback(async (email, password) => {
    const res = await api.login(email, password)
    persist(res)
    return res
  }, [])

  const register = useCallback(async (payload) => {
    const res = await api.register(payload)
    persist(res)
    return res
  }, [])

  const hospitalSignup = useCallback(async (payload) => {
    const res = await api.hospitalSignup(payload)
    persist(res)
    return res
  }, [])

  const logout = useCallback(() => {
    localStorage.removeItem('ht_token')
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
      logout()
    }
    window.addEventListener('ht:unauthorized', handleUnauthorized)
    return () => window.removeEventListener('ht:unauthorized', handleUnauthorized)
  }, [logout])

  return (
    <AuthContext.Provider value={{
      user,
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

export function useAuth() {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth must be used within AuthProvider')
  return ctx
}
