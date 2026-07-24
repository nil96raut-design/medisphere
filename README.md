# 🏥 MediSphere — Multi-Tenant Hospital Management Platform

[![Java 17](https://img.shields.io/badge/Java_17-ED8B00?style=flat&logo=openjdk&logoColor=white)](https://adoptium.net/)
[![Spring Boot 3.3](https://img.shields.io/badge/Spring_Boot_3.3-6DB33F?style=flat&logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![React 18](https://img.shields.io/badge/React_18-20232A?style=flat&logo=react&logoColor=61DAFB)](https://react.dev/)
[![Tailwind v4](https://img.shields.io/badge/Tailwind_v4-06B6D4?style=flat&logo=tailwindcss&logoColor=white)](https://tailwindcss.com/)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-4169E1?style=flat&logo=postgresql&logoColor=white)](https://www.postgresql.org/)
[![Flyway](https://img.shields.io/badge/Flyway-CC0200?style=flat&logo=flyway&logoColor=white)](https://flywaydb.org/)
[![JWT](https://img.shields.io/badge/Auth-JWT-black?style=flat&logo=jsonwebtokens)](https://jwt.io/)
[![Tests](https://img.shields.io/badge/backend_tests-64_✔-2ea44f?style=flat)](https://github.com/)
[![License](https://img.shields.io/badge/license-MIT-blue?style=flat)](LICENSE)

---

## 🚀 Live Demo

> *Demo instances coming soon. Deploy instructions below.*

| Service | URL |
|---------|-----|
| **Frontend** | `https://medisphere.vercel.app` (or your Vercel deployment) |
| **Backend API** | `https://medisphere-backend.onrender.com` (or your Render deployment) |
| **API Collection** | [`MediSphere-Postman-Collection.json`](./MediSphere-Postman-Collection.json) |

---

## 🔑 Demo Credentials

All demo accounts use password: **`password123`**

| Role | Email | Permissions |
|------|-------|-------------|
| 🏢 **Hospital Admin** | `admin@medisphere.com` | Full system access, user management, billing, settings |
| 🩺 **Doctor** | `doctor@medisphere.com` | Consultations, medical records, prescriptions, admissions |
| 🧑‍⚕️ **Receptionist** | `receptionist@medisphere.com` | Patient registration, appointments, billing |
| 👩‍⚕️ **Nurse** | `nurse@medisphere.com` | Triage, IPD nursing logs, vitals recording |
| 💊 **Pharmacist** | `pharmacist@medisphere.com` | Inventory management, dispensing |
| 🔬 **Lab Tech** | `labtech@medisphere.com` | Lab order processing, sample collection, results entry |
| 🏥 **Patient** | `patient@medisphere.com` | View own records, appointments, tasks |

---

## 📸 Screenshots

<!-- 
TODO: Add screenshots here to maximize recruiter impact.
Recommended captures:
1. Login page with role selector
2. Doctor dashboard with queue and patient list
3. Pharmacy inventory management
4. Lab order processing workflow
5. IPD ward view with bed occupancy
6. Billing with itemized invoice
7. Tenant isolation — same app, different hospital data

Format:
<img src="screenshots/dashboard.png" alt="Doctor Dashboard" width="700"/>
<img src="screenshots/pharmacy.png" alt="Pharmacy Module" width="700"/>
-->

---

## 🏗 System Architecture

```
┌──────────────────────────────────────────────────┐
│               React 18 + Vite 5 (SPA)            │
│   Tailwind v4 · Framer Motion · Lucide Icons     │
│                                                   │
│   AuthContext (JWT)   │   ToastContext            │
│   ───────────────────┴────────────────────       │
│              api/client.js (fetch)                │
│          localStorage → Bearer <jwt>              │
└─────────────────────┬────────────────────────────┘
                      │ /api/* proxy (Vite dev)
                      │ CORS-enabled in production
                      ▼
┌──────────────────────────────────────────────────┐
│            Spring Boot 3.3.2 (Port 8085)          │
│                                                   │
│   ┌──────────┐ ┌──────────┐ ┌─────────────────┐  │
│   │Security  │ │   JWT    │ │   RateLimit     │  │
│   │ Config   │ │ Service  │ │   Aspect (AOP)  │  │
│   └────┬─────┘ └────┬─────┘ └───────┬─────────┘  │
│        │            │               │             │
│   ┌────▼────────────▼───────────────▼──────────┐  │
│   │          REST Controllers (13 modules)      │  │
│   └────────────────────┬───────────────────────┘  │
│   ┌────────────────────▼───────────────────────┐  │
│   │       Service Layer + @PreAuthorize RBAC    │  │
│   └────────────────────┬───────────────────────┘  │
│   ┌────────────────────▼───────────────────────┐  │
│   │  JPA Repositories · Hibernate @Filter      │  │
│   │  TenantInterceptor (per-request hook)      │  │
│   │  TenantValidationAspect (AOP guard)        │  │
│   └────────────────────┬───────────────────────┘  │
│   ┌────────────────────▼───────────────────────┐  │
│   │      Flyway Migrations (V1–V13)            │  │
│   │      ddl-auto: validate (fail-fast)       │  │
│   └────────────────────┬───────────────────────┘  │
│                        ▼                          │
│              ┌──────────────────┐                  │
│              │   PostgreSQL    │                  │
│              └──────────────────┘                  │
└──────────────────────────────────────────────────┘
```

### Tenant Isolation Flow (3 Layers of Defense)

```
HTTP Request → TenantInterceptor extracts hospitalId from JWT claims
             → Sets Hibernate @Filter(name="tenantFilter") on every query
             → TenantValidationAspect inspects returned entities post-query
             → Blocks response with 403 if cross-tenant data detected
```

---

## ✨ Features

### 🏢 Multi-Tenant SaaS
- Hospital-level data isolation via Hibernate `@Filter` annotation
- `TenantInterceptor` injects tenant context per HTTP request
- `TenantValidationAspect` provides AOP-level cross-tenant leak detection
- Each hospital sees ONLY its own patients, staff, inventory, and financials

### 🔐 Authentication & RBAC
- JWT-based stateless auth (HS256, 24h expiry)
- 7 roles: `ADMIN`, `DOCTOR`, `RECEPTIONIST`, `NURSE`, `PHARMACIST`, `LAB_TECH`, `PATIENT`
- Method-level security via `@PreAuthorize` + service-layer defense-in-depth
- Rate limiting: 20 requests/min on `/api/auth/login` (AOP aspect)
- BCrypt password hashing

### 🩺 Clinical Modules

| Module | Key Capabilities |
|--------|-----------------|
| **Patient Management** | Registration, demographics, search, history timeline |
| **Triage** | ESI severity assignment, vitals (BP, pulse, temp, SpO2) |
| **Appointments** | Slot booking, reschedule, cancellation with conflict detection |
| **Queue Management** | Real-time token queue per doctor, sequential numbering |
| **Medical Records** | Encounter notes, diagnoses, prescriptions, service requests |
| **IPD / Ward** | Bed availability, admissions, nursing logs, discharge summaries, PDF export |
| **Lab** | Order placement, sample collection, results entry, status workflow |
| **Pharmacy** | Inventory, batch tracking, low-stock alerts, dispensing audit |
| **Billing** | Itemized invoice calculation, settlement, idempotency-key deduplication |
| **Tasks** | Role-scoped board, progress tracking, per-task audit timeline |
| **Dashboard** | Role-specific analytics, KPIs, recent activity |
| **Chatbot** | AI-powered assistant for patients (no auth required) |

### 🛡 Security Checklist

| Control | Implementation |
|---------|---------------|
| **Authentication** | JWT (HS256, 256+ bit secret), BCrypt password hashing |
| **Authorization** | `@PreAuthorize` on controllers + service-layer defense-in-depth |
| **Tenant Isolation** | Hibernate `@Filter` → `TenantInterceptor` → AOP validation |
| **Rate Limiting** | 20 requests/min on `/api/auth/login` |
| **Input Validation** | `@NotBlank`, `@NotNull`, `@Email`, `@Min`/`@Max` on all DTOs |
| **Concurrency** | `PESSIMISTIC_WRITE` locks on critical resources; idempotency keys |
| **CORS** | Restricted to frontend origin via `app.cors.allowed-origins` |
| **Audit Logging** | Login attempts, patient data access, admin operations logged |
| **Error Handling** | Structured JSON errors, no stack trace leaks |

Full audit: [`SECURITY_CHECKLIST.md`](./SECURITY_CHECKLIST.md)

---

## 🧰 Tech Stack

### Backend
| Technology | Version | Purpose |
|-----------|---------|---------|
| Java | 17 | Language |
| Spring Boot | 3.3.2 | Framework (Web, JPA, Security, Validation, AOP) |
| Maven Wrapper | 3.9.9 | Build tool (no local Maven needed) |
| PostgreSQL | 15+ | Primary database |
| Flyway | 9.x | Schema migrations (13 files, `ddl-auto: validate`) |
| H2 | 2.x | In-memory DB for integration tests |
| JWT (jjwt) | 0.11.5 | Token auth |
| Lombok | 1.18.42 | Boilerplate reduction |
| OpenPDF | 1.3.30 | PDF generation |
| Testcontainers | 1.x | (dependency only — tests use H2 profile) |

### Frontend
| Technology | Version | Purpose |
|-----------|---------|---------|
| React | 18.3 | UI framework |
| Vite | 5.4 | Build tool + dev server |
| Tailwind CSS | 4.3 | Utility-first CSS |
| React Router | 6.26 | Client-side routing |
| Framer Motion | 12.x | Animations |
| Lucide React | 1.26 | Icon system |
| Playwright | latest | E2E browser tests |
| tailwind-merge | 3.6 | Class deduplication |

### DevOps
| Tool | Purpose |
|------|---------|
| GitHub Actions | CI/CD (test, build, deploy) |
| Render | Backend hosting (auto-detects `mvnw` + `pom.xml`) |
| Vercel | Frontend hosting (auto-detects Vite) |
| Flyway | Database migrations |
| k6 | Load testing (`scripts/load-test.js`) |

---

## 🚀 Quick Start

### Prerequisites
- JDK 17+ (`java -version`)
- Node.js 18+ (`node -v`)
- PostgreSQL 15+ (or use `dev` profile for H2)

### Backend (H2 in-memory — no Postgres needed)

```bash
cd backend
cp .env.example .env    # review and adjust if needed
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

- Starts on `http://localhost:8085`
- Flyway seeds 7 demo accounts + sample data (hospitals, patients, doctors, beds, inventory)
- Swagger/health: `http://localhost:8085/api/health`

### Frontend

```bash
cd frontend
cp .env.example .env
npm install
npm run dev
```

- Starts on `http://localhost:5173`
- Proxies `/api/*` → `localhost:8085` (configured in `vite.config.js`)

### Run Tests

```bash
cd backend
./mvnw test    # 64 tests, H2-backed, 3-5 seconds, no Docker required
```

---

## 🔌 API Overview

| Method | Endpoint | Auth | Authorized Roles |
|--------|----------|------|-----------------|
| `GET` | `/api/health` | ✗ | — |
| `POST` | `/api/auth/login` | ✗ | — |
| `POST` | `/api/auth/register` | ✗ | — |
| `POST` | `/api/auth/hospital-signup` | ✗ | — |
| `POST` | `/api/chatbot/chat` | ✗ | — |
| `GET` | `/api/dashboard/analytics` | ✓ | All authenticated |
| `GET/POST` | `/api/tasks` | ✓ | All / DOCTOR+, STAFF |
| `GET` | `/api/tasks/search` | ✓ | All authenticated |
| `PATCH` | `/api/tasks/{id}/progress` | ✓ | Assignee, DOCTOR, STAFF |
| `GET` | `/api/tasks/{id}/timeline` | ✓ | All authenticated |
| `GET/POST` | `/api/users` | ✓ | All / ADMIN |
| `GET` | `/api/users/by-role` | ✓ | All authenticated |
| `POST` | `/api/patients` | ✓ | RECEPTIONIST, ADMIN |
| `GET` | `/api/patients/search` | ✓ | All authenticated |
| `POST` | `/api/patients/{id}/triage` | ✓ | NURSE, DOCTOR, ADMIN |
| `GET` | `/api/patients/{id}/history` | ✓ | DOCTOR, ADMIN |
| `POST` | `/api/medical-records` | ✓ | DOCTOR, ADMIN |
| `GET` | `/api/doctors/available` | ✓ | All authenticated |
| `POST` | `/api/appointments` | ✓ | RECEPTIONIST, ADMIN |
| `PATCH` | `/api/appointments/{id}/status` | ✓ | RECEPTIONIST, DOCTOR, ADMIN |
| `GET` | `/api/queue/doctor/{id}` | ✓ | RECEPTIONIST, DOCTOR, ADMIN |
| `GET` | `/api/pharmacy/inventory` | ✓ | PHARMACIST, ADMIN |
| `POST` | `/api/pharmacy/inventory` | ✓ | PHARMACIST, ADMIN |
| `GET` | `/api/pharmacy/inventory/low-stock` | ✓ | PHARMACIST, ADMIN |
| `POST` | `/api/pharmacy/dispense` | ✓ | PHARMACIST, ADMIN |
| `GET` | `/api/billing/calculate/{patientId}` | ✓ | RECEPTIONIST, ADMIN |
| `POST` | `/api/billing/settle` | ✓ | RECEPTIONIST, ADMIN |
| `GET` | `/api/lab/orders` | ✓ | LAB_TECH, ADMIN |
| `PUT` | `/api/lab/orders/{id}/sample` | ✓ | LAB_TECH, ADMIN |
| `PUT` | `/api/lab/orders/{id}/results` | ✓ | LAB_TECH, DOCTOR, ADMIN |
| `GET` | `/api/beds/available` | ✓ | NURSE, DOCTOR, ADMIN |
| `POST` | `/api/admissions` | ✓ | DOCTOR, ADMIN |
| `GET` | `/api/admissions/active` | ✓ | NURSE, DOCTOR, ADMIN |
| `POST` | `/api/admissions/{id}/nursing-log` | ✓ | NURSE, ADMIN |
| `POST` | `/api/admissions/{id}/discharge` | ✓ | DOCTOR, ADMIN |

Auth header: `Authorization: Bearer <jwt>`

---

## 🌐 Deployment

### Backend → Render

[![Deploy to Render](https://render.com/images/deploy-to-render-button.svg)](https://render.com/deploy)

Render auto-detects Java/Maven projects via `mvnw` + `pom.xml` — **no Dockerfile needed**.

**Settings:**
| Field | Value |
|-------|-------|
| Runtime | Java 17 |
| Build Command | `./mvnw clean package -DskipTests` |
| Start Command | `java -jar target/healthtrack-backend-1.0.0.jar --spring.profiles.active=prod` |

**Required Environment Variables:**
```bash
PORT                    # Render sets this automatically
SPRING_DATASOURCE_URL   # jdbc:postgresql://<host>:5432/<db>
SPRING_DATASOURCE_USERNAME
SPRING_DATASOURCE_PASSWORD
JWT_SECRET              # node -e "console.log(require('crypto').randomBytes(32).toString('hex'))"
APP_CORS_ALLOWED_ORIGINS  # https://medisphere.vercel.app
```

### Frontend → Vercel

[![Deploy to Vercel](https://vercel.com/button)](https://vercel.com/new)

| Field | Value |
|-------|-------|
| Root Directory | `frontend/` |
| Framework | Vite (auto-detected) |
| Build Command | `npm run build` |
| Output Directory | `dist` |

**Required Environment Variable:**
```bash
VITE_API_BASE_URL  # https://medisphere-backend.onrender.com/api
```

### CI/CD Pipeline (GitHub Actions)
- `backend-tests`: `./mvnw test` + `./mvnw package` → fails on any test failure
- `frontend-build`: `npm ci` + `npm run build`
- `deploy-backend`: Railway deploy (on push to `main`)
- `deploy-frontend`: Vercel deploy (on push to `main`)

---

## 📁 Project Structure

```
medisphere/
├── backend/
│   ├── .env.example                   # Backend env template
│   ├── .mvn/jvm.config                # Low-memory JVM flags
│   ├── mvnw / mvnw.cmd               # Maven wrapper
│   ├── pom.xml                        # Dependencies (Java 17, SB 3.3.2)
│   └── src/
│       ├── main/java/com/healthtrack/
│       │   ├── config/                # Security, CORS, DataSeeder, RateLimitAspect
│       │   ├── controller/            # 13 REST controllers
│       │   ├── dto/                   # Request/response DTOs
│       │   ├── entity/                # JPA entities with @Filter
│       │   ├── exception/             # GlobalExceptionHandler
│       │   ├── repository/            # Spring Data JPA
│       │   ├── security/              # JWT, TenantInterceptor
│       │   └── service/               # Business logic + RBAC
│       └── resources/
│           ├── db/migration/          # 13 Flyway migrations
│           ├── application.yml        # Default (Postgres + Flyway)
│           ├── application-dev.yml    # H2 in-memory profile
│           ├── application-prod.yml   # Render prod profile
│           └── application-test.yml   # H2 for tests
│       test/                          # 64 JUnit 5 tests
├── frontend/
│   ├── .env.example                   # Frontend env template
│   ├── vercel.json                    # SPA rewrites for Vercel
│   ├── vite.config.js                 # Vite config + API proxy
│   ├── playwright.config.js           # E2E config
│   ├── index.html
│   ├── package.json
│   └── src/
│       ├── api/client.js              # Full API surface (30+ endpoints)
│       ├── components/                # 17 reusable components
│       ├── context/                   # AuthContext, ToastContext
│       ├── pages/                     # 14 role-specific pages
│       └── styles.css                 # Tailwind imports
├── scripts/
│   ├── load-test.js                   # k6 load test (DevOps signal)
│   └── test-runner.js                 # API smoke test with concurrency checks
├── .github/workflows/ci.yml          # CI/CD pipeline
├── .gitignore
├── MediSphere-Postman-Collection.json # API collection
└── SECURITY_CHECKLIST.md             # Security audit
```

---

## 📊 Testing

| Layer | Tool | Scope | How to Run |
|-------|------|-------|-----------|
| Backend Unit/Integration | JUnit 5 + Spring Boot Test | 64 tests across 13 classes | `cd backend && ./mvnw test` |
| Frontend E2E | Playwright | Login flow | `cd frontend && npx playwright test` |
| API Smoke | Node.js (custom) | Full patient lifecycle, RBAC, tenant isolation, concurrency, billing | `node scripts/test-runner.js` |
| Load Testing | k6 | Auth + role-scoped endpoint stress | `k6 run scripts/load-test.js` |

---

## 📄 License

MIT — see [LICENSE](LICENSE) for details.

---

## 💡 Portfolio Enhancement Suggestions

To maximize recruiter impact, consider adding:

1. **Screenshots** — Add a `/screenshots` folder with annotated images of each dashboard
2. **Demo video** — 2-minute walkthrough video (Loom or OBS) embedded in README
3. **Infrastructure-as-Code** — Add Terraform or Pulumi scripts for Render/Vercel provisioning
4. **Monitoring** — Integrate Spring Boot Actuator + Micrometer with Prometheus/Grafana dashboards
5. **Docker** (optional) — Add a `Dockerfile` for local development convenience, but keep Render deploy Docker-free
6. **Swagger/OpenAPI** — Add `springdoc-openapi-starter-webmvc-ui` for auto-generated API docs at `/swagger-ui.html`
7. **Database diagram** — Include an ERD screenshot showing the schema relationships
8. **Mobile responsiveness** — Verify all dashboards render well on tablet/mobile viewports
9. **Performance budget** — Lighthouse scores, bundle size analysis, API response time targets
