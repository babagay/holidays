package com.proxiad.holidaysapp.exception;

import com.proxiad.holidaysapp.dto.HolidayResponseDto;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.Collections;

import static org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(HolidayNotFoundException.class)
    public ResponseEntity<HolidayResponseDto> processHolidayNotFound(HolidayNotFoundException ex) {

        HolidayResponseDto response = new HolidayResponseDto();
        response.setHolidays(Collections.emptyList());
        response.setMessage(ex.getMessage());

        return ResponseEntity.status(ex.getHttpStatus()).body(response);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.badRequest()
                .body(new ErrorResponse() {
                    @NotNull
                    @Override
                    public HttpStatusCode getStatusCode() {
                        return UNPROCESSABLE_ENTITY;
                    }

                    @NotNull
                    @Override
                    public ProblemDetail getBody() {
                        return ProblemDetail.forStatus(UNPROCESSABLE_ENTITY.value());
                    }
                });
    }
}
