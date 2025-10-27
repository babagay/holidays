package com.proxiad.holidaysapp.config;

import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.errors.OpenAIException;
import io.micrometer.observation.ObservationRegistry;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.model.SimpleApiKey;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
@Slf4j
public class OpenAIConfig {

    @Value("${spring.ai.openai.api-key:${openai.api-key:}}")
    private String apiKey;

    @Value("${spring.ai.openai.base-url:https://api.openai.com/v1}")
    private String baseUrl;

    /**
     * Bean fo OpenAI client
     */
    @Bean
    public OpenAIClient openAI() {
        return OpenAIOkHttpClient.builder()
                .apiKey(apiKey)
                .baseUrl(baseUrl)
                .build();
    }

    @Bean
    @Primary
    public ChatModel chatModel(
            OpenAiApi openAiApi,
            OpenAiChatOptions openAiChatOptions,
            RetryTemplate retryTemplate,
            ObservationRegistry observationRegistry) {
        OpenAiChatOptions options = OpenAiChatOptions.builder()
                .model("gpt-4o-mini")
                .temperature(0.7)
                .maxTokens(1500)
                .topP(0.9)
                .frequencyPenalty(0.1)
                .presencePenalty(0.1)
                .build();

        OpenAiChatModel chatModel = OpenAiChatModel.builder()
                .openAiApi(openAiApi)
                .defaultOptions(openAiChatOptions) // alternative options
                .retryTemplate(retryTemplate)
                .observationRegistry(observationRegistry)
                .build();

        log.info("ChatModel создан с моделью: {}", options.getModel());
        return chatModel;
    }

    @Bean
    public OpenAiApi openAiApi(
            RestClient.Builder restClientBuilder,
            WebClient.Builder webClientBuilder) {

        val key = new SimpleApiKey(apiKey);

        // Настраиваем кастомные заголовки
        MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
        headers.add("OpenAI-Beta", "assistants=v2");
        headers.add("X-Custom-Header", "Spring-AI-Application");

        // Кастомные пути (можно оставить по умолчанию)
        String completionsPath = "/chat/completions";
        String embeddingsPath = "/embeddings";

        // Кастомный обработчик ошибок
        val responseErrorHandler = new DefaultResponseErrorHandler();

        return new OpenAiApi(
                baseUrl,
                key,
                headers,
                completionsPath,
                embeddingsPath,
                restClientBuilder,
                webClientBuilder,
                responseErrorHandler
        );
    }

    @Bean
    public RestClient.Builder restClientBuilder() {
        return RestClient.builder()
                //.requestInterceptor(new BasicAuthenticationInterceptor("",""))
                .requestInterceptor((request, body, execution) -> {
                    log.debug("OpenAI API Request: {} {}", request.getMethod(), request.getURI());
                    return execution.execute(request, body);
                });
    }

    @Bean
    public WebClient.Builder webClientBuilder() {
        return WebClient.builder()
                //.filter(new LoggingFilter())
                .codecs(configurer -> configurer
                        .defaultCodecs()
                        .maxInMemorySize(10 * 1024 * 1024)); // 10MB
    }

    @Bean
    public OpenAiChatOptions openAiChatOptions() {
        return OpenAiChatOptions.builder()
                .model("gpt-4o-mini")
                .temperature(0.7)
                .maxTokens(1000)
                .topP(0.9)
                .frequencyPenalty(0.0)
                .presencePenalty(0.0)
                .build();
    }

    @Bean
    public RetryTemplate retryTemplate() {
        return RetryTemplate.builder()
                .maxAttempts(3)
                .fixedBackoff(1000)
                .retryOn(OpenAIException.class)
                .build();
    }

    @Bean
    public ObservationRegistry observationRegistry() {
        return ObservationRegistry.create();
    }

    // Дополнительный бин для другой модели
//    @Bean
//    @Qualifier("gpt4ChatModel")
//    public ChatModel gpt4ChatModel(@Value("${spring.ai.openai.api-key}") String apiKey) {
//        OpenAiChatOptions options = OpenAiChatOptions.builder()
//                .model("gpt-4")
//                .temperature(0.5)
//                .maxTokens(2000)
//                .build();
//
//        return new OpenAiChatModel(apiKey, options);
//    }


    @Bean
    public ChatClient chatClient(ChatModel chatModel) {
        return ChatClient.create(chatModel); // todo - try to just inject the bean ChatClient
    }

    @Bean
    @Qualifier("advancedChatClient")
    public ChatClient advancedChatClient(ChatModel chatModel) {
        return ChatClient.builder(chatModel)
                .defaultSystem("You are a helpful assistant. Answer in Russian.")
                .defaultOptions(OpenAiChatOptions.builder()
                        .temperature(0.7)
                        .maxTokens(1000)
                        .build())
                .build();
    }
}
