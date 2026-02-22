# Check-in Service

## Overview
The Check-in Service orchestrates the complete check-in workflow for passengers, coordinating with multiple microservices to ensure a seamless check-in experience.

## Technology Stack
- **Java**: 21
- **Framework**: Spring Boot 3.5
- **Database**: H2 (filesystem mode)
- **Message Queue**: RabbitMQ
- **Circuit Breaker**: Resilience4j

## Port
8082

## Key Features
- ✅ Workflow orchestration across multiple services
- ✅ State management (IN_PROGRESS, WAITING_FOR_PAYMENT, COMPLETED)
- ✅ Circuit breaker for external service calls
- ✅ Event-driven communication
- ✅ Resume capability for interrupted check-ins

## API Endpoints
- `POST /api/checkin/start` - Start check-in process
- `GET /api/checkin/{checkinId}` - Get check-in status
- `POST /api/checkin/{checkinId}/complete` - Complete check-in
- `POST /api/checkin/{checkinId}/cancel` - Cancel check-in

## Integration Points
- **Seat Management Service**: Seat confirmation
- **Baggage Service**: Baggage validation
- **Payment Service**: Fee processing
- **Notification Service**: Passenger notifications

## Running the Service

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
docker build -t checkin-service .
docker run -p 8082:8082 checkin-service
```

## Testing
```bash
mvn test
```

## Events Subscribed
- `SeatConfirmedEvent` - From Seat Management Service
- `PaymentCompletedEvent` - From Payment Service

## Events Published
- `CheckinStartedEvent` - When check-in begins
- `CheckinCompletedEvent` - When check-in is complete
- `CheckinCancelledEvent` - When check-in is cancelled

## Health Check
- `http://localhost:8082/actuator/health`
