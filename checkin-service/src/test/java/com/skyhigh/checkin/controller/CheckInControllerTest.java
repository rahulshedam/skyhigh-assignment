package com.skyhigh.checkin.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.skyhigh.checkin.exception.CheckInConflictException;
import com.skyhigh.checkin.model.dto.CheckInBaggageRequest;
import com.skyhigh.checkin.model.dto.CheckInCompleteRequest;
import com.skyhigh.checkin.model.dto.CheckInResponse;
import com.skyhigh.checkin.model.dto.CheckInStartRequest;
import com.skyhigh.checkin.model.enums.CheckInStatus;
import com.skyhigh.checkin.service.CheckInService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CheckInController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(com.skyhigh.checkin.exception.GlobalExceptionHandler.class)
class CheckInControllerTest {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private ObjectMapper objectMapper;

        @MockBean
        private CheckInService checkInService;

        @Test
        void startCheckIn_shouldReturnCreated() throws Exception {
                CheckInStartRequest request = new CheckInStartRequest("SKY123", "P100", 1L, 10L, 0.0);
                CheckInResponse response = new CheckInResponse(1L, "SKY123", "P100", 1L, 10L, CheckInStatus.IN_PROGRESS,
                                0.0, 0.0, null, null, null, null);

                when(checkInService.startCheckIn(any(CheckInStartRequest.class))).thenReturn(response);

                mockMvc.perform(post("/api/checkin/start")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
        }

        @Test
        void getCheckInStatus_shouldReturnOk() throws Exception {
                Long checkInId = 1L;
                CheckInResponse response = new CheckInResponse(checkInId, "SKY123", "P100", 1L, 10L,
                                CheckInStatus.COMPLETED, 0.0, 0.0, null, null, null, null);

                when(checkInService.getCheckInStatus(checkInId)).thenReturn(response);

                mockMvc.perform(get("/api/checkin/{id}", checkInId))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.status").value("COMPLETED"));
        }

        @Test
        void completeCheckIn_shouldReturnOk() throws Exception {
                Long checkInId = 1L;
                CheckInCompleteRequest request = new CheckInCompleteRequest("PAY123");
                CheckInResponse response = new CheckInResponse(checkInId, "SKY123", "P100", 1L, 10L,
                                CheckInStatus.COMPLETED, 0.0, 0.0, "PAY123", null, null, null);

                when(checkInService.completeCheckIn(eq(checkInId), eq("PAY123"))).thenReturn(response);

                mockMvc.perform(post("/api/checkin/{id}/complete", checkInId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.status").value("COMPLETED"));
        }

        @Test
        void updateBaggage_shouldReturnOk() throws Exception {
                Long checkInId = 1L;
                CheckInBaggageRequest request = new CheckInBaggageRequest(20.0);
                CheckInResponse response = new CheckInResponse(checkInId, "SKY123", "P100", 1L, 10L,
                                CheckInStatus.COMPLETED, 20.0, 0.0, null, null, null, null);

                when(checkInService.updateBaggage(eq(checkInId), eq(20.0))).thenReturn(response);

                mockMvc.perform(post("/api/checkin/{id}/baggage", checkInId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.baggageWeight").value(20.0));
        }

        @Test
        void cancelCheckIn_shouldReturnNoContent() throws Exception {
                Long checkInId = 1L;

                mockMvc.perform(post("/api/checkin/{id}/cancel", checkInId))
                                .andExpect(status().isNoContent());

                verify(checkInService).cancelCheckIn(checkInId);
        }

        @Test
        void completeCheckIn_whenSeatConflictOrHoldExpired_returns409() throws Exception {
                Long checkInId = 1L;
                CheckInCompleteRequest request = new CheckInCompleteRequest("PAY123");

                when(checkInService.completeCheckIn(eq(checkInId), eq("PAY123")))
                                .thenThrow(new CheckInConflictException("Seat hold has expired"));

                mockMvc.perform(post("/api/checkin/{id}/complete", checkInId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isConflict())
                                .andExpect(jsonPath("$.message").value("Seat hold has expired"));
        }
}
