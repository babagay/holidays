package com.proxiad.holidaysapp.dto;

import java.time.ZonedDateTime; 

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;

@Data
@NoArgsConstructor
public class Holiday {
	
	private Integer id;

	@NonNull
	private String title;

	@NonNull
	private ZonedDateTime date;
}
