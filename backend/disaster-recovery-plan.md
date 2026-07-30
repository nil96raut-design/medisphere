# Enterprise Disaster Recovery & Point-In-Time Recovery (PITR) Strategy

This operational guide defines the Disaster Recovery (DR) protocols, Point-in-Time Recovery (PITR) strategy, and automated database restore testing procedures for the enterprise Hospital Management System.

---

## 1. Service Level Objectives (SLOs)
- **Recovery Point Objective (RPO):** < 1 minute (Continuous WAL Archiving in PostgreSQL/Supabase).
- **Recovery Time Objective (RTO):** < 15 minutes (Automated Failover & Point-in-Time Database Restore).

---

## 2. Automated PostgreSQL Backup Architecture
1. **Continuous WAL Archiving:** All Write-Ahead Logs (WAL) are streamed continuously to S3-compatible encrypted object storage.
2. **Daily Physical Base Backups:** Full PostgreSQL base backups taken daily at `02:00 UTC` using `pg_basebackup`.
3. **Retention Policy:** 30-day point-in-time recovery window with immutable 7-year WORM storage for legal compliance.

---

## 3. Disaster Recovery Execution Runbook

### Scenario A: Accidental Data Corruption / Deletion
1. **Identify Corruption Timestamp (`T_corrupt`).**
2. **Execute Point-in-Time Restore (PITR) to `T_corrupt - 1 sec`:**
   ```bash
   # Supabase / PostgreSQL PITR restore command
   pg_restore --target-time="2026-07-29 10:15:00 UTC" --dbname=postgres_dr
   ```
3. **Verify Integrity & Re-point Connection Pool:** Update HikariCP connection string to the newly restored instance.

### Scenario B: Primary Database Region Outage
1. **Promote Standby Read-Replica in Region B:**
   ```bash
   pg_ctl promote -D /var/lib/postgresql/data
   ```
2. **Failover DNS:** Update Cloudflare / AWS Route53 CNAME record `db.healthtrack.internal` to point to Region B IP.
3. **Resume Write Buffer Processor:** Process any un-flushed writes buffered during the 30-second failover window.

---

## 4. Automated Restore Testing Workflow
- **Frequency:** Weekly automated execution via GitHub Actions / Kubernetes CronJob.
- **Workflow Steps:**
  1. Spin up ephemeral PostgreSQL Docker container in isolated VPC.
  2. Pull latest base backup + WAL logs from S3.
  3. Execute `pg_restore` and run Flyway validate check (`mvn flyway:validate`).
  4. Run integrity test suite querying core patient tables.
  5. Destroy ephemeral instance and post metric `dr.restore_test.status = SUCCESS`.
