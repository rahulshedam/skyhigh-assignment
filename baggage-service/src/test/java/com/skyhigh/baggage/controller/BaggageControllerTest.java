package com.skyhigh.baggage.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.skyhigh.baggage.dto.BaggageData;
import com.skyhigh.baggage.dto.BaggageValidationRequest;
import com.skyhigh.baggage.dto.BaggageValidationResponse;
import com.skyhigh.baggage.model.BaggageStatus;
import com.skyhigh.baggage.service.BaggageService;
import com.skyhigh.common.dto.ApiResponse;
import com.skyhigh.common.exception.GlobalExceptionHandler;
import com.skyhigh.baggage.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Arrays;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = BaggageController.class)
@ContextConfiguration(classes = { BaggageController.class, SecurityConfig.class, GlobalExceptionHandler.class })
class BaggageControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BaggageService baggageService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void validateBaggage_Success() throws Exception {
        BaggageValidationRequest request = BaggageValidationRequest.builder()
                .passengerId("PASS123")
                .bookingReference("BOOK456")
                .weight(new BigDecimal("20.0"))
                .build();

        BaggageValidationResponse responseData = BaggageValidationResponse.builder()
                .baggageReference("BAG-12345678")
                .isValid(true)
                .weight(new BigDecimal("20.0"))
                .excessWeight(BigDecimal.ZERO)
                .excessFee(BigDecimal.ZERO)
                .status(BaggageStatus.VALIDATED)
                .message("Baggage within free limit")
                .build();

        when(baggageService.validateBaggage(any(BaggageValidationRequest.class)))
                .thenReturn(ApiResponse.success(responseData));

        mockMvc.perform(post("/api/baggage/validate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.baggageReference").value("BAG-12345678"))
                .andExpect(jsonPath("$.data.isValid").value(true));
    }

    @Test
    void validateBaggage_ValidationFailure() throws Exception {
        BaggageValidationRequest request = new BaggageValidationRequest(); // Invalid request

        mockMvc.perform(post("/api/baggage/validate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"));
    }

    @Test
    void getBaggageByReference() throws Exception {
        BaggageData responseData = BaggageData.builder()
                .baggageReference("BAG-12345678")
                .passengerId("PASS123")
                .status(BaggageStatus.VALIDATED)
                .build();

        when(baggageService.getBaggageByReference("BAG-12345678"))
                .thenReturn(ApiResponse.success(responseData));

        mockMvc.perform(get("/api/baggage/BAG-12345678"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.baggageReference").value("BAG-12345678"));
    }

    @Test
    void getBaggageByPassengerId() throws Exception {
        BaggageData data = BaggageData.builder()
                .baggageReference("BAG-12345678")
                .passengerId("PASS123")
                .build();

        when(baggageService.getBaggageByPassengerId("PASS123"))
                .thenReturn(ApiResponse.success(Arrays.asList(data)));

        mockMvc.perform(get("/api/baggage/passenger/PASS123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    void getBaggageByBookingReference() throws Exception {
        BaggageData data = BaggageData.builder()
                .baggageReference("BAG-12345678")
                .bookingReference("BOOK456")
                .build();

        when(baggageService.getBaggageByBookingReference("BOOK456"))
                .thenReturn(ApiResponse.success(Arrays.asList(data)));

        mockMvc.perform(get("/api/baggage/booking/BOOK456"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray());
    }
}
