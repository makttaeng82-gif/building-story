package com.game.buildingstory.service;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 기존 저장파일의 H2 ENUM 컬럼을 확장 가능한 문자열 컬럼으로 전환한다.
 *
 * <p>Hibernate의 update 모드는 Java enum에 새 값을 추가해도 기존 H2 ENUM 허용값을 늘리지 않는다.
 * 부서, 튜토리얼과 업무 상태에 새 값이 추가된 기존 저장파일을 시작 시 호환 처리한다. 이미 VARCHAR인
 * 데이터베이스에 같은 명령을 다시 실행해도 기존 값은 유지된다.</p>
 */
@Component
public class CompanySchemaMigration implements ApplicationRunner {
    private final JdbcTemplate jdbcTemplate;
    private final CompanyNewsService companyNewsService;

    public CompanySchemaMigration(JdbcTemplate jdbcTemplate, CompanyNewsService companyNewsService) {
        this.jdbcTemplate = jdbcTemplate;
        this.companyNewsService = companyNewsService;
    }

    @Override
    public void run(ApplicationArguments args) {
        removeRetiredConvertibleBonds();
        jdbcTemplate.execute(
                "ALTER TABLE company_department ALTER COLUMN department_type VARCHAR(40)");
        jdbcTemplate.execute(
                "ALTER TABLE company_core_employee ALTER COLUMN department_type VARCHAR(40)");
        jdbcTemplate.execute(
                "ALTER TABLE player_company ALTER COLUMN tutorial_stage VARCHAR(40)");
        jdbcTemplate.execute(
                "ALTER TABLE company_product_project ALTER COLUMN status VARCHAR(40)");
        jdbcTemplate.execute(
                "ALTER TABLE company_short_term_project ALTER COLUMN status VARCHAR(40)");
        jdbcTemplate.execute(
                "ALTER TABLE company_customer_contract ALTER COLUMN status VARCHAR(40)");
        jdbcTemplate.execute(
                "ALTER TABLE company_service_incident ALTER COLUMN status VARCHAR(40)");
        jdbcTemplate.execute(
                "ALTER TABLE company_compute_construction ALTER COLUMN status VARCHAR(40)");
        companyNewsService.backfillPerformanceArticles();
    }

    private void removeRetiredConvertibleBonds() {
        Integer typeColumnCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS "
                        + "WHERE TABLE_NAME = 'COMPANY_BOND' AND COLUMN_NAME = 'TYPE'",
                Integer.class
        );
        if (typeColumnCount != null && typeColumnCount > 0) {
            jdbcTemplate.update("DELETE FROM company_bond WHERE type = 'CONVERTIBLE' OR status = 'CONVERTED'");
        }
        jdbcTemplate.execute("ALTER TABLE company_bond DROP COLUMN IF EXISTS type");
        jdbcTemplate.execute("ALTER TABLE company_bond DROP COLUMN IF EXISTS conversion_price");
        jdbcTemplate.execute("ALTER TABLE company_bond ALTER COLUMN status VARCHAR(40)");
    }
}
