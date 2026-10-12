package com.vynk.common.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.flyway.autoconfigure.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FlywayConfig {

    private static final Logger log = LoggerFactory.getLogger(FlywayConfig.class);

    @Bean
    public FlywayMigrationStrategy flywayMigrationStrategy(@Value("${spring.datasource.url:}") String dbUrl) {
        return flyway -> {
            if (dbUrl.contains(":h2:")) {
                log.info("Detected in-memory H2 database. Skipping Flyway migrations (Hibernate DDL will create schema).");
                return;
            }
            log.info("Executing Flyway migrations against target database: {}", dbUrl);
            flyway.migrate();
        };
    }
}
