# Payment Service

## Overview
The Payment Service simulates payment processing for excess baggage fees. This is a mock service with a 70% success rate simulation.

## Technology Stack
- **Java**: 21
- **Framework**: Spring Boot 3.5
- **Database**: H2 (filesystem mode)
- **Message Queue**: RabbitMQ

## Port
8084

## Key Features
- ✅ Mock payment gateway (70% success rate)
- ✅ Payment status tracking
- ✅ Transaction history
- ✅ Event-driven notifications

## API Endpoints
- `POST /api/payment/process` - Process payment
- `GET /api/payment/{paymentId}/status` - Get payment status

## Payment Simulation
The service simulates real-world payment behavior:
- **Success Rate**: 70%
- **Failure Rate**: 30%
- **Processing Time**: 1-3 seconds (simulated)

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
docker build -t payment-service .
docker run -p 8084:8084 payment-service
```

## Testing
```bash
mvn test
```

## Events Published
- `PaymentCompletedEvent` - On successful payment
- `PaymentFailedEvent` - On payment failure

## Health Check
- `http://localhost:8084/actuator/health`
