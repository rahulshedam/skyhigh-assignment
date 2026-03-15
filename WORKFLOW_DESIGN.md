# SkyHigh Core – Workflow Design Document

**Document Version**: 1.0  
**Last Updated**: February 21, 2026  
**Purpose**: Implementation flow diagrams, database schema, and state history design

---

## Table of Contents

1. [Overview](#1-overview)
2. [Primary Flow Diagrams](#2-primary-flow-diagrams)
3. [Database Schema](#3-database-schema)
4. [State History Design](#4-state-history-design)
5. [Implementation Notes](#5-implementation-notes)

---

## 1. Overview

SkyHigh Core is a microservices-based digital check-in system. This document describes:

- **Primary flows**: Check-in, seat hold/expiry, baggage validation, payment, waitlist
- **Database schema**: Tables, relationships, indexes
- **State history**: How audit trails and state transitions are stored

### 1.1 System Components

| Component | Purpose |
|-----------|---------|
| **Seat Management Service** | Seat lifecycle, hold/confirm/cancel, waitlist, expiry |
| **Check-in Service** | Orchestrates seat hold, baggage validation, payment |
| **Baggage Service** | Validates weight (25kg limit), calculates excess fees |
| **Payment Service** | Processes excess baggage payments (simulated) |
| **Notification Service** | Consumes events, sends confirmations |
| **Frontend** | React UI for the check-in flow |

---

## 2. Primary Flow Diagrams

### 2.1 Complete Check-in Flow (End-to-End)

```
┌─────────────┐     ┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│  Passenger  │     │   Frontend  │     │   Check-in  │     │ Seat Mgmt   │
│             │     │   (React)   │     │   Service   │     │   Service   │
└──────┬──────┘     └──────┬──────┘     └──────┬──────┘     └──────┬──────┘
       │                   │                   │                   │
       │  Step 1: Login    │                   │                   │
       │                   │                   │                   │
       │ Enter Email       │                   │                   │
       │──────────────────>│                   │                   │
       │                   │ POST /auth/validate-email             │
       │                   │──────────────────────────────────────>│
       │                   │                   │                   │
       │ Valid / Invalid ? │                   │                   │
       │<──────────────────│<──────────────────────────────────────│
       │                   │                   │                   │
       │ [If Valid]        │ POST /otp/send    │                   │
       │                   │──────────────────>│ Notification Svc  │
       │                   │                   │                   │
       │ [User Input]      │ POST /otp/verify  │                   │
       │ Enter OTP         │──────────────────>│ Notification Svc  │
       │                   │                   │                   │
       │ Logged In ✓       │                   │                   │
       │<──────────────────│                   │                   │
       │                   │                   │                   │
       │  Step 2: Check-in │                   │                   │
       │                   │                   │                   │
       │ Enter booking ref │                   │                   │
       │──────────────────>│                   │                   │
       │                   │ POST /checkin/start                   │
       │                   │──────────────────>│                   │
       │                   │                   │ POST /seats/hold  │
       │                   │                   │──────────────────>│
       │                   │                   │   [Redis Lock]    │
       │                   │                   │   [Seat HELD]     │
       │                   │                   │<──────────────────│
       │                   │                   │                   │
       │                   │ Check-in started  │                   │
       │                   │<──────────────────│                   │
       │                   │ (120s countdown)  │                   │
       │                   │                   │                   │
       │ Enter baggage wt  │                   │                   │
       │──────────────────>│                   │                   │
       │                   │ POST /baggage     │                   │
       │                   │──────────────────>│ Baggage Service   │
       │                   │                   │                   │
       │                   │ Weight ≤ 25kg?    │                   │
       │                   │   Yes: Proceed    │                   │
       │                   │   No: Show payment form               │
       │                   │                   │                   │
       │ [If excess] Pay   │ POST /payment     │                   │
       │──────────────────>│──────────────────>│ Payment Service   │
       │                   │                   │                   │
       │                   │ POST /checkin/complete                │
       │                   │──────────────────>│                   │
       │                   │                   │ POST /seats/confirm
       │                   │                   │──────────────────>│
       │                   │                   │   [Seat CONFIRMED]│
       │                   │                   │                   │
       │                   │ Boarding pass     │                   │
       │<──────────────────│<──────────────────│                   │
```

### 2.2 Seat Lifecycle State Diagram

```
                    ┌───────────┐
                    │ AVAILABLE │
                    └─────┬─────┘
                          │ Passenger selects seat
                          │ (distributed lock acquired)
                          ▼
                    ┌───────────┐     120s expires
                    │   HELD    │────────────────────┐
                    └─────┬─────┘                    │
                          │ Confirm within 120s     │
                          │ (payment if needed)     │
                          ▼                         │
                    ┌───────────┐                    │
                    │ CONFIRMED │                    │
                    └─────┬─────┘                    │
                          │ Cancel                  │
                          ▼                         ▼
                    ┌───────────┐             ┌───────────┐
                    │ CANCELLED │             │ AVAILABLE │
                    └─────┬─────┘             └─────▲─────┘
                          │                         │
                          └─────────────────────────┘

### 2.2.1 Authentication Workflow (Email OTP)

```
   User          Frontend        Seat Mgmt      Notification
     │               │               │               │
     │ Submit Email  │               │               │
     │──────────────>│               │               │
     │               │ validate-email│               │
     │               │──────────────>│               │
     │               │ (Check DB)    │               │
     │               │ <─────────────│               │
     │               │               │               │
     │               │ [If Valid]    │               │
     │               │ otp/send      │               │
     │               │──────────────────────────────>│
     │               │               │               │
     │ Enter OTP     │               │               │
     │──────────────>│               │               │
     │               │ otp/verify    │               │
     │               │──────────────────────────────>│
     │               │               │               │
     │ Login Success │               │               │
     │ <─────────────│               │               │
```
```

### 2.3 Seat Hold with Concurrency (Conflict Resolution)

```
  Passenger A              Passenger B              Seat API                Redis Lock
       │                        │                        │                       │
       │  POST /hold 12A         │  POST /hold 12A        │                       │
       │────────────────────────>│───────────────────────>│                       │
       │                         │                        │  tryLock(12A)         │
       │                         │                        │─────────────────────>│
       │                         │                        │  Acquired by A        │
       │                         │                        │<─────────────────────│
       │                         │                        │  Seat = AVAILABLE ✓   │
       │                         │                        │  Update → HELD        │
       │                         │                        │  unlock(12A)          │
       │                         │                        │─────────────────────>│
       │  200 OK                 │                        │                       │
       │<────────────────────────│                        │                       │
       │                         │                        │  tryLock(12A)         │
       │                         │                        │─────────────────────>│
       │                         │                        │  Acquired by B        │
       │                         │                        │  Seat = HELD ✗        │
       │                         │  409 Seat Already Held │                       │
       │                         │<───────────────────────│                       │
```

### 2.3.1 Idempotency in Seat Holds

If Passenger A successfully holds `12A` and submits another `POST /hold 12A` request due to a network retry (same `passengerId` and `bookingReference`):
- The system recognizes Passenger A already holds the seat.
- Bypasses the conflict error (409).
- Returns `200 OK` exactly as it did initially.

### 2.4 Seat Expiry & Waitlist Assignment Flow

```
  Scheduler           SeatExpiryService         Database            WaitlistService
  (every 10s)                │                       │                       │
       │                     │                       │                       │
       │  execute()          │                       │                       │
       │────────────────────>│                       │                       │
       │                     │  SELECT expired holds │                       │
       │                     │──────────────────────>│                       │
       │                     │                       │                       │
       │                     │  For each expired:    │                       │
       │                     │  - Acquire Redis lock │
       │                     │    on seat:{id}       │
       │                     │  - Re-read assignment │
       │                     │    in DB              │
       │                     │  - If still HELD and  │
       │                     │    expires_at < now:  │
       │                     │      · Update seat →  │
       │                     │        AVAILABLE      │
       │                     │      · Mark assignment│
       │                     │        CANCELLED      │
       │                     │      · Write EXPIRED  │
       │                     │        to history     │
       │                     │      · Publish        │
       │                     │        SeatReleasedEvent
       │                     │  - If seat already    │
       │                     │    CONFIRMED or       │
       │                     │    assignment not HELD│
       │                     │    → skip (no-op)     │
       │                     │                       │
       │                     │  processSeatRelease() │                       │
       │                     │──────────────────────────────────────────────>│
       │                     │                       │  SELECT waitlist      │
       │                     │                       │  ORDER BY joined_at   │
       │                     │                       │  LIMIT 1              │
       │                     │                       │<──────────────────────│
       │                     │  holdSeat(next)       │                       │
       │                     │<──────────────────────────────────────────────│
       │                     │  Publish WaitlistAssignedEvent                │
```

### 2.4.1 Cancellation and Waitlist Reassignment

- **Check-in cancel:** Allowed when status is `IN_PROGRESS` or `WAITING_FOR_PAYMENT`. Check-in Service calls Seat Management `DELETE /api/seats/{seatId}/cancel?passengerId=...` to release the hold (or cancel confirmed seat). Check-in status is set to `CANCELLED`. Idempotent if already cancelled.
- **Seat cancel:** Seat Management releases the hold or cancels the confirmed assignment; requires `passengerId`. After the seat becomes AVAILABLE, the **WaitlistProcessorScheduler** (runs every 5 seconds) assigns it to the next waitlisted passenger (FIFO). Concurrency is handled by seat lock and optimistic locking in Seat Management.

---

### 2.5 Baggage & Payment Sub-Flow

```
  Check-in Service        Baggage Service         Payment Service
         │                       │                       │
         │  validate(weight)     │                       │
         │──────────────────────>│                       │
         │                       │  weight ≤ 25kg?       │
         │                       │  fee = 0, VALIDATED   │
         │                       │  OR                   │
         │                       │  weight > 25kg        │
         │                       │  fee = (w-25)*10      │
         │                       │  EXCESS_FEE_REQUIRED  │
         │<──────────────────────│                       │
         │                       │                       │
         │  [If fee > 0]         │  process(payment)     │
         │──────────────────────────────────────────────>│
         │                       │                       │  Simulate gateway
         │                       │                       │  ~70% success
         │  paymentId / error    │                       │
         │<──────────────────────────────────────────────│
         │                       │                       │
         │  complete(paymentId)  │                       │
         │  [verifies payment]   │                       │
```

### 2.5.1 Payment Pause/Resume Flow

When baggage weight exceeds 25 kg, check-in **pauses** at status `WAITING_FOR_PAYMENT`:

1. **When check-in pauses:** After baggage validation returns excess fee (weight > 25 kg), Check-in Service sets status to `WAITING_FOR_PAYMENT` and stores the excess fee; the seat remains held.
2. **Allowed actions while paused:** The client may call `GET /api/checkin/{checkinId}` to read status and fee; no other state change until payment is completed.
3. **Resume:** The client processes payment via Payment Service (`POST /api/payments/process`), then calls `POST /api/checkin/{checkinId}/complete` with the request body `{ "paymentId": "<payment_reference>" }`. Check-in Service records the payment reference, confirms the seat with Seat Management, and sets status to `COMPLETED`.
4. **Verification:** Payment is processed and validated by Payment Service. Check-in Service requires a **non-empty `paymentId` when an excess fee is present** and records that reference; it does **not** recompute the fee amount at completion time (the excess fee was already determined at pause time).

### 2.5.2 Check-in State Machine for Payment-Dependent Operations

The **check-in workflow states** for baggage and payment are:

- `IN_PROGRESS`
  - When: A check-in is first started (seat held successfully) or when baggage is updated with no excess fee.
  - Characteristics:
    - Seat is in `HELD` state in Seat Management.
    - `excessBaggageFee` is `0` or `null`.
  - Allowed transitions:
    - `IN_PROGRESS → WAITING_FOR_PAYMENT` when `baggageWeight > 25kg` and Baggage Service returns `requiresPayment=true`.
    - `IN_PROGRESS → COMPLETED` via `POST /api/checkin/{id}/complete` when no payment is required.
    - `IN_PROGRESS → CANCELLED` via `POST /api/checkin/{id}/cancel`.

- `WAITING_FOR_PAYMENT`
  - When: Baggage Service has calculated an excess fee (`excessBaggageFee > 0`) and the passenger has not yet paid.
  - Characteristics:
    - Seat remains `HELD` (subject to the global 120s hold + expiry safeguards in Seat Management).
    - `excessBaggageFee` is populated on the `checkins` record.
  - Allowed transitions:
    - `WAITING_FOR_PAYMENT → COMPLETED` via `POST /api/checkin/{id}/complete` **only if** a non-empty `paymentId` is provided.
    - `WAITING_FOR_PAYMENT → CANCELLED` via `POST /api/checkin/{id}/cancel` (e.g., passenger abandons).
  - Disallowed:
    - Further baggage updates that would bypass payment.
    - Direct completion without `paymentId` (CheckInService throws a `CheckInException`).

- `COMPLETED`
  - When: Check-in has been finalized:
    - For non-excess baggage: immediately after successful baggage validation.
    - For excess baggage: after a valid `paymentId` is supplied and seat confirmation succeeds.
  - Characteristics:
    - Seat is moved to `CONFIRMED` in Seat Management.
    - `completedAt` is set; `paymentId` is stored when there was an excess fee.
  - Transitions:
    - Terminal state for the check-in workflow: further calls to `complete`, `cancel`, or `updateBaggage` result in `CheckInException`.

- `CANCELLED`
  - When: Passenger (or system) cancels a non-finalized check-in (`IN_PROGRESS` or `WAITING_FOR_PAYMENT`).
  - Characteristics:
    - Check-in status is updated to `CANCELLED`.
    - Check-in history records the cancellation action.
    - Seat Management `cancelSeat` is invoked so that:
      - The seat becomes `AVAILABLE` again, and
      - The Waitlist processor can automatically reassign it to the next waitlisted passenger.
  - Transitions:
    - Terminal state; no further state changes are allowed.

---

### 2.6 Event-Driven Notification Flow

```
  Seat Service    Check-in    Payment    Baggage          RabbitMQ              Notification
       │             │           │          │                  │                     Service
       │ SeatHeld    │           │          │                  │                        │
       │─────────────┼───────────┼──────────┼─────────────────>│                        │
       │             │           │          │   seat.held      │   notification.#       │
       │             │           │          │                  │───────────────────────>│
       │             │           │          │                  │   Send email template  │
       │             │           │          │                  │                        │
       │ SeatConfirmed           │          │                  │                        │
       │─────────────┼───────────┼──────────┼─────────────────>│                        │
       │             │           │          │   seat.confirmed │                        │
       │             │           │          │                  │───────────────────────>│
       │             │           │          │                  │                        │
       │             │ CheckinComplete      │                  │                        │
       │             │─────────────────────┼──────────────────>│                        │
       │             │           │          │   checkin.completed                        │
       │             │           │          │                  │───────────────────────>│
       │             │           │ PaymentSuccess/Failure      │                        │
       │             │           │────────────────────────────>│                        │
       │             │           │          │   payment.success/failure                 │
       │             │           │          │                  │───────────────────────>│
```

---

## 3. Database Schema

### 3.1 Entity-Relationship Overview

```
┌─────────────────────────────────────────────────────────────────────────────────────┐
│                        SEAT MANAGEMENT SERVICE (seat-management-db)                   │
├─────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                      │
│  ┌─────────────┐       ┌─────────────┐       ┌──────────────────┐                   │
│  │   flights   │       │    seats    │       │ seat_assignments  │                   │
│  ├─────────────┤       ├─────────────┤       ├──────────────────┤                   │
│  │ id (PK)     │<──────│ flight_id   │<──────│ seat_id (FK)      │                   │
│  │ flight_num  │       │ seat_number │       │ passenger_id     │                   │
│  │ departure   │       │ seat_class  │       │ booking_reference│                   │
│  │ arrival     │       │ status      │       │ status           │                   │
│  │ origin      │       │ version     │       │ held_at          │                   │
│  │ destination │       └──────┬──────┘       │ expires_at       │                   │
│  └─────────────┘              │              │ confirmed_at     │                   │
│         ▲                     │              └──────────────────┘                   │
│         │                     │                         │                            │
│         │                     │              ┌──────────┴──────────┐                 │
│  ┌──────┴──────┐              │              │                    │                 │
│  │  bookings   │              │       ┌──────┴──────┐     ┌───────┴───────┐         │
│  ├─────────────┤              │       │  waitlist   │     │ seat_history  │         │
│  │ flight_id   │              │       ├─────────────┤     ├───────────────┤         │
│  │ booking_ref │              └──────>│ seat_id     │     │ seat_id       │         │
│  │ passenger_id│                      │ passenger_id│     │ passenger_id  │         │
│  └─────────────┘                      │ status     │     │ action        │         │
│                                       │ joined_at  │     │ timestamp     │         │
│                                       └────────────┘     │ details       │         │
│                                                          └───────────────┘         │
└─────────────────────────────────────────────────────────────────────────────────────┘
```

### 3.2 Table Descriptions

#### Seat Management Service

| Table | Purpose | Key Columns |
|-------|---------|-------------|
| **flights** | Flight master data | `id`, `flight_number`, `departure_time`, `arrival_time`, `origin`, `destination` |
| **seats** | Seats per flight with status | `id`, `flight_id`, `seat_number`, `seat_class`, `status`, `version` (optimistic lock) |
| **seat_assignments** | Active holds/confirmations | `seat_id`, `passenger_id`, `status`, `held_at`, `expires_at`, `confirmed_at` |
| **waitlist** | FIFO queue per seat | `seat_id`, `passenger_id`, `status`, `joined_at`, `assigned_at` |
| **seat_history** | Audit trail of seat state changes | `seat_id`, `passenger_id`, `action`, `timestamp`, `details` |
| **bookings** | Booking verification | `booking_reference`, `passenger_id`, `flight_id`, `passenger_name`, `status` |

#### Check-in Service

| Table | Purpose | Key Columns |
|-------|---------|-------------|
| **checkins** | Check-in session state | `id`, `bookingReference`, `passengerId`, `flightId`, `seatId`, `status`, `baggageWeight`, `paymentId`, `version` |
| **checkin_history** | Audit trail of check-in state transitions | `checkinId`, `fromStatus`, `toStatus`, `action`, `details`, `timestamp` |

#### Baggage Service

| Table | Purpose | Key Columns |
|-------|---------|-------------|
| **baggage_records** | Validated baggage | `baggage_reference`, `passenger_id`, `booking_reference`, `weight`, `excess_fee`, `status` |

#### Payment Service

| Table | Purpose | Key Columns |
|-------|---------|-------------|
| **payments** | Payment transactions | `payment_reference`, `passenger_id`, `amount`, `status`, `transaction_id` |

#### Notification Service

| Table | Purpose | Key Columns |
|-------|---------|-------------|
| **notifications** | Sent notifications | `type`, `recipient`, `subject`, `content`, `status`, `event_type` |

### 3.3 Relationships and Constraints

| Relationship | Type | Constraint |
|--------------|------|------------|
| `seats.flight_id` → `flights.id` | Many-to-One | `fk_seat_flight` |
| `seat_assignments.seat_id` → `seats.id` | Many-to-One | `fk_assignment_seat` |
| `seat_history.seat_id` → `seats.id` | Many-to-One | `fk_history_seat` |
| `waitlist.seat_id` → `seats.id` | Many-to-One | `fk_waitlist_seat` |
| `bookings.flight_id` → `flights.id` | Many-to-One | `fk_bookings_flight` |
| `seats(flight_id, seat_number)` | Unique | `uk_flight_seat_number` |
| `bookings(booking_reference, passenger_id)` | Unique | `uk_booking_passenger` |

### 3.4 Key Indexes

| Table | Index | Purpose |
|-------|-------|---------|
| seats | `idx_flight_id`, `idx_status`, `idx_flight_status` | Seat map queries, availability lookups |
| seat_assignments | `idx_seat_id`, `idx_passenger_id`, `idx_expires_at_status` | Expiry job, passenger lookups |
| seat_history | `idx_seat_id`, `idx_timestamp` | Audit queries |
| waitlist | `idx_seat_status_joined` | FIFO waitlist selection |
| baggage_records | `idx_baggage_passenger`, `idx_baggage_booking` | Lookups by passenger/booking |

---

## 4. State History Design

### 4.1 Purpose

State history tables provide:

- **Audit trail**: Who did what, when, and from/to states
- **Compliance**: Traceability for cancellations and refunds
- **Debugging**: Reconstruct flow for failed or disputed check-ins

### 4.2 Seat History (`seat_history`)

**Stored for every seat state change:**

| Column | Description |
|--------|-------------|
| `seat_id` | Affected seat |
| `passenger_id` | Passenger (if applicable) |
| `action` | e.g. `HELD`, `CONFIRMED`, `RELEASED`, `EXPIRED`, `CANCELLED` |
| `timestamp` | When the change occurred |
| `details` | Optional JSON or text (e.g. hold expiry time) |

**Example rows:**
```
seat_id | passenger_id | action    | timestamp            | details
--------|--------------|-----------|----------------------|---------------------------
101     | P123         | HELD      | 2026-02-21 10:00:00  | expires_at=2026-02-21 10:02:00
101     | P123         | CONFIRMED | 2026-02-21 10:01:30  | checkin_id=42
```

**When written**: In `SeatService` and `SeatExpiryService` whenever seat status changes.

### 4.3 Check-in History (`checkin_history`)

**Stored for every check-in state transition:**

| Column | Description |
|--------|-------------|
| `checkin_id` | Check-in session |
| `fromStatus` | Previous status |
| `toStatus` | New status |
| `action` | Human-readable action (e.g. "Baggage validated", "Payment completed") |
| `details` | Additional context |
| `timestamp` | When the transition occurred |

**Check-in status enum:** `IN_PROGRESS`, `WAITING_FOR_PAYMENT`, `COMPLETED`, `CANCELLED`

**Example rows:**
```
checkin_id | fromStatus         | toStatus            | action              | timestamp
-----------|--------------------|---------------------|---------------------|------------------
42         | null               | IN_PROGRESS         | Check-in started    | 2026-02-21 10:00:00
42         | IN_PROGRESS        | WAITING_FOR_PAYMENT | Excess baggage fee  | 2026-02-21 10:00:30
42         | WAITING_FOR_PAYMENT| IN_PROGRESS         | Payment completed   | 2026-02-21 10:01:00
42         | IN_PROGRESS        | COMPLETED           | Check-in completed  | 2026-02-21 10:01:30
```

**When written**: In `CheckInService` via `CheckInHistoryRepository` on each status change.

### 4.4 State History Flow (Conceptual)

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         State History Flow                                   │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│   Seat State Change                    Check-in State Change                │
│   (SeatService, SeatExpiryService)     (CheckInService)                      │
│            │                                      │                         │
│            ▼                                      ▼                         │
│   ┌─────────────────┐                   ┌─────────────────────┐             │
│   │ seat_history    │                   │ checkin_history     │             │
│   │ - seat_id       │                   │ - checkin_id        │             │
│   │ - action        │                   │ - fromStatus        │             │
│   │ - passenger_id  │                   │ - toStatus          │             │
│   │ - timestamp     │                   │ - action            │             │
│   │ - details       │                   │ - timestamp         │             │
│   └─────────────────┘                   └─────────────────────┘             │
│            │                                      │                         │
│            └──────────────────┬───────────────────┘                         │
│                               ▼                                             │
│                    Immutable append-only records                             │
│                    (no updates or deletes)                                   │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 5. Implementation Notes

### 5.1 Concurrency and Conflict Resolution

- **Distributed locking**: Redis/Redisson lock on `seat:{id}` for hold/confirm/cancel
- **Optimistic locking**: `@Version` on `Seat` and `CheckIn` entities
- **Unique constraint**: `uk_seat_active` on `seat_assignments` limits one active assignment per seat

### 5.2 Seat Hold Expiry

- **Timer**: `SeatExpiryScheduler` runs every 10 seconds
- **Query**: `SELECT * FROM seat_assignments WHERE status = 'HELD' AND expires_at < NOW()`
- **Locking**: For each expired assignment, `SeatExpiryService` acquires the same Redis/Redisson lock (`seat:lock:{id}`) used for hold/confirm/cancel, then re-reads the latest assignment and seat state **inside the lock**.
- **Action**:
  - If the assignment is still `HELD` and `expires_at < now`:
    - Release the seat (set `Seat.status = AVAILABLE`)
    - Mark the assignment `CANCELLED`
    - Write an `EXPIRED` entry to `seat_history` with a 120s-expiry detail
    - Publish `SeatReleasedEvent`
  - If the seat is already `CONFIRMED` or the assignment is no longer `HELD`, the expiry job **skips** that record (no-op), ensuring it never overwrites a confirmed seat.
- **Waitlist**: `WaitlistProcessorScheduler` runs every 5 seconds, assigns seat to first waiting passenger

### 5.3 Baggage and Payment Rules

- **Free weight**: 25 kg
- **Excess fee**: $10 per kg over 25 kg
- **Status flow**:
  - If `weight ≤ 25 kg`, check-in either stays `IN_PROGRESS` (when starting without immediate completion) or moves directly to `COMPLETED` without any payment.
  - If `weight > 25 kg`, check-in moves to `WAITING_FOR_PAYMENT` with `excessBaggageFee` stored on the record; completion from this state **requires a non-empty `paymentId`** to be provided.
  - Payment success/failure (amount, card details, etc.) is handled inside Payment Service; Check-in Service records the `paymentId` and uses it as a reference for audit/history rather than recomputing or re-validating the fee amount.

### 5.4 Event-Driven Notifications

- **Exchange**: Topic exchange (e.g. `skyhigh.events`)
- **Routing keys**: `seat.held`, `seat.confirmed`, `seat.released`, `seat.waitlist.assigned`, `checkin.completed`, `payment.success`, `payment.failure`
- **Consumer**: Notification Service binds to `notification.#` and sends mock emails

### 5.5 Rate Limiting

- **Bucket4j**: 50 requests per 2 seconds per IP
- **Storage**: In-memory (ConcurrentHashMap) per instance
- **Block duration**: Configurable (e.g. 5 minutes on abuse)

---

**Document Version**: 1.0  
**Last Updated**: February 21, 2026
