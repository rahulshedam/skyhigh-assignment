package com.skyhigh.payment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

/**
 * Main application class for Payment Service.
 */
@SpringBootApplication
@ComponentScan(basePackages = { "com.skyhigh.payment", "com.skyhigh.common" })
public class PaymentServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(PaymentServiceApplication.class, args);
    }
}
