# Zero-Downtime Database Migration Strategy (Expand -> Migrate -> Contract)

In an enterprise Hospital Management System running 24/7, database schema changes must never disrupt live API calls or cause downtime. We enforce the **Expand -> Migrate -> Contract** pattern for all schema evolutions.

---

## 1. The Expand Phase (Backward Compatible)
- **Goal:** Introduce new schema elements without modifying or dropping existing columns/tables that running applications rely on.
- **Rules:**
  - Add new columns as `NULLABLE` or with default values.
  - Never rename or drop columns/tables in this phase.
  - Both old and new application versions must function concurrently.

```sql
-- Example Expand: Adding residential_address to patients table
ALTER TABLE patients ADD COLUMN residential_address TEXT;
```

---

## 2. The Migrate Phase (Data Sync & Dual Writing)
- **Goal:** Populate the new structure with historical data and write to both old and new locations during application operation.
- **Steps:**
  - Application writes to both `address` (old) and `residential_address` (new).
  - Backfill script or Flyway migration syncs existing historical data.

```sql
-- Example Migrate: Copying legacy address data
UPDATE patients SET residential_address = address WHERE residential_address IS NULL AND address IS NOT NULL;
```

---

## 3. The Contract Phase (Cleanup)
- **Goal:** Safely remove old columns/tables once all active instances of the application are updated to consume only the new schema.
- **Prerequisite:** Zero remaining queries or code references to the deprecated field.

```sql
-- Example Contract (Run in a subsequent deployment release)
ALTER TABLE patients DROP COLUMN IF EXISTS address;
```

---

## Migration Best Practices for Supabase & PostgreSQL
1. **Always Use `CONCURRENTLY` for Index Creation:** Avoid locking tables in production.
2. **Set Locks and Timeouts:** Prevent long-running migrations from locking transactions.
3. **Idempotent Statements:** Use `IF NOT EXISTS` / `IF EXISTS` / `ON CONFLICT DO NOTHING`.
