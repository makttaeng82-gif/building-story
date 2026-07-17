package com.game.buildingstory.config;

import com.game.buildingstory.service.BuildingCatalog;
import com.game.buildingstory.service.BuildingSpec;
import com.game.buildingstory.service.ReputationCatalog;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Configuration
public class DatabaseMigration {
    /*
     * 파일 기반 H2 DB를 새 코드 구조에 맞춰 보정하는 시작 시점 마이그레이션이다.
     *
     * 개발 중 엔티티 필드가 추가되면 기존 DB 파일에는 컬럼이 없을 수 있다.
     * 이 클래스는 애플리케이션 시작 후 필요한 ALTER TABLE을 실행해 오래된 저장 데이터를 계속 사용할 수 있게 한다.
     */
    @Bean
    ApplicationRunner migrateDatabase(
            JdbcTemplate jdbcTemplate,
            BuildingCatalog buildingCatalog,
            ReputationCatalog reputationCatalog,
            TransactionTemplate transactionTemplate
    ) {
        return args -> {
            jdbcTemplate.execute("ALTER TABLE players ADD COLUMN IF NOT EXISTS coin BIGINT DEFAULT 0");
            jdbcTemplate.update("UPDATE players SET coin = 0 WHERE coin IS NULL");
            jdbcTemplate.execute("ALTER TABLE players ADD COLUMN IF NOT EXISTS securities_cash BIGINT DEFAULT 0");
            jdbcTemplate.update("UPDATE players SET securities_cash = 0 WHERE securities_cash IS NULL");
            jdbcTemplate.execute("ALTER TABLE monthly_record ALTER COLUMN record_type VARCHAR(32)");
            addUniqueConstraint(jdbcTemplate, "owned_stock", "uk_owned_stock_player_key", "player_id, stock_key");
            addUniqueConstraint(jdbcTemplate, "owned_secretary", "uk_owned_secretary_player_key", "player_id, secretary_key");
            addUniqueConstraint(jdbcTemplate, "owned_gift_item", "uk_owned_gift_player_key", "player_id, gift_key");
            addUniqueConstraint(jdbcTemplate, "owned_luxury_item", "uk_owned_luxury_player_key", "player_id, item_key");
            addUniqueConstraint(jdbcTemplate, "secretary_tenant_event", "uk_secretary_tenant_player_key", "player_id, secretary_key");
            removeDuplicateGameEvents(jdbcTemplate);
            addUniqueConstraint(jdbcTemplate, "game_event", "uk_game_event_player_key", "player_id, event_key");
            addUniqueConstraint(jdbcTemplate, "purchase_cooldown", "uk_purchase_cooldown_player_slot", "player_id, city, building_slot");
            jdbcTemplate.execute("ALTER TABLE players ADD COLUMN IF NOT EXISTS economy_version INTEGER DEFAULT 1");
            jdbcTemplate.update("UPDATE players SET economy_version = 1 WHERE economy_version IS NULL");
            transactionTemplate.executeWithoutResult(status -> {
                migrateEconomyVersionTwo(jdbcTemplate, buildingCatalog, reputationCatalog);
                migrateEconomyVersionThree(jdbcTemplate, buildingCatalog);
            });
        };
    }

    private void migrateEconomyVersionTwo(JdbcTemplate jdbcTemplate, BuildingCatalog buildingCatalog, ReputationCatalog reputationCatalog) {
        Integer pendingPlayers = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM players WHERE economy_version < 2", Integer.class);
        if (pendingPlayers == null || pendingPlayers == 0) {
            return;
        }

        migratePlayers(jdbcTemplate, reputationCatalog);
        migrateOwnedBuildings(jdbcTemplate, buildingCatalog);
        markExistingBuildingMilestones(jdbcTemplate);
        migrateOffers(jdbcTemplate, buildingCatalog);
        migrateAuctions(jdbcTemplate, buildingCatalog);
        migrateLegacyLoans(jdbcTemplate);
        jdbcTemplate.update("""
                UPDATE players
                SET move_in_chance_percent = 35,
                    move_out_chance_percent = 18,
                    repair_request_chance_percent = 10
                WHERE economy_version < 2
                """);
        jdbcTemplate.update("UPDATE players SET economy_version = 2 WHERE economy_version < 2");
    }

