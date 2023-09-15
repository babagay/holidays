package com.proxiad.holidaysapp.controller;

import com.proxiad.holidaysapp.Util;
import com.proxiad.holidaysapp.dto.HolidayResponseDto;
import com.proxiad.holidaysapp.exception.HolidayNotCreatedException;
import com.proxiad.holidaysapp.exception.HolidayNotFoundException;
import com.proxiad.holidaysapp.exception.HolidayNotUpdatedException;
import com.proxiad.holidaysapp.service.HolidayService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.proxiad.holidaysapp.dto.Holiday;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/holidays")
public class HolidayController {

    private final HolidayService holidayService;
    private final Util util;

    @Autowired
    public HolidayController(HolidayService holidayService, Util util) {
        this.holidayService = holidayService;
        this.util = util;
    }

    @GetMapping("/holiday/{id}")
    public ResponseEntity<HolidayResponseDto> getHoliday(@PathVariable Integer id) {

        HolidayResponseDto response = new HolidayResponseDto();

        try {
            com.proxiad.holidaysapp.entity.Holiday holiday = holidayService.getById(id);
            Holiday dto = util.produceHolidayDto(holiday);
            response.setHolidays(Collections.singletonList(dto));
        } catch (Exception e) {
            throw new HolidayNotFoundException("Not found");
        }

        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @GetMapping("/{year}")
    public ResponseEntity<List<Holiday>> getHolidayByYear(@PathVariable Integer year) {
        List<com.proxiad.holidaysapp.entity.Holiday> holidays = holidayService.getHolidaysByYear(year);
        List<Holiday> dto = util.holidayEntityToDto(holidays);

        return new ResponseEntity<>(dto, HttpStatus.OK);
    }

    @PostMapping("/holiday")
    public ResponseEntity<HolidayResponseDto> addHoliday(@RequestBody Holiday holiday) {
        List<Holiday> dto;

        try {
            com.proxiad.holidaysapp.entity.Holiday entity = util.holidayDtoToEntity(holiday);
            holidayService.addHoliday(entity);
            dto = util.holidayEntityToDto(Collections.singletonList(entity));
        } catch (Exception e) {
            throw new HolidayNotCreatedException("Can not add holiday");
        }

        HolidayResponseDto response = new HolidayResponseDto(dto, "Holiday added");

        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PutMapping("/holiday")
    public ResponseEntity<HolidayResponseDto> updateHoliday(@RequestBody Holiday holiday) {

        try {
            com.proxiad.holidaysapp.entity.Holiday entity = util.holidayDtoToEntity(holiday);
            holidayService.updateHoliday(entity);
        } catch (Exception e) {
            HolidayNotUpdatedException ex = new HolidayNotUpdatedException("Can not update holiday");
            ex.setHttpStatus(HttpStatus.INTERNAL_SERVER_ERROR);
            throw ex;
        }

        HolidayResponseDto response = new HolidayResponseDto(Collections.singletonList(holiday), "Holiday updated");

        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @DeleteMapping("/holiday")
    public ResponseEntity<HolidayResponseDto> deleteHoliday(@RequestBody Holiday holiday) {

        try {
            com.proxiad.holidaysapp.entity.Holiday entity = util.holidayDtoToEntity(holiday);
            holidayService.deleteHoliday(entity);
        } catch (Exception e) {
            HolidayNotUpdatedException ex = new HolidayNotUpdatedException("Can not delete holiday");
            ex.setHttpStatus(HttpStatus.NO_CONTENT);
        }

        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

}
