package com.proxiad.holidaysapp.client;

import com.proxiad.holidaysapp.Util;
import com.proxiad.holidaysapp.config.TestRestTemplateConfig;
import com.proxiad.holidaysapp.config.TestSecurityConfig;
import com.proxiad.holidaysapp.dto.HolidayResponseDto;
import com.proxiad.holidaysapp.repository.HolidayRepository;
import jakarta.persistence.EntityManager;
import lombok.val;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.AutoConfigureTestEntityManager;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.transaction.TestTransaction;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.boot.test.context.SpringBootTest;

import static com.proxiad.holidaysapp.Util.getTestHoliday;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * [Passed]
 * Basic idea:
 *      use real DB; put test data into real DB; delete data after test
 * Result:
 *      Transactional is used.
 *      real data has been fetched;
 *      To make test data accessible force commit in BeforeEach.
 *      Restrict auto Rollback
 * Issue:
 *      Using Transactional means that all operations in the test,
 *      including @BeforeEach, are performed in a single transaction that is not committed until the end of the test
 *      (or rolled back if @Rollback is used).
 *      However, the HTTP request performed by holidaysClient is processed in a separate thread
 *      and a separate transaction that does not see uncommitted changes from the test transaction.
 *
 * (!) Transactional - create a separate transaction for each test and [by default] rolls back afetr a test
 *      holidayRepository works under the same transaction as a test method, thats why test data is accessible
 *      holidaysClient executed in separate transaction (via HTTP call), which can not see not commited changes from the test
 *      Solution 1: disable transactional behavior. Use manual control over data storing
 *      Solution 2: use manual control over transaction and force commit a test data; manually remove data after test
 *
 *
 * SpringBootTest(webEnvironment = RANDOM_PORT) launch server on random port.
 * LocalServerPort helps to get a currently used port.
 * ReflectionTestUtils.setField dynamically sets a field 'root' in HolidaysClient.
 * H2 DB deployed in memory, every test transaction will be rolled back (@Transactional).
 *
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@AutoConfigureTestEntityManager
@Transactional
@Import({TestSecurityConfig.class, TestRestTemplateConfig.class}) // import security config for tests
@ActiveProfiles("test2") // config points real DB
//@Commit // does not work
@Slf4j
public class HolidaysClientRealDbIntegrationTest {

    @LocalServerPort
    private int port; // Spring will substitute real port

    @Autowired
    private HolidaysClient holidaysClient;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private TestEntityManager testEntityManager;

    @Autowired
    private HolidayRepository holidayRepository;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private Util util;

    @Autowired
    private Environment env;

    private final AtomicInteger holidayId = new AtomicInteger();

    private final AtomicReference<String> holidayTitle = new AtomicReference<>();

    @BeforeEach
    void setUp() {
        checkDatabaseConfiguration("HolidaysClientRealDbIntegrationTest");
        var holiday = getTestHoliday("HolidaysClientRealDbIntegrationTest.java");

        // dynamically set URL in HolidaysClient
        ReflectionTestUtils.setField(
                holidaysClient,
                "root",
                "http://localhost:" + port
        );

        entityManager.persist(holiday);
        entityManager.flush();
        holidayId.set(holiday.getId());
        holidayTitle.set(holiday.getTitle());
        log.info("Holiday added : {}", holiday.getId());

        // [1] Force commit via TestTransaction
        TestTransaction.flagForCommit();
        TestTransaction.end();
        TestTransaction.start();

        // [2]
        // transactionTemplate.execute(status -> {
        //     testEntityManager.merge(holiday);
        //     return null;
        // });

        // [3]
        // testEntityManager.merge(holiday);
        // testEntityManager.flush();
    }

    @AfterEach
    void tearDown() { // use TestTransaction - OK

        // finish current transaction
        if (TestTransaction.isActive()) {
            TestTransaction.flagForCommit();
            TestTransaction.end();
        }

        // start new transaction
        TestTransaction.start();

        try {
            testEntityManager.getEntityManager()
                    .createQuery("DELETE FROM Holiday WHERE id = :id")
                    .setParameter("id", holidayId.get())
                    .executeUpdate();

            // Cleanup data and commit
            TestTransaction.flagForCommit();
        } finally {
            TestTransaction.end();
        }

        log.info("Holiday deleted : {}", holidayId.get());
    }

//    @AfterEach
//    void tearDown() { // [2] use TransactionTemplate to force commit - NOK
//        transactionTemplate.execute(status -> {
//            testEntityManager.getEntityManager()
//                    .createQuery("DELETE FROM Holiday WHERE id = :id")
//                    .setParameter("id", holidayId.get())
//                    .executeUpdate();
//            return null;
//        });
//    }

//    @AfterEach
//    void tearDown() { // [3] explicit transaction control - NOK
//        TransactionStatus status = transactionManager.getTransaction(new DefaultTransactionDefinition());
//        try {
//            testEntityManager.getEntityManager()
//                    .createQuery("DELETE FROM Holiday WHERE id = :id")
//                    .setParameter("id", holidayId.get())
//                    .executeUpdate();
//            transactionManager.commit(status);
//        } catch (Exception e) {
//            transactionManager.rollback(status);
//            throw e;
//        }
//    }

    @Test
    void testGetFirstRealHoliday() { // get Holiday from real application DB
        HolidayResponseDto dto = holidaysClient.getOne(1);
        assertNotNull(dto);
        assertEquals("Knowledge day", dto.getHolidays().getFirst().getTitle());
    }

    @Test
    void testGetMockHoliday() {
        // var items = holidayRepository.findAll(); // debug
        // val all = holidaysClient.getAllHolidays();
        // holidayRepository.findHolidayById(holidayId.get()); // OK

        val dto = holidaysClient.getOne(holidayId.get());
        assertNotNull(dto);
        assertEquals(holidayTitle.get(), dto.getHolidays().getFirst().getTitle());

        log.info("Holiday [{}] title={}", dto.getHolidays().getFirst().getId(), dto.getHolidays().getFirst().getTitle());
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
        assertNotNull(productName);

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
