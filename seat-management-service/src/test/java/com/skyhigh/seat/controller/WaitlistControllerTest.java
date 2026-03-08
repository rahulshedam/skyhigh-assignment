package com.skyhigh.seat.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.skyhigh.seat.config.SecurityConfig;
import com.skyhigh.seat.model.dto.WaitlistJoinRequest;
import com.skyhigh.seat.model.entity.Waitlist;
import com.skyhigh.seat.model.enums.WaitlistStatus;
import com.skyhigh.seat.service.RateLimitAuditService;
import com.skyhigh.seat.service.WaitlistService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(WaitlistController.class)
@Import({ SecurityConfig.class, com.skyhigh.seat.exception.GlobalExceptionHandler.class })
class WaitlistControllerTest {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private ObjectMapper objectMapper;

        @MockitoBean
        private WaitlistService waitlistService;

        @MockitoBean
        private RateLimitAuditService rateLimitAuditService;

        private Waitlist waitlist;
        private WaitlistJoinRequest joinRequest;

        @BeforeEach
        void setUp() {
                waitlist = Waitlist.builder()
                                .id(1L)
                                .seatId(1L)
                                .passengerId("PASS123")
                                .bookingReference("BOOK456")
                                .status(WaitlistStatus.WAITING)
                                .joinedAt(LocalDateTime.now())
                                .build();

                joinRequest = WaitlistJoinRequest.builder()
                                .passengerId("PASS123")
                                .bookingReference("BOOK456")
                                .build();
        }

        @Test
        void joinWaitlist_Success() throws Exception {
                // Arrange
                when(waitlistService.joinWaitlist(eq(1L), any(WaitlistJoinRequest.class)))
                                .thenReturn(waitlist);

                // Act & Assert
                mockMvc.perform(post("/api/seats/1/waitlist")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(joinRequest)))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.id").value(1))
                                .andExpect(jsonPath("$.passengerId").value("PASS123"))
                                .andExpect(jsonPath("$.status").value("WAITING"));
        }

        @Test
        void joinWaitlist_InvalidRequest_BadRequest() throws Exception {
                // Arrange
                WaitlistJoinRequest invalidRequest = WaitlistJoinRequest.builder().build();

                // Act & Assert
                mockMvc.perform(post("/api/seats/1/waitlist")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(invalidRequest)))
                                .andExpect(status().isBadRequest());
        }

        @Test
        void getPassengerWaitlists_Success() throws Exception {
                // Arrange
                when(waitlistService.getPassengerWaitlists("PASS123"))
                                .thenReturn(Collections.singletonList(waitlist));

                // Act & Assert
                mockMvc.perform(get("/api/seats/waitlist/PASS123"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$[0].passengerId").value("PASS123"))
                                .andExpect(jsonPath("$[0].status").value("WAITING"));
        }

        @Test
        void removeFromWaitlist_Success() throws Exception {
                // Arrange
                doNothing().when(waitlistService).removeFromWaitlist(1L);

                // Act & Assert
                mockMvc.perform(delete("/api/seats/waitlist/1"))
                                .andExpect(status().isNoContent());
        }

        @Test
        void removeFromWaitlist_NotFound_Returns404() throws Exception {
                doThrow(new com.skyhigh.seat.exception.WaitlistNotFoundException(999L))
                                .when(waitlistService).removeFromWaitlist(999L);

                mockMvc.perform(delete("/api/seats/waitlist/999"))
                                .andExpect(status().isNotFound())
                                .andExpect(jsonPath("$.message").value("Waitlist entry not found: 999"));
        }
}
