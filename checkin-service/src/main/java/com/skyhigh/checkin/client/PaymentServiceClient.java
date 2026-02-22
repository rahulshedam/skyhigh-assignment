package com.skyhigh.checkin.client;

import com.skyhigh.checkin.client.dto.PaymentRequest;
import com.skyhigh.checkin.client.dto.PaymentResponse;
import com.skyhigh.checkin.exception.ServiceUnavailableException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

/**
 * Client for Payment Service
 */
@Service
@Slf4j
public class PaymentServiceClient {

    private final RestTemplate restTemplate;

    @Value("${services.payment.url}")
    private String paymentServiceUrl;

    public PaymentServiceClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * Process payment for excess baggage
     */
    @CircuitBreaker(name = "paymentService", fallbackMethod = "processPaymentFallback")
    public PaymentResponse processPayment(PaymentRequest request) {
        log.info("Processing payment for booking {}, amount: ${}",
                request.bookingReference(), request.amount());

        String url = paymentServiceUrl + "/api/payments/process";
        ResponseEntity<PaymentResponse> response = restTemplate.postForEntity(url, request, PaymentResponse.class);

        return response.getBody();
    }

    PaymentResponse processPaymentFallback(PaymentRequest request, Exception e) {
        log.error("Payment Service unavailable: {}", e.getMessage());
        throw new ServiceUnavailableException("Payment Service is currently unavailable", e);
    }
}
