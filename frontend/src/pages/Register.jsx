import React, { useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import { api } from '../api/client'
import { Heart, User, Mail, Lock, ArrowRight, Stethoscope, ChevronDown } from 'lucide-react'

export default function Register() {
  const { register } = useAuth()
  const navigate = useNavigate()
  const [fullName, setFullName] = useState('')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [role, setRole] = useState('PATIENT')
  const [doctors, setDoctors] = useState([])
  const [primaryDoctorId, setPrimaryDoctorId] = useState('')
  const [allergies, setAllergies] = useState('')
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  useEffect(() => {
    if (role === 'PATIENT') {
      api.usersByRole('DOCTOR').then(setDoctors).catch(() => {})
    }
  }, [role])

  const submit = async (e) => {
    e.preventDefault()
    setError('')
    setLoading(true)
    try {
      await register({
        fullName, email, password, role,
        primaryDoctorId: role === 'PATIENT' && primaryDoctorId ? Number(primaryDoctorId) : null,
        allergies: role === 'PATIENT' && allergies ? allergies : null,
      })
      navigate('/dashboard')
    } catch (err) {
      setError(err.message)
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="min-h-screen flex items-center justify-center bg-gradient-to-br from-slate-50 via-teal-50/30 to-blue-50/40 p-4">
      {/* Decorative background */}
      <div className="fixed inset-0 overflow-hidden pointer-events-none">
        <div className="absolute -top-40 -right-40 w-96 h-96 bg-primary/5 rounded-full blur-3xl"></div>
        <div className="absolute -bottom-40 -left-40 w-96 h-96 bg-blue-400/5 rounded-full blur-3xl"></div>
      </div>

      <div className="relative bg-white/80 backdrop-blur-xl shadow-xl shadow-slate-200/50 border border-slate-200/80 rounded-3xl p-8 lg:p-10 max-w-lg w-full">
        {/* Branding */}
        <div className="flex items-center gap-3 mb-6 justify-center">
          <div className="w-10 h-10 bg-primary rounded-xl flex items-center justify-center shadow-sm">
            <Heart className="text-white" size={20} />
          </div>
          <span className="text-xl font-display font-bold text-slate-800">MediSphere</span>
        </div>

        <h2 className="text-2xl font-display font-bold text-slate-800 m-0 mb-1 text-center">Create your account</h2>
        <p className="text-sm text-slate-500 mb-6 text-center">Choose the role that fits you.</p>

        {error && (
          <div className="p-3 mb-4 bg-danger-light border border-danger/20 rounded-xl text-sm text-danger font-medium animate-in fade-in slide-in-from-top-2">
            {error}
          </div>
        )}

        <form onSubmit={submit} className="space-y-4">
          <div>
            <label className="form-label text-sm">Full Name</label>
            <div className="relative">
              <User className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" size={18} />
              <input className="input-field pl-10" value={fullName} onChange={(e) => setFullName(e.target.value)} required placeholder="John Doe" />
            </div>
          </div>
          
          <div>
            <label className="form-label text-sm">Email</label>
            <div className="relative">
              <Mail className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" size={18} />
              <input className="input-field pl-10" type="email" value={email} onChange={(e) => setEmail(e.target.value)} required placeholder="you@hospital.com" />
            </div>
          </div>
          
          <div>
            <label className="form-label text-sm">Password</label>
            <div className="relative">
              <Lock className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" size={18} />
              <input className="input-field pl-10" type="password" value={password} onChange={(e) => setPassword(e.target.value)} required minLength={6} placeholder="Min 6 characters" />
            </div>
          </div>
          
          <div>
            <label className="form-label text-sm">I am a…</label>
            <select className="input-field" value={role} onChange={(e) => setRole(e.target.value)}>
              <option value="PATIENT">Patient</option>
              <option value="DOCTOR">Doctor</option>
              <option value="RECEPTIONIST">Care team / Staff</option>
              <option value="ADMIN">Hospital Admin</option>
            </select>
          </div>

          {role === 'PATIENT' && (
            <div className="space-y-4 p-4 bg-slate-50 rounded-xl border border-slate-200 animate-in fade-in slide-in-from-top-2 duration-200">
              <div>
                <label className="form-label text-sm flex items-center gap-1">
                  <Stethoscope size={14} className="text-primary" /> Primary Doctor <span className="text-slate-400">(optional)</span>
                </label>
                <select className="input-field" value={primaryDoctorId} onChange={(e) => setPrimaryDoctorId(e.target.value)}>
                  <option value="">No preference</option>
                  {doctors.map((d) => <option key={d.id} value={d.id}>{d.fullName}</option>)}
                </select>
              </div>
              <div>
                <label className="form-label text-sm">Known Allergies <span className="text-slate-400">(optional)</span></label>
                <input
                  className="input-field"
                  value={allergies}
                  onChange={(e) => setAllergies(e.target.value)}
                  placeholder="e.g. penicillin, latex"
                />
              </div>
            </div>
          )}

          <button 
            className="btn btn-primary w-full py-3 text-base font-semibold bg-primary text-white shadow-md shadow-primary/20 hover:shadow-lg hover:shadow-primary/30 transition-all flex items-center justify-center gap-2 mt-6" 
            type="submit" 
            disabled={loading}
          >
            {loading ? (
              <>
                <div className="w-5 h-5 border-2 border-white/30 border-t-white rounded-full animate-spin"></div>
                Creating account…
              </>
            ) : (
              <>Create account <ArrowRight size={18} /></>
            )}
          </button>
        </form>

        <p className="text-sm text-slate-500 font-medium text-center mt-6">
          Already have an account? <Link to="/login" className="text-primary font-semibold hover:underline">Sign in</Link>
        </p>
      </div>
    </div>
  )
}
