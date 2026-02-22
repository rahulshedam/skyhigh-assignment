# Seat Management Service

## Overview
The Seat Management Service is responsible for managing the complete lifecycle of aircraft seats, including:
- Seat availability management
- Seat hold operations (120-second TTL)
- Seat confirmation and cancellation
- Waitlist management (FIFO)
- Automatic seat hold expiry using Quartz Scheduler

## Technology Stack
- **Java**: 21
- **Framework**: Spring Boot 3.5
- **Database**: H2 (filesystem mode)
- **Cache**: Redis
- **Message Queue**: RabbitMQ
- **Scheduler**: Quartz

## Port
8091

## Key Features
- ✅ Optimistic locking for concurrency control
- ✅ Distributed locking using Redis
- ✅ Rate limiting (50 requests per 2 seconds)
- ✅ Automatic seat hold expiry every 10 seconds
- ✅ Waitlist processing every 5 seconds
- ✅ Event-driven architecture with RabbitMQ

## API Endpoints
- `GET /api/seats/flights/{flightId}/seatmap` - Get seat map
- `POST /api/seats/{seatId}/hold` - Hold a seat
- `POST /api/seats/{seatId}/confirm` - Confirm seat assignment
- `POST /api/seats/{seatId}/cancel` - Cancel confirmed seat
- `POST /api/seats/{seatId}/waitlist` - Join waitlist
- `GET /api/seats/{seatId}/status` - Get seat status

## Database Schema
See [WORKFLOW_DESIGN.md](../WORKFLOW_DESIGN.md) for complete schema.

## Running the Service

### Prerequisites
- Java 21
- Maven 3.9+
- Redis (via Docker)
- RabbitMQ (via Docker)

### Build
```bash
mvn clean install
```

### Run
```bash
mvn spring-boot:run
```

### Docker
```bash
docker build -t seat-management-service .
docker run -p 8091:8091 seat-management-service
```

## Testing
```bash
mvn test
mvn verify
```

## Configuration
Configuration files are located in `src/main/resources/`:
- `application.yml` - Base configuration
- `application-dev.yml` - Development profile
- `application-prod.yml` - Production profile

## Events Published
- `SeatHeldEvent` - When a seat is successfully held
- `SeatConfirmedEvent` - When a seat assignment is confirmed
- `SeatReleasedEvent` - When a held seat expires or is cancelled
- `WaitlistAssignedEvent` - When a waitlisted passenger is assigned a seat

## Health Check
- `http://localhost:8091/actuator/health`

## Metrics
- `http://localhost:8091/actuator/metrics`
