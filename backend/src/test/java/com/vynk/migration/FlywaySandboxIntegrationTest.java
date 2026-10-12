package com.vynk.migration;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.output.MigrateResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class FlywaySandboxIntegrationTest {

    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = System.getenv("DB_PASSWORD");
    private static final String FRESH_SANDBOX_URL = "jdbc:mysql://localhost:3306/vynk_flyway_fresh_sandbox_20261012?serverTimezone=Asia/Kolkata&sslMode=DISABLED&allowPublicKeyRetrieval=true";
    private static final String EXISTING_SANDBOX_URL = "jdbc:mysql://localhost:3306/vynk_flyway_existing_sandbox_20261012?serverTimezone=Asia/Kolkata&sslMode=DISABLED&allowPublicKeyRetrieval=true";

    @Test
    @Order(1)
    @DisplayName("Fresh Sandbox: Flyway executes V1.0.0 baseline schema on empty database")
    void testFreshSandboxMigration() throws Exception {
        assertNotNull(DB_PASSWORD, "DB_PASSWORD environment variable must be set");

        Flyway flyway = Flyway.configure()
                .dataSource(FRESH_SANDBOX_URL, DB_USER, DB_PASSWORD)
                .locations("classpath:db/migration")
                .baselineOnMigrate(true)
                .baselineVersion("1.0.0")
                .load();

        MigrateResult result = flyway.migrate();
        assertTrue(result.success, "Flyway migration should succeed");
        String currentVersion = flyway.info().current() != null ? flyway.info().current().getVersion().getVersion() : result.targetSchemaVersion;
        assertEquals("1.0.2", currentVersion, "Current schema version should be 1.0.2 after V1.0.2 migration");

        // Verify flyway_schema_history table
        try (Connection conn = DriverManager.getConnection(FRESH_SANDBOX_URL, DB_USER, DB_PASSWORD);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT version, description, type, script, success FROM flyway_schema_history ORDER BY installed_rank")) {
            assertTrue(rs.next(), "flyway_schema_history should contain V1.0.0 record");
            assertEquals("1.0.0", rs.getString("version"));
            assertEquals("baseline schema", rs.getString("description"));
            assertEquals(1, rs.getInt("success"));

            assertTrue(rs.next(), "flyway_schema_history should contain V1.0.1 record");
            assertEquals("1.0.1", rs.getString("version"));
            assertEquals("password reset tokens", rs.getString("description"));
            assertEquals(1, rs.getInt("success"));

            assertTrue(rs.next(), "flyway_schema_history should contain V1.0.2 record");
            assertEquals("1.0.2", rs.getString("version"));
            assertEquals("email verification otps", rs.getString("description"));
            assertEquals(1, rs.getInt("success"));
        }

        // Verify all 20 application tables + flyway_schema_history exist
        List<String> tables = new ArrayList<>();
        try (Connection conn = DriverManager.getConnection(FRESH_SANDBOX_URL, DB_USER, DB_PASSWORD);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SHOW TABLES")) {
            while (rs.next()) {
                tables.add(rs.getString(1));
            }
        }
        assertEquals(21, tables.size(), "Expected 20 application tables + 1 flyway_schema_history table");
        assertTrue(tables.contains("colleges"));
        assertTrue(tables.contains("users"));
        assertTrue(tables.contains("student_profiles"));
        assertTrue(tables.contains("communities"));
        assertTrue(tables.contains("password_reset_tokens"));
        assertTrue(tables.contains("email_verification_otps"));
        assertTrue(tables.contains("flyway_schema_history"));
    }

    @Test
    @Order(2)
    @DisplayName("Existing Sandbox: Flyway baseline-on-migrate establishes baseline metadata and migrates V1.0.1 and V1.0.2")
    void testExistingSandboxBaselineBehavior() throws Exception {
        assertNotNull(DB_PASSWORD, "DB_PASSWORD environment variable must be set");

        Flyway flyway = Flyway.configure()
                .dataSource(EXISTING_SANDBOX_URL, DB_USER, DB_PASSWORD)
                .locations("classpath:db/migration")
                .baselineOnMigrate(true)
                .baselineVersion("1.0.0")
                .load();

        MigrateResult result = flyway.migrate();
        assertTrue(result.success, "Flyway baseline and V1.0.1/V1.0.2 migrations should succeed");
        String currentVersion = flyway.info().current() != null ? flyway.info().current().getVersion().getVersion() : result.targetSchemaVersion;
        assertEquals("1.0.2", currentVersion, "Current schema version should be 1.0.2 after V1.0.2 migration");

        // Verify flyway_schema_history table contains BASELINE record, V1.0.1, and V1.0.2
        try (Connection conn = DriverManager.getConnection(EXISTING_SANDBOX_URL, DB_USER, DB_PASSWORD);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT version, description, type, success FROM flyway_schema_history ORDER BY installed_rank")) {
            assertTrue(rs.next(), "flyway_schema_history should contain baseline record");
            assertEquals("1.0.0", rs.getString("version"));
            assertEquals("BASELINE", rs.getString("type"));
            assertEquals(1, rs.getInt("success"));

            assertTrue(rs.next(), "flyway_schema_history should contain V1.0.1 record");
            assertEquals("1.0.1", rs.getString("version"));
            assertEquals("SQL", rs.getString("type"));
            assertEquals(1, rs.getInt("success"));

            assertTrue(rs.next(), "flyway_schema_history should contain V1.0.2 record");
            assertEquals("1.0.2", rs.getString("version"));
            assertEquals("SQL", rs.getString("type"));
            assertEquals(1, rs.getInt("success"));
        }
    }

    @Test
    @Order(3)
    @DisplayName("Repeatability: Flyway migration is idempotent and reports zero pending migrations")
    void testRepeatability() {
        assertNotNull(DB_PASSWORD, "DB_PASSWORD environment variable must be set");

        // Fresh sandbox second run
        Flyway freshFlyway = Flyway.configure()
                .dataSource(FRESH_SANDBOX_URL, DB_USER, DB_PASSWORD)
                .locations("classpath:db/migration")
                .baselineOnMigrate(true)
                .baselineVersion("1.0.0")
                .load();

        MigrateResult freshSecondRun = freshFlyway.migrate();
        assertEquals(0, freshSecondRun.migrationsExecuted, "Second run on fresh sandbox should execute 0 migrations");
        assertEquals(0, freshFlyway.info().pending().length, "No pending migrations should remain");

        // Existing sandbox second run
        Flyway existingFlyway = Flyway.configure()
                .dataSource(EXISTING_SANDBOX_URL, DB_USER, DB_PASSWORD)
                .locations("classpath:db/migration")
                .baselineOnMigrate(true)
                .baselineVersion("1.0.0")
                .load();

        MigrateResult existingSecondRun = existingFlyway.migrate();
        assertEquals(0, existingSecondRun.migrationsExecuted, "Second run on existing sandbox should execute 0 migrations");
        assertEquals(0, existingFlyway.info().pending().length, "No pending migrations should remain");
    }
}
