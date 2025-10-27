package com.proxiad.holidaysapp.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "chat")
@Data
public class ChatConfig {
    private String defaultModel = "gpt-3.5-turbo";
    private Double defaultTemperature = 0.7;
}
