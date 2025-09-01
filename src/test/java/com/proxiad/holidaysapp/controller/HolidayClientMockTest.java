package com.proxiad.holidaysapp.controller;

import com.proxiad.holidaysapp.client.HolidaysClient;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.proxiad.holidaysapp.dto.Holiday;
import com.proxiad.holidaysapp.dto.HolidayResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.web.client.RestTemplateBuilderConfigurer;
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.DefaultUriBuilderFactory;

import java.time.ZonedDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * Тест изолирован от внешнего API (не надо поднимать настоящий holidays-сервис)
 * Проверяется, что твой HolidaysClient:
 * строит правильный URI,
 * умеет работать с RestTemplate,
 * корректно мапит JSON в DTO.
 * Такие тесты находят ошибки типа: «забыл rootUrl», «не тот path», «сломался mapping».
 *
 * Это не интеграционный тест в полном смысле — мы не проверяем работу с реальным API.
 * JSON-ответ мы формируем сами, то есть тест проверяет только локальную десериализацию и URI, но не факт, что реальный сервис вернёт такой же формат.
 * Если API твой собственный и ты контролируешь обе стороны (клиент + сервер), тогда лучше часть таких проверок вынести в интеграционные тесты.
 */

// we can use this method as a config or @Import(RestTemplateBuilderConfig.class)
//    @TestConfiguration
//    static class TestConfig {
//        @Bean
//        public RestTemplateBuilder restTemplateBuilder() {
//            return new RestTemplateBuilder(new MockServerRestTemplateCustomizer());
//        }
//    }

// these 2 annotations need to be used if we need the full spring context
// Otherwise, we have to create all autowired beans (e.g HolidaysClient) using @TestConfiguration or @Import(RestTemplateBuilderConfig.class)
// @SpringBootTest
// @AutoConfigureMockRestServiceServer

// Do we need this:
//          MockRestService
//			RootUriRequestExpectationManager.bindTo(restTemplate)

@RestClientTest(HolidaysClient.class)
@RequiredArgsConstructor
class HolidayClientMockTest {

    private MockRestServiceServer server;

    @Value("${rest.template.rootUrl}")
    String rootUrl;

    @Autowired
    private  HolidaysClient client;

    @Autowired
    private  RestTemplate restTemplate;

    @Autowired
    private  ObjectMapper objectMapper;

    @TestConfiguration
    static class TestConfig {

        @Value("${rest.template.rootUrl}")
        String rootUrl;

        @Bean
        RestTemplateBuilder restTemplateBuilder(RestTemplateBuilderConfigurer configurer) {

            assert rootUrl != null;

            RestTemplateBuilder builder = configurer.configure(new RestTemplateBuilder());
            DefaultUriBuilderFactory uriBuilderFactory = new DefaultUriBuilderFactory(rootUrl);
            return builder.uriTemplateHandler(uriBuilderFactory);
        }

        @Bean
        HolidaysClient holidaysClient(RestTemplate restTemplate) {
            return new HolidaysClient(restTemplate);
        }

        @Bean
        public RestTemplate restTemplate(RestTemplateBuilder builder) {
            return builder.build();
        }
    }

    @BeforeEach
    void setUp() {
        server = MockRestServiceServer.createServer(restTemplate);
    }

//    @BeforeEach
//    void setUp() {
//        RestTemplate restTemplate = restTemplateBuilder.build();
//        server = MockRestServiceServer.bindTo(restTemplate).build();
//    }

//    @BeforeEach
//    void setup() {
//        RestTemplate restTemplate = restTemplateBuilder.build();
//        server = MockRestServiceServer.createServer(restTemplate);
//    }

//    @BeforeEach
//    void setUp() {
//        RestTemplate restTemplate = restTemplateBuilderConfigured.build();
//        server = MockRestServiceServer.bindTo(restTemplate).build();
//        when(mockRestTemplateBuilder.build()).thenReturn(restTemplate);
//        client = new HolidaysClient(mockRestTemplateBuilder);
//    }

//    @Mock
//    RestTemplateBuilder mockRestTemplateBuilder = new RestTemplateBuilder(new MockServerRestTemplateCustomizer());


    @Test
    void getOne_ShouldReturnHoliday_WhenExists() throws JsonProcessingException {
        val expectedDto = getHolidayResponse();
        var responseBody = objectMapper.writeValueAsString(expectedDto);
        var itemId = 1;

        server.expect(method(GET))
                .andExpect(requestTo(rootUrl + "/holidays/" + itemId))
                .andRespond(withSuccess(responseBody, MediaType.APPLICATION_JSON));

        var actualDto = client.getOne(itemId);

        assertThat(actualDto.getHolidays().size()).isGreaterThan(0);
        assertThat(actualDto.getHolidays().get(0).getId()).isEqualTo(1);
        assertThat(actualDto.getHolidays().get(0).getTitle()).isEqualTo("title");
        assertThat(actualDto.getHolidays().get(0).getDate().toInstant())
                .isEqualTo(expectedDto.getHolidays().get(0).getDate().toInstant());

        server.verify();
    }



    HolidayResponseDto getHolidayResponse() {
        var h = new Holiday();
        h.setId(1);
        h.setTitle("title");
        h.setDate(ZonedDateTime.now());
        return new HolidayResponseDto(List.of(h), "message");
    }



}