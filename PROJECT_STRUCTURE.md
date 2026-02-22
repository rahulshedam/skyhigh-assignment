# Project Structure
# SkyHigh Core – Digital Check-In System

## Overview

This document describes the project structure for the SkyHigh Core system, including all microservices, the shared common module, and the frontend application. The structure follows Spring Boot microservices and React (Vite + TypeScript) conventions.

---

## Table of Contents

1. [Root Directory Structure](#1-root-directory-structure)
2. [Common Module](#2-common-module)
3. [Seat Management Service](#3-seat-management-service)
4. [Check-in Service](#4-check-in-service)
5. [Baggage Service](#5-baggage-service)
6. [Payment Service](#6-payment-service)
7. [Notification Service](#7-notification-service)
8. [Frontend Application](#8-frontend-application)
9. [Common Patterns and Conventions](#9-common-patterns-and-conventions)

---

## 1. Root Directory Structure

```
skyhigh-assignment/
│
├── common/                           # Shared DTOs and exception handling (used by baggage, payment)
│   ├── src/main/java/com/skyhigh/common/
│   ├── pom.xml
│   └── (no Dockerfile - library module)
│
├── seat-management-service/          # Seat & Waitlist Management Microservice
│   ├── src/
│   ├── pom.xml
│   ├── Dockerfile
│   └── README.md
│
├── checkin-service/                  # Check-in Orchestration Microservice
│   ├── src/
│   ├── pom.xml
│   └── Dockerfile
│
├── baggage-service/                  # Baggage Validation Microservice
│   ├── src/
│   ├── pom.xml
│   └── Dockerfile
│
├── payment-service/                  # Payment Processing Microservice (Simulated)
│   ├── src/
│   ├── pom.xml
│   └── Dockerfile
│
├── notification-service/             # Notification Microservice
│   ├── src/
│   ├── pom.xml
│   ├── Dockerfile
│   └── NOTIFICATION_SERVICE_README.md
│
├── frontend/                         # React + Vite + TypeScript Frontend
│   ├── src/
│   ├── public/
│   ├── index.html
│   ├── package.json
│   ├── vite.config.ts
│   ├── tsconfig.json
│   ├── nginx.conf
│   ├── Dockerfile
│   └── README.md
│
├── docs/                             # Additional documentation (optional)
│   ├── api-examples/
│   └── diagrams/
│
├── scripts/                          # Utility scripts (optional)
│
├── docker-compose.yml                # Docker orchestration (Redis, RabbitMQ, all services)
├── .gitignore
├── .dockerignore
├── PRD.md                            # Product Requirements Document
├── ARCHITECTURE.md                   # Architecture documentation
└── PROJECT_STRUCTURE.md              # This file
```

**Purpose of root directories:**
- **common/**: Shared library used by baggage-service and payment-service for consistent API responses and exception handling
- **docs/**: API examples, diagrams (may be empty)
- **scripts/**: Optional setup or seed scripts

---

## 2. Common Module

**Purpose**: Shared DTOs and exception handling for consistent API responses across microservices  
**Used by**: baggage-service, payment-service

```
common/
│
├── src/main/java/com/skyhigh/common/
│   ├── dto/
│   │   ├── ApiResponse.java          # Standard API response wrapper
│   │   ├── ErrorResponse.java        # Error response structure
│   │   └── ErrorDetail.java          # Error detail for validation
│   │
│   └── exception/
│       ├── BaseException.java        # Base exception class
│       └── GlobalExceptionHandler.java   # @ControllerAdvice for consistent error handling
│
└── pom.xml
```

---

## 3. Seat Management Service

**Purpose**: Manages seat lifecycle, waitlist, seat expiry timer, and booking verification  
**Port**: 8091  
**Database**: H2 (in-memory: seat-management-db via docker-compose)

```
seat-management-service/
│
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/
│   │   │       └── skyhigh/
│   │   │           └── seat/
│   │   │               │
│   │   │               ├── SeatManagementApplication.java          # Main Spring Boot class
│   │   │               │
│   │   │               ├── config/                                 # Configuration classes
│   │   │               │   ├── DataInitializationConfig.java       # Initial data seeding
│   │   │               │   ├── RateLimitConfig.java                # Rate limiting (Bucket4j)
│   │   │               │   ├── RedisConfig.java                    # Redis cache configuration
│   │   │               │   ├── RabbitMQConfig.java                 # RabbitMQ messaging config
│   │   │               │   ├── SchedulerConfig.java                # Scheduler configuration
│   │   │               │   └── SecurityConfig.java                 # Security configuration
│   │   │               │
│   │   │               ├── controller/                             # REST API Controllers
│   │   │               │   ├── SeatController.java                 # Seat operations
│   │   │               │   ├── SeatMapController.java              # Seat map retrieval
│   │   │               │   ├── WaitlistController.java             # Waitlist management
│   │   │               │   └── BookingController.java              # Booking verification
│   │   │               │
│   │   │               ├── service/                                # Business logic
│   │   │               │   ├── SeatService.java                    # Seat hold/confirm/cancel
│   │   │               │   ├── SeatMapService.java                 # Seat map with caching
│   │   │               │   ├── WaitlistService.java                # Waitlist management
│   │   │               │   ├── SeatExpiryService.java              # Expiry handling
│   │   │               │   ├── LockService.java                    # Distributed locking
│   │   │               │   ├── EventPublisherService.java          # RabbitMQ event publisher
│   │   │               │   └── BookingService.java                 # Booking verification
│   │   │               │
│   │   │               ├── repository/                             # Data access layer
│   │   │               │   ├── SeatRepository.java
│   │   │               │   ├── FlightRepository.java
│   │   │               │   ├── SeatAssignmentRepository.java
│   │   │               │   ├── WaitlistRepository.java
│   │   │               │   ├── SeatHistoryRepository.java          # Audit trail
│   │   │               │   └── BookingRepository.java              # Booking verification
│   │   │               │
│   │   │               ├── model/                                  # Data models
│   │   │               │   ├── entity/
│   │   │               │   │   ├── Seat.java
│   │   │               │   │   ├── Flight.java
│   │   │               │   │   ├── SeatAssignment.java
│   │   │               │   │   ├── Waitlist.java
│   │   │               │   │   ├── SeatHistory.java
│   │   │               │   │   └── Booking.java                    # Booking entity
│   │   │               │   │
│   │   │               │   ├── dto/
│   │   │               │   │   ├── SeatMapResponse.java
│   │   │               │   │   ├── WaitlistJoinRequest.java
│   │   │               │   │   ├── BookingVerificationRequest.java
│   │   │               │   │   └── BookingVerificationResponse.java
│   │   │               │   │
│   │   │               │   ├── event/                              # RabbitMQ events
│   │   │               │   │   ├── SeatHeldEvent.java
│   │   │               │   │   ├── SeatConfirmedEvent.java
│   │   │               │   │   ├── SeatReleasedEvent.java
│   │   │               │   │   └── WaitlistAssignedEvent.java
│   │   │               │   │
│   │   │               │   └── enums/
│   │   │               │       ├── SeatStatus.java                 # AVAILABLE, HELD, CONFIRMED
│   │   │               │       ├── SeatClass.java
│   │   │               │       ├── WaitlistStatus.java
│   │   │               │       └── SeatAssignmentStatus.java
│   │   │               │
│   │   │               ├── scheduler/                              # Spring @Scheduled jobs
│   │   │               │   ├── SeatExpiryScheduler.java            # Seat hold expiry
│   │   │               │   └── WaitlistProcessorScheduler.java     # Waitlist assignment
│   │   │               │
│   │   │               ├── filter/
│   │   │               │   └── RateLimitFilter.java                # Rate limiting filter
│   │   │               │
│   │   │               └── exception/
│   │   │                   ├── ResourceNotFoundException.java
│   │   │                   ├── SeatNotFoundException.java
│   │   │                   ├── SeatAlreadyHeldException.java
│   │   │                   ├── SeatUnavailableException.java
│   │   │                   └── GlobalExceptionHandler.java
│   │   │
│   │   └── resources/
│   │       ├── application.yml
│   │       └── db/migration/                                       # Flyway migrations
│   │           ├── V1__create_flights_table.sql
│   │           ├── V2__create_seats_table.sql
│   │           ├── V3__create_seat_assignments_table.sql
│   │           ├── V4__create_waitlist_table.sql
│   │           ├── V5__create_seat_history_table.sql
│   │           ├── V6__seed_initial_data.sql
│   │           └── V7__create_bookings_table.sql
│   │
│   └── test/
│       ├── java/
│       │   └── com/
│       │       └── skyhigh/
│       │           └── seat/
│       │               ├── controller/                             # Controller tests
│       │               │   ├── SeatControllerTest.java
│       │               │   └── SeatMapControllerTest.java
│       │               │
│       │               ├── service/                                # Service tests
│       │               │   ├── SeatServiceTest.java
│       │               │   ├── SeatMapServiceTest.java
│       │               │   └── WaitlistServiceTest.java
│       │               │
│       │               ├── repository/                             # Repository tests
│       │               │   ├── SeatRepositoryTest.java
│       │               │   └── SeatAssignmentRepositoryTest.java
│       │               │
│       │               ├── scheduler/                              # Scheduler tests
│       │               │   └── SeatExpiryJobTest.java
│       │               │
│       │               ├── integration/                            # Integration tests
│       │               │   ├── SeatManagementIntegrationTest.java
│       │               │   └── ConcurrencyTest.java
│       │               │
│       │               └── util/                                   # Test utilities
│       │                   └── TestDataFactory.java
│       │
│       └── resources/
│           └── application-test.yml                                # Test configuration
│
├── pom.xml                                                        # Maven dependencies
├── Dockerfile                                                     # Docker build file
└── README.md                                                      # Service-specific README
```

### Key Dependencies (pom.xml)
- Spring Boot Starter Web
- Spring Boot Starter Data JPA
- Spring Boot Starter Cache
- Spring Boot Starter AMQP (RabbitMQ)
- Spring Boot Starter Quartz
- Spring Boot Starter Validation
- Spring Boot Starter Actuator
- H2 Database
- Redis (Jedis/Lettuce)
- Redisson (Distributed Locks)
- Bucket4j (Rate Limiting)
- Resilience4j
- Lombok
- SpringDoc OpenAPI
- JUnit 5, Mockito, AssertJ

---

## 4. Check-in Service

**Purpose**: Orchestrates the check-in workflow (seat hold, baggage validation, payment)  
**Port**: 8082  
**Database**: H2 (in-memory: checkin-db)

```
checkin-service/
│
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/
│   │   │       └── skyhigh/
│   │   │           └── checkin/
│   │   │               │
│   │   │               ├── CheckInServiceApplication.java          # Main application class
│   │   │               │
│   │   │               ├── config/
│   │   │               │   ├── AppConfig.java                      # RestTemplate, Feign config
│   │   │               │   └── SecurityConfig.java
│   │   │               │
│   │   │               ├── controller/
│   │   │               │   └── CheckInController.java              # Check-in start, complete
│   │   │               │
│   │   │               ├── service/
│   │   │               │   └── CheckInService.java                 # Main orchestration
│   │   │               │
│   │   │               ├── client/                                 # Feign service clients
│   │   │               │   ├── SeatManagementClient.java           # Seat hold/confirm
│   │   │               │   ├── BaggageServiceClient.java           # Baggage validation
│   │   │               │   ├── PaymentServiceClient.java           # Payment processing
│   │   │               │   └── dto/                                # Client DTOs
│   │   │               │       ├── SeatHoldRequest.java
│   │   │               │       ├── SeatConfirmRequest.java
│   │   │               │       ├── BaggageValidationRequest.java
│   │   │               │       ├── BaggageValidationResponse.java
│   │   │               │       ├── PaymentRequest.java
│   │   │               │       └── PaymentResponse.java
│   │   │               │
│   │   │               ├── repository/
│   │   │               │   ├── CheckInRepository.java
│   │   │               │   └── CheckInHistoryRepository.java       # Check-in audit
│   │   │               │
│   │   │               ├── model/
│   │   │               │   ├── entity/
│   │   │               │   │   ├── CheckIn.java
│   │   │               │   │   └── CheckInHistory.java
│   │   │               │   ├── dto/
│   │   │               │   │   ├── CheckInStartRequest.java
│   │   │               │   │   ├── CheckInCompleteRequest.java
│   │   │               │   │   ├── CheckInBaggageRequest.java
│   │   │               │   │   └── CheckInResponse.java
│   │   │               │   └── enums/
│   │   │               │       └── CheckInStatus.java
│   │   │               │
│   │   │               └── exception/
│   │   │                   ├── CheckInNotFoundException.java
│   │   │                   ├── CheckInException.java
│   │   │                   ├── ServiceUnavailableException.java
│   │   │                   └── GlobalExceptionHandler.java
│   │   │
│   │   └── resources/
│   │       └── application.yml
│   │           # Uses JPA/Hibernate DDL - no Flyway migrations
│   │
│   └── test/
│       └── java/com/skyhigh/checkin/
│           ├── controller/CheckInControllerTest.java
│           ├── service/CheckInServiceTest.java
│           ├── client/SeatManagementClientTest.java
│           ├── client/BaggageServiceClientTest.java
│           ├── client/PaymentServiceClientTest.java
│           └── exception/GlobalExceptionHandlerTest.java
│       │
│       └── resources/
│           └── application-test.yml
│
├── pom.xml
├── Dockerfile
└── README.md
```

### Key Dependencies
- Spring Boot Starter Web
- Spring Boot Starter Data JPA
- Spring Boot Starter AMQP
- Spring Boot Starter Actuator
- Spring Cloud OpenFeign (for service clients)
- Resilience4j (Circuit Breaker)
- H2 Database
- Lombok
- JUnit 5, Mockito

---

## 5. Baggage Service

**Purpose**: Validates baggage weight and calculates fees (25kg free limit)  
**Port**: 8083  
**Database**: H2 (in-memory: baggage-db)  
**Dependencies**: common module

```
baggage-service/
│
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/
│   │   │       └── skyhigh/
│   │   │           └── baggage/
│   │   │               │
│   │   │               ├── BaggageServiceApplication.java
│   │   │               │
│   │   │               ├── config/
│   │   │               │   ├── RabbitMQConfig.java
│   │   │               │   └── SecurityConfig.java
│   │   │               │
│   │   │               ├── controller/
│   │   │               │   └── BaggageController.java              # Validation endpoints
│   │   │               │
│   │   │               ├── service/
│   │   │               │   ├── BaggageService.java                 # Validation logic
│   │   │               │   └── MetricsService.java
│   │   │               │
│   │   │               ├── repository/
│   │   │               │   └── BaggageRecordRepository.java
│   │   │               │
│   │   │               ├── model/
│   │   │               │   ├── BaggageRecord.java
│   │   │               │   └── BaggageStatus.java
│   │   │               │
│   │   │               ├── dto/
│   │   │               │   ├── BaggageValidationRequest.java
│   │   │               │   ├── BaggageValidationResponse.java
│   │   │               │   └── BaggageData.java
│   │   │               │
│   │   │               ├── event/
│   │   │               │   ├── EventPublisher.java
│   │   │               │   └── BaggageEvent.java
│   │   │               │
│   │   │               ├── exception/
│   │   │               │   ├── InvalidWeightException.java
│   │   │               │   ├── BaggageNotFoundException.java
│   │   │               │   └── (uses common GlobalExceptionHandler)
│   │   │               │
│   │   │               └── util/
│   │   │                   └── BaggageConstants.java              # MAX_WEIGHT = 25kg
│   │   │
│   │   └── resources/
│   │       ├── application.yml
│   │       └── db/migration/
│   │           └── V1__create_baggage_records_table.sql
│   │
│   └── test/
│       └── java/
│           └── com/
│               └── skyhigh/
│                   └── baggage/
│                       ├── controller/
│                       │   └── BaggageControllerTest.java
│                       ├── service/
│                       │   ├── BaggageServiceTest.java
│                       │   └── FeeCalculationServiceTest.java
│                       └── integration/
│                           └── BaggageValidationTest.java
│
├── pom.xml
├── Dockerfile
└── README.md
```

---

## 6. Payment Service

**Purpose**: Simulates payment processing for excess baggage fees (~70% success rate)  
**Port**: 8084  
**Database**: H2 (in-memory: payment-db)  
**Dependencies**: common module

```
payment-service/
│
├── src/
│   ├── main/
│   │   ├── java/com/skyhigh/payment/
│   │   │   ├── PaymentServiceApplication.java
│   │   │   ├── config/
│   │   │   │   ├── RabbitMQConfig.java
│   │   │   │   └── SecurityConfig.java
│   │   │   ├── controller/PaymentController.java
│   │   │   ├── service/
│   │   │   │   ├── PaymentService.java
│   │   │   │   ├── PaymentSimulationService.java     # Simulated gateway
│   │   │   │   └── MetricsService.java
│   │   │   ├── repository/PaymentRepository.java
│   │   │   ├── model/ (Payment, PaymentStatus, PaymentType)
│   │   │   ├── dto/ (PaymentRequest, PaymentData)
│   │   │   ├── event/ (EventPublisher, PaymentEvent)
│   │   │   └── exception/ (PaymentProcessingException, PaymentNotFoundException)
│   │   │
│   │   └── resources/application.yml
│   │       # Uses JPA DDL - no Flyway migrations
│   │
│   └── test/java/com/skyhigh/payment/
│       └── service/PaymentSimulationServiceTest.java
│
├── pom.xml
└── Dockerfile
```

---

## 7. Notification Service

**Purpose**: Listens to RabbitMQ events and sends notifications (email simulation)  
**Port**: 8085  
**Database**: H2 (in-memory: notification-db)

```
notification-service/
│
├── src/
│   ├── main/
│   │   ├── java/com/skyhigh/notification/
│   │   │   ├── NotificationServiceApplication.java
│   │   │   ├── config/
│   │   │   │   ├── RabbitMQConfig.java
│   │   │   │   └── SecurityConfig.java
│   │   │   ├── controller/NotificationController.java
│   │   │   ├── service/
│   │   │   │   ├── NotificationService.java
│   │   │   │   ├── TemplateService.java
│   │   │   │   └── MockEmailService.java
│   │   │   ├── repository/NotificationRepository.java
│   │   │   ├── model/ (Notification, NotificationType, NotificationStatus, NotificationChannel)
│   │   │   ├── dto/ (NotificationRequest, NotificationResponse, EmailRequest)
│   │   │   ├── event/ (SeatHeldEvent, SeatConfirmedEvent, SeatReleasedEvent, WaitlistAssignedEvent, PaymentEvent)
│   │   │   └── listener/
│   │   │       ├── SeatEventListener.java
│   │   │       └── PaymentEventListener.java
│   │   │
│   │   └── resources/
│   │       ├── application.yml
│   │       └── templates/email/                                    # HTML email templates
│   │           ├── seat-hold.html
│   │           ├── seat-confirmation.html
│   │           ├── payment-confirmation.html
│   │           ├── payment-failure.html
│   │           └── waitlist-assigned.html
│   │           # Uses JPA DDL - no Flyway migrations
│   │
│   └── test/java/com/skyhigh/notification/
│       ├── service/ (NotificationServiceTest, TemplateServiceTest, MockEmailServiceTest)
│       ├── listener/ (SeatEventListenerTest, PaymentEventListenerTest)
│       └── controller/NotificationControllerIntegrationTest.java
│
├── pom.xml
├── Dockerfile
└── README.md
```

---

## 8. Frontend Application

**Purpose**: React + Vite + TypeScript user interface for the digital check-in flow  
**Port**: 3000 (dev), 80 in Docker (nginx)

```
frontend/
│
├── public/
│   └── index.html
│
├── src/
│   ├── components/
│   │   ├── layout/
│   │   │   ├── Header.tsx
│   │   │   └── MainLayout.tsx
│   │   │
│   │   └── features/
│   │       └── SeatGrid.tsx
│   │
│   ├── pages/
│   │   ├── HomePage/HomePage.tsx
│   │   ├── SeatSelectionPage/SeatSelectionPage.tsx
│   │   ├── CheckinPage/CheckinPage.tsx
│   │   ├── WaitlistPage/WaitlistPage.tsx
│   │   ├── ConfirmationPage/ConfirmationPage.tsx
│   │   └── NotFoundPage/NotFoundPage.tsx
│   │
│   ├── store/
│   │   ├── index.ts
│   │   └── slices/
│   │       ├── seatSlice.ts
│   │       └── checkinSlice.ts
│   │
│   ├── api/                                                       # API layer (Axios)
│   │   ├── base.ts                                                # Axios instance + interceptors
│   │   └── services.ts                                            # BookingService, SeatService, CheckinService, etc.
│   │
│   ├── types/index.ts
│   ├── styles/ (global.css, theme.ts)
│   ├── App.tsx                                                    # Main App (routing in App)
│   └── main.tsx                                                   # Entry point
│
├── package.json
├── tsconfig.json
├── vite.config.ts
├── nginx.conf
├── Dockerfile
└── README.md
```

### Key Dependencies (package.json)
- react, react-dom, react-router-dom
- @reduxjs/toolkit, react-redux
- axios
- @mui/material, @emotion/react, @emotion/styled
- formik, yup (form validation)
- date-fns, lodash
- react-toastify
- socket.io-client
- vite (build), typescript
- @testing-library/react, jest, playwright (tests)

---

## 9. Common Patterns and Conventions

### 9.1 Naming Conventions

**Java (Backend)**:
- **Packages**: Lowercase, singular (e.g., `com.skyhigh.seat.service`)
- **Classes**: PascalCase (e.g., `SeatService`, `SeatController`)
- **Interfaces**: PascalCase, no "I" prefix (e.g., `SeatService`)
- **Methods**: camelCase (e.g., `holdSeat()`, `getSeatMap()`)
- **Constants**: UPPER_SNAKE_CASE (e.g., `MAX_HOLD_TIME`)
- **Variables**: camelCase (e.g., `seatId`, `passengerName`)

**TypeScript (Frontend)**:
- **Files**: PascalCase for components (e.g., `SeatMap.tsx`)
- **Files**: camelCase for utilities (e.g., `apiClient.ts`)
- **Components**: PascalCase (e.g., `SeatMap`, `CheckinFlow`)
- **Functions**: camelCase (e.g., `formatDate()`, `validateInput()`)
- **Types/Interfaces**: PascalCase with "I" prefix for interfaces (e.g., `ISeat`, `ICheckinRequest`)
- **Constants**: UPPER_SNAKE_CASE (e.g., `API_BASE_URL`)

### 9.2 File Organization Principles

1. **Separation of Concerns**: Each layer has clear responsibilities
2. **Feature-based Structure**: Group related functionality together
3. **Dependency Direction**: Dependencies flow inward (Controller → Service → Repository)
4. **Test Mirroring**: Test structure mirrors source structure
5. **Configuration Externalization**: Use YAML/properties for configuration

### 9.3 Layer Responsibilities

**Controller Layer**:
- Handle HTTP requests/responses
- Validate request parameters
- Delegate to service layer
- Return appropriate HTTP status codes

**Service Layer**:
- Implement business logic
- Coordinate between repositories
- Handle transactions
- Publish events

**Repository Layer**:
- Database interactions only
- Use Spring Data JPA conventions
- Define custom queries when needed

**Model Layer**:
- **Entity**: JPA entities mapped to database tables
- **DTO**: Data Transfer Objects for API communication
- **Event**: Event objects for messaging
- **Enum**: Type-safe constants

### 9.4 Testing Strategy

**Unit Tests**: 80%+ coverage target
- Service layer: Mock repositories
- Controller layer: Mock services, use MockMvc
- Repository layer: Use @DataJpaTest

**Integration Tests**:
- Test full request-response cycle
- Use TestContainers for database
- Test event publishing/consuming

**E2E Tests** (Frontend):
- Test complete user workflows
- Use Playwright or Cypress

### 9.5 Configuration Management

**application.yml structure**:
```yaml
spring:
  application:
    name: seat-management-service
  datasource:
    url: jdbc:h2:file:./data/seat-db
  rabbitmq:
    host: rabbitmq
  cache:
    type: redis

server:
  port: 8091

app:
  seat:
    hold-duration: 120
    max-retries: 3
  rate-limit:
    capacity: 50
    refill-rate: 25
```

### 9.6 Error Handling

**Backend**:
- Use `@ControllerAdvice` for global exception handling
- Return consistent error response format:
```json
{
  "timestamp": "2026-02-08T10:30:45.123Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Seat is already held",
  "path": "/api/seats/12A/hold"
}
```

**Frontend**:
- Use ErrorBoundary for component errors
- Display user-friendly error messages
- Log errors to monitoring service

### 9.7 API Response Format

**Success Response**:
```json
{
  "success": true,
  "data": { ... },
  "timestamp": "2026-02-08T10:30:45.123Z"
}
```

**Error Response**:
```json
{
  "success": false,
  "error": {
    "code": "SEAT_UNAVAILABLE",
    "message": "This seat is currently held by another passenger"
  },
  "timestamp": "2026-02-08T10:30:45.123Z"
}
```

### 9.8 Docker Best Practices

**Multi-stage builds**:
```dockerfile
# Stage 1: Build
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Runtime
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
```

---

## Summary

This project structure provides:

1. ✅ **Clear Separation of Concerns**: Each service has well-defined responsibilities
2. ✅ **Scalability**: Microservices can be scaled independently
3. ✅ **Maintainability**: Consistent structure across all services
4. ✅ **Testability**: Comprehensive test structure with high coverage potential
5. ✅ **Best Practices**: Follows industry standards for Spring Boot and React
6. ✅ **Documentation**: Each service has its own README
7. ✅ **Containerization**: Docker-ready with multi-stage builds

**Total Services**: 5 backend microservices + 1 common library + 1 frontend application  
**Infrastructure**: Redis, RabbitMQ (via docker-compose)  
**Test Coverage Target**: 80%+

---

**Document Version**: 2.0  
**Last Updated**: February 21, 2026  
**Maintained By**: Development Team
