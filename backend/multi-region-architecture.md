# Multi-Region Architecture & Read-Write Splitting Design

This document details the multi-region high-availability topology, database replication strategy, and read-write splitting implementation for global deployment.

---

## 1. Global Multi-Region Topology

```
                  [ Cloudflare Global Anycast DNS / WAF ]
                                    |
          +-------------------------+-------------------------+
          |                                                   |
[ US-East Region (Primary) ]                      [ EU-West Region (Secondary) ]
  ├── Active API Cluster                            ├── Active API Cluster
  ├── Primary PostgreSQL (Read-Write)               ├── Read-Replica PostgreSQL (Read-Only)
  ├── Primary Redis Cluster                         ├── Secondary Redis Cluster
  └── Kafka Cluster (Primary)                       └── Kafka MirrorMaker 2 (Replica)
```

---

## 2. Database Read-Write Splitting in Spring Boot
The application utilizes Spring Data JPA with `@Transactional(readOnly = true)` annotations to automatically route read queries to local Read Replicas while sending mutating transactions (`INSERT`/`UPDATE`/`DELETE`) to the Primary DB.

### Routing Data Source Implementation:
```java
public class ReplicationRoutingDataSource extends AbstractRoutingDataSource {
    @Override
    protected Object determineCurrentLookupKey() {
        return TransactionSynchronizationManager.isCurrentTransactionReadOnly()
                ? DataSourceType.READ_REPLICA
                : DataSourceType.PRIMARY;
    }
}
```

---

## 3. Data Replication & Consistency Guarantees
- **PostgreSQL Streaming Replication:** Asynchronous physical replication from Primary to Read Replicas with < 100ms replication lag.
- **Kafka MirrorMaker 2:** Multi-cluster Kafka topic replication maintaining event ordering across regions.
- **Cross-Region Redis Replication:** Active-Passive Redis replication for JWT session blocklists and feature flag caching.

---

## 4. Automatic Regional Health Check & Failover
- **Health Probes:** Cloudflare Edge workers send HTTP health checks to `/api/admin/health` every 5 seconds.
- **Failover Trigger:** If Primary Region fails 3 consecutive health probes, DNS automatically switches write traffic to the Secondary Region after promoting its Read Replica.
