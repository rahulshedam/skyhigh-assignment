package com.skyhigh.seat.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.skyhigh.seat.config.SecurityConfig;
import com.skyhigh.seat.model.dto.SeatConfirmRequest;
import com.skyhigh.seat.model.dto.SeatHoldRequest;
import com.skyhigh.seat.model.dto.SeatResponse;
import com.skyhigh.seat.model.enums.SeatClass;
import com.skyhigh.seat.model.enums.SeatStatus;
import com.skyhigh.seat.service.RateLimitAuditService;
import com.skyhigh.seat.service.SeatService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import com.skyhigh.seat.exception.SeatHoldExpiredException;
import com.skyhigh.seat.exception.SeatNotFoundException;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SeatController.class)
@Import({ SecurityConfig.class, com.skyhigh.seat.exception.GlobalExceptionHandler.class })
class SeatControllerTest {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private ObjectMapper objectMapper;

        @MockitoBean
        private SeatService seatService;

        @MockitoBean
        private RateLimitAuditService rateLimitAuditService;

        private SeatResponse seatResponse;
        private SeatHoldRequest holdRequest;
        private SeatConfirmRequest confirmRequest;

        @BeforeEach
        void setUp() {
                seatResponse = SeatResponse.builder()
                                .id(1L)
                                .seatNumber("1A")
                                .flightId(100L)
                                .seatClass(SeatClass.ECONOMY)
                                .status(SeatStatus.HELD)
                                .passengerId("PASS123")
                                .bookingReference("BOOK456")
                                .build();

                holdRequest = SeatHoldRequest.builder()
                                .passengerId("PASS123")
                                .bookingReference("BOOK456")
                                .build();

                confirmRequest = SeatConfirmRequest.builder()
                                .passengerId("PASS123")
                                .build();
        }

        @Test
        void holdSeat_Success() throws Exception {
                // Arrange
                when(seatService.holdSeat(eq(1L), any(SeatHoldRequest.class)))
                                .thenReturn(seatResponse);

                // Act & Assert
                mockMvc.perform(post("/api/seats/1/hold")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(holdRequest)))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.id").value(1))
                                .andExpect(jsonPath("$.seatNumber").value("1A"))
                                .andExpect(jsonPath("$.passengerId").value("PASS123"));
        }

        @Test
        void holdSeat_InvalidRequest_BadRequest() throws Exception {
                // Arrange
                SeatHoldRequest invalidRequest = SeatHoldRequest.builder().build();

                // Act & Assert
                mockMvc.perform(post("/api/seats/1/hold")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(invalidRequest)))
                                .andExpect(status().isBadRequest());
        }

        @Test
        void confirmSeat_Success() throws Exception {
                // Arrange
                seatResponse.setStatus(SeatStatus.CONFIRMED);
                when(seatService.confirmSeat(eq(1L), any(SeatConfirmRequest.class)))
                                .thenReturn(seatResponse);

                // Act & Assert
                mockMvc.perform(post("/api/seats/1/confirm")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(confirmRequest)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.status").value("CONFIRMED"));
        }

        @Test
        void cancelSeat_Success() throws Exception {
                // Arrange
                doNothing().when(seatService).cancelSeat(1L, "PASS123");

                // Act & Assert
                mockMvc.perform(delete("/api/seats/1/cancel")
                                .param("passengerId", "PASS123"))
                                .andExpect(status().isNoContent());
        }

        @Test
        void getSeatStatus_Success() throws Exception {
                // Arrange
                seatResponse.setStatus(SeatStatus.AVAILABLE);
                seatResponse.setPassengerId(null);
                when(seatService.getSeatStatus(1L)).thenReturn(seatResponse);

                // Act & Assert
                mockMvc.perform(get("/api/seats/1/status"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.id").value(1))
                                .andExpect(jsonPath("$.status").value("AVAILABLE"))
                                .andExpect(jsonPath("$.passengerId").doesNotExist());
        }

        @Test
        void cancelSeat_MissingPassengerId_BadRequest() throws Exception {
                mockMvc.perform(delete("/api/seats/1/cancel"))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.message").exists());
        }

        @Test
        void cancelSeat_BlankPassengerId_BadRequest() throws Exception {
                mockMvc.perform(delete("/api/seats/1/cancel").param("passengerId", "   "))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.message").isNotEmpty());
        }

        @Test
        void cancelSeat_InvalidSeatIdType_BadRequest() throws Exception {
                mockMvc.perform(delete("/api/seats/not-a-number/cancel").param("passengerId", "PASS123"))
                                .andExpect(status().isBadRequest());
        }

        @Test
        void confirmSeat_HoldExpired_Returns409Conflict() throws Exception {
                doThrow(new SeatHoldExpiredException("Seat hold has expired"))
                                .when(seatService).confirmSeat(eq(1L), any(SeatConfirmRequest.class));

                mockMvc.perform(post("/api/seats/1/confirm")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(confirmRequest)))
                                .andExpect(status().isConflict())
                                .andExpect(jsonPath("$.message").value("Seat hold has expired"));
        }

        @Test
        void getSeatStatus_SeatNotFound_Returns404() throws Exception {
                doThrow(new SeatNotFoundException(1L)).when(seatService).getSeatStatus(1L);

                mockMvc.perform(get("/api/seats/1/status"))
                                .andExpect(status().isNotFound())
                                .andExpect(jsonPath("$.message").exists());
        }
}
