# Baggage Service

## Overview
The Baggage Service validates baggage weight and calculates excess baggage fees based on airline policies.

## Technology Stack
- **Java**: 21
- **Framework**: Spring Boot 3.5
- **Database**: H2 (filesystem mode)
- **Message Queue**: RabbitMQ

## Port
8083

## Key Features
- ✅ Baggage weight validation (max 25kg free)
- ✅ Automatic fee calculation for excess baggage
- ✅ Mock Weight Service integration
- ✅ Event publishing for downstream services

## API Endpoints
- `POST /api/baggage/validate` - Validate baggage weight
- `GET /api/baggage/fee/{weight}` - Calculate baggage fee
- `POST /api/baggage/record` - Record baggage details

## Business Rules
- **Free Baggage Limit**: 25kg
- **Excess Fee Calculation**: $10 per kg over limit

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
docker build -t baggage-service .
docker run -p 8083:8083 baggage-service
```

## Testing
```bash
mvn test
```

## Events Published
- `BaggageValidatedEvent` - After validation
- `ExcessBaggageFeeEvent` - When excess fee is calculated

## Health Check
- `http://localhost:8083/actuator/health`
