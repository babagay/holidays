package com.proxiad.holidaysapp.client;

import com.proxiad.holidaysapp.dto.Holiday;
import com.proxiad.holidaysapp.dto.HolidayResponseDto;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.time.ZonedDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

/**
 * [PASSED]
 */
@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Slf4j
public class HolidaysClientMockIntegrationTest {

    @MockBean
    private HolidaysClient holidaysClient; // мокируем сервис

    private HolidayResponseDto mockHolidayResponse;

    @BeforeEach
    void setUp() {
        // Подготавливаем тестовые данные
        Holiday holiday = new Holiday();
        holiday.setId(100);
        holiday.setTitle("Test Holiday");
        holiday.setDate(ZonedDateTime.now());

        mockHolidayResponse = new HolidayResponseDto();
        mockHolidayResponse.setHolidays(List.of(holiday));

        // Настраиваем мок, чтобы при вызове getOne(100) возвращался тестовый объект
        when(holidaysClient.getOne(100)).thenReturn(mockHolidayResponse);
    }

    @Test
    void testGetMockHoliday() {
        HolidayResponseDto dto = holidaysClient.getOne(100);
        assertNotNull(dto);
        assertEquals("Test Holiday", dto.getHolidays().get(0).getTitle());
    }
}
