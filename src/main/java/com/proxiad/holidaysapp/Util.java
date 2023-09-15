package com.proxiad.holidaysapp;

import com.proxiad.holidaysapp.entity.Holiday;
import org.springframework.stereotype.Component;

import java.time.ZonedDateTime;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class Util {

    public List<com.proxiad.holidaysapp.dto.Holiday> holidayEntityToDto(List<Holiday> holidays) {
        return holidays.stream().map(this::produceHolidayDto).collect(Collectors.toList());
    }

    public com.proxiad.holidaysapp.dto.Holiday produceHolidayDto(Holiday holiday) {
        String title = holiday.getTitle();
        ZonedDateTime date = holiday.getHolidayDate();
        com.proxiad.holidaysapp.dto.Holiday dto = new com.proxiad.holidaysapp.dto.Holiday();
        dto.setId(holiday.getId());
        dto.setTitle(title);
        dto.setDate(date);
        return dto;
    }

    public Holiday holidayDtoToEntity(com.proxiad.holidaysapp.dto.Holiday holiday) {
        Holiday entity = new Holiday();
        if (holiday.getId() != null) {
            entity.setId(holiday.getId());
        }
        entity.setTitle(holiday.getTitle());
        entity.setHolidayDate(holiday.getDate());

        return entity;
    }
}
