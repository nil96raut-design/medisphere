# AGENTS.md — HealthTrack / MediSphere

## Quick start

```bash
# Start Postgres
docker compose up -d db

# Backend (port 8085)
cd backend && mvn spring-boot:run

# Frontend (port 5173, proxies /api -> localhost:8085)
cd frontend && npm install && npm run dev
```

## Commands

| What | Command | Notes |
|---|---|---|
| Backend tests | `cd backend && mvn test` | Uses H2 (PostgreSQL mode) — **no Docker needed** despite README claim |
| Single test class | `cd backend && mvn test -Dtest=TaskServiceTest` | |
| Frontend e2e | `cd frontend && npx playwright test` | Needs backend+frontend running; `frontend/e2e/` |
| Frontend build | `cd frontend && npm run build` | |
| Black-box API test | `node test-runner.js` | Needs backend running on 8085 |
| Load test | `k6 run load-test.js` | Needs backend running |

**No frontend unit tests exist.** No lint or typecheck scripts configured.

## Architecture

- **Backend:** Java 17, Spring Boot 3.3.2, Maven, `com.healthtrack.HealthtrackApplication`
- **Frontend:** React 18 + Vite, Tailwind CSS v4, React Router 6, Framer Motion
- **Auth:** JWT (Bearer token, 24h expiry), BCrypt, `@PreAuthorize` method-level guards, `SubscriptionGateFilter` post-auth
- **Multitenancy:** `TenantInterceptor` extracts hospital from JWT; `TenantValidationAspect` blocks cross-tenant access
- **Security extras:** `RateLimitingAspect` (login endpoint), `AuditAspect` (timeline logging)

## Backend profiles

| Profile | DB | Flyway | Use case |
|---|---|---|---|
| `default` | Postgres (via `DB_URL`) | enabled, `validate` | Production-like local dev |
| `dev` | H2 (PostgreSQL mode) | disabled, `ddl-auto: update` | Quick dev without Docker |
| `test` | H2 (PostgreSQL mode) | disabled, `ddl-auto: create-drop` | **Tests** (this profile) |
| `prod` | Config via env vars | enabled | Docker/CI deployment |

Schema changes go in `backend/src/main/resources/db/migration/V{next}__*.sql` — never touch `ddl-auto`.

## Backend structure

```
com.healthtrack.config/       SecurityConfig, DataSeeder, WebMvcConfig
com.healthtrack.controller/   16 controllers (Auth, Task, Patient, Billing, Lab, Ipd, etc.)
com.healthtrack.service/      15 services
com.healthtrack.entity/       35 entity classes
com.healthtrack.security/     JwtAuthFilter, JwtService, TenantInterceptor, AuditAspect, RateLimitingAspect
com.healthtrack.repository/   Spring Data JPA repos
```

## Roles

`ADMIN`, `DOCTOR`, `RECEPTIONIST`, `NURSE`, `LAB_TECH`, `PHARMACIST`, `PATIENT`

Demo accounts (password: `password123`):
- admin@healthtrack.dev, doctor@healthtrack.dev, patient@healthtrack.dev, staff@healthtrack.dev

## Key API endpoints

- `POST /api/auth/login` — returns `{ token, user }`
- `GET /api/tasks` — scoped by role
- `POST /api/tasks` — doctor/staff only
- `POST /api/tasks/{id}/progress` — assignee, doctor, or staff
- `POST /api/auth/hospital-signup` — registers a new tenant hospital

Full API in `frontend/src/api/client.js`.

## CI/CD

- GitHub Actions (`.github/workflows/ci.yml`): test → build JAR, then deploy backend to Railway, frontend to Vercel
- Backend Dockerfile at `backend/Dockerfile` (multi-stage, uses `--spring.profiles.active=prod`)

## Quirks

- Backend port is **8085**, not 8080 (README.md is wrong)
- `test-runner.js` is a custom Node.js validation script (not Jest/Mocha), uses `fetch` directly
- `load-test.js` is a k6 load test script
- Lombok is used extensively on the backend
- OpenPDF dependency for PDF lab reports
- No frontend unit tests or lint scripts exist