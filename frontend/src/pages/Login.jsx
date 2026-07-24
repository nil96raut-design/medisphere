import React, { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import ChatWidget from '../components/ChatWidget'
import { Heart, Mail, Lock, ArrowRight, Stethoscope, UserCog, Users, Pill, FlaskConical, ClipboardList, Activity } from 'lucide-react'

const DEMO_ACCOUNTS = [
  { role: 'ADMIN', label: 'Hospital Admin', email: 'admin@medisphere.com', icon: UserCog, color: 'text-violet-500 bg-violet-50' },
  { role: 'DOCTOR', label: 'Doctor', email: 'doctor@medisphere.com', icon: Stethoscope, color: 'text-teal-500 bg-teal-50' },
  { role: 'RECEPTIONIST', label: 'Front Desk', email: 'receptionist@medisphere.com', icon: Users, color: 'text-blue-500 bg-blue-50' },
  { role: 'NURSE', label: 'Nurse', email: 'nurse@medisphere.com', icon: Activity, color: 'text-pink-500 bg-pink-50' },
  { role: 'PHARMACIST', label: 'Pharmacist', email: 'pharmacist@medisphere.com', icon: Pill, color: 'text-amber-500 bg-amber-50' },
  { role: 'LAB_TECH', label: 'Lab Tech', email: 'labtech@medisphere.com', icon: FlaskConical, color: 'text-emerald-500 bg-emerald-50' },
  { role: 'PATIENT', label: 'Patient', email: 'patient@medisphere.com', icon: ClipboardList, color: 'text-sky-500 bg-sky-50' },
]

export default function Login() {
  const { login } = useAuth()
  const navigate = useNavigate()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  const submit = async (e) => {
    e.preventDefault()
    setError('')
    setLoading(true)
    try {
      const res = await login(email, password)
      navigate(getDefaultRoute(res.user.role))
    } catch (err) {
      setError(err.message)
    } finally {
      setLoading(false)
    }
  }

  const getDefaultRoute = (role) => {
    switch (role) {
      case 'ADMIN': return '/dashboard'
      case 'DOCTOR': return '/doctor'
      case 'RECEPTIONIST': return '/frontdesk'
      case 'NURSE': return '/ipd'
      case 'PHARMACIST': return '/pharmacy'
      case 'LAB_TECH': return '/lab'
      case 'PATIENT': return '/patient'
      default: return '/dashboard'
    }
  }

  const fillDemo = (account) => {
    setEmail(account.email)
    setPassword('password123')
    setError('')
  }

  return (
    <div className="min-h-screen flex items-center justify-center bg-gradient-to-br from-slate-50 via-teal-50/30 to-blue-50/40 p-4">
      {/* Decorative background elements */}
      <div className="fixed inset-0 overflow-hidden pointer-events-none">
        <div className="absolute -top-40 -right-40 w-96 h-96 bg-primary/5 rounded-full blur-3xl"></div>
        <div className="absolute -bottom-40 -left-40 w-96 h-96 bg-blue-400/5 rounded-full blur-3xl"></div>
      </div>

      <div className="relative w-full max-w-5xl grid grid-cols-1 lg:grid-cols-2 gap-8 items-center">
        {/* Left - Branding Panel */}
        <div className="hidden lg:flex flex-col justify-center py-12 px-8">
          <div className="flex items-center gap-3 mb-8">
            <div className="w-12 h-12 bg-primary rounded-2xl flex items-center justify-center shadow-md shadow-primary/30">
              <Heart className="text-white" size={24} />
            </div>
            <div>
              <h1 className="text-2xl font-display font-bold text-slate-800 m-0 tracking-tight">MediSphere</h1>
              <p className="text-xs text-slate-500 m-0 font-medium tracking-wider uppercase">Hospital Management System</p>
            </div>
          </div>
          
              <h2 className="text-4xl font-display font-bold text-slate-800 leading-tight mb-4">
            Enterprise Healthcare,<br />
            <span className="bg-gradient-to-r from-primary to-blue-500 bg-clip-text text-transparent">Simplified.</span>
          </h2>
          <p className="text-slate-500 text-lg leading-relaxed max-w-md">
            Manage patients, prescriptions, billing, labs, wards, and more — all from one unified platform built for modern hospitals.
          </p>

          <div className="mt-10 grid grid-cols-3 gap-4">
            {[
              { label: 'Multi-Role', desc: '7 dashboards' },
              { label: 'Real-Time', desc: 'Live updates' },
              { label: 'Secure', desc: 'RBAC + JWT' },
            ].map((f) => (
              <div key={f.label} className="bg-white/70 backdrop-blur-sm border border-slate-200/70 rounded-xl p-3 text-center shadow-sm">
                <p className="font-bold text-slate-800 text-sm m-0">{f.label}</p>
                <p className="text-xs text-slate-500 m-0 mt-0.5">{f.desc}</p>
              </div>
            ))}
          </div>
        </div>

        {/* Right - Login Form */}
        <div className="bg-white/80 backdrop-blur-xl shadow-xl shadow-slate-200/50 border border-slate-200/80 rounded-3xl p-8 lg:p-10 max-w-md w-full mx-auto lg:mx-0">
          {/* Mobile branding */}
          <div className="lg:hidden flex items-center gap-3 mb-6 justify-center">
            <div className="w-10 h-10 bg-primary rounded-xl flex items-center justify-center shadow-sm">
              <Heart className="text-white" size={20} />
            </div>
            <span className="text-xl font-display font-bold text-slate-800">MediSphere</span>
          </div>

          <h2 className="text-2xl font-display font-bold text-slate-800 m-0 mb-1">Welcome back</h2>
          <p className="text-sm text-slate-500 mb-6">Sign in to access your role-based dashboard</p>

          {error && (
            <div className="p-3 mb-4 bg-danger-light border border-danger/20 rounded-xl text-sm text-danger font-medium animate-in fade-in slide-in-from-top-2">
              {error}
            </div>
          )}

          <form onSubmit={submit} className="space-y-4">
            <div>
              <label className="form-label text-sm">Email</label>
              <div className="relative">
                <Mail className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" size={18} />
                <input 
                  className="input-field pl-10" 
                  type="email" 
                  value={email} 
                  onChange={(e) => setEmail(e.target.value)} 
                  required 
                  placeholder="you@hospital.com"
                />
              </div>
            </div>
            <div>
              <label className="form-label text-sm">Password</label>
              <div className="relative">
                <Lock className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" size={18} />
                <input 
                  className="input-field pl-10" 
                  type="password" 
                  value={password} 
                  onChange={(e) => setPassword(e.target.value)} 
                  required 
                  placeholder="••••••••"
                />
              </div>
            </div>
            <button 
              className="btn btn-primary w-full py-3 text-base font-semibold bg-primary text-white shadow-md shadow-primary/20 hover:shadow-lg hover:shadow-primary/30 transition-all flex items-center justify-center gap-2" 
              type="submit" 
              disabled={loading}
            >
              {loading ? (
                <>
                  <div className="w-5 h-5 border-2 border-white/30 border-t-white rounded-full animate-spin"></div>
                  Signing in…
                </>
              ) : (
                <>Sign in <ArrowRight size={18} /></>
              )}
            </button>
          </form>

          <div className="flex items-center gap-3 mt-6 text-sm text-slate-500 font-medium">
            <span>New here? <Link to="/register" className="text-primary font-semibold hover:underline">Create account</Link></span>
            <span className="text-slate-300">·</span>
            <Link to="/hospital-register" className="text-primary font-semibold hover:underline">Register hospital</Link>
          </div>

          {/* Demo Accounts */}
          <div className="mt-8 pt-6 border-t border-slate-100">
            <p className="text-xs font-semibold text-slate-400 uppercase tracking-wider mb-3">Quick Demo Access</p>
            <div className="grid grid-cols-2 gap-2">
              {DEMO_ACCOUNTS.map((account) => {
                const Icon = account.icon
                return (
                  <button
                    key={account.role}
                    type="button"
                    className={`flex items-center gap-2.5 p-2.5 rounded-xl border border-slate-100 hover:border-primary/30 hover:bg-primary-light/10 transition-all text-left group ${email === account.email ? 'border-primary/50 bg-primary-light/15 shadow-sm' : ''}`}
                    onClick={() => fillDemo(account)}
                  >
                    <div className={`w-8 h-8 rounded-lg flex items-center justify-center shrink-0 ${account.color}`}>
                      <Icon size={16} />
                    </div>
                    <div className="min-w-0">
                      <p className="text-xs font-bold text-slate-700 m-0 truncate">{account.label}</p>
                      <p className="text-[10px] text-slate-400 m-0 truncate">{account.email}</p>
                    </div>
                  </button>
                )
              })}
            </div>
            <p className="text-[10px] text-slate-400 mt-2 text-center">Password: <code className="bg-slate-100 px-1 py-0.5 rounded text-slate-500 font-mono">password123</code></p>
          </div>
        </div>
      </div>
      <ChatWidget />
    </div>
  )
}
