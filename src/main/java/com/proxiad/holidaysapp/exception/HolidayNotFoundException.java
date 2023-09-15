package com.proxiad.holidaysapp.exception;

import lombok.Getter;
import lombok.Setter;
import org.springframework.http.HttpStatus;

@Getter
public class HolidayNotFoundException extends RuntimeException {
    public HolidayNotFoundException(String message) {
        super(message);
        this.message = message;
    }

    private String message = "";

    @Setter
    private HttpStatus httpStatus = HttpStatus.OK;
}
