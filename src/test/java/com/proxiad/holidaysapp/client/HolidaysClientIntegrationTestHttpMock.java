package com.proxiad.holidaysapp.client;

import com.proxiad.holidaysapp.dto.HolidayResponseDto;
import com.proxiad.holidaysapp.entity.Holiday;
import jakarta.persistence.EntityManager;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.response.MockRestResponseCreators;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.ZonedDateTime;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;

/**
 * [Passed]
 * <p>
 * вариант интеграционного теста, где HolidaysClient использует реальный HTTP-вызов к приложению, но часть внешнего API можно мокировать через MockRestServiceServer. Так ты сможешь тестировать разные сценарии без необходимости поднимать весь внешний сервис.
 * <p>
 * MockRestServiceServer позволяет перехватывать HTTP-вызовы, сделанные через RestTemplate, и возвращать подготовленные ответы.
 * <p>
 * testGetHolidayFromHttpRealApp() — интеграционный тест с реальной базой H2 и реальным приложением.
 * <p>
 * testGetHolidayMockedExternalApi() — тест с подменой ответа внешнего API, не трогая реальную БД или сервис.
 * <p>
 * Используем ReflectionTestUtils.setField(), чтобы подставить динамический URL с портом.
 * <p>
 * Здесь
 * Сохраняется сущность в H2,
 * Получается её id,
 * Формируется JSON с этим id,
 * И этот JSON мокается как ответ от HTTP-клиента
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Transactional
@Slf4j
public class HolidaysClientIntegrationTestHttpMock {

    @LocalServerPort
    private int port;

    @Autowired
    private HolidaysClient holidaysClient;

    @Autowired
    private EntityManager entityManager;

    private MockRestServiceServer mockServer;

    @Autowired
    private RestTemplate restTemplate; // тот же, что используется в HolidaysClient

    private AtomicInteger holidayId = new AtomicInteger();

    @BeforeEach
    void setUp() {
        // Настраиваем мок-сервер для RestTemplate
        mockServer = MockRestServiceServer.createServer(restTemplate);

        // Загружаем тестовую сущность в H2
        Holiday holiday = new Holiday();
        holiday.setTitle("Test Holiday");
        holiday.setHolidayDate(ZonedDateTime.now());

        entityManager.persist(holiday);
        entityManager.flush();

        val id = holiday.getId();
        holidayId.set(id);

        // Подставляем правильный URL с портом
        ReflectionTestUtils.setField(
                holidaysClient,
                "root",
                "http://localhost:" + port
        );
    }


    @Test
    void testGetHolidayMockedExternalApi() throws Exception {
        // Настраиваем мок-ответ для внешнего API

        val id = holidayId.get();

        String responseBody = """
                {
                    "holidays": [
                        {"id":%d,"title":"Mocked Holiday","holidayDate":"2025-08-20T10:00:00Z"}
                    ]
                }
                """.formatted(id);

        mockServer.expect(ExpectedCount.once(),
                        requestTo("http://localhost:" + port + "/holidays/" + id))
                .andRespond(MockRestResponseCreators.withSuccess(responseBody, MediaType.APPLICATION_JSON));

        // Вызываем HolidaysClient
        HolidayResponseDto dto = holidaysClient.getOne(id);

        assertNotNull(dto);
        assertEquals("Mocked Holiday", dto.getHolidays().get(0).getTitle());

        mockServer.verify(); // Проверяем, что мок был вызван
    }
}

