package com.proxiad.holidaysapp.dto;

import java.time.ZonedDateTime; 

import lombok.Data;

@Data
public class Holiday {
	
	private Integer id;
	
	private String title;

	private ZonedDateTime date;
}
