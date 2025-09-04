package com.proxiad.holidaysapp.client;

import com.proxiad.holidaysapp.config.TestRestTemplateConfig;
import com.proxiad.holidaysapp.config.TestSecurityConfig;
import com.proxiad.holidaysapp.repository.HolidayRepository;
import com.zaxxer.hikari.HikariDataSource;
import jakarta.persistence.EntityManager;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicInteger;

import static com.proxiad.holidaysapp.Util.getTestHoliday;
import static org.junit.jupiter.api.Assertions.*;

/**
 * [PASSED]
 * basic idea:
 *      use real DB; put test data into real DB; delete data after test
 *      To share data between test and app, Transactional is not used
 *      Manually delete data after test
 *
 * (!) Could we use Transactional and Rollback to remove data automatically?
 *
 * SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
 * — поднимает реальное приложение на случайном порту.
 * LocalServerPort
 * — Spring подставляет реальный порт, на котором поднят сервер.
 * ReflectionTestUtils.setField(...)
 * — устанавливаем корректный URL с портом для HolidaysClient.
 * entityManager.persist(holiday)
 * — сохраняет тестовую сущность в H2 перед тестом.
 * Тест полностью интеграционный: Spring Boot + H2 + HTTP вызов через RestTemplate.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Import({TestSecurityConfig.class, TestRestTemplateConfig.class}) // import security config for tests
@ActiveProfiles("test2") // config points real DB
//@Transactional
@Slf4j
public class HolidaysClientRealDbIntegrationTest2 {

    @LocalServerPort
    private int port; // Spring substitute real port

    @Autowired
    private HolidaysClient holidaysClient;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private HolidayRepository repository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private final AtomicInteger holidayId = new AtomicInteger();

    @BeforeEach
    void setUp() throws SQLException {
        // start a transaction
        val transactionStatus = transactionManager.getTransaction(new DefaultTransactionDefinition());

        checkBDused();

        val holiday = getTestHoliday("HolidaysClientRealDbIntegrationTest2.java");
        entityManager.merge(holiday); // persist
        entityManager.flush();

        // force commit - data will be visible for app
        transactionManager.commit(transactionStatus);

        val id = holiday.getId();
        val saved = repository.findHolidayById(id);
        if (!saved.isEmpty()) {
            log.info("Saved holiday with id: {}", saved.stream().findFirst().get().getId());
            holidayId.set(id);
        }

        // set real url in HolidaysClient
        ReflectionTestUtils.setField(
                holidaysClient,
                "root",
                "http://localhost:" + port
        );
    }

    @AfterEach
    void tearDown() {
        // cleanup data
        TransactionStatus cleanupStatus = transactionManager.getTransaction(new DefaultTransactionDefinition());
        entityManager.createQuery("DELETE FROM Holiday h WHERE h.id = :id")
                .setParameter("id", holidayId.get())
                .executeUpdate();
        entityManager.flush();
        transactionManager.commit(cleanupStatus);
    }

    @Test
    void testGetHolidayFromHttp() {
        // call real HolidaysClient, which uses a real app datasource via RestTemplate
        val dto = holidaysClient.getOne(holidayId.get());

        // val all = holidaysClient.getAllHolidays(); // for debug

        assertNotNull(dto);
        assertNotNull(dto.getHolidays());
        assertNotNull(dto.getHolidays().getFirst());
        assertTrue(dto.getHolidays().getFirst().getTitle().startsWith("Test Holiday"));
    }

    private void checkBDused() throws SQLException {
        // check which DB is utilized
        Object dataSource = entityManager.getEntityManagerFactory()
                .getProperties()
                .get("javax.persistence.nonJtaDataSource");
        log.info("Connection: {}", ((HikariDataSource) dataSource).getConnection());
        // HikariProxyConnection@1860982348 wrapping conn1: url=jdbc:h2:file:~/data/demo user=SA
    }
}
