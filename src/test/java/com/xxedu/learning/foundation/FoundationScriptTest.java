package com.xxedu.learning.foundation;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class FoundationScriptTest {

    @Test
    void flywayScriptCreatesSystemTablesOnly() throws Exception {
        String sql = Files.readString(Path.of("src/main/resources/db/migration/V1__init.sql"));

        assertThat(sql).contains("CREATE TABLE sys_user");
        assertThat(sql).contains("CREATE TABLE sys_role");
        assertThat(sql).contains("CREATE TABLE sys_permission");
        assertThat(sql).contains("CREATE TABLE sys_user_role");
        assertThat(sql).contains("CREATE TABLE sys_role_permission");
        assertThat(sql).contains("created_at");
        assertThat(sql).contains("updated_at");
        assertThat(sql).contains("created_by");
        assertThat(sql).contains("updated_by");
        assertThat(sql).contains("deleted");
        assertThat(sql).doesNotContain("CREATE TABLE category");
        assertThat(sql).doesNotContain("CREATE TABLE content");
        assertThat(sql).doesNotContain("CREATE TABLE article");
        assertThat(sql).doesNotContain("CREATE TABLE video");
        assertThat(sql).doesNotContain("INSERT INTO");
    }

    @Test
    void contentMigrationSeedsCategoriesWithoutSelectStar() throws Exception {
        String sql = Files.readString(Path.of("src/main/resources/db/migration/V2__create_content_tables.sql"));

        assertThat(sql).contains("CREATE TABLE category");
        assertThat(sql).contains("CREATE TABLE content");
        assertThat(sql).contains("CREATE TABLE article");
        assertThat(sql).contains("CREATE TABLE video");
        assertThat(sql).contains("剑桥英语");
        assertThat(sql).contains("数学思维");
        assertThat(sql).contains("初中自主学习");
        assertThat(sql).contains("WHERE NOT EXISTS");
        assertThat(sql).contains("CONTENT_PUBLISH");
        assertThat(sql).contains("VIDEO_MANAGE");
        assertThat(sql.toLowerCase()).doesNotContain("select *");
    }

    @Test
    void logicDeleteValuesMatchDeleteFlag() throws Exception {
        String config = Files.readString(Path.of("src/main/resources/application.yml"));

        assertThat(config).contains("logic-delete-value: 1");
        assertThat(config).contains("logic-not-delete-value: 0");
    }
}
