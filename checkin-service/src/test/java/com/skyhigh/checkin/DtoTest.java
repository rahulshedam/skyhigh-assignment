package com.skyhigh.checkin;

import com.skyhigh.checkin.client.dto.*;
import com.skyhigh.checkin.model.dto.*;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class DtoTest {

    @Test
    void testPaymentRequest() {
        PaymentRequest dto1 = new PaymentRequest("REF", "PASS", 10.0, "DESC");
        PaymentRequest dto2 = new PaymentRequest("REF", "PASS", 10.0, "DESC");
        assertEquals(dto1, dto2);
        assertEquals(dto1.hashCode(), dto2.hashCode());
        assertNotEquals(dto1, new PaymentRequest("REF", "PASS", 20.0, "DESC"));
        assertEquals("REF", dto1.bookingReference());
    }

    @Test
    void testPaymentResponse() {
        PaymentResponse dto1 = new PaymentResponse("ID", "STATUS", 10.0, "MSG");
        PaymentResponse dto2 = new PaymentResponse("ID", "STATUS", 10.0, "MSG");
        assertEquals(dto1, dto2);
        assertEquals(dto1.hashCode(), dto2.hashCode());
        assertEquals("ID", dto1.paymentId());
    }

    @Test
    void testSeatHoldRequest() {
        SeatHoldRequest dto1 = new SeatHoldRequest("PASS", "REF");
        SeatHoldRequest dto2 = new SeatHoldRequest("PASS", "REF");
        assertEquals(dto1, dto2);
        assertEquals(dto1.hashCode(), dto2.hashCode());
        assertEquals("PASS", dto1.passengerId());
    }

    @Test
    void testSeatConfirmRequest() {
        SeatConfirmRequest dto1 = new SeatConfirmRequest("PASS", "REF");
        SeatConfirmRequest dto2 = new SeatConfirmRequest("PASS", "REF");
        assertEquals(dto1, dto2);
        assertEquals(dto1.hashCode(), dto2.hashCode());
        assertEquals("PASS", dto1.passengerId());
    }

    @Test
    void testSeatResponse() {
        SeatResponse dto1 = new SeatResponse(1L, "1A", "ECO", "PASS", "REF");
        SeatResponse dto2 = new SeatResponse(1L, "1A", "ECO", "PASS", "REF");
        assertEquals(dto1, dto2);
        assertEquals(dto1.hashCode(), dto2.hashCode());
        assertEquals(1L, dto1.id());
    }

    @Test
    void testBaggageValidationRequest() {
        BaggageValidationRequest dto1 = new BaggageValidationRequest("P100", "SKY123", BigDecimal.valueOf(20.0));
        BaggageValidationRequest dto2 = new BaggageValidationRequest("P100", "SKY123", BigDecimal.valueOf(20.0));
        assertEquals(dto1, dto2);
        assertEquals(dto1.hashCode(), dto2.hashCode());
        assertEquals(BigDecimal.valueOf(20.0), dto1.weight());
        assertEquals("P100", dto1.passengerId());
        assertEquals("SKY123", dto1.bookingReference());
    }

    @Test
    void testBaggageValidationResponse() {
        BaggageValidationResponse dto1 = new BaggageValidationResponse(
                "BAG-001", true, BigDecimal.valueOf(20.0), BigDecimal.ZERO, BigDecimal.valueOf(50.0), "EXCESS_FEE_REQUIRED", "Heavy");
        BaggageValidationResponse dto2 = new BaggageValidationResponse(
                "BAG-001", true, BigDecimal.valueOf(20.0), BigDecimal.ZERO, BigDecimal.valueOf(50.0), "EXCESS_FEE_REQUIRED", "Heavy");
        assertEquals(dto1, dto2);
        assertEquals(dto1.hashCode(), dto2.hashCode());
        assertEquals(BigDecimal.valueOf(50.0), dto1.excessFee());
        assertEquals(50.0, dto1.excessFeeAsDouble());
    }

    @Test
    void testCheckInStartRequest() {
        CheckInStartRequest dto1 = new CheckInStartRequest("REF", "PASS", 1L, 1L, 10.0);
        CheckInStartRequest dto2 = new CheckInStartRequest("REF", "PASS", 1L, 1L, 10.0);
        assertEquals(dto1, dto2);
        assertEquals(dto1.hashCode(), dto2.hashCode());
        assertEquals("REF", dto1.bookingReference());
    }

    @Test
    void testCheckInResponse() {
        LocalDateTime now = LocalDateTime.now();
        CheckInResponse dto1 = new CheckInResponse(1L, "REF", "PASS", 1L, 1L,
                com.skyhigh.checkin.model.enums.CheckInStatus.IN_PROGRESS, 10.0, 0.0, null, "MSG", now, null);
        CheckInResponse dto2 = new CheckInResponse(1L, "REF", "PASS", 1L, 1L,
                com.skyhigh.checkin.model.enums.CheckInStatus.IN_PROGRESS, 10.0, 0.0, null, "MSG", now, null);
        assertEquals(dto1, dto2);
        assertEquals(dto1.hashCode(), dto2.hashCode());
    }

    @Test
    void testCheckInCompleteRequest() {
        CheckInCompleteRequest dto1 = new CheckInCompleteRequest("PAY");
        CheckInCompleteRequest dto2 = new CheckInCompleteRequest("PAY");
        assertEquals(dto1, dto2);
        assertEquals(dto1.hashCode(), dto2.hashCode());
        assertEquals("PAY", dto1.paymentId());
    }

    @Test
    void testCheckInBaggageRequest() {
        CheckInBaggageRequest dto1 = new CheckInBaggageRequest(20.0);
        CheckInBaggageRequest dto2 = new CheckInBaggageRequest(20.0);
        assertEquals(dto1, dto2);
        assertEquals(dto1.hashCode(), dto2.hashCode());
        assertEquals(20.0, dto1.weight());
    }
}
