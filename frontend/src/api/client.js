import { refreshAccessToken } from '../context/AuthContext'

const BASE_URL = import.meta.env.VITE_API_BASE_URL || '/api'
export { BASE_URL }

let currentAccessToken = null

export function setAccessToken(token) {
  currentAccessToken = token
}

export function clearAccessToken() {
  currentAccessToken = null
}

async function getValidToken() {
  if (!currentAccessToken) {
    const storedRefresh = localStorage.getItem('ht_refresh_token')
    if (!storedRefresh) return null
    const newToken = await refreshAccessToken()
    return newToken
  }
  return currentAccessToken
}

async function request(path, { method = 'GET', body, auth = true } = {}) {
  const headers = { 'Content-Type': 'application/json' }
  if (auth) {
    const token = await getValidToken()
    if (token) headers['Authorization'] = `Bearer ${token}`
  }

  console.debug(`[API] ${method} ${path}`, body || '')

  const res = await fetch(`${BASE_URL}${path}`, {
    method,
    headers,
    body: body ? JSON.stringify(body) : undefined,
  })

  if (res.status === 401 && auth) {
    const newToken = await refreshAccessToken()
    if (newToken) {
      headers['Authorization'] = `Bearer ${newToken}`
      const retryRes = await fetch(`${BASE_URL}${path}`, {
        method,
        headers,
        body: body ? JSON.stringify(body) : undefined,
      })
      if (retryRes.ok) {
        if (retryRes.status === 204) return null
        const retryData = await retryRes.json()
        return retryData
      }
    }
    window.dispatchEvent(new CustomEvent('ht:unauthorized'))
    let message = `Request failed (${res.status})`
    try {
      const data = await res.json()
      message = data.message || message
    } catch (_) {}
    throw new Error(message)
  }

  if (!res.ok) {
    let message = `Request failed (${res.status})`
    try {
      const data = await res.json()
      message = data.message || message
    } catch (_) {}
    console.error(`[API] FAILED ${method} ${path}: ${message}`)
    throw new Error(message)
  }

  if (res.status === 204) return null
  const data = await res.json()
  console.debug(`[API] ${method} ${path} ->`, data)
  return data
}

export const api = {
  getAnalytics: () => request('/dashboard/analytics'),
  login: (email, password) => request('/auth/login', { method: 'POST', body: { email, password }, auth: false }),
  register: (payload) => request('/auth/register', { method: 'POST', body: payload, auth: false }),
  hospitalSignup: (payload) => request('/auth/hospital-signup', { method: 'POST', body: payload, auth: false }),

  listTasks: () => request('/tasks'),
  searchTasks: ({ q = '', page = 0, size = 20 } = {}) =>
    request(`/tasks/search?q=${encodeURIComponent(q)}&page=${page}&size=${size}`),
  createTask: (payload) => request('/tasks', { method: 'POST', body: payload }),
  updateProgress: (taskId, payload) => request(`/tasks/${taskId}/progress`, { method: 'PATCH', body: payload }),
  getTimeline: (taskId) => request(`/tasks/${taskId}/timeline`),

  usersByRole: (role) => request(`/users/by-role?role=${role}`),
  createUser: (payload) => request('/users', { method: 'POST', body: payload }),

  registerPatient: (payload) => request('/patients', { method: 'POST', body: payload }),
  searchPatients: ({ q = '', page = 0, size = 20 } = {}) =>
    request(`/patients/search?q=${encodeURIComponent(q)}&page=${page}&size=${size}`),
  logTriage: (patientId, payload) => request(`/patients/${patientId}/triage`, { method: 'POST', body: payload }),

  chat: (payload) => request('/chatbot/chat', { method: 'POST', body: payload, auth: false }),

  getAvailableDoctors: () => request('/doctors/available'),
  bookAppointment: (payload) => request('/appointments', { method: 'POST', body: payload }),
  updateAppointmentStatus: (id, payload) => request(`/appointments/${id}/status`, { method: 'POST', body: payload }),
  getQueue: (doctorId) => request(`/queue/doctor/${doctorId}`),

  getPatientHistory: (patientId) => request(`/patients/${patientId}/history`),
  createMedicalRecord: (payload) => request('/medical-records', { method: 'POST', body: payload }),

  getPharmacyInventory: () => request('/pharmacy/inventory'),
  getLowStock: () => request('/pharmacy/inventory/low-stock'),
  addStock: (payload) => request('/pharmacy/inventory', { method: 'POST', body: payload }),
  dispenseMedicine: (payload) => request('/pharmacy/dispense', { method: 'POST', body: payload }),

  calculateBill: (patientId) => request(`/billing/calculate/${patientId}`),
  settleBill: (payload) => request('/billing/settle', { method: 'POST', body: payload }),

  getLabOrders: (status) => request(`/lab/orders${status ? `?status=${status}` : ''}`),
  collectLabSample: (id, payload) => request(`/lab/orders/${id}/sample`, { method: 'PUT', body: payload }),
  enterLabResults: (id, payload) => request(`/lab/orders/${id}/results`, { method: 'PUT', body: payload }),

  getAvailableBeds: () => request('/beds/available'),
  admitPatient: (payload) => request('/admissions', { method: 'POST', body: payload }),
  addNursingLog: (admissionId, payload) => request(`/admissions/${admissionId}/nursing-log`, { method: 'POST', body: payload }),
  dischargePatient: (admissionId, payload) => request(`/admissions/${admissionId}/discharge`, { method: 'POST', body: payload }),
  getActiveAdmissions: () => request('/admissions/active'),
}
