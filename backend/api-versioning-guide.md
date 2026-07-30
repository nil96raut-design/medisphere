# Enterprise API Versioning & Backward Compatibility Guide

This guide establishes the API versioning specification, breaking change governance, and client migration lifecycle for HealthTrack.

---

## 1. Versioning Strategy
We enforce **URI Path Versioning** (`/api/v1/...`, `/api/v2/...`) for all enterprise public and internal APIs.

### Rules:
- All new features and major architectural enhancements MUST be published under `/api/v1/` or subsequent version prefixes.
- Pre-existing `/api/...` routes are maintained as backward-compatible aliases.

---

## 2. Breaking vs Non-Breaking Changes

### Non-Breaking Changes (Minor Version Increment - No Route Change Required):
- Adding new optional query parameters.
- Adding new fields to response JSON payloads.
- Adding new API endpoints.

### Breaking Changes (Requires New Major Version `/api/v2/`):
- Removing or renaming existing fields in response JSON.
- Changing data types or format of existing response fields.
- Changing authorization/authentication scope requirements.

---

## 3. Deprecation & Sunset Headers
When an API version or endpoint is scheduled for deprecation, the backend automatically emits standardized IETF HTTP headers:

```http
HTTP/1.1 200 OK
Deprecation: @1773993600
Sunset: Wed, 31 Dec 2026 23:59:59 GMT
Link: <https://docs.healthtrack.io/api/v1-migration>; rel="deprecation"
```
