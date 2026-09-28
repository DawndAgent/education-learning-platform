package com.xxedu.learning.foundation;

import com.xxedu.learning.support.IntegrationTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

class FlywayMigrationTest extends IntegrationTestSupport {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void createsSystemTablesAndContentSeed() {
        assertThat(count("sys_user")).isEqualTo(1);
        assertThat(count("sys_role")).isEqualTo(1);
        assertThat(count("sys_permission")).isEqualTo(19);
        assertThat(count("sys_user_role")).isEqualTo(1);
        assertThat(count("sys_role_permission")).isEqualTo(19);
        assertThat(count("category")).isEqualTo(15);
        assertThat(count("content")).isZero();
        assertThat(count("article")).isZero();
        assertThat(count("video")).isZero();
        assertThat(count("question")).isZero();
        assertThat(count("weekly_question")).isZero();
        assertThat(count("document")).isZero();
        var history = jdbcTemplate.queryForList(
                "select version, description, type, script from flyway_schema_history where success = true");
        assertThat(history)
                .as(history.toString())
                .filteredOn(row -> "1".equals(String.valueOf(row.get("version"))))
                .hasSize(1);
        assertThat(history)
                .filteredOn(row -> "2".equals(String.valueOf(row.get("version"))))
                .hasSize(1);
        assertThat(history)
                .filteredOn(row -> "3".equals(String.valueOf(row.get("version"))))
                .hasSize(1);
        assertThat(history)
                .filteredOn(row -> "4".equals(String.valueOf(row.get("version"))))
                .hasSize(1);
        assertThat(history)
                .filteredOn(row -> "5".equals(String.valueOf(row.get("version"))))
                .hasSize(1);
    }

    private int count(String table) {
        Integer count = jdbcTemplate.queryForObject("select count(*) from " + table, Integer.class);
        return count == null ? 0 : count;
    }
}
