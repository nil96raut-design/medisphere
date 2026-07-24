# MediSphere Security Validation Checklist

## 1. Authentication
- [ ] JWT token is generated on valid login
- [ ] Invalid credentials return 401
- [ ] Missing token on protected endpoints returns 401/403
- [ ] Tampered/expired token returns 401/403
- [ ] Password is hashed (BCrypt) in storage
- [ ] Rate limiting on `/api/auth/login` returns 429 after 20+ rapid requests
- [ ] JWT secret is 256+ bits and not the default value in production

## 2. Multi-Tenant Isolation
- [ ] Hospital A user cannot access Hospital B patients (`GET /api/patients/search`)
- [ ] Hospital A user cannot access Hospital B appointments (`GET /api/appointments`)
- [ ] Hospital A user cannot access Hospital B medical records (`GET /api/patients/{id}/history`)
- [ ] Hospital A doctor cannot admit patient to Hospital B bed
- [ ] Hospital A user cannot triage Hospital B patient
- [ ] Hibernate `@Filter` (tenantFilter) is active on all tenant-scoped entities
- [ ] `TenantValidationAspect` catches cross-tenant data leaks at AOP level
- [ ] `TenantInterceptor` enables Hibernate filter on every HTTP request

## 3. Role-Based Access Control (RBAC)
- [ ] Only ADMIN can create users (`POST /api/users`)
- [ ] Only RECEPTIONIST/ADMIN/DOCTOR can register patients
- [ ] Only DOCTOR/ADMIN can create medical records
- [ ] Only DOCTOR/ADMIN can admit/discharge patients
- [ ] Only PHARMACIST/ADMIN can dispense medication
- [ ] Only PHARMACIST/ADMIN can add inventory stock
- [ ] Only LAB_TECH can process lab results
- [ ] Only RECEPTIONIST/ADMIN can book appointments
- [ ] Patients can only see their own tasks
- [ ] `@PreAuthorize` annotations are present on all controller endpoints
- [ ] Service-layer role checks exist as defense-in-depth

## 4. Input Validation
- [ ] All request DTOs use `@NotBlank`, `@NotNull`, `@Email` validation
- [ ] Invalid input returns 400 with structured JSON error
- [ ] SQL injection attempts do not leak data
- [ ] XSS attempts in string fields are sanitized
- [ ] Numeric fields have `@Min`/`@Max` constraints

## 5. Concurrency Safety
- [ ] Double-booking same doctor slot returns 409
- [ ] Double-booking same bed returns 409
- [ ] Billing with duplicate idempotency key returns 409
- [ ] Token numbers are sequential per doctor per day (no duplicates)
- [ ] `PESSIMISTIC_WRITE` locks are used on critical resources (Doctor, Bed, MedicineStock)

## 6. API Security
- [ ] No sensitive data in URL parameters
- [ ] CORS is restricted to frontend origin
- [ ] Session is stateless (JWT, no server-side session)
- [ ] CSRF protection is disabled intentionally (stateless API)
- [ ] Error messages don't leak stack traces
- [ ] Subscription gate filter blocks expired/free-tier hospitals

## 7. Audit & Logging
- [ ] Audit logs are created for sensitive operations
- [ ] Login attempts are logged
- [ ] Patient data access is logged
- [ ] Admin operations are logged

## 8. Data Protection
- [ ] Patient PII is protected by tenant isolation
- [ ] Password hashes are never exposed in API responses
- [ ] Hospital-scoped queries are filtered by tenant ID
- [ ] OrphanRemoval prevents data leaks on entity deletion
