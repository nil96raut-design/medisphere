# HealthTrack

A full-stack task & progress tracking app for **patients**, **doctors**, and **care team staff**.
Each role signs in and sees a dashboard scoped to them:

- **Patients** see tasks assigned to them and log progress updates.
- **Doctors** and **care team staff** assign tasks to patients, see the whole board, and update any task.
- Every update is recorded on a per-task **timeline** (who changed what, and when).

## Stack

- **Backend:** Java 17, Spring Boot 3 (Web, Data JPA, Security), JWT auth, BCrypt password hashing,
  role-based authorization (`PATIENT` / `DOCTOR` / `STAFF`), Postgres with Flyway-managed schema
  (`db/migration`). `ddl-auto` is `validate` only — schema changes go through a new migration file,
  never through Hibernate auto-DDL.
- **Frontend:** React 18 + Vite, React Router, plain CSS design system (no UI kit) with a custom
  "pulse line" progress indicator.

## Run the backend

Start Postgres (via Docker) first:

```bash
docker compose up -d db
```

Then run the app — it connects to `jdbc:postgresql://localhost:5432/healthtrack` by default
(override with the `DB_URL` / `DB_USER` / `DB_PASSWORD` env vars):

```bash
cd backend
mvn spring-boot:run
```

Starts on `http://localhost:8080`. Flyway applies `db/migration/V1__init.sql` on startup, then
seeds four demo accounts (password for all: `password123`):

| Role          | Email                     |
|---------------|----------------------------|
| Hospital admin| admin@healthtrack.dev      |
| Doctor        | doctor@healthtrack.dev     |
| Patient       | patient@healthtrack.dev    |
| Staff (other) | staff@healthtrack.dev      |

## Run the backend tests

Integration tests spin up a throwaway Postgres via Testcontainers (needs Docker running):

```bash
cd backend
mvn test
```

## Run the frontend

```bash
cd frontend
npm install
npm run dev
```

Starts on `http://localhost:5173` and proxies `/api` calls to the backend on port 8080.

`/login` shows a portal picker (Hospital admin / Doctor / Patient / Other), each linking to its
own tailored sign-in screen (`/login/admin`, `/login/doctor`, `/login/patient`, `/login/staff`).
Every portal has a one-click "use demo" button that fills in the matching seeded account.

## API overview

| Method | Endpoint                       | Who                  |
|--------|---------------------------------|-----------------------|
| POST   | `/api/auth/register`            | anyone                |
| POST   | `/api/auth/login`                | anyone                |
| GET    | `/api/tasks`                     | any signed-in user (scoped by role), unpaginated |
| GET    | `/api/tasks/search?q=&page=&size=`| any signed-in user (scoped by role), paginated + text search |
| POST   | `/api/tasks`                     | doctor / staff        |
| PATCH  | `/api/tasks/{id}/progress`       | assignee, doctor, or staff |
| GET    | `/api/tasks/{id}/timeline`       | any signed-in user    |
| GET    | `/api/users/by-role?role=PATIENT`| any signed-in user    |

Auth uses a `Bearer <jwt>` header. Tokens expire after 24h (`app.jwt.expiration-ms`).

## Production notes

- Replace `app.jwt.secret` in `application.yml` with a securely generated secret before deploying.
- Restrict `app.cors.allowed-origins` to your real frontend domain.
- `docker-compose.yml` is for local dev only — point `DB_URL`/`DB_USER`/`DB_PASSWORD` at a managed
  Postgres instance in production.
