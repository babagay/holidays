package com.proxiad.holidaysapp.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

@Data
@AllArgsConstructor
public class ChatMessage {

    @Pattern(regexp = "system|user|assistant")
    private String role;

    @NotNull
    private String content;
}
