package com.skyhigh.payment.repository;

import com.skyhigh.payment.model.Payment;
import com.skyhigh.payment.model.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for Payment entity.
 */
@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    /**
     * Find payment by payment reference
     */
    Optional<Payment> findByPaymentReference(String paymentReference);

    /**
     * Find all payments for a passenger
     */
    List<Payment> findByPassengerId(String passengerId);

    /**
     * Find all payments for a booking reference
     */
    List<Payment> findByBookingReference(String bookingReference);

    /**
     * Find payments by status
     */
    List<Payment> findByStatus(PaymentStatus status);
}
