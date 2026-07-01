package com.game.buildingstory.config;

import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
public class DatabaseMigration {
    @Bean
    ApplicationRunner migrateDatabase(JdbcTemplate jdbcTemplate) {
        return args -> {
            jdbcTemplate.execute("ALTER TABLE players ADD COLUMN IF NOT EXISTS coin BIGINT DEFAULT 0");
            jdbcTemplate.update("UPDATE players SET coin = 0 WHERE coin IS NULL");
        };
    }
}
