package com.proxiad.holidaysapp.controller;

import com.openai.models.chat.completions.ChatCompletion;
import com.proxiad.holidaysapp.dto.ChatMessage;
import com.proxiad.holidaysapp.dto.ChatRequest;
import com.proxiad.holidaysapp.service.ChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

/**
 * Chat OpenAI api test
 * Examples:
 * GET http://localhost:8080/chat/simpleOne?q=Как подготовиться к Ironman за год
 * GET http://localhost:8080/chat/simpleTwo?q=Как подготовиться к Ironman за год
 * POST http://localhost:8080/chat/withParams
 * {
 * "model":"gpt-4",
 * "maxTokens":1000,
 * "temperature":2.0,
 * "topP":0.8,
 * "messages":[
 * {
 * "role":"user",
 * "content":"назови 5 самых интересных вершин Болгарии, которые подходят для осеннего хайкинга без ночёвки с семьёй"
 * }
 * ]
 * }
 */

@RestController
@RequestMapping("/chat")
@RequiredArgsConstructor
@Slf4j
public class ChatController {

    private final ChatService chatService;

    @GetMapping("/simpleOne")
    public String ask(@RequestParam String q) {
        return chatService.ask(q);
        // return chatService.generateReply(q);
    }

    @GetMapping("/simpleTwo")
    public String simpleAskTwo(@RequestParam String q) {
        return chatService.getAnswer(q);
    }

    @PostMapping("/withParams")
    public ResponseEntity<ChatCompletion> askWithParams(@RequestBody ChatRequest request) {
        return new ResponseEntity<>(chatService.askWithParams(request), HttpStatus.OK);
    }

    @PostMapping("/stream/simple")
    public String streamChat(@RequestBody ChatRequest request, Model model) {
        val fullResponse = new StringBuilder();

        chatService.askStreaming(request, chunk -> {
            fullResponse.append(chunk);
            // В реальном вебе это не будет работать как стриминг
            // но покажет, что ответ приходит частями
            System.out.println("CHUNK: " + chunk);
        });

        model.addAttribute("response", fullResponse.toString());
        return "chat";
    }

    @PostMapping("/stream/emit")
    public SseEmitter streamChat(@RequestBody ChatRequest request) {
        val emitter = new SseEmitter(60_000L); // 60 секунд timeout

        chatService.askStreaming(request, chunk -> {
            try {
                emitter.send(SseEmitter.event()
                        .data(chunk)
                        .id(UUID.randomUUID().toString()));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });

        emitter.onCompletion(() -> log.info("Stream completed"));
        emitter.onError(throwable -> log.error("Stream error", throwable));

        return emitter;
    }


    @PostMapping(value = "/stream/flux", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> streamChatFlux(@RequestBody ChatRequest request) {
        return Flux.create(sink -> {
            StringBuilder wordBuffer = new StringBuilder();

            chatService.askStreaming(request, character -> {
                wordBuffer.append(character);

                // Отправляем когда накопилось слово или знак препинания
                if (character.matches("[\\s,.!?;:]") || wordBuffer.length() > 8) {
                    if (!wordBuffer.isEmpty()) {
                        ServerSentEvent<String> event = ServerSentEvent.<String>builder()
                                .data(wordBuffer.toString())
                                .id(UUID.randomUUID().toString())
                                .event("message")
                                .build();
                        sink.next(event);
                        wordBuffer.setLength(0); // Очищаем буфер
                    }
                }
            });

            // Отправляем остаток
            if (wordBuffer.length() > 0) {
                ServerSentEvent<String> event = ServerSentEvent.<String>builder()
                        .data(wordBuffer.toString())
                        .build();
                sink.next(event);
            }

            sink.complete();
        });
    }

    // sends data chunk by chunk
    @PostMapping(value = "/stream/test-flux", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> testStream(@RequestBody ChatRequest req) { // chunks demo
        String question = "Расскажи подробно о 10 самых высоких горах Болгарии, " +
                "их истории, маршрутах восхождения и особенностях природы. " +
                "Ответ должен быть развернутым и содержательным.";
        if (req.getMessages().getFirst().getContent() != null){
            question = req.getMessages().getFirst().getContent();
        }
        val request = ChatRequest.builder()
                .model("gpt-4")
                .messages(List.of(new ChatMessage("user", question)))
                .temperature(0.7)
                .build();

        return Flux.create(sink -> {
            // Запускаем askStreaming в отдельном потоке, чтобы не блокировать
            new Thread(() -> {
                try {
                    chatService.askStreaming(request, chunk -> {
                        if (chunk != null && !chunk.isEmpty()) {
                            val event = ServerSentEvent.<String>builder()
                                    .data(chunk)
                                    .event("message")
                                    .id(UUID.randomUUID().toString())
                                    .build();
                            sink.next(event.data() != null ? event.data() : "");
                        }
                    });
                    sink.complete();
                } catch (Exception e) {
                    sink.error(e);
                }
            }).start();
        });
    }

    // displays chunks on UI one by one
    @GetMapping(value = "/stream/test-flux-2", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> streamData() {
        // каждую секунду отправляем новое сообщение
        return Flux.fromStream(IntStream.range(1, 21).mapToObj(i -> "Сообщение №" + i))
                .delayElements(Duration.ofSeconds(1))
                .doOnNext(s -> System.out.println("Отправлено: " + s));
    }
}
