package com.proxiad.holidaysapp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
//@Import({RestTemplateBuilderConfig.class})
public class HolidaysAppApplication {


	public static void main(String[] args) {
		SpringApplication.run(HolidaysAppApplication.class, args);
	}


}
