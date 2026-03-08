# Architecture Document
# SkyHigh Core – Digital Check-In System

## Table of Contents
1. [System Overview](#1-system-overview)
2. [Architecture Principles](#2-architecture-principles)
3. [Technology Stack](#3-technology-stack)
4. [Microservices Architecture](#4-microservices-architecture)
5. [Component Diagrams](#5-component-diagrams)
6. [Database Architecture](#6-database-architecture)
7. [Communication Patterns](#7-communication-patterns)
8. [Caching Strategy](#8-caching-strategy)
9. [Concurrency & Distributed Locking](#9-concurrency--distributed-locking)
10. [Seat Hold Timer Mechanism](#10-seat-hold-timer-mechanism)
11. [Rate Limiting & Abuse Detection](#11-rate-limiting--abuse-detection)
12. [Security Architecture](#12-security-architecture)
13. [Scalability & Performance](#13-scalability--performance)
14. [Monitoring & Observability](#14-monitoring--observability)
15. [Deployment Architecture](#15-deployment-architecture)

---

## 1. System Overview

SkyHigh Core is a high-performance, distributed backend system built using **microservices architecture** to handle concurrent digital check-in operations for airline passengers. The system is designed to prevent race conditions, manage seat state lifecycle, and maintain data consistency under heavy load.

### 1.1 Key Characteristics

- **Architecture Pattern**: Microservices
- **Communication**: REST APIs (synchronous) + RabbitMQ (asynchronous)
- **Backend Framework**: Java 21 with Spring Boot 3.5
- **Frontend Framework**: React 18+ with TypeScript
- **Database**: H2 Database (filesystem for local; in-memory for Docker) - separate DB per service
- **Caching**: Redis
- **Message Queue**: RabbitMQ
- **Containerization**: Docker with Docker Compose

---

## 2. Architecture Principles

### 2.1 Design Principles

1. **Single Responsibility**: Each microservice owns a specific business capability
2. **Database Per Service**: Each service has its own H2 database instance
3. **Asynchronous Communication**: Event-driven architecture for decoupled services
4. **Idempotency**: All critical operations are idempotent
5. **Resilience**: Circuit breakers and fallback mechanisms
6. **Observability**: Centralized logging and distributed tracing

### 2.2 Design Decisions

| Decision | Choice | Rationale |
|----------|--------|-----------|
| **No API Gateway** | Direct frontend-to-service communication | Reduces complexity for this scope, easier local testing |
| **Rate Limiting** | Bucket4j (in-memory per instance) | Simple integration, 50 requests per 2 seconds per IP |
| **Seat + Waitlist** | Merged into Seat Management Service | Strong cohesion - waitlist is tightly coupled with seat availability |
| **Database** | H2 (filesystem locally; in-memory in Docker) | Easy setup, suitable for assignment scope, supports ACID transactions |
| **Message Queue** | RabbitMQ | Simpler setup than Kafka, excellent for task queues and notifications |
| **Timer Mechanism** | Spring @Scheduled | Built-in Spring Boot support, no external dependencies |

---

## 3. Technology Stack

### 3.1 Backend Technologies

| Component | Technology | Version | Purpose |
|-----------|-----------|---------|---------|
| **Language** | Java | 21 LTS | Primary backend language |
| **Framework** | Spring Boot | 3.5.x | Microservices framework |
| **Persistence** | Spring Data JPA | 3.5.x | Database access layer |
| **Database** | H2 Database | Latest | In-memory DB with persistence |
| **Caching** | Redis | 7.x | Distributed cache for seat maps |
| **Message Queue** | RabbitMQ | 3.13.x | Asynchronous messaging |
| **Scheduler** | Spring @Scheduled | Built-in | Seat hold expiry and waitlist processor |
| **Rate Limiting** | Bucket4j + Resilience4j | Latest | Rate limiting & circuit breaker |
| **Validation** | Hibernate Validator | 8.x | Input validation |
| **Testing** | JUnit 5 + Mockito | Latest | Unit and integration testing |
| **API Documentation** | SpringDoc OpenAPI | 2.x | API documentation |
| **Logging** | SLF4J + Logback | Latest | Structured logging |

### 3.2 Frontend Technologies

| Component | Technology | Version | Purpose |
|-----------|-----------|---------|---------|
| **Framework** | React | 18.x | UI framework |
| **Language** | TypeScript | 5.x | Type-safe JavaScript |
| **State Management** | Redux Toolkit | 2.x | Centralized state management |
| **API Client** | Axios | 1.x | HTTP client |
| **UI Library** | Material-UI (MUI) | 5.x | Component library |
| **Build Tool** | Vite | 5.x | Fast build tool |
| **Testing** | Jest + React Testing Library | Latest | Frontend testing |

### 3.3 Infrastructure

- **Containerization**: Docker
- **Orchestration**: Docker Compose
- **Service Discovery**: N/A (direct service-to-service URLs in Docker network)

---

## 4. Microservices Architecture

### 4.1 Service Decomposition

```
┌─────────────────────────────────────────────────────────────────────┐
│                         Frontend Application                        │
│                    (React + TypeScript + Redux)                     │
└────────┬────────┬────────┬────────┬────────┬─────────────────┬─────┘
         │        │        │        │        │                 │
         ▼        ▼        ▼        ▼        ▼                 ▼
    ┌────────┐ ┌────────┐ ┌────────┐ ┌────────┐ ┌─────────┐ ┌──────────┐
    │  Seat  │ │ Check  │ │Baggage │ │Payment │ │  Notif  │ │  Redis   │
    │  Mgmt  │ │   In   │ │Service │ │Service │ │ Service │ │  Cache   │
    │Service │ │Service │ │        │ │ (Mock) │ │         │ │          │
    └───┬────┘ └───┬────┘ └───┬────┘ └────────┘ └────┬────┘ └──────────┘
        │          │          │                       │
        ▼          ▼          ▼                       ▼
    ┌────────┐ ┌────────┐ ┌────────┐           ┌──────────┐
    │   H2   │ │   H2   │ │   H2   │           │ RabbitMQ │
    │   DB   │ │   DB   │ │   DB   │           │  Queues  │
    │(Seat)  │ │(Check) │ │(Bag)   │           │          │
    └────────┘ └────────┘ └────────┘           └──────────┘
```

### 4.2 Service Details

#### 4.2.1 Seat Management Service
**Port**: 8091  
**Database**: H2 (seat-db)  
**Responsibilities**:
- Manage seat lifecycle (AVAILABLE → HELD → CONFIRMED → CANCELLED)
- Handle seat hold reservations with 120-second TTL
- Manage waitlist queue (FIFO); join per seat, get by passenger, remove; automatic assignment when a seat is released or expired
- **Fair allocation:** Currently FIFO (first-in-first-out). Future: priority/tier-based waitlist not implemented
- Automatically assign seats to waitlisted passengers via WaitlistProcessorScheduler (every 5 seconds)
- Provide seat map data with caching
- Implement distributed locking for concurrent seat access
- Publish events: `SeatHeldEvent`, `SeatConfirmedEvent`, `SeatReleasedEvent`, `WaitlistAssignedEvent`

**Key APIs**:
- `GET /api/seats/flights/{flightId}/seatmap` - Get seat map
- `POST /api/seats/{seatId}/hold` - Hold a seat
- `POST /api/seats/{seatId}/confirm` - Confirm seat assignment
- `DELETE /api/seats/{seatId}/cancel?passengerId=...` - Cancel confirmed seat
- `POST /api/seats/{seatId}/waitlist` - Join waitlist
- `GET /api/seats/{seatId}/status` - Get seat status
- `POST /api/bookings/verify` - Verify booking for check-in

#### 4.2.2 Check-in Service
**Port**: 8082  
**Database**: H2 (checkin-db)  
**Responsibilities**:
- Orchestrate check-in workflow
- Track check-in states: IN_PROGRESS, WAITING_FOR_PAYMENT, COMPLETED
- Coordinate with Seat, Baggage, and Payment services
- Handle check-in cancellations
- Maintain check-in history
- Subscribe to: `SeatConfirmedEvent`, `PaymentCompletedEvent`
- **Payment pause/resume:** When excess baggage is required, check-in pauses at `WAITING_FOR_PAYMENT`; the client pays via Payment Service and resumes by calling complete with the payment reference (see WORKFLOW_DESIGN.md).

**Key APIs**:
- `POST /api/checkin/start` - Start check-in process
- `GET /api/checkin/{checkinId}` - Get check-in status
- `POST /api/checkin/{checkinId}/complete` - Complete check-in
- `POST /api/checkin/{checkinId}/baggage` - Update baggage weight
- `POST /api/checkin/{checkinId}/cancel` - Cancel check-in

#### 4.2.3 Baggage Service
**Port**: 8083  
**Database**: H2 (baggage-db)  
**Responsibilities**:
- Validate baggage weight (max 25kg free)
- Calculate excess baggage fees
- Simulate Weight Service integration
- Maintain baggage records
- Publish events: `BaggageValidatedEvent`, `ExcessBaggageFeeEvent`

**Key APIs**:
- `POST /api/baggage/validate` - Validate baggage weight, calculate fees, and record (combined)
- `GET /api/baggage/{baggageReference}` - Get baggage by reference
- `GET /api/baggage/passenger/{passengerId}` - Get baggage by passenger
- `GET /api/baggage/booking/{bookingReference}` - Get baggage by booking

#### 4.2.4 Payment Service (Mock)
**Port**: 8084  
**Database**: H2 (payment-db)  
**Responsibilities**:
- Simulate payment processing (70% success rate simulation)
- Handle excess baggage fee payments
- Generate payment receipts
- Publish events: `PaymentCompletedEvent`, `PaymentFailedEvent`

**Key APIs**:
- `POST /api/payments/process` - Process payment
- `GET /api/payments/{paymentReference}` - Get payment status
- `GET /api/payments/passenger/{passengerId}` - Get payments by passenger
- `GET /api/payments/booking/{bookingReference}` - Get payments by booking

#### 4.2.5 Notification Service
**Port**: 8085  
**Database**: H2 (notification-db)  
**Responsibilities**:
- Send notifications to passengers (email/SMS simulation)
- Handle event-driven notifications
- Log all notification attempts
- Subscribe to: All events from other services

**Key Notifications**:
- Seat hold confirmation
- Seat assignment success
- Check-in completion
- Waitlist assignment
- Payment success/failure
- Seat hold expiry warning

**Event-driven notifications (implemented):** Events published: `seat.held`, `seat.confirmed`, `seat.released`, `waitlist.assigned`, `payment.success` / `payment.failure`, `checkin.completed`. Notification Service subscribes and sends email/template notifications.

---

## 5. Component Diagrams

### 5.1 High-Level Component Diagram

```
┌───────────────────────────────────────────────────────────────────────┐
│                                                                       │
│                         PRESENTATION LAYER                            │
│                                                                       │
│  ┌─────────────────────────────────────────────────────────────┐    │
│  │          React Frontend (TypeScript)                        │    │
│  │  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐   │    │
│  │  │  Seat    │  │ Check-in │  │ Baggage  │  │ Payment  │   │    │
│  │  │Selection │  │  Flow    │  │  Form    │  │  Form    │   │    │
│  │  └──────────┘  └──────────┘  └──────────┘  └──────────┘   │    │
│  │                                                             │    │
│  │                    Redux Store (State Management)           │    │
│  └─────────────────────────────────────────────────────────────┘    │
│                                                                       │
└───────────────────────────────────────────────────────────────────────┘
                                   │
                                   │ REST APIs (HTTPS)
                                   ▼
┌───────────────────────────────────────────────────────────────────────┐
│                                                                       │
│                         APPLICATION LAYER                             │
│                                                                       │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐            │
│  │   Seat   │  │ Check-in │  │ Baggage  │  │ Payment  │            │
│  │   Mgmt   │  │ Service  │  │ Service  │  │ Service  │            │
│  │ Service  │  │          │  │          │  │  (Mock)  │            │
│  │          │  │          │  │          │  │          │            │
│  │ :8091    │  │  :8082   │  │  :8083   │  │  :8084   │            │
│  └────┬─────┘  └────┬─────┘  └────┬─────┘  └────┬─────┘            │
│       │             │             │             │                    │
│       └─────────────┴─────────────┴─────────────┘                    │
│                        │                                              │
│                        ▼                                              │
│              ┌──────────────────┐                                    │
│              │  Notification    │                                    │
│              │    Service       │                                    │
│              │     :8085        │                                    │
│              └──────────────────┘                                    │
│                                                                       │
└───────────────────────────────────────────────────────────────────────┘
                                   │
                                   │
                                   ▼
┌───────────────────────────────────────────────────────────────────────┐
│                                                                       │
│                      INFRASTRUCTURE LAYER                             │
│                                                                       │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐            │
│  │    H2    │  │    H2    │  │    H2    │  │    H2    │            │
│  │ Database │  │ Database │  │ Database │  │ Database │            │
│  │ (Seat)   │  │(Check-in)│  │(Baggage) │  │(Payment) │            │
│  └──────────┘  └──────────┘  └──────────┘  └──────────┘            │
│                                                                       │
│  ┌──────────────────────┐           ┌──────────────────────┐        │
│  │   Redis Cache        │           │     RabbitMQ         │        │
│  │   (Seat Maps)        │           │  (Event Bus)         │        │
│  └──────────────────────┘           └──────────────────────┘        │
│                                                                       │
└───────────────────────────────────────────────────────────────────────┘
```

### 5.2 Seat Management Service Internal Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│              Seat Management Service (Port 8091)                │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  ┌──────────────────────────────────────────────────────┐      │
│  │             REST Controllers                         │      │
│  │  - SeatController, SeatMapController                 │      │
│  │  - WaitlistController, BookingController             │      │
│  └───────────────────┬──────────────────────────────────┘      │
│                      │                                          │
│  ┌───────────────────▼──────────────────────────────────┐      │
│  │    Rate Limiting Filter (Bucket4j)                   │      │
│  │    - 50 requests per 2 seconds per IP                │      │
│  └───────────────────┬──────────────────────────────────┘      │
│                      │                                          │
│  ┌───────────────────▼──────────────────────────────────┐      │
│  │             Service Layer                            │      │
│  │  - SeatService (seat hold/confirm/cancel)            │      │
│  │  - SeatMapService (caching + retrieval)              │      │
│  │  - WaitlistService (queue management)                │      │
│  │  - SeatExpiryService (release logic)                 │      │
│  └───────────────────┬──────────────────────────────────┘      │
│                      │                                          │
│  ┌───────────────────▼──────────────────────────────────┐      │
│  │          Repository Layer (Spring Data JPA)          │      │
│  │  - SeatRepository                                    │      │
│  │  - SeatAssignmentRepository                          │      │
│  │  - WaitlistRepository                                │      │
│  │  - SeatHistoryRepository (audit)                     │      │
│  └───────────────────┬──────────────────────────────────┘      │
│                      │                                          │
│  ┌───────────────────▼──────────────────────────────────┐      │
│  │               H2 Database (seat-db)                  │      │
│  │  Tables: seats, seat_assignments, waitlist,          │      │
│  │          seat_history                                │      │
│  └──────────────────────────────────────────────────────┘      │
│                                                                 │
│  ┌──────────────────────────────────────────────────────┐      │
│  │           Event Publisher (RabbitMQ)                 │      │
│  │  - SeatHeldEvent                                     │      │
│  │  - SeatConfirmedEvent                                │      │
│  │  - SeatReleasedEvent                                 │      │
│  │  - WaitlistAssignedEvent                             │      │
│  └──────────────────────────────────────────────────────┘      │
│                                                                 │
│  ┌──────────────────────────────────────────────────────┐      │
│  │          Spring @Scheduled Jobs                      │      │
│  │  - SeatExpiryScheduler (runs every 10 seconds)       │      │
│  │  - WaitlistProcessorScheduler (runs every 5 seconds) │      │
│  └──────────────────────────────────────────────────────┘      │
│                                                                 │
│  ┌──────────────────────────────────────────────────────┐      │
│  │          Redis Cache Integration                     │      │
│  │  - Cache seat maps (TTL: 30 seconds)                 │      │
│  │  - Distributed locks for seat assignment (Redisson)  │      │
│  └──────────────────────────────────────────────────────┘      │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

---

## 6. Database Architecture

### 6.1 Database Per Service Pattern

Each microservice maintains its own H2 database:
- **Local development**: Filesystem persistence (e.g. `./data/seat-management-db`)
- **Docker Compose**: In-memory (`jdbc:h2:mem:...`) for simplified deployment; data does not persist across container restarts

### 6.2 Seat Management Database Schema

```sql
-- Flights table
CREATE TABLE flights (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    flight_number VARCHAR(10) NOT NULL UNIQUE,
    departure_time TIMESTAMP NOT NULL,
    origin VARCHAR(3) NOT NULL,
    destination VARCHAR(3) NOT NULL,
    aircraft_type VARCHAR(50),
    status VARCHAR(20),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Seats table
CREATE TABLE seats (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    flight_id BIGINT NOT NULL,
    seat_number VARCHAR(5) NOT NULL,
    seat_class VARCHAR(20) NOT NULL, -- ECONOMY, BUSINESS, FIRST
    status VARCHAR(20) NOT NULL,     -- AVAILABLE, HELD, CONFIRMED, CANCELLED
    version BIGINT NOT NULL DEFAULT 0, -- Optimistic locking
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (flight_id) REFERENCES flights(id),
    UNIQUE KEY uk_flight_seat (flight_id, seat_number)
);

-- Seat Assignments table
CREATE TABLE seat_assignments (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    seat_id BIGINT NOT NULL,
    passenger_id VARCHAR(50) NOT NULL,
    booking_reference VARCHAR(10) NOT NULL,
    assignment_status VARCHAR(20) NOT NULL, -- HELD, CONFIRMED, CANCELLED
    hold_expiry_time TIMESTAMP,
    confirmed_at TIMESTAMP,
    cancelled_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (seat_id) REFERENCES seats(id),
    INDEX idx_expiry (hold_expiry_time, assignment_status)
);

-- Waitlist table
CREATE TABLE waitlist (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    seat_id BIGINT NOT NULL,
    passenger_id VARCHAR(50) NOT NULL,
    booking_reference VARCHAR(10) NOT NULL,
    status VARCHAR(20) NOT NULL, -- WAITING, ASSIGNED, EXPIRED, CANCELLED
    priority INT NOT NULL,       -- For FIFO ordering
    requested_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    assigned_at TIMESTAMP,
    notified_at TIMESTAMP,
    FOREIGN KEY (seat_id) REFERENCES seats(id),
    INDEX idx_waitlist_seat_status (seat_id, status, priority)
);

-- Seat History (Audit Trail)
CREATE TABLE seat_history (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    seat_id BIGINT NOT NULL,
    passenger_id VARCHAR(50),
    previous_status VARCHAR(20),
    new_status VARCHAR(20) NOT NULL,
    action VARCHAR(50) NOT NULL,
    changed_by VARCHAR(50),
    changed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    metadata TEXT, -- JSON for additional context
    FOREIGN KEY (seat_id) REFERENCES seats(id),
    INDEX idx_seat_history (seat_id, changed_at)
);
```

### 6.3 Check-in Service Database Schema

```sql
-- Check-ins table
CREATE TABLE checkins (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    booking_reference VARCHAR(10) NOT NULL,
    passenger_id VARCHAR(50) NOT NULL,
    flight_id BIGINT NOT NULL,
    seat_id BIGINT NOT NULL,
    status VARCHAR(30) NOT NULL, -- IN_PROGRESS, WAITING_FOR_PAYMENT, COMPLETED, CANCELLED
    baggage_weight DECIMAL(5,2) NOT NULL,
    excess_baggage_fee DECIMAL(10,2),
    payment_id VARCHAR(50),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    completed_at TIMESTAMP,
    version BIGINT NOT NULL,             -- Optimistic locking
    INDEX idx_passenger (passenger_id),
    INDEX idx_booking (booking_reference)
);
```

### 6.4 Baggage Service Database Schema

```sql
-- Baggage Records table
CREATE TABLE baggage_records (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    baggage_reference VARCHAR(50) NOT NULL UNIQUE,
    passenger_id VARCHAR(50) NOT NULL,
    booking_reference VARCHAR(50) NOT NULL,
    weight DECIMAL(10,2) NOT NULL,
    excess_weight DECIMAL(10,2),
    excess_fee DECIMAL(10,2),
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    INDEX idx_passenger (passenger_id),
    INDEX idx_booking (booking_reference),
    INDEX idx_status (status)
);
```

### 6.5 Payment Service Database Schema

```sql
-- Payments table
CREATE TABLE payments (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    payment_reference VARCHAR(20) UNIQUE NOT NULL,
    passenger_id VARCHAR(50) NOT NULL,
    booking_reference VARCHAR(10) NOT NULL,
    payment_type VARCHAR(20) NOT NULL,   -- EXCESS_BAGGAGE, OTHER
    amount DECIMAL(10,2) NOT NULL,
    status VARCHAR(20) NOT NULL,         -- PENDING, COMPLETED, FAILED
    transaction_id VARCHAR(50),
    initiated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP,
    INDEX idx_passenger (passenger_id)
);
```

### 6.6 Notification Service Database Schema

```sql
-- Notifications table
CREATE TABLE notifications (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    notification_type VARCHAR(50) NOT NULL,
    passenger_id VARCHAR(50) NOT NULL,
    recipient_email VARCHAR(100),
    recipient_phone VARCHAR(20),
    subject VARCHAR(200),
    message TEXT NOT NULL,
    status VARCHAR(20) NOT NULL,         -- PENDING, SENT, FAILED
    sent_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_passenger (passenger_id),
    INDEX idx_status (status, created_at)
);
```

---

## 7. Communication Patterns

### 7.1 Synchronous Communication (REST)

**Pattern**: Request-Response via HTTP/REST

**Use Cases**:
- Frontend ↔ Backend services
- Service-to-service for immediate responses (e.g., Check-in → Baggage validation)

**Example Flow**:
```
Frontend → [POST /api/seats/{seatId}/hold] → Seat Management Service
                                             ↓
                                    (sync response)
                                             ↓
Frontend ← [200 OK with hold details] ← Seat Management Service
```

### 7.2 Asynchronous Communication (RabbitMQ)

**Pattern**: Event-Driven Architecture using Publish-Subscribe

**Message Exchange Configuration**:
- **Exchange Type**: Topic Exchange
- **Seat Service Exchange**: `seat.exchange`
- **Routing Keys**: `seat.held`, `seat.confirmed`, `seat.released`, `waitlist.assigned`

**Event Flow Diagram**:

```
┌─────────────────┐        SeatConfirmedEvent         ┌─────────────────┐
│  Seat Service   │───────────────────────────────────▶│   RabbitMQ      │
│                 │        (routing: seat.confirmed)   │   Exchange      │
└─────────────────┘                                    └────────┬────────┘
                                                                │
                            ┌───────────────────────────────────┼──────────┐
                            ▼                                   ▼          ▼
                   ┌────────────────┐              ┌────────────────┐  ┌───────────┐
                   │  Check-in      │              │  Notification  │  │  Others   │
                   │  Service       │              │  Service       │  │           │
                   │  (Subscriber)  │              │  (Subscriber)  │  │           │
                   └────────────────┘              └────────────────┘  └───────────┘
```

### 7.3 Event Catalog

| Event Name | Publisher | Subscribers | Routing Key | Payload |
|------------|-----------|-------------|-------------|---------|
| `SeatHeldEvent` | Seat Mgmt | Notification | `seat.held` | seatId, passengerId, expiryTime |
| `SeatConfirmedEvent` | Seat Mgmt | Check-in, Notification | `seat.confirmed` | seatId, passengerId, assignmentId |
| `SeatReleasedEvent` | Seat Mgmt | Check-in, Notification | `seat.released` | seatId, reason |
| `WaitlistAssignedEvent` | Seat Mgmt | Notification | `waitlist.assigned` | passengerId, seatId |
| `BaggageValidatedEvent` | Baggage | Check-in | `baggage.validated` | passengerId, weight, isExcess, feeAmount |
| `PaymentCompletedEvent` | Payment | Check-in, Notification | `payment.completed` | paymentId, passengerId, amount |
| `PaymentFailedEvent` | Payment | Check-in, Notification | `payment.failed` | paymentId, passengerId, reason |
| `CheckinCompletedEvent` | Check-in | Notification | `checkin.completed` | checkinId, passengerId, seatNumber |

---

## 8. Caching Strategy

### 8.1 Redis Cache Architecture

**Cache Type**: Write-Through + Cache-Aside Pattern

**Cached Data**:
1. **Seat Maps**: Complete seat layout with availability (TTL: 30 seconds)
2. **Distributed Locks**: For seat assignment operations (Redisson)

### 8.2 Cache Configuration

```yaml
Cache Key Pattern: "seatmap:{flightId}"
TTL: 30 seconds
Eviction Policy: LRU (Least Recently Used)
Max Memory: 256MB
Persistence: RDB Snapshots every 60 seconds
```

### 8.3 Cache Invalidation Strategy

```
Event: SeatConfirmedEvent
  ↓
Action: Evict cache key "seatmap:{flightId}"
  ↓
Next Request: Cache Miss → Fetch from DB → Update Cache
```

**Invalidation Triggers**:
- Seat status change (HELD, CONFIRMED, CANCELLED)
- Seat released after hold expiry
- Waitlist assignment

---

## 9. Concurrency & Distributed Locking

### 9.1 Challenge

**Problem**: Multiple passengers attempting to hold the same seat simultaneously

**Solution**: Optimistic Locking + Distributed Lock

### 9.2 Optimistic Locking (Database Level)

```java
@Entity
@Table(name = "seats")
public class Seat {
    @Id
    private Long id;
    
    @Version  // JPA Optimistic Locking
    private Long version;
    
    @Enumerated(EnumType.STRING)
    private SeatStatus status;
    
    // ... other fields
}
```

**Flow**:
1. Read seat with version number
2. Attempt to update seat status
3. If version mismatch → `OptimisticLockException` → Retry or fail gracefully

### 9.3 Distributed Lock (Redis-based)

```java
// Using Redisson for distributed locks
RLock lock = redisson.getLock("seat:hold:" + seatId);

try {
    // Wait up to 5 seconds, lock auto-expires after 10 seconds
    if (lock.tryLock(5, 10, TimeUnit.SECONDS)) {
        try {
            // Critical section: hold seat logic
            seatRepository.updateStatus(seatId, SeatStatus.HELD);
        } finally {
            lock.unlock();
        }
    } else {
        throw new SeatUnavailableException("Seat is being held by another user");
    }
} catch (InterruptedException e) {
    Thread.currentThread().interrupt();
    throw new ServiceException("Lock acquisition interrupted");
}
```

### 9.4 Race Condition Prevention

```
Passenger A                    Passenger B
     │                              │
     ├─ GET /seats/12A/hold         │
     │  (acquire lock)              │
     │       │                      │
     │       ▼                      ├─ GET /seats/12A/hold
     │  Check: AVAILABLE            │  (lock wait...)
     │  Update: HELD                │       │
     │  Release lock ───────────────┼───────▼
     │                              │  Lock acquired
     │  Response: Success           │  Check: HELD (fails)
     │                              │  Response: Seat already held
```

---

## 10. Seat Hold Timer Mechanism

### 10.1 Spring @Scheduled Configuration

**Job**: `SeatExpiryScheduler`  
**Trigger**: `@Scheduled(fixedDelay = 10000)` – runs every 10 seconds  

### 10.2 Expiry Logic

```java
@Component
public class SeatExpiryScheduler {
    
    @Scheduled(fixedDelay = 10000)  // 10 seconds
    public void processExpiredHolds() {
        // Find all seats with status=HELD and hold_expiry_time < NOW
        List<SeatAssignment> expiredHolds = seatAssignmentRepository
            .findExpiredHolds(LocalDateTime.now());
        
        for (SeatAssignment assignment : expiredHolds) {
            try {
                // Release seat back to AVAILABLE
                seatExpiryService.releaseExpiredSeat(assignment);
                
                // Publish event for waitlist processing
                eventPublisher.publish(new SeatReleasedEvent(
                    assignment.getSeatId(), 
                    "HOLD_EXPIRED"
                ));
                
            } catch (Exception e) {
                log.error("Failed to process expired seat: {}", assignment.getId(), e);
            }
        }
    }
}
```

### 10.3 Seat Hold Expiry – Fail-Safe

Expiry is enforced in two ways so that seat release is guaranteed even if the scheduler is delayed:

1. **Scheduler (batch release):** `SeatExpiryScheduler` runs every 10 seconds and releases all held seats whose `hold_expiry_time` has passed (batch size limit 100). Released seats are published for waitlist assignment.
2. **Confirm-time check (authoritative):** Before confirming a seat, `SeatService.confirmSeat` checks whether the hold has expired (`assignment.getExpiresAt()` vs current time). If expired, it rejects with 409 Conflict. Thus even if a scheduler run is delayed, a confirm after the TTL always fails and the seat cannot be confirmed on an expired hold.

Optional: batch size and run interval (e.g. 10s) keep the backlog of expired holds bounded.

### 10.4 Timer Accuracy

- **Query Frequency**: Every 10 seconds
- **Expected Expiry**: 120 seconds ± 10 seconds
- **Acceptable Variance**: ±10 seconds

**Database Query**:
```sql
SELECT * FROM seat_assignments 
WHERE assignment_status = 'HELD' 
  AND hold_expiry_time <= CURRENT_TIMESTAMP 
ORDER BY hold_expiry_time ASC
LIMIT 100;
```

---

## 11. Rate Limiting & Abuse Detection

### 11.1 Implementation: Bucket4j

**Library**: Bucket4j (Token Bucket Algorithm)  
**Storage**: In-memory (ConcurrentHashMap per service instance)

### 11.2 Rate Limit Configuration

- **Scope**: All seat APIs (`/api/seats/*`)
- **Limit**: 50 requests per 2 seconds per IP
- **Response**: HTTP 429 (Too Many Requests) when exceeded

### 11.3 Filter Implementation

```java
@Component
public class RateLimitFilter implements Filter {
    
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();
    
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) {
        String clientIp = getClientIP((HttpServletRequest) request);
        Bucket bucket = buckets.computeIfAbsent(clientIp, k -> createNewBucket());
        
        if (bucket.tryConsume(1)) {
            chain.doFilter(request, response);
        } else {
            log.warn("Rate limit exceeded for IP: {}", clientIp);
            ((HttpServletResponse) response).setStatus(429);
            // Return JSON error response
        }
    }
    
    private Bucket createNewBucket() {
        Bandwidth limit = Bandwidth.classic(50, Refill.intervally(50, Duration.ofSeconds(2)));
        return Bucket.builder().addLimit(limit).build();
    }
}
```

*Note: Abuse detection with blocking (e.g. 5-minute block on threshold exceed) is not implemented in the current scope.*

**Audit of rate-limit events (abuse detection):** When the rate limit is exceeded, a WARN is logged and an audit record is written to the `rate_limit_audit` table (client IP, path, timestamp). This supports abuse analysis and monitoring; admins can query repeated violations per IP. The Seat Management Service implements this via `RateLimitFilter` and `RateLimitAuditService`.

### 11.4 Waitlist Management (Advanced Feature)

Waitlist allows passengers to queue for a specific seat when it is not available:

- **Join:** `POST /api/seats/{seatId}/waitlist` – adds passenger to the waitlist for that seat (FIFO ordering).
- **Status:** `GET /api/seats/waitlist/{passengerId}` – returns all waitlist entries for the passenger (status WAITING, ASSIGNED, etc.).
- **Remove:** `DELETE /api/seats/waitlist/{waitlistId}` – removes the entry; returns 404 if the waitlist entry does not exist.

When a seat is released (cancel or hold expiry), the `WaitlistProcessorScheduler` runs periodically and assigns the seat to the next waitlisted passenger (FIFO). A `WaitlistAssignedEvent` is published for notifications. This is documented in API-SPECIFICATION.md §1.4 and in the Seat Management Service description in §4.2.1.

---

## 12. Security Architecture

### 12.1 API Request Validations and Error Handling

All APIs enforce request validations and return consistent error responses:

- **Validation:** Request bodies use `@Valid` with Bean Validation (e.g. `@NotBlank`, `@NotNull`, `@Positive`). Required query/path parameters are validated (e.g. `passengerId` on seat cancel). Invalid or missing data returns **400 Bad Request** with a body containing `timestamp`, `status`, `error`, `message`, and optionally `errors` (field-level validation details).
- **HTTP status codes:** 400 (validation/invalid state), 404 (resource not found), 409 (conflict, e.g. seat already held or hold expired), 429 (rate limit exceeded), 503 (downstream unavailable). See API-SPECIFICATION.md "Error Responses".
- **Edge cases:** Missing request parameters, type mismatch (e.g. non-numeric seatId), and business rule violations are mapped to the appropriate status and a consistent JSON error shape across Seat, Check-in, and shared handlers.

### 12.2 Threat Model

- **Current scope:** Development/demo. No authentication; endpoints are open. Rate limiting on Seat Management reduces abuse and simple bot traffic.
- **Threats not mitigated:** Unauthorized access, impersonation, no RBAC; sensitive operations are not gated by identity.
- **Recommended for production:** JWT-based authentication, RBAC (PASSENGER, STAFF, ADMIN), HTTPS, audit logging for sensitive actions, and threat hardening (e.g. input sanitization, secure headers).

### 12.3 Authentication & Authorization

**Current Implementation**: Simplified for development/testing. All endpoints use `permitAll()` with CSRF disabled. No JWT or authentication filter.

*JWT and RBAC (as described below) are recommended for production but not implemented in the current scope.*

### 12.4 Planned Role-Based Access Control (RBAC)

**Roles** (for future implementation):
- `PASSENGER`: Can book seats, check-in
- `ADMIN`: Can view all data, override operations
- `STAFF`: Can assist passengers, view check-in status

### 12.5 Data Protection

- **In Transit**: TLS 1.3 (HTTPS)
- **At Rest**: H2 database encryption (optional)
- **Sensitive Data**: PII masked in logs
- **Password Storage**: BCrypt hashing (not in scope for this assignment)

---

## 13. Scalability & Performance

### 13.1 Horizontal Scaling

**Stateless Services**: All microservices are stateless, enabling horizontal scaling

```yaml
# Docker Compose scaling example
docker-compose up --scale seat-management=3 --scale checkin-service=2
```

**Load Balancing**: 
- For production: Use NGINX or cloud load balancer
- For this assignment: Docker internal DNS round-robin

### 13.2 Performance Optimizations

| Layer | Optimization | Impact |
|-------|--------------|--------|
| **Database** | Indexes on seat_id, flight_id, expiry_time | 80% query speedup |
| **Caching** | Redis for seat maps (30s TTL) | 95% cache hit rate target |
| **Connection Pooling** | HikariCP (default in Spring Boot) | Handle 500+ concurrent connections |
| **Async Processing** | RabbitMQ for notifications | Decouple slow operations |
| **Query Optimization** | JPA fetch strategies, N+1 prevention | Reduce DB roundtrips |

### 13.3 Performance Targets (from NFRs)

| Metric | Target | Implementation |
|--------|--------|----------------|
| Seat Map P95 | < 1 second | Redis caching + database indexes |
| Seat Hold P95 | < 500ms | Optimistic locking + Redis distributed lock |
| Check-in P95 | < 2 seconds | Async event processing |
| Concurrent Users | 500+ | Stateless services + connection pooling |
| Throughput | 100+ bookings/sec | Horizontal scaling capability |

---

## 14. Monitoring & Observability

### 14.1 Logging Strategy

**Framework**: SLF4J + Logback  
**Format**: JSON (structured logging)

**Log Levels**:
- `ERROR`: System failures, exceptions
- `WARN`: Business rule violations, rate limit hits
- `INFO`: Key business events (seat hold, check-in complete)
- `DEBUG`: Detailed flow for troubleshooting

**Example**:
```json
{
  "timestamp": "2026-02-08T10:30:45.123Z",
  "level": "INFO",
  "service": "seat-management",
  "traceId": "abc123",
  "userId": "P12345",
  "action": "SEAT_HOLD",
  "seatId": 42,
  "flightId": 101,
  "result": "SUCCESS"
}
```

### 14.2 Metrics (Micrometer + Prometheus)

**Key Metrics**:
- **Business Metrics**:
  - `seat.hold.count` (Counter)
  - `seat.hold.success.rate` (Gauge)
  - `seat.hold.conflict.count` (Counter)
  - `checkin.completion.rate` (Gauge)
  - `rate.limit.violations` (Counter)
  
- **Technical Metrics**:
  - `http.requests.duration` (Histogram)
  - `jvm.memory.used` (Gauge)
  - `database.connections.active` (Gauge)
  - `cache.hit.rate` (Gauge)

### 14.3 Health Checks

**Endpoints**:
- `/actuator/health` - Overall service health
- `/actuator/health/readiness` - Ready to accept traffic
- `/actuator/health/liveness` - Service is alive

**Health Indicators**:
- Database connectivity
- Redis connectivity
- RabbitMQ connectivity
- Disk space

### 14.4 Observability – Audit Tables and Events

- **Audit tables:** `seat_history` (seat state changes), `checkin_history` (check-in state transitions). Optional: `rate_limit_audit` for rate-limit violations (see Rate Limiting).
- **Events emitted:** `seat.held`, `seat.confirmed`, `seat.released`, `waitlist.assigned`, `payment.success`, `payment.failure`, `checkin.completed` — for monitoring and notification consumption.

### 14.5 Distributed Tracing

**Implementation**: Spring Cloud Sleuth + Zipkin (optional)

**Trace ID Propagation**:
```
Request arrives → Trace ID generated → Propagated via HTTP headers
    │
    ├─ Seat Service (trace-id: abc123)
    ├─ Check-in Service (trace-id: abc123)
    └─ Notification Service (trace-id: abc123)
```

---

## 15. Design Decisions and Trade-offs

- **FIFO waitlist:** Simplicity and fairness; first-come-first-served. Alternative (priority/tier) deferred.
- **In-memory rate limit:** Simple integration; trade-off is that limits are per instance (not shared across replicas). For multi-instance, use Redis-backed rate limit.
- **Confirm-time check for hold expiry:** Fail-safe so that even if the scheduler is delayed, a confirm after TTL always fails; avoids confirming on an expired hold.
- **Check-in orchestrates payment flow:** Single place for pause/resume (WAITING_FOR_PAYMENT) and completion; avoids scattered payment logic across clients.

---

## 16. Deployment Architecture

### 16.1 Docker Compose Architecture

```yaml
version: '3.8'

services:
  # Infrastructure
  redis:
    image: redis:7-alpine
    ports: ["6379:6379"]
    healthcheck: ...

  rabbitmq:
    image: rabbitmq:3.13-management-alpine
    ports: ["5672:5672", "15672:15672"]
    environment:
      RABBITMQ_DEFAULT_USER: guest
      RABBITMQ_DEFAULT_PASS: guest

  # Microservices
  seat-management:
    build: ./seat-management-service
    ports: ["8091:8091"]
    environment:
      SPRING_DATASOURCE_URL: jdbc:h2:mem:seat-management-db;DB_CLOSE_DELAY=-1
      SPRING_DATA_REDIS_HOST: redis
      SPRING_RABBITMQ_HOST: rabbitmq
    depends_on: [redis, rabbitmq]

  checkin-service:
    build: ./checkin-service
    ports: ["8082:8082"]
    environment:
      SPRING_DATASOURCE_URL: jdbc:h2:mem:checkin-db;DB_CLOSE_DELAY=-1
      SPRING_RABBITMQ_HOST: rabbitmq
      SPRING_CLOUD_OPENFEIGN_CLIENT_CONFIG_SEAT-MANAGEMENT-SERVICE_URL: http://seat-management:8091
      SPRING_CLOUD_OPENFEIGN_CLIENT_CONFIG_BAGGAGE-SERVICE_URL: http://baggage-service:8083
      SPRING_CLOUD_OPENFEIGN_CLIENT_CONFIG_PAYMENT-SERVICE_URL: http://payment-service:8084
    depends_on: [rabbitmq, seat-management]

  baggage-service:
    build: ./baggage-service
    ports: ["8083:8083"]
    environment:
      SPRING_DATASOURCE_URL: jdbc:h2:mem:baggage-db;DB_CLOSE_DELAY=-1
      SPRING_RABBITMQ_HOST: rabbitmq

  payment-service:
    build: ./payment-service
    ports: ["8084:8084"]
    environment:
      SPRING_DATASOURCE_URL: jdbc:h2:mem:payment-db;DB_CLOSE_DELAY=-1
      SPRING_RABBITMQ_HOST: rabbitmq

  notification-service:
    build: ./notification-service
    ports: ["8085:8085"]
    environment:
      SPRING_DATASOURCE_URL: jdbc:h2:mem:notification-db;DB_CLOSE_DELAY=-1
      SPRING_RABBITMQ_HOST: rabbitmq

  frontend:
    build: ./frontend
    ports: ["3000:80"]   # Nginx serves on 80
    depends_on: [seat-management, checkin-service, baggage-service, payment-service]

volumes:
  redis-data:
  rabbitmq-data:

networks:
  skyhigh-network:
```

### 16.2 Deployment Diagram

```
┌─────────────────────────────────────────────────────────────────┐
│                        Docker Host                              │
│                                                                 │
│  ┌────────────────┐   ┌────────────────┐   ┌────────────────┐ │
│  │   Frontend     │   │ Seat Mgmt      │   │  Check-in      │ │
│  │  Container     │   │  Container     │   │  Container     │ │
│  │   (Port 3000)  │   │   (Port 8091)  │   │   (Port 8082)  │ │
│  └────────────────┘   └────────────────┘   └────────────────┘ │
│                                                                 │
│  ┌────────────────┐   ┌────────────────┐   ┌────────────────┐ │
│  │   Baggage      │   │   Payment      │   │  Notification  │ │
│  │  Container     │   │  Container     │   │  Container     │ │
│  │   (Port 8083)  │   │   (Port 8084)  │   │   (Port 8085)  │ │
│  └────────────────┘   └────────────────┘   └────────────────┘ │
│                                                                 │
│  ┌────────────────┐   ┌────────────────────────────────────┐  │
│  │    Redis       │   │        RabbitMQ                    │  │
│  │  Container     │   │       Container                    │  │
│  │  (Port 6379)   │   │  (Ports 5672, 15672)               │  │
│  └────────────────┘   └────────────────────────────────────┘  │
│                                                                 │
│  ┌─────────────────────────────────────────────────────────┐  │
│  │              Shared Docker Network                      │  │
│  │              (skyhigh-network)                          │  │
│  └─────────────────────────────────────────────────────────┘  │
│                                                                 │
│  ┌─────────────────────────────────────────────────────────┐  │
│  │  H2: In-memory per container (no data persistence)      │  │
│  │  Redis/RabbitMQ: Volume-backed for state persistence    │  │
│  └─────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
```

### 16.3 Network Communication

**Internal Service Discovery**:
- Services communicate using container names as hostnames
- Example: `http://seat-management:8091/api/seats`

**External Access**:
- Frontend: `http://localhost:3000`
- Seat API: `http://localhost:8091`
- RabbitMQ Management: `http://localhost:15672`

---

## Appendix: Technology Justification

### Why Java 21 + Spring Boot 3.5?
- **Virtual Threads**: Improved concurrency handling (Project Loom)
- **Pattern Matching**: Cleaner code for state transitions
- **Mature Ecosystem**: Extensive libraries for all requirements
- **Production Ready**: Battle-tested for high-traffic systems

### Why H2 Database?
- **Easy Setup**: No installation required
- **Local**: Filesystem persistence; **Docker**: In-memory for simplified deployment
- **ACID Compliance**: Full transaction support
- **Development Friendly**: Built-in web console

### Why RabbitMQ over Kafka?
- **Simpler Setup**: Faster to configure and deploy
- **Task Queues**: Perfect for notification jobs
- **Lower Overhead**: Sufficient for assignment scope
- **Management UI**: Easy monitoring and debugging

### Why No API Gateway?
- **Reduced Complexity**: Fewer moving parts for local development
- **Direct Communication**: Frontend can call services directly
- **Assignment Scope**: Gateway adds overhead without significant benefit
- **Future Addition**: Can be added later if needed

---

**Document Version**: 2.0  
**Last Updated**: February 21, 2026  
**Author**: Development Team  
**Reviewers**: Architecture Review Board
