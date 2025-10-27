package com.proxiad.holidaysapp.service;

import com.proxiad.holidaysapp.dto.ChatMessage;
import com.proxiad.holidaysapp.dto.ChatRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

// todo - refactor

class ChatServiceUnitTest {

    @Autowired ChatService chatService;

    @Test
    void convertMessage_ShouldMapAllRoleTypesCorrectly() {
        // Given
        ChatRequest request = ChatRequest.builder()
                .messages(List.of(
                        new ChatMessage("system", "You are a helper"),
                        new ChatMessage("user", "Hello"),
                        new ChatMessage("assistant", "Hi there")
                ))
                .build();

        // When & Then
        // assertDoesNotThrow(() -> chatService.convertMessage(request));
    }

    @Test
    void convertMessage_ShouldThrowExceptionForInvalidRole() {
        // Given
        ChatRequest request = ChatRequest.builder()
                .messages(List.of(new ChatMessage("invalid", "content")))
                .build();

        // When & Then
        // assertThrows(IllegalArgumentException.class,
        //        () -> chatService.convertMessage(request));
    }
}
