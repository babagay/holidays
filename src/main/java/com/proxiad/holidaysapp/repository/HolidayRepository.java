package com.proxiad.holidaysapp.repository;

import com.proxiad.holidaysapp.entity.Holiday;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HolidayRepository extends ListCrudRepository<Holiday,Integer> {

    @Query(value = "SELECT * FROM Holidays WHERE YEAR(holidayDate) IN (:year)", nativeQuery = true)
    List<Holiday> findHolidaysByYear(@Param("year") int year);

    List<Holiday> findHolidayById(@Param("id") int id);
}
