package com.ecommerce.sportcenter;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SportcenterApplication {

	public static void main(String[] args) {
		SpringApplication.run(SportcenterApplication.class, args);
	}

}
