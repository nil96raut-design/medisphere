package com.healthtrack.support;

import org.junit.jupiter.api.Tag;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/**
 * Test base that uses H2 in PostgreSQL-compatibility mode (no Docker needed).
 * Entity schema is auto-created by Hibernate (ddl-auto: create-drop).
 *
 * Extend this when Docker is not available.  When Docker IS available,
 * prefer PostgresTestBase for production-fidelity schema validation
 * (Flyway migrations on real Postgres via TestContainers).
 */
@Tag("integration")
@SpringBootTest
@ActiveProfiles("test")
@Transactional
public abstract class H2TestBase {

}
