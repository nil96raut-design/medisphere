package com.healthtrack.support;

import org.junit.jupiter.api.Tag;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/**
 * Base class for integration tests.
 *
 * When Docker + TestContainers ARE available, swap the @ActiveProfiles
 * below to "docker-test" and create a corresponding application-docker-test.yml
 * that uses TestContainers' dynamic PostgreSQL connection.
 *
 * Current configuration (profile "test") uses H2 in PostgreSQL-compatibility
 * mode — no Docker required.  Schema is auto-created by Hibernate with
 * ddl-auto: create-drop.
 */
@Tag("integration")
@SpringBootTest
@ActiveProfiles("test")
@Transactional
public abstract class PostgresTestBase {

}
