package com.proxiad.holidaysapp.exception;

import com.proxiad.holidaysapp.dto.HolidayResponseDto;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.Collections;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(HolidayNotFoundException.class)
    public ResponseEntity<HolidayResponseDto> processHolidayNotFound(HolidayNotFoundException ex) {

        HolidayResponseDto response = new HolidayResponseDto();
        response.setHolidays(Collections.emptyList());
        response.setMessage(ex.getMessage());

        return ResponseEntity.status(ex.getHttpStatus()).body(response);
    }
}
