package com.proxiad.holidaysapp.config;

import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;

// todo - JdbcSQLSyntaxErrorException: Function "CURRENT_URL" not found

@Configuration
@Slf4j
@RequiredArgsConstructor
public class DataSourceConfig {


    private final EntityManager entityManager;
    private final DataSource dataSource;
    private final JdbcTemplate jdbcTemplate;
    private final Environment env;

    @PostConstruct
    void init(){
        checkDatabaseConfiguration("App Start");
    }

    void checkDatabaseConfiguration(String comment) {
        // Проверка dataSource 1
        log.info("Active profiles ({}): {}", comment, Arrays.toString(env.getActiveProfiles()));
        log.info("Datasource URL ({}): {}", comment, env.getProperty("spring.datasource.url"));
        // Datasource URL: jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE

        // Проверка, какая БД используется
        Object dataSrc = entityManager.getEntityManagerFactory()
                .getProperties()
                .get("javax.persistence.nonJtaDataSource");
        log.info("DataSource ({}): {}", comment, dataSrc);
        // HikariDataSource (HikariPool-1) | EmbeddedDataSourceProxy
        // log.info("Connection: {}", ((HikariDataSource) dataSrc).getConnection());
        // EmbeddedDataSourceProxy  cannot be cast to class com.zaxxer.hikari.HikariDataSource
        // HikariProxyConnection@361198803 wrapping conn1: url=jdbc:h2:mem:testdb user=SA


        // Проверка dataSource 2
//        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
//        String url = jdbcTemplate.queryForObject("SELECT CURRENT_URL() FROM DUAL", String.class);
//        log.info("Current database URL: {}", url);
        String productName = jdbcTemplate.queryForObject(
                "SELECT H2VERSION() FROM DUAL", String.class);
        log.info("Database product ({}): H2 version {}", comment, productName);
        // assertNotNull(productName);

        // Способ 1: Через Environment
        String datasourceUrl = env.getProperty("spring.datasource.url");
        log.info("Configured datasource URL: {}", datasourceUrl);

        // Способ 2: Через JdbcTemplate (более надежно)
        try {
            String actualUrl = jdbcTemplate.queryForObject("CALL CURRENT_URL()", String.class);
            log.info("Actual database URL ({}): {}", comment, actualUrl);

            if (!actualUrl.contains("h2:mem")) {
                log.error("ERROR: Using non-H2 database: {}", actualUrl);
            } else {
                log.info("OK: Using H2 in-memory database");
            }
        } catch (Exception e) {
            log.error("Failed to check database connection: {}", e.getMessage());
        }

        // Способ 3: Проверка через DataSource
        try (Connection connection = dataSource.getConnection()) {
            String url = connection.getMetaData().getURL();
            log.info("DataSource connection URL: {}", url);
        } catch (SQLException e) {
            log.error("Failed to get connection: {}", e.getMessage());
        }
    }
}
