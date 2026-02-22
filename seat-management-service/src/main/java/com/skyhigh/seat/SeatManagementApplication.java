package com.skyhigh.seat;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Main application class for Seat Management Service.
 */
@SpringBootApplication
@EnableScheduling
public class SeatManagementApplication {

    public static void main(String[] args) {
        SpringApplication.run(SeatManagementApplication.class, args);
    }
}
