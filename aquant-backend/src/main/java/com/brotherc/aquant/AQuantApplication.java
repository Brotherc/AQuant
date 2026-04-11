package com.brotherc.aquant;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class AQuantApplication {

	public static void main(String[] args) {
		SpringApplication.run(AQuantApplication.class, args);
	}

}
