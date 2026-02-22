package com.skyhigh.baggage.service;

import com.skyhigh.baggage.dto.BaggageData;
import com.skyhigh.baggage.dto.BaggageValidationRequest;
import com.skyhigh.baggage.dto.BaggageValidationResponse;
import com.skyhigh.baggage.event.EventPublisher;
import com.skyhigh.baggage.exception.BaggageNotFoundException;
import com.skyhigh.baggage.model.BaggageRecord;
import com.skyhigh.baggage.model.BaggageStatus;
import com.skyhigh.baggage.repository.BaggageRecordRepository;
import com.skyhigh.common.dto.ApiResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BaggageServiceTest {

    @Mock
    private BaggageRecordRepository baggageRecordRepository;

    @Mock
    private EventPublisher eventPublisher;

    @Mock
    private MetricsService metricsService;

    @InjectMocks
    private BaggageService baggageService;

    private BaggageValidationRequest validRequest;
    private BaggageRecord savedRecord;

    @BeforeEach
    void setUp() {
        validRequest = BaggageValidationRequest.builder()
                .passengerId("PASS123")
                .bookingReference("BOOK456")
                .weight(new BigDecimal("20.0"))
                .build();

        savedRecord = BaggageRecord.builder()
                .id(1L)
                .baggageReference("BAG-12345678")
                .passengerId("PASS123")
                .bookingReference("BOOK456")
                .weight(new BigDecimal("20.0"))
                .excessWeight(BigDecimal.ZERO)
                .excessFee(BigDecimal.ZERO)
                .status(BaggageStatus.VALIDATED)
                .build();
    }

    @Test
    void validateBaggage_WithinLimit_Success() {
        // Arrange
        when(baggageRecordRepository.save(any(BaggageRecord.class))).thenReturn(savedRecord);

        // Act
        ApiResponse<BaggageValidationResponse> response = baggageService.validateBaggage(validRequest);

        // Assert
        assertTrue(response.isSuccess());
        assertNotNull(response.getData());
        assertTrue(response.getData().isValid());
        assertEquals(0, response.getData().getExcessFee().compareTo(BigDecimal.ZERO));
        assertEquals(BaggageStatus.VALIDATED, response.getData().getStatus());

        verify(baggageRecordRepository).save(any(BaggageRecord.class));
        verify(eventPublisher).publishBaggageValidated(any());
        verify(metricsService).recordValidation();
        verify(metricsService).recordValidBaggage();
        verify(eventPublisher, never()).publishExcessFee(any());
    }

    @Test
    void validateBaggage_ExcessWeight_CalculatesFee() {
        // Arrange
        BaggageValidationRequest excessRequest = BaggageValidationRequest.builder()
                .passengerId("PASS123")
                .bookingReference("BOOK456")
                .weight(new BigDecimal("30.0")) // 5kg excess
                .build();

        BaggageRecord excessRecord = BaggageRecord.builder()
                .id(1L)
                .baggageReference("BAG-12345678")
                .passengerId("PASS123")
                .bookingReference("BOOK456")
                .weight(new BigDecimal("30.0"))
                .excessWeight(new BigDecimal("5.0"))
                .excessFee(new BigDecimal("50.0"))
                .status(BaggageStatus.EXCESS_FEE_REQUIRED)
                .build();

        when(baggageRecordRepository.save(any(BaggageRecord.class))).thenReturn(excessRecord);

        // Act
        ApiResponse<BaggageValidationResponse> response = baggageService.validateBaggage(excessRequest);

        // Assert
        assertTrue(response.isSuccess());
        assertNotNull(response.getData());
        assertFalse(response.getData().isValid());
        assertEquals(0, new BigDecimal("5.0").compareTo(response.getData().getExcessWeight()));
        assertEquals(0, new BigDecimal("50.0").compareTo(response.getData().getExcessFee()));
        assertEquals(BaggageStatus.EXCESS_FEE_REQUIRED, response.getData().getStatus());

        verify(baggageRecordRepository).save(any(BaggageRecord.class));
        verify(eventPublisher).publishBaggageValidated(any());
        verify(eventPublisher).publishExcessFee(any());
        verify(metricsService).recordValidation();
        verify(metricsService).recordExcessBaggage();
    }

    @Test
    void getBaggageByReference_Found_ReturnsData() {
        // Arrange
        when(baggageRecordRepository.findByBaggageReference("BAG-12345678"))
                .thenReturn(Optional.of(savedRecord));

        // Act
        ApiResponse<BaggageData> response = baggageService.getBaggageByReference("BAG-12345678");

        // Assert
        assertTrue(response.isSuccess());
        assertNotNull(response.getData());
        assertEquals("BAG-12345678", response.getData().getBaggageReference());
        assertEquals("PASS123", response.getData().getPassengerId());

        verify(baggageRecordRepository).findByBaggageReference("BAG-12345678");
    }

    @Test
    void getBaggageByReference_NotFound_ThrowsException() {
        // Arrange
        when(baggageRecordRepository.findByBaggageReference("INVALID"))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(BaggageNotFoundException.class, () -> baggageService.getBaggageByReference("INVALID"));

        verify(baggageRecordRepository).findByBaggageReference("INVALID");
    }

    @Test
    void getBaggageByPassengerId_ReturnsMultipleRecords() {
        // Arrange
        List<BaggageRecord> records = Arrays.asList(savedRecord, savedRecord);
        when(baggageRecordRepository.findByPassengerId("PASS123")).thenReturn(records);

        // Act
        ApiResponse<List<BaggageData>> response = baggageService.getBaggageByPassengerId("PASS123");

        // Assert
        assertTrue(response.isSuccess());
        assertNotNull(response.getData());
        assertEquals(2, response.getData().size());

        verify(baggageRecordRepository).findByPassengerId("PASS123");
    }

    @Test
    void getBaggageByBookingReference_ReturnsRecords() {
        // Arrange
        List<BaggageRecord> records = Arrays.asList(savedRecord);
        when(baggageRecordRepository.findByBookingReference("BOOK456")).thenReturn(records);

        // Act
        ApiResponse<List<BaggageData>> response = baggageService.getBaggageByBookingReference("BOOK456");

        // Assert
        assertTrue(response.isSuccess());
        assertNotNull(response.getData());
        assertEquals(1, response.getData().size());

        verify(baggageRecordRepository).findByBookingReference("BOOK456");
    }
}
