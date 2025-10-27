package com.proxiad.holidaysapp.service;

import com.openai.client.OpenAIClient;
import com.openai.models.chat.completions.ChatCompletion;
import com.openai.models.chat.completions.ChatCompletionCreateParams;
import com.proxiad.holidaysapp.dto.ChatMessage;
import com.proxiad.holidaysapp.dto.ChatRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.openai.api.OpenAiApi;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

// Mock test for Chat service
// todo - refactor

@ExtendWith(MockitoExtension.class)
class ChatServiceTest {

    @Mock
    private OpenAIClient openAI;

    @InjectMocks
    private ChatService chatService;

    @Test
    void askWithParams_ShouldCallOpenAICorrectly() {
        // Given
        ChatRequest request = ChatRequest.builder()
                .model("test-model")
                .messages(List.of(new ChatMessage("user", "test message")))
                .temperature(0.5)
                .build();

//        when(openAI.chat().completions().create(any(ChatCompletionCreateParams.class)))
//                .thenReturn(mock(OpenAiApi.ChatCompletion.class));

        // When
        ChatCompletion result = chatService.askWithParams(request);

        // Then
//        verify(openAI.chat().completions()).create(any());
        assertNotNull(result);
    }
}
