package com.proxiad.holidaysapp.client;

import com.proxiad.holidaysapp.Util;
import com.proxiad.holidaysapp.dto.HolidayResponseDto;
import com.proxiad.holidaysapp.entity.Holiday;
import com.proxiad.holidaysapp.repository.HolidayRepository;
import com.proxiad.holidaysapp.service.HolidayService;
import jakarta.persistence.EntityManager;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.AutoConfigureTestEntityManager;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static com.proxiad.holidaysapp.Util.getTestHoliday;
import static org.junit.jupiter.api.Assertions.*;

/**
 * [PASSED]
 * Basic idea:
 *      use real DB (original DataSource). Insert test data right here in the test. Call real app via HolidaysClient.
 *      drop test data after test
 * Issues:
 *      Transactional breakes test, so we not using it
 *      Thus, we should cleanup data manually
 *      (!) Could be tested, if data will be stored in original DB after test
 *      Checked
 *
 *
 * Используем реальную БД (original DataSource) как для теста, так и для приложения
 * Вставляем данные через EntityManager или репозитории
 * Делаем HTTP-вызов через HolidaysClient — приложение видит эти данные
 * После теста удаляем тестовые данные вручную
 *
 * Тестирование реального http-вызова к приложению.
 * Transactional - чтобы после теста данные откатывались,
 * при этом Holiday реально сохраняется в базу и доступен для HTTP-запроса
 * (для этого используется TestEntityManager в связке с TestInstance)
 * <p>
 * persistFlushFind сохраняет сущность,
 * делает flush(),
 * делает find() → теперь Holiday гарантированно в базе и виден для контроллера при HTTP-запросе.
 * Id берём уже у saved (а не у объекта holiday из памяти).
 * за счёт flush запись реально доступна серверу.
 * После теста транзакция откатится, и база останется чистой.
 * <p>
 * Возможен вариант без @Transactional, где база "чистится" автоматически через @DirtiesContext
 *
 * Transactional - Spring автоматически управляет транзакциями
 * Rollback - данные автоматически откатываются после каждого теста
 * TestEntityManager - для вспомогательных операций с БД
 * Автоматическая очистка в @AfterEach
 * Изоляция тестов - каждый тест работает в своей транзакции
 */

//@Transactional
//@Rollback
//@DirtiesContext(classMode = AFTER_EACH_TEST_METHOD) // cleanup data automatically
//@TestPropertySource(properties = {
//        "spring.datasource.url=jdbc:h2:mem:testdb",
//        "spring.jpa.hibernate.ddl-auto=create-drop"
//})
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@AutoConfigureTestEntityManager
@Slf4j
class HolidaysClientIntegrationTestHttpReal {

    @LocalServerPort
    private int port;

    @Autowired
    private HolidaysClient holidaysClient;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private TestEntityManager testEntityManager;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private HolidayRepository holidayRepository;

    @Autowired
    private HolidayService holidayService;

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
//    @Transactional(propagation = REQUIRES_NEW)
    @Sql(statements = """
            INSERT INTO holidays (id, title, holidayDate)
            VALUES (100, 'Quest Holiday 100', '2025-08-21T10:00:00Z');
             """) // not working
    void setUp() {
        checkDatabaseConfiguration("BeforeEach");
        log.info("Holidays count Before: {}", holidayRepository.findAll().size());

        val holiday = getTestHoliday("HolidaysClientIntegrationTestHttpReal.java");

        // [0] not working
        // EntityTransaction tx = entityManager.getTransaction(); // берём транзакцию. Здесь ошибка
        // tx.begin();

        // [1]
        // entityManager.persist(holiday);
        // entityManager.flush();

        // tx.commit(); // коммитим, данные. чтобы были видны другим EntityManager'ам

        // val id = holiday.getId();

        // [2]
        // val saved = testEntityManager.persistFlushFind(holiday);
        // val id = saved.getId();

        // holidayId.set(id);

        // [3]
        /* entityManager.getTransaction().begin();   // A открываем транзакцию вручную
        entityManager.persist(holiday);
        entityManager.flush();
        entityManager.getTransaction().commit();
        val id = holiday.getId();
        holidayId.set(id); */

        // [4] cleanup DB before adding test data
        cleanupTestData();
        transactionTemplate.execute(status -> {
            entityManager.persist(holiday);
            entityManager.flush();
            return null;
        });

        val id = holiday.getId();
        holidayId.set(id);

        log.info("Added entity : [{}] {}", id, holiday.getTitle());

        // Настроим root URL
        ReflectionTestUtils.setField(
                holidaysClient,
                "root",
                "http://localhost:" + port
        );
    }

