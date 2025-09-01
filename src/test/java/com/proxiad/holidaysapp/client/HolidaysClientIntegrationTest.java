package com.proxiad.holidaysapp.client;

import com.proxiad.holidaysapp.dto.HolidayResponseDto;
import com.proxiad.holidaysapp.entity.Holiday;
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
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.AutoConfigureTestEntityManager;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.env.Environment;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static com.proxiad.holidaysapp.Util.getTestHoliday;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.*;

/**
 * [PASSED]
 * basic idea:
 *      use test DB; seed initial data; insert specific data for each test; test real REST API
 * options:
 * a) use @Sql() for seeding
 * b) use sql.init.mode=always + data-locations + schema-locations (Spring will init DB)
 * c) use  hibernate.ddl-auto: create-drop + generate-ddl: true (Hibernate will init DB. not worked for me)
 * (!) Transactional + Rollback are not needed
 *      If use Transactional, there will be 2 different transactions (one for BeforeEach, second for REST)
 *      and data added in BeforeEach will be invisible for REST service
 * (!) It could be checked to use Transactional and 1 of the next:
 *      Explicitly commit transaction in @BeforeEach
 *      Use TestEntityManager with explicit flush()
 *      Switch off transaction for setUp() only (Propagation.NOT_SUPPORTED)
 *
 * Если есть Transactional, приложение использует свою базу, а мок-данные вставляются в тестовую
 *      Либо работа идет в разных транзакциях.
 * Если убрать Transactional, мок-данные попадают в реальную базу приложения и тест проходит
 * Без Transactional, при использовании TransactionTemplate, тест проходит. Юзается основная база
 *
 * Вариант 4 использует Транзакшанал, но данные в тесте выгребаются из основной базы, вместо тестовой
 *
 * Используем тестовую БД как для теста, так и для приложения
 * Кладем в нее данные
 * Получаем их через реальный сервис, а также, прямым вызовом АПИ
 *
 * (!) сейчас проблема в том, что в тестовую базу загружаются данные из src\main\resources\data.sql
 * А куда уходят данные из test-data.sql и BeforeEach, не понятно.
 * Т.е. тестируемый сервис видит основную базу,
 * а контекст теста общается с тестовой базой, в которой загружены данные из test-data.sql и @BeforeEach записи,
 * т.е. в тесте используется EntityManager, который Spring сконфигурил для теста
 * А REST-запросы внутри теста идут в поднятый Spring Boot контекст с контроллерами.
 * И testEntityManager работает с тестовой транзакцией, которая не видна этому REST-контексту.
 */

//@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
//@TestPropertySource(properties = {
//        "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
//        "spring.datasource.driver-class-name=org.h2.Driver",
//        "spring.datasource.username=sa",
//        "spring.datasource.password=",
//        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
//        "spring.jpa.hibernate.ddl-auto=create-drop",
//        "spring.h2.console.enabled=true",
//        "holidays.api.path=/holidays"
//})
//@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)

//@SpringBootTest(
//        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
//        properties = {
//                "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
//                "spring.datasource.driver-class-name=org.h2.Driver",
//                "spring.datasource.username=sa",
//                "spring.datasource.password=",
//                "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
//                "spring.jpa.hibernate.ddl-auto=create-drop",
//                "spring.h2.console.enabled=true"
//        }
//)
//@ActiveProfiles("test")
//@TestInstance(TestInstance.Lifecycle.PER_CLASS)
//@Transactional
//@Rollback


@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource("classpath:application-test.yml")
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY) // ANY (Spring сам настроит базу) или NONE - если использовать свою конфигурацию
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@AutoConfigureTestEntityManager
@Slf4j
//@Transactional
//@Rollback
//@Sql(scripts = {
//        "classpath:schema-h2.sql","/test-data.sql"          // вставляем данные (как альтернатива BeforeEach)
//}, executionPhase = BEFORE_TEST_CLASS)
//@Sql(scripts = {
//        "/test-data-cleanup.sql", // очищаем данные (как альтернатива AfterEach)
//}, executionPhase = AFTER_TEST_CLASS)
public class HolidaysClientIntegrationTest {

    @Autowired
    private HolidaysClient holidaysClient;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private HolidayRepository repository;

    @LocalServerPort
    private int port;

//    @Autowired
//    private PlatformTransactionManager transactionManager;

    @Autowired
    private TestRestTemplate testRestTemplate;

    @Autowired
    private TransactionTemplate transactionTemplate;

    // убираем EntityManager и TransactionManager если используете TestEntityManager
    @Autowired
    private TestEntityManager testEntityManager;

    @Autowired
    private Environment env;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private HolidayRepository holidayRepository;