    private void migratePlayers(JdbcTemplate jdbcTemplate, ReputationCatalog reputationCatalog) {
        jdbcTemplate.query("SELECT id, cash, reputation, employed FROM players WHERE economy_version < 2", resultSet -> {
            long playerId = resultSet.getLong("id");
            long cash = mapMoney(resultSet.getLong("cash"));
            int reputation = mapReputation(resultSet.getInt("reputation"));
            boolean resigned = !resultSet.getBoolean("employed");
            String title = reputationCatalog.currentTier(reputation, resigned).title();
            jdbcTemplate.update("UPDATE players SET cash = ?, reputation = ?, title = ? WHERE id = ?", cash, reputation, title, playerId);
        });
    }

    private void migrateOwnedBuildings(JdbcTemplate jdbcTemplate, BuildingCatalog catalog) {
        jdbcTemplate.query("""
                SELECT b.id, b.city, b.building_slot, b.market_price, b.purchase_price
                FROM owned_building b
                JOIN players p ON p.id = b.player_id
                WHERE p.economy_version < 2
                """, resultSet -> {
            String city = resultSet.getString("city");
            long oldMarketPrice = resultSet.getLong("market_price");
            int slot = resultSet.getObject("building_slot") == null
                    ? inferLegacySlot(city, oldMarketPrice)
                    : resultSet.getInt("building_slot");
            BuildingSpec spec = findSpec(catalog, city, slot);
            long purchasePrice = scaleAmount(resultSet.getLong("purchase_price"), oldMarketPrice, spec.marketPrice());
            jdbcTemplate.update("""
                    UPDATE owned_building
                    SET building_slot = ?, type_name = ?, name = ?, market_price = ?, purchase_price = ?, monthly_rent = ?, trade_cooldown_days = ?
                    WHERE id = ?
                    """, slot, spec.typeName(), spec.name(), spec.marketPrice(), purchasePrice, spec.monthlyRent(), spec.tradeCooldownDays(), resultSet.getLong("id"));
        });
    }

    private void migrateOffers(JdbcTemplate jdbcTemplate, BuildingCatalog catalog) {
        jdbcTemplate.query("""
                SELECT o.id, o.city, o.building_slot, o.market_price, o.offer_price
                FROM building_offer o
                JOIN players p ON p.id = o.player_id
                WHERE p.economy_version < 2
                """, resultSet -> {
            String city = resultSet.getString("city");
            int slot = resultSet.getObject("building_slot") == null ? inferLegacySlot(city, resultSet.getLong("market_price")) : resultSet.getInt("building_slot");
            BuildingSpec spec = findSpec(catalog, city, slot);
            long offerPrice = scaleAmount(resultSet.getLong("offer_price"), resultSet.getLong("market_price"), spec.marketPrice());
            jdbcTemplate.update("""
                    UPDATE building_offer
                    SET building_slot = ?, type_name = ?, name = ?, market_price = ?, offer_price = ?, monthly_rent = ?, trade_cooldown_days = ?
                    WHERE id = ?
                    """, slot, spec.typeName(), spec.name(), spec.marketPrice(), offerPrice, spec.monthlyRent(), spec.tradeCooldownDays(), resultSet.getLong("id"));
        });
    }

    private void markExistingBuildingMilestones(JdbcTemplate jdbcTemplate) {
        jdbcTemplate.query("""
                SELECT DISTINCT b.player_id, b.city, b.building_slot
                FROM owned_building b
                JOIN players p ON p.id = b.player_id
                WHERE p.economy_version < 2
                """, resultSet -> {
            String milestone = "|" + resultSet.getString("city") + ":" + resultSet.getInt("building_slot") + "|";
            jdbcTemplate.update("""
                    UPDATE players
                    SET rewarded_building_milestones = CONCAT(COALESCE(rewarded_building_milestones, ''), ?)
                    WHERE id = ? AND POSITION(? IN COALESCE(rewarded_building_milestones, '')) = 0
                    """, milestone, resultSet.getLong("player_id"), milestone);
        });
    }

    private void migrateAuctions(JdbcTemplate jdbcTemplate, BuildingCatalog catalog) {
        jdbcTemplate.query("""
                SELECT a.id, a.city, a.building_slot, a.market_price
                FROM auction_event a
                JOIN players p ON p.id = a.player_id
                WHERE p.economy_version < 2
                """, resultSet -> {
            String city = resultSet.getString("city");
            int slot = resultSet.getObject("building_slot") == null ? inferLegacySlot(city, resultSet.getLong("market_price")) : resultSet.getInt("building_slot");
            BuildingSpec spec = findSpec(catalog, city, slot);
            jdbcTemplate.update("""
                    UPDATE auction_event
                    SET building_slot = ?, type_name = ?, name = ?, market_price = ?, monthly_rent = ?, trade_cooldown_days = ?
                    WHERE id = ?
                    """, slot, spec.typeName(), spec.name(), spec.marketPrice(), spec.monthlyRent(), spec.tradeCooldownDays(), resultSet.getLong("id"));
        });
    }

