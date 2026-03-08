package com.skyhigh.checkin.client;

import com.skyhigh.checkin.client.dto.SeatConfirmRequest;
import com.skyhigh.checkin.client.dto.SeatHoldRequest;
import com.skyhigh.checkin.client.dto.SeatResponse;
import com.skyhigh.checkin.exception.CheckInConflictException;
import com.skyhigh.checkin.exception.ServiceUnavailableException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

/**
 * Client for Seat Management Service
 */
@Service
@Slf4j
public class SeatManagementClient {

    private final RestTemplate restTemplate;

    @Value("${services.seat-management.url}")
    private String seatServiceUrl;

    public SeatManagementClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * Hold a seat for a passenger
     */
    @CircuitBreaker(name = "seatManagement", fallbackMethod = "holdSeatFallback")
    public SeatResponse holdSeat(Long seatId, SeatHoldRequest request) {
        log.info("Holding seat {} for passenger {}", seatId, request.passengerId());

        String url = seatServiceUrl + "/api/seats/" + seatId + "/hold";
        try {
            ResponseEntity<SeatResponse> response = restTemplate.postForEntity(url, request, SeatResponse.class);
            return response.getBody();
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode().value() == 409 || e.getStatusCode().value() == 400) {
                throw new CheckInConflictException(extractMessage(e), e);
            }
            throw e;
        }
    }

    SeatResponse holdSeatFallback(Long seatId, SeatHoldRequest request, Exception e) {
        log.error("Seat Management Service unavailable for holdSeat: {}", e.getMessage());
        throw new ServiceUnavailableException("Seat Management Service is currently unavailable", e);
    }

    /**
     * Confirm a seat for a passenger
     */
    @CircuitBreaker(name = "seatManagement", fallbackMethod = "confirmSeatFallback")
    public SeatResponse confirmSeat(Long seatId, SeatConfirmRequest request) {
        log.info("Confirming seat {} for passenger {}", seatId, request.passengerId());

        String url = seatServiceUrl + "/api/seats/" + seatId + "/confirm";
        try {
            ResponseEntity<SeatResponse> response = restTemplate.postForEntity(url, request, SeatResponse.class);
            return response.getBody();
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode().value() == 409 || e.getStatusCode().value() == 400) {
                throw new CheckInConflictException(extractMessage(e), e);
            }
            throw e;
        }
    }

    SeatResponse confirmSeatFallback(Long seatId, SeatConfirmRequest request, Exception e) {
        log.error("Seat Management Service unavailable for confirmSeat: {}", e.getMessage());
        throw new ServiceUnavailableException("Seat Management Service is currently unavailable", e);
    }

    /**
     * Cancel a seat hold
     */
    @CircuitBreaker(name = "seatManagement", fallbackMethod = "cancelSeatFallback")
    public void cancelSeat(Long seatId, String passengerId) {
        log.info("Cancelling seat {} for passenger {}", seatId, passengerId);

        String url = seatServiceUrl + "/api/seats/" + seatId + "/cancel?passengerId=" + passengerId;
        restTemplate.delete(url);
    }

    private void cancelSeatFallback(Long seatId, String passengerId, Exception e) {
        log.error("Seat Management Service unavailable for cancelSeat: {}", e.getMessage());
        // Don't throw exception for cancel - best effort
    }

    private String extractMessage(HttpClientErrorException e) {
        String body = e.getResponseBodyAsString();
        if (body != null && body.contains("\"message\"")) {
            try {
                int start = body.indexOf("\"message\"") + 11;
                int end = body.indexOf("\"", start);
                if (end > start) {
                    return body.substring(start, end);
                }
            } catch (Exception ignored) {
            }
        }
        return e.getMessage() != null ? e.getMessage() : "Seat conflict or hold expired";
    }
}
