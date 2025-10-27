package com.proxiad.holidaysapp.service;

import com.proxiad.holidaysapp.dto.ChatRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.function.Consumer;

@Service
@Slf4j
@RequiredArgsConstructor
public class DebugChatService {

    private final ChatService chatService;

    public void askStreaming(ChatRequest request, Consumer<String> onChunk) {
        log.info("Starting stream for question: {}", request.getMessages().getFirst().getContent());

        chatService.askStreaming(request, chunk -> {
            log.debug("Chunk received: {} chars", chunk.length());
            System.out.print("[" + chunk.length() + " chars] " + chunk);
            System.out.flush();
            onChunk.accept(chunk);
        });

        log.info("Stream completed");
    }
}