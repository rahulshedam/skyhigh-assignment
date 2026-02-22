package com.skyhigh.checkin.client;

import com.skyhigh.checkin.client.dto.SeatConfirmRequest;
import com.skyhigh.checkin.client.dto.SeatHoldRequest;
import com.skyhigh.checkin.client.dto.SeatResponse;
import com.skyhigh.checkin.exception.ServiceUnavailableException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SeatManagementClientTest {

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private SeatManagementClient seatClient;

    private String seatServiceUrl = "http://localhost:8081";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(seatClient, "seatServiceUrl", seatServiceUrl);
    }

    @Test
    void holdSeat_shouldReturnSeatResponse_whenSuccess() {
        // Arrange
        Long seatId = 1L;
        SeatHoldRequest request = new SeatHoldRequest("P100", "SKY123");
        SeatResponse expectedResponse = new SeatResponse(1L, "1A", "HELD", "P100", "SKY123");

        when(restTemplate.postForEntity(eq(seatServiceUrl + "/api/seats/" + seatId + "/hold"), eq(request),
                eq(SeatResponse.class)))
                .thenReturn(new ResponseEntity<>(expectedResponse, HttpStatus.OK));

        // Act
        SeatResponse response = seatClient.holdSeat(seatId, request);

        // Assert
        assertNotNull(response);
        assertEquals("HELD", response.status());
        verify(restTemplate).postForEntity(anyString(), any(), any());
    }

    @Test
    void confirmSeat_shouldReturnSeatResponse_whenSuccess() {
        // Arrange
        Long seatId = 1L;
        SeatConfirmRequest request = new SeatConfirmRequest("P100", "SKY123");
        SeatResponse expectedResponse = new SeatResponse(1L, "1A", "CONFIRMED", "P100", "SKY123");

        when(restTemplate.postForEntity(eq(seatServiceUrl + "/api/seats/" + seatId + "/confirm"), eq(request),
                eq(SeatResponse.class)))
                .thenReturn(new ResponseEntity<>(expectedResponse, HttpStatus.OK));

        // Act
        SeatResponse response = seatClient.confirmSeat(seatId, request);

        // Assert
        assertNotNull(response);
        assertEquals("CONFIRMED", response.status());
    }

    @Test
    void cancelSeat_shouldCallRestTemplate_whenSuccess() {
        // Arrange
        Long seatId = 1L;
        String passengerId = "P100";

        // Act
        seatClient.cancelSeat(seatId, passengerId);

        // Assert
        verify(restTemplate).delete(eq(seatServiceUrl + "/api/seats/" + seatId + "/cancel?passengerId=" + passengerId));
    }

    @Test
    void holdSeatFallback_shouldThrowServiceUnavailableException() {
        // Arrange
        Long seatId = 1L;
        SeatHoldRequest request = new SeatHoldRequest("P100", "SKY123");
        Exception exception = new RuntimeException("Connection refused");

        // Act & Assert
        assertThrows(ServiceUnavailableException.class, () -> seatClient.holdSeatFallback(seatId, request, exception));
    }

    @Test
    void confirmSeatFallback_shouldThrowServiceUnavailableException() {
        // Arrange
        Long seatId = 1L;
        SeatConfirmRequest request = new SeatConfirmRequest("P100", "SKY123");
        Exception exception = new RuntimeException("Connection refused");

        // Act & Assert
        assertThrows(ServiceUnavailableException.class,
                () -> seatClient.confirmSeatFallback(seatId, request, exception));
    }
}
