package com.proxiad.holidaysapp.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class HolidayResponseDto {

    private List<Holiday> holidays;

    private String message;
}
