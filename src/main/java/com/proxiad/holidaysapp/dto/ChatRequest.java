package com.proxiad.holidaysapp.dto;

import lombok.Builder;
import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.DecimalMax;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.util.List;


/**
 * Model examples:
 *        gpt-3.5-turbo
 *        gpt-4
 *        gpt-4o-mini
 *        gpt-4.1-nano|gpt-4.1-mini|gpt-4.1
 *        gpt-5|gpt-5-codex|gpt-5-pro
 *        o1
 *        dall-e-2
 *        ...
 */
@Data
@Builder
public class ChatRequest {

    private String model;

    @Valid
    @NotNull
    private List<ChatMessage> messages;

    @Min(1)
    private Integer maxTokens;

    @DecimalMin("0.0") @DecimalMax("2.0")
    private Double temperature;

    @DecimalMin("0.0") @DecimalMax("1.0")
    private Double topP;
}
