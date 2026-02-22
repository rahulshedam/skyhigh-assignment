package com.skyhigh.checkin.client;

import com.skyhigh.checkin.client.dto.PaymentRequest;
import com.skyhigh.checkin.client.dto.PaymentResponse;
import com.skyhigh.checkin.exception.ServiceUnavailableException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceClientTest {

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private PaymentServiceClient paymentClient;

    private PaymentRequest request;
    private PaymentResponse expectedResponse;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(paymentClient, "paymentServiceUrl", "http://payment-service");
        request = new PaymentRequest("REF123", "PASS123", 50.0, "Excess Baggage");
        expectedResponse = new PaymentResponse("PAY123", "SUCCESS", 50.0, "Payment successful");
    }

    @Test
    void processPayment_shouldReturnResponse_whenSuccess() {
        // Arrange
        when(restTemplate.postForEntity(anyString(), any(PaymentRequest.class), eq(PaymentResponse.class)))
                .thenReturn(new ResponseEntity<>(expectedResponse, HttpStatus.OK));

        // Act
        PaymentResponse response = paymentClient.processPayment(request);

        // Assert
        assertNotNull(response);
        assertEquals("PAY123", response.paymentId());
        assertEquals("SUCCESS", response.status());
        verify(restTemplate).postForEntity(eq("http://payment-service/api/payments/process"), eq(request),
                eq(PaymentResponse.class));
    }

    @Test
    void processPaymentFallback_shouldThrowServiceUnavailableException() {
        // Arrange
        Exception exception = new RuntimeException("Service down");

        // Act & Assert
        ServiceUnavailableException thrown = assertThrows(ServiceUnavailableException.class,
                () -> paymentClient.processPaymentFallback(request, exception));

        assertTrue(thrown.getMessage().contains("Payment Service is currently unavailable"));
    }
}