    private AtomicInteger holidayId = new AtomicInteger();



//    @MockBean
//    private RestTemplate restTemplate;

//    @DynamicPropertySource
//    static void dynamicProperties(DynamicPropertyRegistry registry) {
//        registry.add("rest.template.rootUrl", () -> "http://localhost:" + port); // static port
//        registry.add("holidays.api.path", () -> "/holidays");
//    }

//    @DynamicPropertySource
//    static void configureProperties(DynamicPropertyRegistry registry) {
//        registry.add("spring.datasource.url",
//                () -> "jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1");
//        registry.add("spring.datasource.driver-class-name",
//                () -> "org.h2.Driver");
//        registry.add("spring.datasource.username", () -> "sa");
//        registry.add("spring.datasource.password", () -> "");
//    }

//    @TestConfiguration
//    static class TestConfig {
//        // Переопределение RestTemplate в тестовом контексте
//        @Bean
//        @Primary
//        public RestTemplate testRestTemplate() {
//            return new RestTemplate();
//        }
//    }

    /*
    // IllegalArgumentException: Not a managed type: class com.proxiad.holidaysapp.entity.Holiday
    @TestConfiguration
    static class TestConfig {
        @Bean
        @Primary
        public DataSource dataSource() {
            return DataSourceBuilder.create()
                    .url("jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE")
                    .driverClassName("org.h2.Driver")
                    .username("sa")
                    .password("")
                    .build();
        }

        @Bean
        @Primary
        public LocalContainerEntityManagerFactoryBean entityManagerFactory(
                EntityManagerFactoryBuilder builder,
                @Qualifier("dataSource") DataSource dataSource) {
            return builder
                    .dataSource(dataSource)
                    .packages("com.yourpackage.model")
                    .persistenceUnit("testUnit")
                    .properties(Map.of(
                            "hibernate.hbm2ddl.auto", "create-drop",
                            "hibernate.dialect", "org.hibernate.dialect.H2Dialect",
                            "hibernate.show_sql", "true"
                    ))
                    .build();
        }
    } */

    // УБЕРИТЕ DynamicPropertySource - Spring Boot автоматически настроит тестовую БД
    // Если используем @AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)

//    @DynamicPropertySource
//    static void dynamicProperties(DynamicPropertyRegistry registry) {
//        // Эти настройки имеют высший приоритет и переопределяют все остальные
//        registry.add("spring.datasource.url",
//                () -> "jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE");
//        registry.add("spring.datasource.driver-class-name", () -> "org.h2.Driver");
//        registry.add("spring.datasource.username", () -> "sa");
//        registry.add("spring.datasource.password", () -> "");
//        registry.add("spring.jpa.database-platform", () -> "org.hibernate.dialect.H2Dialect");
//        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
//        registry.add("spring.jpa.properties.hibernate.show_sql", () -> "true");
//        registry.add("spring.jpa.properties.hibernate.format_sql", () -> "true");
//    }


    @BeforeEach
    void setUp() throws SQLException {
        checkDatabaseConfiguration("BeforeEach");

        /*
        // [1]
        val transactionStatus = transactionManager.getTransaction(new DefaultTransactionDefinition());

        // Устанавливаем root URL напрямую после того, как порт известен
        ReflectionTestUtils.setField(holidaysClient, "root", "http://localhost:" + port);

        // Загружаем тестовые данные в H2
        Holiday holiday = new Holiday();
        holiday.setTitle("Best Holiday");
        holiday.setHolidayDate(ZonedDateTime.of( 2025, 8, 15, 14, 30, 0, 0, ZoneId.of("Europe/Sofia")));

        // entityManager.merge(holiday); // persist
        // entityManager.flush();

        repository.save(holiday);

        transactionManager.commit(transactionStatus);

        holidayId.set(holiday.getId());
        log.info("Added entity : [{}] {}", holiday.getId(), holiday.getTitle()); //  Added entity : [5] Test Holiday

        // Настроим root URL
        ReflectionTestUtils.setField(
                holidaysClient,
                "root",
                "http://localhost:" + port
        );
        */

          // [2] Используем TransactionTemplate вместо прямого управления EntityManager
        /* transactionTemplate.execute(status -> {
            Holiday holiday = new Holiday();
            // Уникальное название для избежания конфликтов
            holiday.setTitle("Test Holiday " + UUID.randomUUID().toString().substring(0, 8));
            holiday.setHolidayDate(ZonedDateTime.now().plusDays(new Random().nextInt(100)));

            repository.save(holiday);
            entityManager.flush(); // Явная синхронизация
            holidayId.set(holiday.getId());
            log.info("Added entity : [{}] {}", holiday.getId(), holiday.getTitle());

            return null;
        });

        ReflectionTestUtils.setField(holidaysClient, "root", "http://localhost:" + port);
        */

        // [3] на основе TestEntityManager
        /*
        Holiday holiday = new Holiday();
        holiday.setTitle("Test Holiday " + UUID.randomUUID().toString().substring(0, 8));
        holiday.setHolidayDate(ZonedDateTime.now());

        // TestEntityManager автоматически управляет транзакциями
        Holiday savedHoliday = testEntityManager.persistFlushFind(holiday);
        holidayId.set(savedHoliday.getId());

        log.info("Added entity : [{}] {}", savedHoliday.getId(), savedHoliday.getTitle());

        ReflectionTestUtils.setField(holidaysClient, "root", "http://localhost:" + port);
         */

        // [4] используем TestEntityManager в связке с Transactional
        /*
        Holiday holiday = new Holiday();
        holiday.setTitle("Test Holiday " + UUID.randomUUID().toString().substring(0, 8));
        holiday.setHolidayDate(ZonedDateTime.now());

        Holiday savedHoliday = testEntityManager.persistFlushFind(holiday); // работает только внутри транзакции
        holidayId.set(savedHoliday.getId());

        log.info("Added entity : [{}] {}", savedHoliday.getId(), savedHoliday.getTitle());
        ReflectionTestUtils.setField(holidaysClient, "root", "http://localhost:" + port);
         */

        // [5] use JpaRepository
        long countBefore = holidayRepository.count();
        log.info("Count before Adding data: {}", countBefore);

        val holiday = getTestHoliday("HolidaysClientIntegrationTest.java");

        Holiday savedHoliday = holidayRepository.save(holiday);
        holidayId.set(savedHoliday.getId());

        var countAfter = holidayRepository.count();
        log.info("Count After data: {}", countAfter);

        log.info("Added entity : [{}] {}", savedHoliday.getId(), savedHoliday.getTitle());
        ReflectionTestUtils.setField(holidaysClient, "root", "http://localhost:" + port);

    }

