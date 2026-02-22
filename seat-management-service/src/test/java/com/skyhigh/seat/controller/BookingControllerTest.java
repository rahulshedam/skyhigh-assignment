package com.skyhigh.seat.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.skyhigh.seat.config.SecurityConfig;
import com.skyhigh.seat.model.dto.BookingVerificationRequest;
import com.skyhigh.seat.model.dto.BookingVerificationResponse;
import com.skyhigh.seat.service.BookingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.Disabled;

@Disabled
@WebMvcTest(BookingController.class)
@Import(SecurityConfig.class)
class BookingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private BookingService bookingService;

    private BookingVerificationResponse verificationResponse;
    private BookingVerificationRequest verificationRequest;

    @BeforeEach
    void setUp() {
        verificationRequest = new BookingVerificationRequest("ABC123", "john.doe@email.com");
        
        verificationResponse = BookingVerificationResponse.builder()
                .flightId(101L)
                .flightNumber("SH101")
                .origin("JFK")
                .destination("LAX")
                .bookingReference("ABC123")
                .passengerId("john.doe@email.com")
                .valid(true)
                .build();
    }

    @Test
    void testVerifyBooking_Success() throws Exception {
        when(bookingService.verifyBooking(any(BookingVerificationRequest.class)))
                .thenReturn(verificationResponse);

        mockMvc.perform(post("/api/bookings/verify")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(verificationRequest)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.flightId").value(101L))
                .andExpect(jsonPath("$.flightNumber").value("SH101"))
                .andExpect(jsonPath("$.bookingReference").value("ABC123"))
                .andExpect(jsonPath("$.passengerId").value("john.doe@email.com"))
                .andExpect(jsonPath("$.valid").value(true));
    }

    @Test
    void testVerifyBooking_InvalidRequest() throws Exception {
        BookingVerificationRequest invalidRequest = new BookingVerificationRequest("", "");

        mockMvc.perform(post("/api/bookings/verify")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }
}
