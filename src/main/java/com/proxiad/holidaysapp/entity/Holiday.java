package com.proxiad.holidaysapp.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.ZonedDateTime;

@Entity
@Table(name = "HOLIDAYS")
@Getter
@Setter
public class Holiday {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Integer id;

    @Column
    String title;

    @Column(name = "HOLIDAYDATE")
    ZonedDateTime holidayDate;
}
