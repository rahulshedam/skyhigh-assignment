package com.skyhigh.seat.controller;

import com.skyhigh.seat.config.SecurityConfig;
import com.skyhigh.seat.model.dto.SeatMapResponse;
import com.skyhigh.seat.model.dto.SeatResponse;
import com.skyhigh.seat.model.enums.SeatClass;
import com.skyhigh.seat.model.enums.SeatStatus;
import com.skyhigh.seat.service.SeatMapService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.Disabled;

@Disabled
@WebMvcTest(SeatMapController.class)
@Import(SecurityConfig.class)
class SeatMapControllerTest {

        @Autowired
        private MockMvc mockMvc;

        @MockitoBean
        private SeatMapService seatMapService;

        private SeatMapResponse seatMapResponse;

        @BeforeEach
        void setUp() {
                SeatResponse seat1 = SeatResponse.builder()
                                .id(1L)
                                .seatNumber("1A")
                                .flightId(100L)
                                .seatClass(SeatClass.ECONOMY)
                                .status(SeatStatus.AVAILABLE)
                                .build();

                SeatResponse seat2 = SeatResponse.builder()
                                .id(2L)
                                .seatNumber("1B")
                                .flightId(100L)
                                .seatClass(SeatClass.ECONOMY)
                                .status(SeatStatus.HELD)
                                .passengerId("PASS123")
                                .build();

                seatMapResponse = SeatMapResponse.builder()
                                .flightId(100L)
                                .flightNumber("SK101")
                                .seats(Arrays.asList(seat1, seat2))
                                .totalSeats(2)
                                .availableSeats(1)
                                .heldSeats(1)
                                .confirmedSeats(0)
                                .build();
        }

        @Test
        void getSeatMap_Success() throws Exception {
                // Arrange
                when(seatMapService.getSeatMap(100L)).thenReturn(seatMapResponse);

                // Act & Assert
                mockMvc.perform(get("/api/seats/flights/100/seatmap"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.flightId").value(100))
                                .andExpect(jsonPath("$.flightNumber").value("SK101"))
                                .andExpect(jsonPath("$.totalSeats").value(2))
                                .andExpect(jsonPath("$.availableSeats").value(1))
                                .andExpect(jsonPath("$.heldSeats").value(1))
                                .andExpect(jsonPath("$.seats.length()").value(2));
        }

        @Test
        void getSeatMap_FlightNotFound() throws Exception {
                // Arrange
                when(seatMapService.getSeatMap(999L))
                                .thenThrow(new RuntimeException("Flight not found"));

                // Act & Assert
                mockMvc.perform(get("/api/seats/flights/999/seatmap"))
                                .andExpect(status().is5xxServerError());
        }
}