    @AfterEach
    void tearDown() {
        // [a]
        // entityManager.clear();

        //  [1] Not allowed to create transaction on shared EntityManager - use Spring transactions or EJB CMT instead
//        entityManager.getTransaction().begin();
//        entityManager.createQuery("DELETE FROM Holiday h WHERE h.id = :id")
//                .setParameter("id", holidayId.get())
//                .executeUpdate();
//        entityManager.getTransaction().commit();
//        log.info("Deleted test holiday with id {}", holidayId.get());

        // [2] Очищаем данные после каждого теста
        /*
        transactionTemplate.execute(status -> {
            entityManager.createQuery("DELETE FROM Holiday h WHERE h.id = :id").setParameter("id", holidayId.get()).executeUpdate();
            entityManager.flush();
            log.info("Deleted test holiday with id {}", holidayId.get());
            return null;
        });
         */

        // [4] Очистка через TestEntityManager
        // При использовании hibernate.ddl-auto: create-drop
        // база удаляется после каждого теста перед хуком @AfterEach, поэтому, здесь таблица Holidays уже отсутствует
        // Особенно, если еще и используется связка @Transactional + @Rollback
//        testEntityManager.remove(testEntityManager.find(Holiday.class, holidayId.get()));
//        testEntityManager.flush();
//        log.info("Deleted test holiday with id {}", holidayId.get());
    }

    @Test
    void testGetOneReal() {
        var count = holidayRepository.count();
        log.info("Count data rows before test: {}", count);

        var items = holidayRepository.findAll();

        val all = holidaysClient.getAllHolidays();

        val id = holidayId.get();
        HolidayResponseDto response = holidaysClient.getOne(id);
        assertNotNull(response, "Response should not be null");
        assertNotNull(response.getHolidays(), "Holidays should not be null");
        assertNotEquals(0, response.getHolidays().size(), "Holidays size should not be 0"); // fail
        assertEquals(id, response.getHolidays().getFirst().getId(), "Holiday ID should match " + id);
        assertTrue( response.getHolidays().getFirst().getTitle().startsWith("Test Holiday"), "Holiday Title should start from 'Test Holiday'" );
        assertNotNull(response.getHolidays().getFirst().getTitle(), "Holiday name should not be null");

        log.info("Received holiday: {} - {}", response, response.getHolidays().getFirst().getTitle());
    }

    @Test
    void testGetOneDirect() {
        val all = holidaysClient.getAllHolidays(); // for debug
        val id = 10; //holidayId.get(); // id: 10 - test-data.sql

        checkDatabaseConfiguration("testGetOneDirect");

        // Прямой запрос к API для проверки
        ResponseEntity<HolidayResponseDto> response = testRestTemplate.getForEntity(
                "/holidays/" + id,
                HolidayResponseDto.class
        );

        log.info("Direct call for id = {}", id);

        assertEquals(OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().getHolidays());
        assertNotEquals(0, response.getBody().getHolidays().size(), "Holidays size should not be 0");
        assertTrue(response.getBody().getHolidays().get(0).getTitle().startsWith("Test Knowledge day"));

        log.info("Directly received holiday = {}", response.getBody().getHolidays().getFirst().getTitle());
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
