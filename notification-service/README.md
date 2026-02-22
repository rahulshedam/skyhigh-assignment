# Notification Service

## Overview
The Notification Service sends notifications to passengers via email and SMS (simulated). It listens to events from all other services and sends appropriate notifications.

## Technology Stack
- **Java**: 21
- **Framework**: Spring Boot 3.5
- **Database**: H2 (filesystem mode)
- **Message Queue**: RabbitMQ
- **Async Processing**: Spring @Async

## Port
8085

## Key Features
- ✅ Event-driven notifications
- ✅ Email simulation
- ✅ SMS simulation
- ✅ Notification templates
- ✅ Notification history tracking

## API Endpoints
- `GET /api/notifications/passenger/{passengerId}` - Get passenger notifications
- `GET /api/notifications/{notificationId}` - Get notification details

## Notification Types
- Seat hold confirmation
- Seat assignment success
- Check-in completion
- Waitlist assignment
- Payment success/failure
- Seat hold expiry warning

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
docker build -t notification-service .
docker run -p 8085:8085 notification-service
```

## Testing
```bash
mvn test
```

## Events Subscribed
- `SeatHeldEvent`
- `SeatConfirmedEvent`
- `SeatReleasedEvent`
- `WaitlistAssignedEvent`
- `CheckinCompletedEvent`
- `PaymentCompletedEvent`
- `PaymentFailedEvent`

## Health Check
- `http://localhost:8085/actuator/health`
