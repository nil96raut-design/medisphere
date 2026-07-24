import React, { useEffect, useRef } from 'react'
import { Link, Navigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import ChatWidget from '../components/ChatWidget'
import { motion, useInView } from 'framer-motion'
import {
  HeartPulse,
  Activity,
  ShieldCheck,
  Layers,
  Bot,
  Database,
  Sparkles,
  ArrowRight,
  UserCog,
  Stethoscope,
  Users,
  FlaskConical,
  Pill,
  ClipboardList,
  Hospital,
  CheckCircle2,
  Zap,
  Globe,
  Lock,
  BarChart3,
  ChevronRight,
} from 'lucide-react'

/* ─── Feature cards for the "Why MediSphere" section ─── */
const FEATURES = [
  {
    icon: ShieldCheck,
    title: 'Multi-Tenant SaaS',
    desc: 'Hospital-level data isolation enforced at the ORM layer via Hibernate session filters — zero cross-tenant leaks by design.',
    gradient: 'from-teal-500 to-emerald-500',
  },
  {
    icon: Database,
    title: 'Ledger-Based Billing',
    desc: 'Immutable transaction logs with idempotent settlement endpoints. Supports partial payments, insurance splits, and credit adjustments.',
    gradient: 'from-indigo-500 to-blue-500',
  },
  {
    icon: Layers,
    title: 'Clinical EMR Engine',
    desc: 'Structured SOAP consultation notes, ICD-10 ready diagnosis codes, timeline medical history, and real-time triage queue dispatch.',
    gradient: 'from-rose-500 to-pink-500',
  },
  {
    icon: Bot,
    title: 'AI Diagnostics Co-Pilot',
    desc: 'Rule-based vitals flagging, diagnostic suggestions, and a patient-facing chatbot for symptom pre-registration and FAQ resolution.',
    gradient: 'from-violet-500 to-purple-500',
  },
]

/* ─── Stats for the social proof bar ─── */
const STATS = [
  { value: '7', label: 'Role Dashboards' },
  { value: '10+', label: 'Bounded Contexts' },
  { value: '24/7', label: 'Audit Logging' },
  { value: '100%', label: 'Tenant Isolation' },
]

/* ─── Role portal cards ─── */
const ROLE_CARDS = [
  {
    role: 'ADMIN',
    icon: UserCog,
    title: 'Hospital Admin',
    desc: 'Revenue analytics, bed occupancy metrics, staff management, and system-wide operations dashboard.',
    accent: '#7c3aed',
    bg: 'from-violet-50 to-purple-50',
    border: 'hover:border-violet-300',
    badge: 'bg-violet-100 text-violet-700',
  },
  {
    role: 'DOCTOR',
    icon: Stethoscope,
    title: 'Doctor Portal',
    desc: 'Patient queue, SOAP consultation workbench, prescription builder, lab & pharmacy service requests.',
    accent: '#0d9488',
    bg: 'from-teal-50 to-emerald-50',
    border: 'hover:border-teal-300',
    badge: 'bg-teal-100 text-teal-700',
  },
  {
    role: 'RECEPTIONIST',
    icon: Users,
    title: 'Front Desk',
    desc: 'Patient intake, appointment scheduling, triage vitals capture, and live queue board management.',
    accent: '#2563eb',
    bg: 'from-blue-50 to-sky-50',
    border: 'hover:border-blue-300',
    badge: 'bg-blue-100 text-blue-700',
  },
  {
    role: 'NURSE',
    icon: Activity,
    title: 'Nurse / IPD',
    desc: 'Ward bed visualization, admission tracking, nursing observation logs, and discharge workflows.',
    accent: '#ec4899',
    bg: 'from-pink-50 to-rose-50',
    border: 'hover:border-pink-300',
    badge: 'bg-pink-100 text-pink-700',
  },
  {
    role: 'PHARMACIST',
    icon: Pill,
    title: 'Pharmacy',
    desc: 'Medicine inventory ledger, low-stock alerts, prescription verification, and dispensation records.',
    accent: '#d97706',
    bg: 'from-amber-50 to-yellow-50',
    border: 'hover:border-amber-300',
    badge: 'bg-amber-100 text-amber-700',
  },
  {
    role: 'LAB_TECH',
    icon: FlaskConical,
    title: 'Laboratory',
    desc: 'Test order pipeline, specimen collection tracking, result entry, and PDF report generation.',
    accent: '#059669',
    bg: 'from-emerald-50 to-green-50',
    border: 'hover:border-emerald-300',
    badge: 'bg-emerald-100 text-emerald-700',
  },
  {
    role: 'PATIENT',
    icon: ClipboardList,
    title: 'Patient Portal',
    desc: 'Care plan tracker, lab result viewer, appointment history, and wellness task progress logs.',
    accent: '#0284c7',
    bg: 'from-sky-50 to-cyan-50',
    border: 'hover:border-sky-300',
    badge: 'bg-sky-100 text-sky-700',
  },
]

/* ─── Reusable animated-on-scroll section wrapper ─── */
function AnimatedSection({ children, className = '', delay = 0 }) {
  const ref = useRef(null)
  const isInView = useInView(ref, { once: true, margin: '-60px' })
  return (
    <motion.div
      ref={ref}
      initial={{ opacity: 0, y: 40 }}
      animate={isInView ? { opacity: 1, y: 0 } : {}}
      transition={{ duration: 0.6, delay, ease: [0.22, 1, 0.36, 1] }}
      className={className}
    >
      {children}
    </motion.div>
  )
}

/* ═══════════════════ MAIN COMPONENT ═══════════════════ */
export default function Home() {
  const { user } = useAuth()
  if (user) return <Navigate to="/dashboard" replace />

  return (
    <div className="min-h-screen flex flex-col font-sans antialiased text-slate-800 selection:bg-teal-500/20 selection:text-teal-900 overflow-x-hidden">

      {/* ─── Background blobs ─── */}
      <div className="fixed inset-0 pointer-events-none z-0 overflow-hidden">
        <div className="absolute -top-48 -right-48 w-[600px] h-[600px] rounded-full bg-gradient-to-br from-teal-200/30 to-emerald-100/20 blur-3xl" />
        <div className="absolute top-1/3 -left-48 w-[500px] h-[500px] rounded-full bg-gradient-to-br from-indigo-200/20 to-violet-100/15 blur-3xl" />
        <div className="absolute -bottom-48 right-1/4 w-[500px] h-[500px] rounded-full bg-gradient-to-br from-rose-100/20 to-pink-100/15 blur-3xl" />
      </div>

      {/* ════════════ NAVBAR ════════════ */}
      <header className="sticky top-0 z-50 backdrop-blur-xl bg-white/60 border-b border-slate-200/40">
        <div className="max-w-7xl mx-auto px-6 h-16 flex items-center justify-between">
          <Link to="/" className="flex items-center gap-2.5 no-underline">
            <div className="w-9 h-9 rounded-xl bg-gradient-to-br from-teal-600 to-teal-500 flex items-center justify-center shadow-lg shadow-teal-600/20">
              <HeartPulse className="text-white" size={18} />
            </div>
            <span className="text-lg font-display font-bold tracking-tight text-slate-800">MediSphere</span>
            <span className="hidden sm:inline text-[9px] font-bold tracking-widest uppercase bg-gradient-to-r from-teal-600 to-emerald-600 text-white px-2 py-0.5 rounded-full">HMS</span>
          </Link>
          <nav className="flex items-center gap-2">
            <Link to="/hospital-register" className="hidden sm:inline-flex text-sm font-medium text-slate-500 hover:text-slate-800 px-3 py-2 rounded-lg hover:bg-slate-100/60 transition-all no-underline">
              For Hospitals
            </Link>
            <Link to="/login" className="text-sm font-semibold text-slate-600 hover:text-slate-900 px-4 py-2 rounded-xl hover:bg-slate-100/60 transition-all no-underline">
              Sign in
            </Link>
            <Link to="/register" className="text-sm font-bold text-white bg-gradient-to-r from-teal-600 to-teal-500 hover:from-teal-700 hover:to-teal-600 px-5 py-2.5 rounded-xl shadow-md shadow-teal-600/15 hover:shadow-lg hover:shadow-teal-600/20 transition-all hover:-translate-y-px no-underline">
              Get Started
            </Link>
          </nav>
        </div>
      </header>

      {/* ════════════ HERO ════════════ */}
      <section className="relative z-10 max-w-7xl mx-auto px-6 pt-20 sm:pt-28 pb-16 text-center">
        <motion.div
          initial={{ opacity: 0, y: 16 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.5 }}
          className="inline-flex items-center gap-2 bg-white/70 backdrop-blur border border-teal-200/60 rounded-full px-4 py-1.5 mb-8 shadow-sm"
        >
          <Sparkles className="text-teal-600" size={14} />
          <span className="text-xs font-bold text-teal-700 tracking-wide uppercase">Enterprise-Grade · Multi-Hospital · AI-Powered</span>
        </motion.div>

        <motion.h1
          initial={{ opacity: 0, y: 24 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.08, duration: 0.6, ease: [0.22, 1, 0.36, 1] }}
          className="text-4xl sm:text-5xl md:text-6xl lg:text-7xl font-display font-extrabold tracking-tight leading-[1.08] mb-6 max-w-4xl mx-auto"
        >
          The Operating System for{' '}
          <span className="bg-gradient-to-r from-teal-600 via-emerald-500 to-teal-600 bg-clip-text text-transparent bg-[length:200%_auto] animate-[shimmer_3s_linear_infinite]">
            Modern Hospitals
          </span>
        </motion.h1>

        <motion.p
          initial={{ opacity: 0, y: 24 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.16, duration: 0.6 }}
          className="text-slate-500 text-base sm:text-lg md:text-xl max-w-2xl mx-auto leading-relaxed mb-10"
        >
          MediSphere unifies patient care, pharmacy, diagnostics, billing, and ward management
          on a single audit-hardened SaaS platform with strict tenant isolation.
        </motion.p>

        <motion.div
          initial={{ opacity: 0, y: 24 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.24, duration: 0.6 }}
          className="flex flex-col sm:flex-row items-center justify-center gap-4"
        >
          <a
            href="#portals"
            className="group w-full sm:w-auto px-7 py-3.5 bg-gradient-to-r from-teal-600 to-teal-500 hover:from-teal-700 hover:to-teal-600 text-white font-bold rounded-2xl shadow-xl shadow-teal-600/15 hover:shadow-2xl hover:shadow-teal-600/20 transition-all hover:-translate-y-0.5 flex items-center justify-center gap-2 no-underline"
          >
            Explore Demo Portals
            <ArrowRight size={18} className="transition-transform group-hover:translate-x-0.5" />
          </a>
          <Link
            to="/hospital-register"
            className="group w-full sm:w-auto px-7 py-3.5 bg-white hover:bg-slate-50 border border-slate-200 text-slate-700 font-bold rounded-2xl shadow-sm hover:shadow-md transition-all hover:-translate-y-0.5 flex items-center justify-center gap-2 no-underline"
          >
            <Hospital size={18} className="text-teal-600" />
            Register Your Hospital
          </Link>
        </motion.div>
      </section>

      {/* ════════════ STATS BAR ════════════ */}
      <AnimatedSection className="relative z-10 max-w-5xl mx-auto px-6 pb-16 w-full">
        <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
          {STATS.map((s, i) => (
            <motion.div
              key={s.label}
              initial={{ opacity: 0, scale: 0.9 }}
              whileInView={{ opacity: 1, scale: 1 }}
              viewport={{ once: true }}
              transition={{ delay: i * 0.08, duration: 0.4 }}
              className="bg-white/60 backdrop-blur border border-slate-200/50 rounded-2xl py-5 px-4 text-center shadow-sm"
            >
              <p className="text-3xl font-display font-extrabold text-slate-800 m-0">{s.value}</p>
              <p className="text-xs font-semibold text-slate-500 uppercase tracking-wider mt-1 m-0">{s.label}</p>
            </motion.div>
          ))}
        </div>
      </AnimatedSection>

      {/* ════════════ FEATURES ════════════ */}
      <section className="relative z-10 max-w-7xl mx-auto px-6 py-16">
        <AnimatedSection className="text-center mb-14">
          <span className="text-xs font-bold tracking-widest uppercase text-teal-600 mb-3 block">Architecture Highlights</span>
          <h2 className="text-3xl sm:text-4xl font-display font-bold tracking-tight text-slate-800 mb-3">
            Enterprise-Grade by Design
          </h2>
          <p className="text-slate-500 text-base max-w-lg mx-auto">
            Production patterns — not prototypes. Every layer is built for auditability, scalability, and clinical correctness.
          </p>
        </AnimatedSection>

        <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
          {FEATURES.map((feat, idx) => {
            const Icon = feat.icon
            return (
              <AnimatedSection key={feat.title} delay={idx * 0.1}>
                <div className="group relative bg-white/70 backdrop-blur border border-slate-200/50 rounded-2xl p-6 sm:p-8 shadow-sm hover:shadow-lg transition-all duration-300 hover:-translate-y-1 hover:border-slate-300/60 overflow-hidden">
                  {/* subtle gradient glow on hover */}
                  <div className={`absolute inset-0 bg-gradient-to-br ${feat.gradient} opacity-0 group-hover:opacity-[0.03] transition-opacity duration-500 rounded-2xl`} />

                  <div className="relative flex gap-5 items-start">
                    <div className={`w-12 h-12 rounded-xl bg-gradient-to-br ${feat.gradient} flex items-center justify-center shrink-0 shadow-lg`}>
                      <Icon size={22} className="text-white" />
                    </div>
                    <div>
                      <h3 className="text-lg font-bold text-slate-800 mb-2 tracking-tight">{feat.title}</h3>
                      <p className="text-slate-500 text-sm leading-relaxed m-0">{feat.desc}</p>
                    </div>
                  </div>
                </div>
              </AnimatedSection>
            )
          })}
        </div>
      </section>

      {/* ════════════ WORKFLOW RIBBON ════════════ */}
      <AnimatedSection className="relative z-10 max-w-7xl mx-auto px-6 py-16 w-full">
        <div className="bg-gradient-to-r from-slate-900 via-slate-800 to-slate-900 rounded-3xl p-8 sm:p-12 shadow-2xl overflow-hidden relative">
          <div className="absolute inset-0 bg-[url('data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iNjAiIGhlaWdodD0iNjAiIHhtbG5zPSJodHRwOi8vd3d3LnczLm9yZy8yMDAwL3N2ZyI+PGRlZnM+PHBhdHRlcm4gaWQ9ImciIHdpZHRoPSI2MCIgaGVpZ2h0PSI2MCIgcGF0dGVyblVuaXRzPSJ1c2VyU3BhY2VPblVzZSI+PHBhdGggZD0iTTEwIDEwaDQwdjQwSDEweiIgZmlsbD0ibm9uZSIgc3Ryb2tlPSJyZ2JhKDI1NSwyNTUsMjU1LDAuMDMpIiBzdHJva2Utd2lkdGg9IjEiLz48L3BhdHRlcm4+PC9kZWZzPjxyZWN0IGZpbGw9InVybCgjZykiIHdpZHRoPSIxMDAlIiBoZWlnaHQ9IjEwMCUiLz48L3N2Zz4=')] opacity-50" />
          <div className="relative">
            <h2 className="text-2xl sm:text-3xl font-display font-bold text-white tracking-tight mb-3">
              Real Hospital Workflow — End to End
            </h2>
            <p className="text-slate-400 text-sm sm:text-base max-w-xl mb-8">
              From patient check-in to discharge receipt, every step is covered by a dedicated bounded context and enforced by role-based access control.
            </p>
            <div className="flex flex-wrap gap-3">
              {['Registration', 'Triage', 'Queue', 'Consultation', 'Lab Order', 'Pharmacy', 'Ward Admit', 'Billing', 'Discharge'].map((step, i) => (
                <div key={step} className="flex items-center gap-2">
                  <span className="bg-white/10 backdrop-blur text-white text-xs sm:text-sm font-semibold px-4 py-2 rounded-full border border-white/10">
                    {step}
                  </span>
                  {i < 8 && <ChevronRight size={14} className="text-slate-500 hidden sm:block" />}
                </div>
              ))}
            </div>
          </div>
        </div>
      </AnimatedSection>

      {/* ════════════ PORTAL PICKER ════════════ */}
      <section id="portals" className="relative z-10 max-w-7xl mx-auto px-6 py-16 scroll-mt-24">
        <AnimatedSection className="text-center mb-14">
          <span className="text-xs font-bold tracking-widest uppercase text-teal-600 mb-3 block">Interactive Demo</span>
          <h2 className="text-3xl sm:text-4xl font-display font-bold tracking-tight text-slate-800 mb-3">
            7 Role-Based Portals
          </h2>
          <p className="text-slate-500 text-base max-w-lg mx-auto">
            Each role gets a purpose-built workspace with isolated APIs, restricted routes, and tailored dashboards.
          </p>
        </AnimatedSection>

        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-5">
          {ROLE_CARDS.map((card, idx) => {
            const Icon = card.icon
            return (
              <AnimatedSection key={card.role} delay={idx * 0.06}>
                <div className={`group relative bg-gradient-to-br ${card.bg} border border-slate-200/60 ${card.border} rounded-2xl p-6 shadow-sm hover:shadow-xl transition-all duration-300 hover:-translate-y-1.5 flex flex-col h-full`}>
                  <div className="flex items-center justify-between mb-5">
                    <span className={`text-[10px] font-bold tracking-widest uppercase px-2.5 py-1 rounded-full ${card.badge}`}>
                      {card.role.replace('_', ' ')}
                    </span>
                    <div
                      className="w-10 h-10 rounded-xl flex items-center justify-center shadow-sm border border-white/80 bg-white/90"
                    >
                      <Icon size={20} style={{ color: card.accent }} />
                    </div>
                  </div>

                  <h3 className="text-lg font-bold text-slate-800 mb-2 tracking-tight">{card.title}</h3>
                  <p className="text-slate-500 text-sm leading-relaxed mb-6 flex-grow">{card.desc}</p>

                  <Link
                    to="/login"
                    className="group/btn w-full py-2.5 bg-slate-800 hover:bg-slate-900 text-white text-center font-bold text-sm rounded-xl transition-all shadow-sm hover:shadow-lg flex items-center justify-center gap-1.5 no-underline"
                  >
                    Enter Portal
                    <ArrowRight size={14} className="transition-transform group-hover/btn:translate-x-0.5" />
                  </Link>
                </div>
              </AnimatedSection>
            )
          })}
        </div>
      </section>

      {/* ════════════ TECH TRUST BAR ════════════ */}
      <AnimatedSection className="relative z-10 max-w-5xl mx-auto px-6 py-12 w-full">
        <div className="flex flex-wrap items-center justify-center gap-6 sm:gap-10">
          {[
            { icon: Lock, text: 'JWT + RBAC Auth' },
            { icon: Globe, text: 'Multi-Tenant' },
            { icon: Zap, text: 'Spring Boot 3' },
            { icon: BarChart3, text: 'Flyway Migrations' },
            { icon: CheckCircle2, text: 'Testcontainers' },
          ].map((t) => {
            const TIcon = t.icon
            return (
              <div key={t.text} className="flex items-center gap-2 text-slate-400">
                <TIcon size={16} />
                <span className="text-xs font-semibold tracking-wide uppercase">{t.text}</span>
              </div>
            )
          })}
        </div>
      </AnimatedSection>

      {/* ════════════ FOOTER ════════════ */}
      <footer className="relative z-10 mt-auto border-t border-slate-200/40 bg-white/30 backdrop-blur-sm">
        <div className="max-w-7xl mx-auto px-6 py-10 flex flex-col md:flex-row items-center justify-between gap-6">
          <div className="flex items-center gap-3">
            <div className="w-8 h-8 rounded-lg bg-gradient-to-br from-teal-600 to-teal-500 flex items-center justify-center shadow-sm">
              <HeartPulse className="text-white" size={14} />
            </div>
            <div>
              <span className="font-display font-bold text-slate-800 text-sm">MediSphere HMS</span>
              <span className="block text-[10px] text-slate-400">Enterprise Hospital Management System</span>
            </div>
          </div>

          <div className="flex flex-col items-center md:items-end gap-1 text-center md:text-right">
            <p className="text-sm font-semibold text-slate-700 m-0">Built by Nilesh Raut</p>
            <p className="text-xs text-slate-400 m-0">Mobile: 9021866275 · Email: Nil96raut@gmail.com</p>
            <p className="text-[10px] text-slate-300 mt-1 m-0">© {new Date().getFullYear()} MediSphere Inc. All rights reserved.</p>
          </div>
        </div>
      </footer>

      <ChatWidget />

      {/* Shimmer animation keyframes */}
      <style>{`
        @keyframes shimmer {
          0% { background-position: 200% center; }
          100% { background-position: -200% center; }
        }
      `}</style>
    </div>
  )
}
