package com.skyhigh.seat.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.skyhigh.seat.config.SecurityConfig;
import com.skyhigh.seat.model.dto.SeatConfirmRequest;
import com.skyhigh.seat.model.dto.SeatHoldRequest;
import com.skyhigh.seat.model.dto.SeatResponse;
import com.skyhigh.seat.model.enums.SeatClass;
import com.skyhigh.seat.model.enums.SeatStatus;
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
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.Disabled;

@Disabled
@WebMvcTest(SeatController.class)
@Import(SecurityConfig.class)
class SeatControllerTest {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private ObjectMapper objectMapper;

        @MockitoBean
        private SeatService seatService;

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
}
