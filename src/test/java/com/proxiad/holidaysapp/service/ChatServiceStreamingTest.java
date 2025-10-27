package com.proxiad.holidaysapp.service;

import com.proxiad.holidaysapp.config.TestRestTemplateConfig;
import com.proxiad.holidaysapp.config.TestSecurityConfig;
import com.proxiad.holidaysapp.dto.ChatMessage;
import com.proxiad.holidaysapp.dto.ChatRequest;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.boot.test.autoconfigure.orm.jpa.AutoConfigureTestEntityManager;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestConstructor;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@SpringBootTest
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
@Import({TestSecurityConfig.class, TestRestTemplateConfig.class})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@AutoConfigureTestEntityManager
@RequiredArgsConstructor
public class ChatServiceStreamingTest {

    public static final String SYTEM_PROMPT = "Ты - эксперт по Болгарии.";
    private final ChatService chatService;
    private final DebugChatService debugChatService;
    private final String longPrompt = """
        Напиши развернутое эссе о туризме в Болгарии, охватывая следующие аспекты:
        1. Горные маршруты и треккинг
        2. Исторические достопримечательности 
        3. Культурные особенности
        4. Советы по планированию поездки
        5. Лучшее время для посещения
        Ответ должен быть подробным и структурированным.
        """;

    @Test
    void demonstrateStreamingEffect() {

        AtomicInteger chunkCount = new AtomicInteger(0);
        AtomicLong totalChars = new AtomicLong(0);

        System.out.println("🎬 Начинаем стриминг...\n");
        System.out.println("=" .repeat(50));

        long startTime = System.currentTimeMillis();

        debugChatService.askStreaming(getChatRequest(), chunk -> {
            chunkCount.incrementAndGet();
            totalChars.addAndGet(chunk.length());

            // Эмулируем "печать" в реальном времени
            System.out.print(chunk);
            System.out.flush();

            // Небольшая задержка для наглядности
            try { Thread.sleep(15); } catch (InterruptedException ignored) {}
        });

        long endTime = System.currentTimeMillis();

        System.out.println("\n" + "=" .repeat(50));
        System.out.print("\n📊 Статистика:\n");
        System.out.printf("Чанков получено: %d\n", chunkCount.get());
        System.out.printf("Всего символов: %d\n", totalChars.get());
        System.out.printf("Общее время: %d мс\n", endTime - startTime);
    }

    private ChatRequest getChatRequest(){
        return ChatRequest.builder()
                .model("gpt-4")
                .messages(List.of(
                        new ChatMessage("assistant", SYTEM_PROMPT),
                        new ChatMessage("user", longPrompt)))
                .build();
    }
}
