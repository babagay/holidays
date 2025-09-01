package com.proxiad.holidaysapp.client;

import com.proxiad.holidaysapp.dto.HolidayResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;

import static org.springframework.http.HttpMethod.DELETE;
import static org.springframework.http.MediaType.APPLICATION_JSON;

/**
 * (!) we can pass url via constructor - its more safe and clean
 */
@RequiredArgsConstructor
@Service
@Slf4j
public class HolidaysClient {

    private final RestTemplate restTemplate;

    // private final RestTemplateBuilder restTemplateBuilder;

    @Value("${holidays.api.path}")
    private String holidayPath;

    @Value("${holidays.api.path.all}")
    private String holidayAllPath;

    @Value("${rest.template.rootUrl}")
    private String root;

    public HolidayResponseDto getOne(Integer id) {
        log.info("Fetching holiday with id: {}", id);

        try {
            URI uri = UriComponentsBuilder
                    .fromHttpUrl(root)
                    .path(holidayPath)
                    .pathSegment(id.toString())
                    .build().toUri();
            log.info("Uri: {}", uri);

            ResponseEntity<HolidayResponseDto> response = restTemplate.getForEntity(uri, HolidayResponseDto.class);

            log.info("Response: {}", response);

            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new RuntimeException("API returned: " + response.getStatusCode());
            }

            return response.getBody();
        } catch (RestClientException e) {
            log.error("Failed to fetch holiday with id: {}", id, e);
            throw new RuntimeException("API request failed", e);
        }
    }

    public List<HolidayResponseDto> getAllHolidays() {
        log.info("Fetching all holidays  ");

        URI uri = UriComponentsBuilder
                .fromHttpUrl(root)
                .path(holidayAllPath)
                .build().toUri();

        List<HolidayResponseDto> response = restTemplate.getForObject(uri, List.class);

        assert response != null;
        log.info("Response size: {}", response.size());

        return response;
    }

    public HolidayResponseDto createHoliday(com.proxiad.holidaysapp.dto.Holiday bean) {
        log.info("Create a holiday");

        URI uri = UriComponentsBuilder
                .fromHttpUrl(root)
                .path(holidayPath)
                .build().toUri();

        val dto = restTemplate.postForObject(uri, bean, HolidayResponseDto.class);

        assert dto != null;
        assert !dto.getHolidays().isEmpty();
        log.info("Added holiday : {}", dto.getHolidays().getFirst().getId());

        return dto;
    }

    public void updateHoliday(com.proxiad.holidaysapp.dto.Holiday bean){
        log.info("Update holiday");

        URI uri = UriComponentsBuilder
                .fromHttpUrl(root)
                .path(holidayPath)
                .build().toUri();
         restTemplate.put(uri, bean);
    }

    public boolean deleteHoliday(com.proxiad.holidaysapp.dto.Holiday bean) {
        log.info("Delete holiday : {}", bean.getId());
        URI uri = UriComponentsBuilder
                .fromHttpUrl(root)
                .path(holidayPath)
                .build().toUri();
        val headers = new HttpHeaders();
        headers.setContentType(APPLICATION_JSON);
        val requestEntity = new HttpEntity<>(bean, headers);
        val response = restTemplate.exchange(
                uri,
                DELETE,
                requestEntity,
                HolidayResponseDto.class
        );
        return response.getStatusCode().is2xxSuccessful();
    }

}