    private Holiday generateHoliday(){
        Holiday holiday = new Holiday();
        val title = "Test Holiday " + UUID.randomUUID().toString().substring(0, 8);
        holiday.setTitle(title);
        holiday.setHolidayDate(ZonedDateTime.now());

        holidayTitle.set(title);

        return holiday;
    }

//    @Transactional
    @AfterEach
    void tearDown() {
        // [1] Очистка через TestEntityManager
        // testEntityManager.clear();

        // [2] Или через EntityManager
        // val tx = entityManager.getTransaction();
        // tx.begin();
        // entityManager.createQuery("DELETE FROM Holiday").executeUpdate();
        // tx.commit();

        // [3] через EntityManager
        // entityManager.clear();

        // [4] Или через EntityManager с транзакцией
        // transactionTemplate.execute(status -> {
        //    entityManager.createQuery("DELETE FROM Holiday").executeUpdate();
        //    return null;
        // });

        // [5] Альтернативный вариант - удаление всех данных c entityManager
        // entityManager.createQuery("DELETE FROM Holiday").executeUpdate();
        // entityManager.flush();

        // [6]:a Удаляем тестовую запись
        //  Not allowed to create transaction on shared EntityManager - use Spring transactions or EJB CMT instead
        /* entityManager.getTransaction().begin();
        entityManager.createQuery("DELETE FROM Holiday h WHERE h.id = :id")
                .setParameter("id", holidayId.get())
                .executeUpdate();
        entityManager.getTransaction().commit();
        log.info("Deleted test holiday with id {}", holidayId.get());
        */

        // [6]:b use Transactional on tearDown(). Not working
        /* entityManager.createNativeQuery("DELETE FROM Holidays h WHERE h.id = :id")
                .setParameter("id", holidayId.get())
                .executeUpdate();
        log.info("Deleted test holiday with id {}", holidayId.get());
        */

        // [7] use repository. Working
        // holidayRepository.deleteById(holidayId.get());
        // log.info("Deleted test holiday with id {}", holidayId.get());

         cleanupTestData();

        // [8] use TransactionTemplate. Working
        /* transactionTemplate.execute(status -> {
            entityManager.createQuery("DELETE FROM Holiday h WHERE h.id = :id")
                    .setParameter("id", holidayId.get())
                    .executeUpdate();
            log.info("Deleted test holiday with id {}", holidayId.get());
            return null;
        });
        */

        log.info("Holidays count After: {}", holidayRepository.findAll().size());
    }

    //@Transactional(propagation = NOT_SUPPORTED)
    @Test
    void testGetHoliday() {
        // HTTP-call to real app
        log.info("Fetching Holiday with id = {}", holidayId.get());
        HolidayResponseDto dto = holidaysClient.getOne(holidayId.get());

        val holidays = holidaysClient.getAllHolidays(); // debug

        assertNotNull(dto);
        assertEquals(holidayTitle.get(), dto.getHolidays().getFirst().getTitle());
    }


    @Test
    void testGetAllHolidays() {
        // When
        val result = holidaysClient.getAllHolidays();

        // Then
        assertNotNull(result);
        assertTrue(result.size() >= 1);
        log.info("Found {} holidays", result.size());
    }

    @Test
    void testCreateHoliday() {
        // Given
        var newHoliday = new com.proxiad.holidaysapp.dto.Holiday();
        newHoliday.setTitle("New Test Holiday 155");
        newHoliday.setDate(ZonedDateTime.now().plusDays(1));

        // When
        val createdHolidayDto = holidaysClient.createHoliday(newHoliday);

        // Then
        assertNotNull(createdHolidayDto);
        assertNotNull(createdHolidayDto.getHolidays().getFirst());
        assertNotNull(createdHolidayDto.getHolidays().getFirst().getId());
        assertTrue(createdHolidayDto.getHolidays().getFirst().getId() > 0);
        assertEquals("New Test Holiday 155", createdHolidayDto.getHolidays().getFirst().getTitle(), "WRONG TITLE");
        log.info("Created holiday: {}", createdHolidayDto.getHolidays().getFirst().getTitle());

        // Cleanup data
        holidayRepository.deleteById(createdHolidayDto.getHolidays().getFirst().getId());
        log.info("Deleted holiday: {}", createdHolidayDto.getHolidays().getFirst().getTitle());
    }

    @Test
    void testUpdateHoliday() {
        // Given
        val id = holidayId.get();
        val holiday = holidayService.getById(id);
        log.info("Holiday before update [{}:{}]", holiday.getId(), holiday.getTitle());
        var newTitle = holiday.getTitle() + "-foo_bar";
        holiday.setTitle(newTitle);
        holiday.setHolidayDate(ZonedDateTime.now());
        val bean = util.produceHolidayDto(holiday);

        // When
        holidaysClient.updateHoliday(bean);
        val updatedHoliday = holidayRepository.findHolidayById(id);

        // Then
        assertNotNull(updatedHoliday);
        assertEquals( newTitle, updatedHoliday.getFirst().getTitle());
        log.info("Updated Test Holiday [{}] Title: {}", updatedHoliday.getFirst().getId(), updatedHoliday.getFirst().getTitle());
    }

    @Test
    void testDeleteHoliday() {
        // Given
        val id = holidayId.get();
        val holiday = holidayService.getById(id);
        val bean = util.produceHolidayDto(holiday);
        log.info("Holiday before deleting [{}:{}]", holiday.getId(), holiday.getTitle());

        // When
        boolean deleted = holidaysClient.deleteHoliday(bean);

        // Then
        assertTrue(deleted);
        val found = holidaysClient.getOne(id);
        assertTrue(found.getHolidays().isEmpty());
        log.info("Holiday with id {} was successfully deleted", id);
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

    private void cleanupTestData() {
        transactionTemplate.execute(status -> {
            try {
                testEntityManager.getEntityManager()
                        .createQuery("DELETE FROM Holiday WHERE title like 'Test Holiday%'")
                        .executeUpdate();
            } catch (Exception e) {
                log.warn("Cleanup failed: {}", e.getMessage());
            }
            return null;
        });
    }
}

