import http from 'k6/http';
import { check, sleep } from 'k6';
import { SharedArray } from 'k6/data';

// k6 load test for MediSphere HMS
// Run: k6 run load-test.js --vus 10 --duration 30s

export const options = {
  stages: [
    { duration: '10s', target: 10 },
    { duration: '20s', target: 20 },
    { duration: '10s', target: 0 },
  ],
  thresholds: {
    http_req_duration: ['p(95)<2000'],
    http_req_failed: ['rate<0.05'],
  },
};

const BASE_URL = 'http://localhost:8085/api';

const credentials = [
  { email: 'admin@medisphere.com', password: 'password123', role: 'ADMIN' },
  { email: 'doctor@medisphere.com', password: 'password123', role: 'DOCTOR' },
  { email: 'receptionist@medisphere.com', password: 'password123', role: 'RECEPTIONIST' },
  { email: 'nurse@medisphere.com', password: 'password123', role: 'NURSE' },
  { email: 'pharmacist@medisphere.com', password: 'password123', role: 'PHARMACIST' },
  { email: 'patient@medisphere.com', password: 'password123', role: 'PATIENT' },
];

export default function () {
  const cred = credentials[__VU % credentials.length];
  const token = login(cred);

  if (!token) {
    sleep(1);
    return;
  }

  const headers = {
    'Authorization': `Bearer ${token}`,
    'Content-Type': 'application/json',
  };

  const role = cred.role;

  if (role === 'ADMIN' || role === 'DOCTOR') {
    const resp = http.get(`${BASE_URL}/patients/search?q=`, { headers });
    check(resp, { 'search patients ok': (r) => r.status === 200 });
  }

  if (role === 'RECEPTIONIST' || role === 'ADMIN') {
    const resp = http.get(`${BASE_URL}/doctors/available`, { headers });
    check(resp, { 'available doctors ok': (r) => r.status === 200 });
  }

  if (role === 'DOCTOR' || role === 'NURSE' || role === 'ADMIN') {
    const resp = http.get(`${BASE_URL}/admissions/active`, { headers });
    check(resp, { 'active admissions ok': (r) => r.status === 200 });
  }

  if (role === 'PHARMACIST' || role === 'ADMIN') {
    const resp = http.get(`${BASE_URL}/pharmacy/inventory`, { headers });
    check(resp, { 'inventory ok': (r) => r.status === 200 });
  }

  sleep(1);
}

function login(cred) {
  const res = http.post(`${BASE_URL}/auth/login`, JSON.stringify({
    email: cred.email,
    password: cred.password,
  }), { headers: { 'Content-Type': 'application/json' } });

  if (res.status === 200) {
    return JSON.parse(res.body).token;
  }
  return null;
}
