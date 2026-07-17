package com.game.buildingstory.config;

import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
public class DatabaseMigration {
    /*
     * 파일 기반 H2 DB를 새 코드 구조에 맞춰 보정하는 시작 시점 마이그레이션이다.
     *
     * 개발 중 엔티티 필드가 추가되면 기존 DB 파일에는 컬럼이 없을 수 있다.
     * 이 클래스는 애플리케이션 시작 후 필요한 ALTER TABLE을 실행해 오래된 저장 데이터를 계속 사용할 수 있게 한다.
     */
    @Bean
    ApplicationRunner migrateDatabase(JdbcTemplate jdbcTemplate) {
        return args -> {
            jdbcTemplate.execute("ALTER TABLE players ADD COLUMN IF NOT EXISTS coin BIGINT DEFAULT 0");
            jdbcTemplate.update("UPDATE players SET coin = 0 WHERE coin IS NULL");
            addUniqueConstraint(jdbcTemplate, "owned_stock", "uk_owned_stock_player_key", "player_id, stock_key");
            addUniqueConstraint(jdbcTemplate, "owned_secretary", "uk_owned_secretary_player_key", "player_id, secretary_key");
            addUniqueConstraint(jdbcTemplate, "owned_gift_item", "uk_owned_gift_player_key", "player_id, gift_key");
            addUniqueConstraint(jdbcTemplate, "owned_luxury_item", "uk_owned_luxury_player_key", "player_id, item_key");
            addUniqueConstraint(jdbcTemplate, "secretary_tenant_event", "uk_secretary_tenant_player_key", "player_id, secretary_key");
            removeDuplicateGameEvents(jdbcTemplate);
            addUniqueConstraint(jdbcTemplate, "game_event", "uk_game_event_player_key", "player_id, event_key");
            addUniqueConstraint(jdbcTemplate, "purchase_cooldown", "uk_purchase_cooldown_player_slot", "player_id, city, building_slot");
        };
    }

    private void addUniqueConstraint(JdbcTemplate jdbcTemplate, String table, String constraint, String columns) {
        Integer duplicateGroups = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM (SELECT 1 FROM " + table + " GROUP BY " + columns + " HAVING COUNT(*) > 1)",
                Integer.class
        );
        if (duplicateGroups != null && duplicateGroups > 0) {
            throw new IllegalStateException(table + " 테이블에 중복 자연키가 있어 고유 제약을 추가할 수 없습니다");
        }
        jdbcTemplate.execute("ALTER TABLE " + table + " ADD CONSTRAINT IF NOT EXISTS " + constraint + " UNIQUE (" + columns + ")");
    }

    private void removeDuplicateGameEvents(JdbcTemplate jdbcTemplate) {
        // 과거 버전에서 같은 이벤트가 중복 생성된 경우 활성 이벤트를 우선 보존한다.
        // 모두 완료된 중복이라면 가장 최근 행만 남겨 화면 상태를 잃지 않으면서 자연키를 복구한다.
        jdbcTemplate.update("""
                DELETE FROM game_event
                WHERE id IN (
                    SELECT id
                    FROM (
                        SELECT id,
                               ROW_NUMBER() OVER (
                                   PARTITION BY player_id, event_key
                                   ORDER BY CASE WHEN status = 'ACTIVE' THEN 0 ELSE 1 END, id DESC
                               ) AS duplicate_order
                        FROM game_event
                    ) ranked_events
                    WHERE duplicate_order > 1
                )
                """);
    }
}
