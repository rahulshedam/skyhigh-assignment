package com.skyhigh.baggage.service;

import com.skyhigh.baggage.dto.BaggageData;
import com.skyhigh.baggage.dto.BaggageValidationRequest;
import com.skyhigh.baggage.dto.BaggageValidationResponse;
import com.skyhigh.baggage.event.BaggageEvent;
import com.skyhigh.baggage.event.EventPublisher;
import com.skyhigh.baggage.exception.BaggageNotFoundException;
import com.skyhigh.baggage.model.BaggageRecord;
import com.skyhigh.baggage.model.BaggageStatus;
import com.skyhigh.baggage.repository.BaggageRecordRepository;
import com.skyhigh.baggage.util.BaggageConstants;
import com.skyhigh.common.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for baggage validation and fee calculation.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BaggageService {

    private final BaggageRecordRepository baggageRecordRepository;
    private final EventPublisher eventPublisher;
    private final MetricsService metricsService;

    /**
     * Validate baggage and calculate fees if necessary.
     */
    @Transactional
    public ApiResponse<BaggageValidationResponse> validateBaggage(BaggageValidationRequest request) {
        log.info("Validating baggage for passenger: {}, weight: {} kg",
                request.getPassengerId(), request.getWeight());

        metricsService.recordValidation();

        // Generate unique reference
        String baggageReference = generateBaggageReference();

        // Calculate excess weight and fee
        BigDecimal excessWeight = calculateExcessWeight(request.getWeight());
        BigDecimal excessFee = calculateExcessFee(excessWeight);

        boolean hasExcess = excessWeight.compareTo(BigDecimal.ZERO) > 0;
        BaggageStatus status = hasExcess ? BaggageStatus.EXCESS_FEE_REQUIRED : BaggageStatus.VALIDATED;

        // Create and save baggage record
        BaggageRecord record = BaggageRecord.builder()
                .baggageReference(baggageReference)
                .passengerId(request.getPassengerId())
                .bookingReference(request.getBookingReference())
                .weight(request.getWeight())
                .excessWeight(excessWeight)
                .excessFee(excessFee)
                .status(status)
                .build();

        baggageRecordRepository.save(record);

        // Publish events
        BaggageEvent event = createBaggageEvent(record, "BAGGAGE_VALIDATED");
        eventPublisher.publishBaggageValidated(event);

        if (hasExcess) {
            metricsService.recordExcessBaggage();
            BaggageEvent feeEvent = createBaggageEvent(record, "EXCESS_FEE_CALCULATED");
            eventPublisher.publishExcessFee(feeEvent);
        } else {
            metricsService.recordValidBaggage();
        }

        // Build response
        BaggageValidationResponse response = BaggageValidationResponse.builder()
                .baggageReference(baggageReference)
                .isValid(!hasExcess)
                .weight(request.getWeight())
                .excessWeight(excessWeight)
                .excessFee(excessFee)
                .status(status)
                .message(hasExcess ? String.format("Excess baggage fee of $%.2f required", excessFee)
                        : "Baggage within free limit")
                .build();

        log.info("Baggage validation completed: {}, excess fee: ${}", baggageReference, excessFee);

        return ApiResponse.success(response);
    }

    /**
     * Get baggage by reference.
     */
    public ApiResponse<BaggageData> getBaggageByReference(String reference) {
        log.info("Fetching baggage by reference: {}", reference);

        BaggageRecord record = baggageRecordRepository.findByBaggageReference(reference)
                .orElseThrow(() -> BaggageNotFoundException.byReference(reference));

        return ApiResponse.success(mapToData(record));
    }

    /**
     * Get all baggage for a passenger.
     */
    public ApiResponse<List<BaggageData>> getBaggageByPassengerId(String passengerId) {
        log.info("Fetching baggage for passenger: {}", passengerId);

        List<BaggageRecord> records = baggageRecordRepository.findByPassengerId(passengerId);
        List<BaggageData> data = records.stream()
                .map(this::mapToData)
                .collect(Collectors.toList());

        return ApiResponse.success(data);
    }

    /**
     * Get all baggage for a booking.
     */
    public ApiResponse<List<BaggageData>> getBaggageByBookingReference(String bookingReference) {
        log.info("Fetching baggage for booking: {}", bookingReference);

        List<BaggageRecord> records = baggageRecordRepository.findByBookingReference(bookingReference);
        List<BaggageData> data = records.stream()
                .map(this::mapToData)
                .collect(Collectors.toList());

        return ApiResponse.success(data);
    }

    /**
     * Calculate excess weight.
     */
    private BigDecimal calculateExcessWeight(BigDecimal weight) {
        BigDecimal excess = weight.subtract(BaggageConstants.MAX_FREE_WEIGHT);
        return excess.max(BigDecimal.ZERO);
    }

    /**
     * Calculate excess fee.
     */
    private BigDecimal calculateExcessFee(BigDecimal excessWeight) {
        return excessWeight.multiply(BaggageConstants.EXCESS_FEE_PER_KG);
    }

    /**
     * Generate unique baggage reference.
     */
    private String generateBaggageReference() {
        return BaggageConstants.BAGGAGE_REFERENCE_PREFIX +
                UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    /**
     * Create baggage event.
     */
    private BaggageEvent createBaggageEvent(BaggageRecord record, String eventType) {
        return BaggageEvent.builder()
                .baggageReference(record.getBaggageReference())
                .passengerId(record.getPassengerId())
                .bookingReference(record.getBookingReference())
                .weight(record.getWeight())
                .excessWeight(record.getExcessWeight())
                .excessFee(record.getExcessFee())
                .status(record.getStatus().name())
                .timestamp(LocalDateTime.now())
                .eventType(eventType)
                .build();
    }

    /**
     * Map entity to DTO.
     */
    private BaggageData mapToData(BaggageRecord record) {
        return BaggageData.builder()
                .baggageReference(record.getBaggageReference())
                .passengerId(record.getPassengerId())
                .bookingReference(record.getBookingReference())
                .weight(record.getWeight())
                .excessWeight(record.getExcessWeight())
                .excessFee(record.getExcessFee())
                .status(record.getStatus())
                .createdAt(record.getCreatedAt())
                .build();
    }
}
