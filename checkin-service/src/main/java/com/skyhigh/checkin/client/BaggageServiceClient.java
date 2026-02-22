package com.skyhigh.checkin.client;

import com.skyhigh.checkin.client.dto.BaggageValidationRequest;
import com.skyhigh.checkin.client.dto.BaggageValidationResponse;
import com.skyhigh.checkin.exception.ServiceUnavailableException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Client for Baggage Service
 */
@Service
@Slf4j
public class BaggageServiceClient {

    private final RestTemplate restTemplate;

    @Value("${services.baggage.url}")
    private String baggageServiceUrl;

    public BaggageServiceClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * Validate baggage weight and calculate fees
     */
    @CircuitBreaker(name = "baggageService", fallbackMethod = "validateBaggageFallback")
    public BaggageValidationResponse validateBaggage(String passengerId, String bookingReference, Double weight) {
        log.info("Validating baggage weight: {} kg for passenger: {}", weight, passengerId);

        String url = baggageServiceUrl + "/api/baggage/validate";
        BaggageValidationRequest request = new BaggageValidationRequest(
                passengerId,
                bookingReference,
                BigDecimal.valueOf(weight)
        );
        
        // The Baggage Service returns ApiResponse<BaggageValidationResponse>
        // We need to unwrap the data field
        HttpEntity<BaggageValidationRequest> entity = new HttpEntity<>(request);
        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                entity,
                new ParameterizedTypeReference<Map<String, Object>>() {}
        );

        Map<String, Object> responseBody = response.getBody();
        if (responseBody != null && responseBody.containsKey("data")) {
            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) responseBody.get("data");
            
            return new BaggageValidationResponse(
                    (String) data.get("baggageReference"),
                    (Boolean) data.getOrDefault("isValid", true),
                    data.get("weight") != null ? new BigDecimal(data.get("weight").toString()) : null,
                    data.get("excessWeight") != null ? new BigDecimal(data.get("excessWeight").toString()) : null,
                    data.get("excessFee") != null ? new BigDecimal(data.get("excessFee").toString()) : null,
                    (String) data.get("status"),
                    (String) data.get("message")
            );
        }
        
        throw new ServiceUnavailableException("Invalid response from Baggage Service");
    }

    BaggageValidationResponse validateBaggageFallback(String passengerId, String bookingReference, Double weight, Exception e) {
        log.error("Baggage Service unavailable: {}", e.getMessage());
        throw new ServiceUnavailableException("Baggage Service is currently unavailable", e);
    }
}
