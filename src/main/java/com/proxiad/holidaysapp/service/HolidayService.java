package com.proxiad.holidaysapp.service;

import com.proxiad.holidaysapp.entity.Holiday;
import com.proxiad.holidaysapp.repository.HolidayRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class HolidayService {

    private final HolidayRepository holidayRepository;

    public HolidayService(HolidayRepository holidayRepository) {
        this.holidayRepository = holidayRepository;
    }

    public Holiday getById(int id) {
        return holidayRepository.findHolidayById(id).stream().findFirst().orElse(null);
    }

    public List<Holiday> getHolidaysByYear(int year) {
        return holidayRepository.findHolidaysByYear(year);
    }

    @Transactional
    public Holiday addHoliday(Holiday holiday) {
        return holidayRepository.save(holiday);
    }

    @Transactional
    public void updateHoliday(Holiday holiday) {
        holidayRepository.save(holiday);
    }

    @Transactional
    public void deleteHoliday(Holiday holiday) {
        holidayRepository.delete(holiday);
    }
}
