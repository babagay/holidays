package com.proxiad.holidaysapp.service;

import com.openai.core.RequestOptions;
import com.openai.core.http.StreamResponse;
import com.openai.models.ChatModel;
import com.openai.models.chat.completions.*;
import com.openai.models.responses.Response;
import com.openai.models.responses.ResponseCreateParams;
import com.openai.models.responses.ResponseOutputMessage;
import com.proxiad.holidaysapp.config.ChatConfig;
import com.proxiad.holidaysapp.dto.ChatMessage;
import com.proxiad.holidaysapp.dto.ChatRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.stereotype.Service;
import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;

import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.springframework.ai.chat.client.ChatClient;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatService {

    private final ChatConfig chatConfig;
    //    private final OpenAIClient openAIclient;
    //    private final ChatClient springChatClient;
    private final ChatClient.Builder chatClientBuilder;
    private final org.springframework.ai.chat.model.ChatModel chatModel;

    public static final String SYSTEM_PROMPT_SUPPORT_ENGINEER = """
            Ты - профессиональный помощник службы поддержки.
            Напиши вежливый и полезный ответ клиенту на русском языке.
            Будь empathetic и решай проблему клиента.""";
    private final OpenAIClient openAI;
    private static final String SYSTEM_PROMPT_DEFAULT = "Ты полезный помощник. Ты разбираешься во всех сферах жизни."; // Basic system prompt
    private static final String SYSTEM_PROMPT_CLIENT = "Напиши вежливый ответ клиенту на следующее сообщение.";
    private static final String GPT_4O_MINI = "gpt-4o-mini";
    private static final String SYSTEM_ROLE = "system";
    private static final String USER_ROLE = "user";

    public String getAnswer(String question) { // use Spring Chat Client
        val promptTemplate = new PromptTemplate(question);

        val spChatClient = ChatClient.create(chatModel);
        return spChatClient.prompt(promptTemplate.create())
                .system(SYSTEM_PROMPT_DEFAULT)
                .user(question)
                .call()
                .content();
    }

    public Mono<String> getAnswerAsync(String question) {
        return Mono.fromCallable(() -> getAnswer(question))
                .subscribeOn(Schedulers.boundedElastic());
    }

    public String ask(String question) { // use OpenAIClient
        return askWithSystemPrompt(question, SYSTEM_PROMPT_DEFAULT);
    }

    public ChatCompletion askWithParams(ChatRequest request) {
        val messageParams = convertMessage(request);
        val model = request.getModel() != null ? request.getModel() : chatConfig.getDefaultModel();
        val temp = request.getTemperature() != null ? request.getTemperature() : chatConfig.getDefaultTemperature();

        ChatCompletionCreateParams params = ChatCompletionCreateParams.builder()
                .model(model)
                .messages(messageParams)
                //.maxTokens(request.getMaxTokens())
                .temperature(temp)
                .topP(request.getTopP())
                .build();

        log.info("Processing chat request with model [{}], temperature [{}], topP [{}]", model, temp, request.getTopP());

        return openAI.chat().completions().create(params);
    }

    /**
     * New Responses API - recommended
     */
    public String askWithResponsesAPI(String question) {
        log.info("");

        val params = ResponseCreateParams.builder()
                .input(question)
                .model(ChatModel.GPT_4O_MINI)
                .build();

        Response response = openAI.responses().create(params);

        return response.output().stream()
                .filter(Objects::nonNull)
                .map(t -> t.asMessage().content().stream().map(ResponseOutputMessage.Content::toString)
                        .reduce("", (a, b) -> a + b))
                .collect(Collectors.joining());
    }

    public void askStreaming(ChatRequest request, Consumer<String> onChunk) {
        val prompt = new AtomicReference<>("");
        request.getMessages().stream().findFirst().ifPresent(m -> prompt.set(m.getContent()));
        val model = request.getModel();
        val params = ChatCompletionCreateParams.builder()
                .addAssistantMessage(SYSTEM_PROMPT_DEFAULT)
                .addUserMessage(prompt.get())
                .model(model)
                .build()
                .messages();
        val options = ChatCompletionStreamOptions.builder().build();

        val chatParams = ChatCompletionCreateParams.builder()
                .model(model)
                .messages(params)
                .streamOptions(options)
                .build();

        try (val response = openAI.chat().completions().createStreaming(chatParams)) {
            response.stream().forEach(chunk -> {
                chunk.choices()
                        .forEach(choice -> choice.delta().content().ifPresent(onChunk));
            });
        }
    }

    public String askWithSystemPrompt(String question, String systemPrompt) {
        val params = ChatCompletionCreateParams.builder()
                .model(GPT_4O_MINI)
                .messages(createMessages(systemPrompt, question, GPT_4O_MINI))
                //.maxTokens(1000) // Ограничение длины ответа
                .temperature(0.7) // Контроль креативности
                .build();

        try {
            val response = openAI.chat().completions().create(params);
            return extractContent(response);
        } catch (Exception e) {
            log.error("Ошибка при запросе к OpenAI: {}", e.getMessage(), e);
            throw new RuntimeException("Ошибка взаимодействия с OpenAI API", e);
        }
    }

    public String generateReply(String incomingMessage) {
        String userPrompt = "Сообщение клиента: " + incomingMessage;
        return askWithSystemPrompt(userPrompt, SYSTEM_PROMPT_SUPPORT_ENGINEER);
    }

    public String generateReply(String incomingMessage, String systemPrompt) {
        val system = systemPrompt != null ? systemPrompt : SYSTEM_PROMPT_CLIENT;
        val model = chatConfig.getDefaultModel() != null ? chatConfig.getDefaultModel() : ChatModel.GPT_5.asString();
        val params = ChatCompletionCreateParams.builder()
                .model(model)
                .messages(createMessages(system, incomingMessage, model))
                .temperature(chatModel.getDefaultOptions().getTemperature())
                .build();
        val options = RequestOptions.builder().timeout(Duration.ofMinutes(3L)).build();

        val response = openAI.chat().completions().create(params, options);
        return extractContent(response);
    }

    private List<ChatCompletionMessageParam> createMessages(
            String systemMessage, String userMessage, String model) {
        return ChatCompletionCreateParams.builder()
                .addAssistantMessage(systemMessage)
                .addUserMessage(userMessage)
                .model(model)
                .build()
                .messages();
    }

    private String extractContent(ChatCompletion response) {

        if (response == null || response.choices().isEmpty()) {
            throw new RuntimeException("Пустой ответ от OpenAI");
        }

        return response.choices().getFirst().message().content()
                .orElseThrow(() -> new RuntimeException("Пустой контент в ответе"));
    }

    private List<ChatCompletionMessageParam> convertMessage(ChatRequest request) {
        return request.getMessages().stream()
                .map(chatMessage -> switch (chatMessage.getRole()) {
                    case "system" -> {
                        val param = ChatCompletionSystemMessageParam.builder().content(chatMessage.getContent()).build();
                        yield ChatCompletionMessageParam.ofSystem(param);
                    }
                    case "user" -> {
                        val param = ChatCompletionUserMessageParam.builder().content(chatMessage.getContent()).build();
                        yield ChatCompletionMessageParam.ofUser(param);
                    }
                    case "assistant" -> {
                        val param = ChatCompletionAssistantMessageParam.builder().content(chatMessage.getContent()).build();
                        yield ChatCompletionMessageParam.ofAssistant(param);
                    }
                    default -> throw new IllegalStateException("Unexpected value: " + chatMessage.getRole());
                })
                .toList();
    }

    // Examples ...

    // async version for non blocking calls
    public CompletableFuture<String> askAsync(String question) {
        return CompletableFuture.supplyAsync(() -> ask(question));
    }

    public String usePrompt(String prompt) {

        OpenAIClient client = OpenAIOkHttpClient.fromEnv();

        // Создание параметров запроса
        ChatCompletionCreateParams params = ChatCompletionCreateParams.builder()
                .addUserMessage(prompt)
                .model(ChatModel.GPT_4O_MINI)
                .build();
        ChatCompletion chatCompletion = client.chat().completions().create(params);
        return chatCompletion.choices().stream()
                .flatMap(choice -> choice.message().content().stream())
                .reduce("", (a, b) -> a + " " + b);
    }

    public void usePrompt2(String prompt) {
        OpenAIClient client = OpenAIOkHttpClient.fromEnv();

        ResponseCreateParams params = ResponseCreateParams.builder()
                .input("Напиши 'Hello World' на Java")
                .model(ChatModel.GPT_4O_MINI)
                .build();

        Response response = client.responses().create(params);

        // Обработка ответа
        response.output().forEach(content -> {
            if (content != null) {
                System.out.println(content.message());
            }
        });
    }

    public void usePromptAsync(String prompt) throws ExecutionException, InterruptedException {
        OpenAIClient client = OpenAIOkHttpClient.fromEnv();

        ChatCompletionCreateParams params = ChatCompletionCreateParams.builder()
                .addUserMessage("Напиши пример класса на Java")
                .model(ChatModel.GPT_4O_MINI)
                .build();

        // Асинхронный вызов
        CompletableFuture<ChatCompletion> future = client.async()
                .chat().completions().create(params);

        // Обработка результата
        future.thenAccept(chatCompletion -> {
            chatCompletion.choices().stream()
                    .flatMap(choice -> choice.message().content().stream())
                    .forEach(System.out::println);
        }).get(); // get() для ожидания в демо-коде
    }
}
