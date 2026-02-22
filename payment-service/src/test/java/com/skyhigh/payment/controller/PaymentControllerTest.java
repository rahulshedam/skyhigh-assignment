package com.skyhigh.payment.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.skyhigh.common.dto.ApiResponse;
import com.skyhigh.common.exception.GlobalExceptionHandler;
import com.skyhigh.payment.config.SecurityConfig;
import com.skyhigh.payment.dto.PaymentData;
import com.skyhigh.payment.dto.PaymentRequest;
import com.skyhigh.payment.model.PaymentStatus;
import com.skyhigh.payment.model.PaymentType;
import com.skyhigh.payment.service.PaymentService;
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

@WebMvcTest(controllers = PaymentController.class)
@ContextConfiguration(classes = { PaymentController.class, SecurityConfig.class, GlobalExceptionHandler.class })
class PaymentControllerTest {

        @Autowired
        private MockMvc mockMvc;

        @MockitoBean
        private PaymentService paymentService;

        @Autowired
        private ObjectMapper objectMapper;

        @Test
        void processPayment_Success() throws Exception {
                PaymentRequest request = PaymentRequest.builder()
                                .passengerId("PASS123")
                                .bookingReference("BOOK456")
                                .amount(new BigDecimal("100.00"))
                                .currency("USD")
                                .paymentType(PaymentType.SEAT_UPGRADE)
                                .build();

                PaymentData responseData = PaymentData.builder()
                                .paymentReference("PAY-123")
                                .status(PaymentStatus.COMPLETED)
                                .amount(new BigDecimal("100.00"))
                                .build();

                when(paymentService.processPayment(any(PaymentRequest.class)))
                                .thenReturn(ApiResponse.success(responseData));

                mockMvc.perform(post("/api/payments/process")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.success").value(true))
                                .andExpect(jsonPath("$.data.paymentReference").value("PAY-123"));
        }

        @Test
        void processPayment_Failure() throws Exception {
                PaymentRequest request = PaymentRequest.builder()
                                .passengerId("PASS123")
                                .bookingReference("BOOK456")
                                .amount(new BigDecimal("100.00"))
                                .currency("USD")
                                .paymentType(PaymentType.SEAT_UPGRADE)
                                .build();

                when(paymentService.processPayment(any(PaymentRequest.class)))
                                .thenReturn(ApiResponse.error("Payment failed"));

                mockMvc.perform(post("/api/payments/process")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.success").value(false))
                                .andExpect(jsonPath("$.message").value("Payment failed"));
        }

        @Test
        void processPayment_ValidationFailure() throws Exception {
                PaymentRequest request = new PaymentRequest(); // Invalid request (missing required fields)

                mockMvc.perform(post("/api/payments/process")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.status").value(400))
                                .andExpect(jsonPath("$.error").value("Bad Request"));
        }

        @Test
        void getPaymentByReference() throws Exception {
                PaymentData responseData = PaymentData.builder()
                                .paymentReference("PAY-123")
                                .status(PaymentStatus.COMPLETED)
                                .build();

                when(paymentService.getPaymentByReference("PAY-123"))
                                .thenReturn(ApiResponse.success(responseData));

                mockMvc.perform(get("/api/payments/PAY-123"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.success").value(true))
                                .andExpect(jsonPath("$.data.paymentReference").value("PAY-123"));
        }

        @Test
        void getPaymentsByPassengerId() throws Exception {
                PaymentData data = PaymentData.builder().paymentReference("PAY-123").build();
                when(paymentService.getPaymentsByPassengerId("PASS123"))
                                .thenReturn(ApiResponse.success(Arrays.asList(data)));

                mockMvc.perform(get("/api/payments/passenger/PASS123"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.success").value(true))
                                .andExpect(jsonPath("$.data").isArray());
        }

        @Test
        void getPaymentsByBookingReference() throws Exception {
                PaymentData data = PaymentData.builder().paymentReference("PAY-123").build();
                when(paymentService.getPaymentsByBookingReference("BOOK456"))
                                .thenReturn(ApiResponse.success(Arrays.asList(data)));

                mockMvc.perform(get("/api/payments/booking/BOOK456"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.success").value(true))
                                .andExpect(jsonPath("$.data").isArray());
        }
}