    private void migrateLegacyLoans(JdbcTemplate jdbcTemplate) {
        jdbcTemplate.update("""
                UPDATE loan
                SET original_principal = principal,
                    total_repayment = principal,
                    remaining_months = 24,
                    monthly_payment = CASE WHEN MOD(principal, 250) = 0 THEN principal / 250 ELSE principal / 250 + 1 END,
                    delinquent_months = 0
                WHERE player_id IN (SELECT id FROM players WHERE economy_version < 2)
                """);
    }

    private void migrateEconomyVersionThree(JdbcTemplate jdbcTemplate, BuildingCatalog catalog) {
        Integer pendingPlayers = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM players WHERE economy_version < 3", Integer.class);
        if (pendingPlayers == null || pendingPlayers == 0) {
            return;
        }

        refreshOwnedBuildingBalance(jdbcTemplate, catalog);
        refreshOfferBalance(jdbcTemplate, catalog);
        refreshAuctionBalance(jdbcTemplate, catalog);
        jdbcTemplate.update("""
                UPDATE players
                SET securities_cash = coin * 100,
                    coin = 0
                WHERE economy_version < 3
                """);
        jdbcTemplate.update("""
                UPDATE owned_stock
                SET quantity = quantity * 100
                WHERE player_id IN (SELECT id FROM players WHERE economy_version < 3)
                """);
        jdbcTemplate.update("""
                UPDATE stock_trade_history
                SET quantity = quantity * 100,
                    gross_amount = gross_amount * 100,
                    fee = fee * 100,
                    net_amount = net_amount * 100
                WHERE player_id IN (SELECT id FROM players WHERE economy_version < 3)
                """);
        jdbcTemplate.update("UPDATE players SET economy_version = 3 WHERE economy_version < 3");
    }

    private void refreshOwnedBuildingBalance(JdbcTemplate jdbcTemplate, BuildingCatalog catalog) {
        jdbcTemplate.query("""
                SELECT b.id, b.city, b.building_slot, b.market_price
                FROM owned_building b
                JOIN players p ON p.id = b.player_id
                WHERE p.economy_version < 3
                """, resultSet -> {
            String city = resultSet.getString("city");
            int slot = resultSet.getObject("building_slot") == null
                    ? inferLegacySlot(city, resultSet.getLong("market_price"))
                    : resultSet.getInt("building_slot");
            BuildingSpec spec = findSpec(catalog, city, slot);
            jdbcTemplate.update("""
                    UPDATE owned_building
                    SET building_slot = ?, market_price = ?, monthly_rent = ?, trade_cooldown_days = ?
                    WHERE id = ?
                    """, slot, spec.marketPrice(), spec.monthlyRent(), spec.tradeCooldownDays(), resultSet.getLong("id"));
        });
    }

    private void refreshOfferBalance(JdbcTemplate jdbcTemplate, BuildingCatalog catalog) {
        jdbcTemplate.query("""
                SELECT o.id, o.city, o.building_slot, o.market_price, o.offer_price
                FROM building_offer o
                JOIN players p ON p.id = o.player_id
                WHERE p.economy_version < 3
                """, resultSet -> {
            String city = resultSet.getString("city");
            long oldMarketPrice = resultSet.getLong("market_price");
            int slot = resultSet.getObject("building_slot") == null
                    ? inferLegacySlot(city, oldMarketPrice)
                    : resultSet.getInt("building_slot");
            BuildingSpec spec = findSpec(catalog, city, slot);
            long offerPrice = scaleAmount(resultSet.getLong("offer_price"), oldMarketPrice, spec.marketPrice());
            jdbcTemplate.update("""
                    UPDATE building_offer
                    SET building_slot = ?, market_price = ?, offer_price = ?, monthly_rent = ?, trade_cooldown_days = ?
                    WHERE id = ?
                    """, slot, spec.marketPrice(), offerPrice, spec.monthlyRent(), spec.tradeCooldownDays(), resultSet.getLong("id"));
        });
    }

