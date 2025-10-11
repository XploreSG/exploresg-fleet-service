package com.exploresg.fleetservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling // <-- ADD THIS LINE
public class ExploresgFleetServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(ExploresgFleetServiceApplication.class, args);
	}

}
