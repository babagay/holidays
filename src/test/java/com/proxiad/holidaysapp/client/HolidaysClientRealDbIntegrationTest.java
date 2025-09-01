package com.proxiad.holidaysapp.client;

import com.proxiad.holidaysapp.dto.HolidayResponseDto;
import com.proxiad.holidaysapp.repository.HolidayRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.AutoConfigureTestEntityManager;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.boot.test.web.server.LocalServerPort;
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

    private final AtomicInteger holidayId = new AtomicInteger();

    private final AtomicReference<String> holidayTitle = new AtomicReference<>();

    @BeforeEach
    void setUp() {
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

        HolidayResponseDto dto = holidaysClient.getOne(holidayId.get());
        assertNotNull(dto);
        assertEquals(holidayTitle.get(), dto.getHolidays().getFirst().getTitle());
    }


}