    private void refreshAuctionBalance(JdbcTemplate jdbcTemplate, BuildingCatalog catalog) {
        jdbcTemplate.query("""
                SELECT a.id, a.city, a.building_slot, a.market_price
                FROM auction_event a
                JOIN players p ON p.id = a.player_id
                WHERE p.economy_version < 3
                """, resultSet -> {
            String city = resultSet.getString("city");
            int slot = resultSet.getObject("building_slot") == null
                    ? inferLegacySlot(city, resultSet.getLong("market_price"))
                    : resultSet.getInt("building_slot");
            BuildingSpec spec = findSpec(catalog, city, slot);
            jdbcTemplate.update("""
                    UPDATE auction_event
                    SET building_slot = ?, market_price = ?, monthly_rent = ?, trade_cooldown_days = ?
                    WHERE id = ?
                    """, slot, spec.marketPrice(), spec.monthlyRent(), spec.tradeCooldownDays(), resultSet.getLong("id"));
        });
    }

    private BuildingSpec findSpec(BuildingCatalog catalog, String city, int slot) {
        return catalog.byCity(city).stream()
                .filter(spec -> spec.slot() == slot)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("경제 이전 대상 건물을 찾을 수 없습니다: " + city + " " + slot));
    }

    private int inferLegacySlot(String city, long marketPrice) {
        long[] prices = switch (city) {
            case "청주" -> new long[]{30_000_000L, 65_000_000L, 100_000_000L, 160_000_000L};
            case "세종" -> new long[]{200_000_000L, 300_000_000L, 460_000_000L, 550_000_000L};
            case "대전" -> new long[]{750_000_000L, 1_350_000_000L, 1_800_000_000L, 2_500_000_000L};
            case "부산" -> new long[]{3_500_000_000L, 4_500_000_000L, 7_000_000_000L, 17_500_000_000L};
            case "인천" -> new long[]{40_000_000_000L, 75_000_000_000L, 160_000_000_000L, 370_000_000_000L};
            case "서울" -> new long[]{700_000_000_000L, 1_200_000_000_000L, 3_500_000_000_000L, 10_000_000_000_000L};
            default -> throw new IllegalStateException("알 수 없는 도시: " + city);
        };
        int closestSlot = 1;
        long closestDistance = Long.MAX_VALUE;
        for (int index = 0; index < prices.length; index++) {
            long distance = absoluteDifference(marketPrice, prices[index]);
            if (distance < closestDistance) {
                closestDistance = distance;
                closestSlot = index + 1;
            }
        }
        return closestSlot;
    }

    private long mapMoney(long amount) {
        long[] oldAnchors = {0L, 1_000_000_000L, 10_000_000_000L, 100_000_000_000L, 500_000_000_000L, 1_000_000_000_000L, 3_500_000_000_000L, 10_000_000_000_000L};
        long[] newAnchors = {0L, 1_000_000_000L, 10_000_000_000L, 70_000_000_000L, 140_000_000_000L, 180_000_000_000L, 330_000_000_000L, 450_000_000_000L};
        return interpolate(amount, oldAnchors, newAnchors);
    }

    private int mapReputation(int amount) {
        long[] oldAnchors = {0, 200, 400, 1_000, 2_000, 5_000, 8_500, 15_000, 23_000, 31_000, 38_000, 50_000, 70_000, 120_000, 250_000, 450_000, 700_000, 1_300_000, 6_000_000, 17_000_000, 40_000_000, 70_000_000, 140_000_000, 300_000_000, 500_000_000};
        long[] newAnchors = {0, 50, 120, 250, 500, 800, 1_200, 1_700, 2_300, 3_000, 3_800, 4_800, 6_000, 7_500, 9_000, 11_000, 13_500, 16_000, 19_000, 22_500, 26_000, 30_000, 35_000, 42_000, 50_000};
        return Math.toIntExact(interpolate(amount, oldAnchors, newAnchors));
    }

    private long interpolate(long value, long[] source, long[] target) {
        if (value <= source[0]) {
            return target[0];
        }
        for (int index = 1; index < source.length; index++) {
            if (value <= source[index]) {
                BigDecimal progress = BigDecimal.valueOf(value - source[index - 1])
                        .divide(BigDecimal.valueOf(source[index] - source[index - 1]), 12, RoundingMode.HALF_UP);
                return BigDecimal.valueOf(target[index - 1])
                        .add(progress.multiply(BigDecimal.valueOf(target[index] - target[index - 1])))
                        .setScale(0, RoundingMode.HALF_UP)
                        .longValueExact();
            }
        }
        return target[target.length - 1];
    }

    private long scaleAmount(long amount, long oldBase, long newBase) {
        if (oldBase <= 0) {
            return amount;
        }
        return BigDecimal.valueOf(amount)
                .multiply(BigDecimal.valueOf(newBase))
                .divide(BigDecimal.valueOf(oldBase), 0, RoundingMode.HALF_UP)
                .longValueExact();
    }

    private long absoluteDifference(long left, long right) {
        return left >= right ? left - right : right - left;
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
