package com.skyhigh.checkin.client;

import com.skyhigh.checkin.client.dto.BaggageValidationRequest;
import com.skyhigh.checkin.client.dto.BaggageValidationResponse;
import com.skyhigh.checkin.exception.ServiceUnavailableException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BaggageServiceClientTest {

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private BaggageServiceClient baggageClient;

    private Map<String, Object> expectedApiResponse;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(baggageClient, "baggageServiceUrl", "http://baggage-service");
        
        // Create ApiResponse wrapper structure
        Map<String, Object> data = new HashMap<>();
        data.put("baggageReference", "BAG-001");
        data.put("isValid", true);
        data.put("weight", 20.0);
        data.put("excessWeight", 0.0);
        data.put("excessFee", 50.0);
        data.put("status", "EXCESS_FEE_REQUIRED");
        data.put("message", "Heavy");
        
        expectedApiResponse = new HashMap<>();
        expectedApiResponse.put("success", true);
        expectedApiResponse.put("data", data);
    }

    @Test
    void validateBaggage_shouldReturnResponse_whenSuccess() {
        // Arrange
        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                any(ParameterizedTypeReference.class)))
                .thenReturn(new ResponseEntity<>(expectedApiResponse, HttpStatus.OK));

        // Act
        BaggageValidationResponse response = baggageClient.validateBaggage("P100", "SKY123", 25.0);

        // Assert
        assertNotNull(response);
        assertTrue(response.requiresPayment());
        assertEquals(50.0, response.excessFeeAsDouble());
        verify(restTemplate).exchange(
                eq("http://baggage-service/api/baggage/validate"),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                any(ParameterizedTypeReference.class));
    }

    @Test
    void validateBaggageFallback_shouldThrowServiceUnavailableException() {
        // Arrange
        Exception exception = new RuntimeException("Service down");

        // Act & Assert
        ServiceUnavailableException thrown = assertThrows(ServiceUnavailableException.class,
                () -> baggageClient.validateBaggageFallback("P100", "SKY123", 25.0, exception));

        assertTrue(thrown.getMessage().contains("Baggage Service is currently unavailable"));
    }
}
