package com.skyhigh.checkin.service;

import com.skyhigh.checkin.client.BaggageServiceClient;
import com.skyhigh.checkin.client.PaymentServiceClient;
import com.skyhigh.checkin.client.SeatManagementClient;
import com.skyhigh.checkin.client.dto.*;
import com.skyhigh.checkin.exception.CheckInException;
import com.skyhigh.checkin.exception.CheckInNotFoundException;
import com.skyhigh.checkin.model.dto.CheckInResponse;
import com.skyhigh.checkin.model.dto.CheckInStartRequest;
import com.skyhigh.checkin.model.entity.CheckIn;
import com.skyhigh.checkin.model.entity.CheckInHistory;
import com.skyhigh.checkin.model.enums.CheckInStatus;
import com.skyhigh.checkin.repository.CheckInHistoryRepository;
import com.skyhigh.checkin.repository.CheckInRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CheckInServiceTest {

        @Mock
        private CheckInRepository checkinRepository;

        @Mock
        private CheckInHistoryRepository historyRepository;

        @Mock
        private SeatManagementClient seatClient;

        @Mock
        private BaggageServiceClient baggageClient;

        @Mock
        private PaymentServiceClient paymentClient;

        @InjectMocks
        private CheckInService checkInService;

        @Test
        void startCheckIn_shouldCreateInProgressCheckIn_whenNoBaggage() {
                // Arrange
                CheckInStartRequest request = new CheckInStartRequest(
                                "SKY123", "P100", 1L, 10L, null);

                CheckIn checkIn = CheckIn.builder()
                                .id(1L)
                                .bookingReference("SKY123")
                                .passengerId("P100")
                                .flightId(1L)
                                .seatId(10L)
                                .status(CheckInStatus.IN_PROGRESS)
                                .baggageWeight(0.0)
                                .build();

                when(checkinRepository.save(any(CheckIn.class))).thenReturn(checkIn);
                when(seatClient.holdSeat(anyLong(), any(SeatHoldRequest.class)))
                                .thenReturn(new SeatResponse(10L, "1A", "HELD", "P100", "SKY123"));

                // Act
                CheckInResponse response = checkInService.startCheckIn(request);

                // Assert
                assertEquals(CheckInStatus.IN_PROGRESS, response.status());
                assertEquals(0.0, response.baggageWeight());
                verify(baggageClient, never()).validateBaggage(anyString(), anyString(), anyDouble());
                verify(seatClient, never()).confirmSeat(anyLong(), any());
        }

        @Test
        void startCheckIn_shouldCompleteCheckIn_whenBaggageProvidedAndNoFee() {
                // Arrange
                CheckInStartRequest request = new CheckInStartRequest(
                                "SKY123", "P100", 1L, 10L, 15.0);

                CheckIn checkIn = CheckIn.builder()
                                .id(1L)
                                .bookingReference("SKY123")
                                .passengerId("P100")
                                .flightId(1L)
                                .seatId(10L)
                                .status(CheckInStatus.IN_PROGRESS) // Initially
                                .baggageWeight(15.0)
                                .build();

                when(checkinRepository.save(any(CheckIn.class))).thenReturn(checkIn);
                when(checkinRepository.findById(1L)).thenReturn(Optional.of(checkIn)); // For completeCheckIn

                when(seatClient.holdSeat(anyLong(), any(SeatHoldRequest.class)))
                                .thenReturn(new SeatResponse(10L, "1A", "HELD", "P100", "SKY123"));

                when(baggageClient.validateBaggage(eq("P100"), eq("SKY123"), eq(15.0)))
                                .thenReturn(new BaggageValidationResponse(
                                        "BAG-001",
                                        true,
                                        BigDecimal.valueOf(15.0),
                                        BigDecimal.ZERO,
                                        BigDecimal.ZERO,
                                        "VALIDATED",
                                        "OK"
                                ));

                // Act
                CheckInResponse response = checkInService.startCheckIn(request);

                // Assert
                assertEquals(CheckInStatus.COMPLETED, response.status());
                verify(seatClient).confirmSeat(anyLong(), any());
        }

        @Test
        void startCheckIn_shouldRequirePayment_whenExcessBaggage() {
                // Arrange
                CheckInStartRequest request = new CheckInStartRequest(
                                "SKY123", "P100", 1L, 10L, 30.0);

                CheckIn checkIn = CheckIn.builder()
                                .id(1L)
                                .bookingReference("SKY123")
                                .passengerId("P100")
                                .flightId(1L)
                                .seatId(10L)
                                .status(CheckInStatus.IN_PROGRESS)
                                .baggageWeight(30.0)
                                .build();

                when(checkinRepository.save(any(CheckIn.class))).thenReturn(checkIn);

                when(seatClient.holdSeat(anyLong(), any(SeatHoldRequest.class)))
                                .thenReturn(new SeatResponse(10L, "1A", "HELD", "P100", "SKY123"));

                when(baggageClient.validateBaggage(eq("P100"), eq("SKY123"), eq(30.0)))
                                .thenReturn(new BaggageValidationResponse(
                                        "BAG-002",
                                        true,
                                        BigDecimal.valueOf(30.0),
                                        BigDecimal.valueOf(5.0),
                                        BigDecimal.valueOf(50.0),
                                        "EXCESS_FEE_REQUIRED",
                                        "Heavy"
                                ));

                // Act
                CheckInResponse response = checkInService.startCheckIn(request);

                // Assert
                assertEquals(CheckInStatus.WAITING_FOR_PAYMENT, checkIn.getStatus());
                assertEquals(50.0, checkIn.getExcessBaggageFee());
        }

        @Test
        void updateBaggage_shouldUpdateAndComplete_whenNoFee() {
                // Arrange
                Long checkinId = 1L;
                Double weight = 20.0;

                CheckIn checkIn = CheckIn.builder()
                                .id(checkinId)
                                .bookingReference("SKY123")
                                .passengerId("P100")
                                .status(CheckInStatus.IN_PROGRESS)
                                .baggageWeight(0.0)
                                .build();

                when(checkinRepository.findById(checkinId)).thenReturn(Optional.of(checkIn));
                when(baggageClient.validateBaggage(eq("P100"), eq("SKY123"), eq(weight)))
                                .thenReturn(new BaggageValidationResponse(
                                        "BAG-003",
                                        true,
                                        BigDecimal.valueOf(weight),
                                        BigDecimal.ZERO,
                                        BigDecimal.ZERO,
                                        "VALIDATED",
                                        "OK"
                                ));
                when(checkinRepository.save(any(CheckIn.class))).thenReturn(checkIn);

                // Act
                CheckInResponse response = checkInService.updateBaggage(checkinId, weight);

                // Assert
                assertEquals(CheckInStatus.COMPLETED, checkIn.getStatus());
                assertEquals(weight, checkIn.getBaggageWeight());
                verify(seatClient).confirmSeat(any(), any());
        }

        @Test
        void updateBaggage_shouldRequirePayment_whenExcessFee() {
                // Arrange
                Long checkinId = 1L;
                Double weight = 30.0;

                CheckIn checkIn = CheckIn.builder()
                                .id(checkinId)
                                .bookingReference("SKY123")
                                .passengerId("P100")
                                .status(CheckInStatus.IN_PROGRESS)
                                .baggageWeight(0.0)
                                .build();

                when(checkinRepository.findById(checkinId)).thenReturn(Optional.of(checkIn));
                when(checkinRepository.save(any(CheckIn.class))).thenReturn(checkIn);

                when(baggageClient.validateBaggage(eq("P100"), eq("SKY123"), eq(weight)))
                                .thenReturn(new BaggageValidationResponse(
                                        "BAG-004",
                                        true,
                                        BigDecimal.valueOf(weight),
                                        BigDecimal.valueOf(5.0),
                                        BigDecimal.valueOf(50.0),
                                        "EXCESS_FEE_REQUIRED",
                                        "Heavy"
                                ));

                // Act
                CheckInResponse response = checkInService.updateBaggage(checkinId, weight);

                // Assert
                assertEquals(CheckInStatus.WAITING_FOR_PAYMENT, checkIn.getStatus());
                assertEquals(50.0, checkIn.getExcessBaggageFee());
                verify(seatClient, never()).confirmSeat(any(), any());
        }

        @Test
        void getCheckInStatus_shouldReturnStatus() {
                // Arrange
                Long checkinId = 1L;
                CheckIn checkIn = CheckIn.builder()
                                .id(checkinId)
                                .bookingReference("SKY123")
                                .passengerId("P100")
                                .status(CheckInStatus.IN_PROGRESS)
                                .baggageWeight(0.0)
                                .build();

                when(checkinRepository.findById(checkinId)).thenReturn(Optional.of(checkIn));

                // Act
                CheckInResponse response = checkInService.getCheckInStatus(checkinId);

                // Assert
                assertEquals(CheckInStatus.IN_PROGRESS, response.status());
                assertEquals("SKY123", response.bookingReference());
        }

        @Test
        void getCheckInStatus_shouldThrowException_whenNotFound() {
                // Arrange
                Long checkinId = 1L;
                when(checkinRepository.findById(checkinId)).thenReturn(Optional.empty());

                // Act & Assert
                assertThrows(CheckInNotFoundException.class, () -> checkInService.getCheckInStatus(checkinId));
        }

        @Test
        void cancelCheckIn_shouldCancelAndReleaseSeat() {
                // Arrange
                Long checkinId = 1L;
                CheckIn checkIn = CheckIn.builder()
                                .id(checkinId)
                                .bookingReference("SKY123")
                                .passengerId("P100")
                                .seatId(10L)
                                .status(CheckInStatus.IN_PROGRESS)
                                .build();

                when(checkinRepository.findById(checkinId)).thenReturn(Optional.of(checkIn));
                when(checkinRepository.save(any(CheckIn.class))).thenReturn(checkIn);

                // Act
                checkInService.cancelCheckIn(checkinId);

                // Assert
                assertEquals(CheckInStatus.CANCELLED, checkIn.getStatus());
                verify(seatClient).cancelSeat(10L, "P100");
        }

        @Test
        void completeCheckIn_shouldComplete_whenNoPaymentRequired() {
                // Arrange
                Long checkinId = 1L;
                CheckIn checkIn = CheckIn.builder()
                                .id(checkinId)
                                .bookingReference("SKY123")
                                .passengerId("P100")
                                .seatId(10L)
                                .status(CheckInStatus.WAITING_FOR_PAYMENT)
                                .excessBaggageFee(50.0)
                                .build();

                when(checkinRepository.findById(checkinId)).thenReturn(Optional.of(checkIn));
                when(checkinRepository.save(any(CheckIn.class))).thenReturn(checkIn);

                // Act
                CheckInResponse response = checkInService.completeCheckIn(checkinId, "PAY123");

                // Assert
                assertEquals(CheckInStatus.COMPLETED, response.status());
                verify(seatClient).confirmSeat(anyLong(), any());
        }
}
