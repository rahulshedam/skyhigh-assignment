package com.skyhigh.baggage;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main application class for Baggage Service.
 * Validates baggage weight and calculates excess fees.
 */
@SpringBootApplication
public class BaggageServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(BaggageServiceApplication.class, args);
    }
}
