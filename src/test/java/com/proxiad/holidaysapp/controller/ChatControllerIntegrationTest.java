package com.proxiad.holidaysapp.controller;

import com.openai.models.chat.completions.ChatCompletion;
import com.proxiad.holidaysapp.dto.ChatMessage;
import com.proxiad.holidaysapp.dto.ChatRequest;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.client.RestOperations;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Integration Chat service test
 * todo - refactor
 */
@SpringBootTest
@AutoConfigureTestDatabase
class ChatControllerIntegrationTest {

    private RestOperations restTemplate;

    @Test
    void withParams_ShouldReturnCompletion_WhenValidRequest() {
        // Given
        ChatRequest request = ChatRequest.builder()
                .model("gpt-3.5-turbo")
                .messages(List.of(new ChatMessage("user", "Hello")))
                .temperature(0.7)
                .build();

        // When
        var response = restTemplate.postForObject("/withParams", request, ChatCompletion.class);

        // Then
        assertNotNull(response);
    }
}
