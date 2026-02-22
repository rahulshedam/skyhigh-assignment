package com.skyhigh.checkin.exception;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = GlobalExceptionHandlerTest.TestController.class)
@AutoConfigureMockMvc(addFilters = false)
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @Configuration
    static class TestConfig {
        @Bean
        public TestController testController() {
            return new TestController();
        }

        @Bean
        public GlobalExceptionHandler globalExceptionHandler() {
            return new GlobalExceptionHandler();
        }
    }

    @RestController
    static class TestController {
        @GetMapping("/test/not-found")
        public void notFound() {
            throw new CheckInNotFoundException(1L);
        }

        @GetMapping("/test/checkin-exception")
        public void checkinException() {
            throw new CheckInException("Check-in error");
        }

        @GetMapping("/test/service-unavailable")
        public void serviceUnavailable() {
            throw new ServiceUnavailableException("Service unavailable", new RuntimeException());
        }

        @GetMapping("/test/generic-exception")
        public void genericException() throws Exception {
            throw new Exception("Generic error");
        }
    }

    @Test
    void handleCheckInNotFound_shouldReturnNotFound() throws Exception {
        mockMvc.perform(get("/test/not-found")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Check-in not found with ID: 1"));
    }

    @Test
    void handleCheckInException_shouldReturnBadRequest() throws Exception {
        mockMvc.perform(get("/test/checkin-exception")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Check-in error"));
    }

    @Test
    void handleServiceUnavailable_shouldReturnServiceUnavailable() throws Exception {
        mockMvc.perform(get("/test/service-unavailable")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.message").value("Service unavailable"));
    }

    @Test
    void handleGenericException_shouldReturnInternalServerError() throws Exception {
        mockMvc.perform(get("/test/generic-exception")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("An unexpected error occurred"));
    }
}
