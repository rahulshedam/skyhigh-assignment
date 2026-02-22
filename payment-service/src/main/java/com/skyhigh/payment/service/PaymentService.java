package com.skyhigh.payment.service;

import com.skyhigh.common.dto.ApiResponse;
import com.skyhigh.payment.dto.PaymentData;
import com.skyhigh.payment.dto.PaymentRequest;
import com.skyhigh.payment.event.EventPublisher;
import com.skyhigh.payment.event.PaymentEvent;
import com.skyhigh.payment.exception.PaymentNotFoundException;
import com.skyhigh.payment.exception.PaymentProcessingException;
import com.skyhigh.payment.model.Payment;
import com.skyhigh.payment.model.PaymentStatus;
import com.skyhigh.payment.repository.PaymentRepository;
import com.skyhigh.payment.service.PaymentSimulationService.PaymentSimulationResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Core payment service for processing payments.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentSimulationService simulationService;
    private final EventPublisher eventPublisher;
    private final MetricsService metricsService;

    /**
     * Process a payment request
     */
    @Transactional
    public ApiResponse<PaymentData> processPayment(PaymentRequest request) {
        log.info("Processing payment for passenger: {}, booking: {}, amount: {} {}",
                request.getPassengerId(), request.getBookingReference(),
                request.getAmount(), request.getCurrency());

        try {
            // Create payment record
            Payment payment = createPaymentRecord(request);
            payment.setStatus(PaymentStatus.PROCESSING);
            payment = paymentRepository.save(payment);

            // Simulate payment processing (probabilistic: success rate from payment.simulation.success-rate)
            PaymentSimulationResult result = simulationService.simulatePayment(request.getSimulateFailure());

            // Update payment based on result
            if (result.isSuccess()) {
                payment.setStatus(PaymentStatus.COMPLETED);
                payment.setTransactionId(result.getTransactionId());
                payment.setCompletedAt(LocalDateTime.now());
                payment = paymentRepository.save(payment);

                // Publish success event
                publishPaymentEvent(payment, true);

                // Update metrics
                metricsService.incrementSuccessfulPayments();

                log.info("Payment completed successfully: {}", payment.getPaymentReference());

                return ApiResponse.success(
                        toPaymentData(payment),
                        "Payment processed successfully");
            } else {
                payment.setStatus(PaymentStatus.FAILED);
                payment.setFailureReason(result.getFailureReason());
                payment.setCompletedAt(LocalDateTime.now());
                payment = paymentRepository.save(payment);

                // Publish failure event
                publishPaymentEvent(payment, false);

                // Update metrics
                metricsService.incrementFailedPayments();

                log.warn("Payment failed: {} - Reason: {}",
                        payment.getPaymentReference(), result.getFailureReason());

                return ApiResponse.error(
                        String.format("Payment failed: %s", result.getFailureReason()));
            }

        } catch (Exception e) {
            log.error("Error processing payment for passenger: {}", request.getPassengerId(), e);
            metricsService.incrementFailedPayments();
            throw new PaymentProcessingException("Failed to process payment", e);
        }
    }

    /**
     * Get payment by reference
     */
    public ApiResponse<PaymentData> getPaymentByReference(String paymentReference) {
        log.debug("Fetching payment with reference: {}", paymentReference);

        Payment payment = paymentRepository.findByPaymentReference(paymentReference)
                .orElseThrow(() -> new PaymentNotFoundException(paymentReference));

        return ApiResponse.success(
                toPaymentData(payment),
                "Payment retrieved successfully");
    }

    /**
     * Get all payments for a passenger
     */
    public ApiResponse<List<PaymentData>> getPaymentsByPassengerId(String passengerId) {
        log.debug("Fetching payments for passenger: {}", passengerId);

        List<Payment> payments = paymentRepository.findByPassengerId(passengerId);
        List<PaymentData> paymentDataList = payments.stream()
                .map(this::toPaymentData)
                .collect(Collectors.toList());

        return ApiResponse.success(
                paymentDataList,
                String.format("Found %d payment(s) for passenger", payments.size()));
    }

    /**
     * Get all payments for a booking reference
     */
    public ApiResponse<List<PaymentData>> getPaymentsByBookingReference(String bookingReference) {
        log.debug("Fetching payments for booking: {}", bookingReference);

        List<Payment> payments = paymentRepository.findByBookingReference(bookingReference);
        List<PaymentData> paymentDataList = payments.stream()
                .map(this::toPaymentData)
                .collect(Collectors.toList());

        return ApiResponse.success(
                paymentDataList,
                String.format("Found %d payment(s) for booking", payments.size()));
    }

    /**
     * Create payment record from request
     */
    private Payment createPaymentRecord(PaymentRequest request) {
        return Payment.builder()
                .paymentReference(generatePaymentReference())
                .passengerId(request.getPassengerId())
                .bookingReference(request.getBookingReference())
                .amount(request.getAmount())
                .currency(request.getCurrency())
                .paymentType(request.getPaymentType())
                .status(PaymentStatus.PENDING)
                .metadata(request.getMetadata())
                .initiatedAt(LocalDateTime.now())
                .build();
    }

    /**
     * Generate unique payment reference
     */
    private String generatePaymentReference() {
        return "PAY-" + System.currentTimeMillis() + "-" +
                UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    /**
     * Convert Payment entity to PaymentData DTO
     */
    private PaymentData toPaymentData(Payment payment) {
        return PaymentData.builder()
                .paymentReference(payment.getPaymentReference())
                .status(payment.getStatus())
                .transactionId(payment.getTransactionId())
                .amount(payment.getAmount())
                .currency(payment.getCurrency())
                .failureReason(payment.getFailureReason())
                .timestamp(payment.getCompletedAt() != null ? payment.getCompletedAt() : payment.getInitiatedAt())
                .build();
    }

    /**
     * Publish payment event to RabbitMQ
     */
    private void publishPaymentEvent(Payment payment, boolean success) {
        PaymentEvent event = PaymentEvent.builder()
                .paymentReference(payment.getPaymentReference())
                .passengerId(payment.getPassengerId())
                .bookingReference(payment.getBookingReference())
                .amount(payment.getAmount())
                .currency(payment.getCurrency())
                .timestamp(LocalDateTime.now())
                .metadata(payment.getMetadata())
                .build();

        if (success) {
            eventPublisher.publishPaymentCompletedEvent(event);
        } else {
            eventPublisher.publishPaymentFailedEvent(event);
        }
    }
}
