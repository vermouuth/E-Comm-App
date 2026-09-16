package com.ecomm.sb_ecomm;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SbEcommApplication {

	public static void main(String[] args) {
		SpringApplication.run(SbEcommApplication.class, args);
	}

}
