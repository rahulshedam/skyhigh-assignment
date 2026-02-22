# Notification Service - Mock Email Implementation

## Overview

The Notification Service is a complete implementation that handles email notifications for the SkyHigh Airlines booking system. It uses a **mock email approach** that logs emails to the console and stores them in the database, making it perfect for assignments and demonstrations without requiring external SMTP services.

## Why Mock Email Implementation?

✅ **No Cost** - Everything runs locally, no paid services required  
✅ **No External Dependencies** - No SMTP server or third-party email service needed  
✅ **Verifiable** - All emails logged to console and stored in H2 database  
✅ **Fast** - No network latency from external email services  
✅ **Testable** - Easy to write and run tests  
✅ **Demo-Ready** - Clear visual output in logs for presentations  
✅ **Production-Ready Architecture** - Same structure as real email service

## Architecture

```
┌─────────────────┐       ┌─────────────────┐       ┌─────────────────┐
│  Seat Service   │──────▶│    RabbitMQ     │──────▶│   Notification  │
│  (Events)       │       │   (Topic Exch)  │       │     Service     │
└─────────────────┘       └─────────────────┘       └─────────────────┘
                                   │                         │
┌─────────────────┐                │                         │
│ Payment Service │────────────────┘                         │
│  (Events)       │                                          ▼
└─────────────────┘                                 ┌─────────────────┐
                                                    │ Mock Email      │
                                                    │ Service         │
                                                    └─────────────────┘
                                                            │
                                        ┌───────────────────┴───────────────────┐
                                        │                                       │
                                        ▼                                       ▼
                                ┌──────────────┐                       ┌──────────────┐
                                │   Console    │                       │ H2 Database  │
                                │   Logs       │                       │  (Storage)   │
                                └──────────────┘                       └──────────────┘
```

## Features Implemented

### 1. Domain Model
- **Notification Entity** - Stores notification records in H2 database
- **Enums**: NotificationType, NotificationStatus, NotificationChannel
- Automatic timestamp management with `@PrePersist` and `@PreUpdate`

### 2. Event Handling
Listens to RabbitMQ events:
- ✅ Seat Confirmed
- ✅ Seat Held
- ✅ Seat Released (logged, no email)
- ✅ Waitlist Assigned
- ✅ Payment Completed
- ✅ Payment Failed

### 3. Mock Email Service
Logs beautifully formatted emails to console:

```
================================================================================
📧 MOCK EMAIL SENT
================================================================================
From: noreply@skyhigh-airlines.com
To: passenger@example.com
Subject: SkyHigh Airlines - Seat Confirmation
Content Type: text/html

--- HTML Content ---
<!DOCTYPE html>
<html>
  ...email content...
</html>
================================================================================
```

### 4. Email Templates
Professional Thymeleaf HTML templates:
- `seat-confirmation.html` - Booking confirmation
- `seat-hold.html` - Temporary seat hold notification
- `payment-confirmation.html` - Payment receipt
- `payment-failure.html` - Payment failure notification
- `waitlist-assigned.html` - Waitlist seat assignment

### 5. REST API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/notifications` | Get all notifications (paginated) |
| GET | `/api/notifications/{id}` | Get notification by ID |
| GET | `/api/notifications/recipient/{recipientId}` | Get notifications by recipient |
| GET | `/api/notifications/status/{status}` | Get notifications by status |
| GET | `/api/notifications/booking/{bookingReference}` | Get notifications by booking |
| GET | `/api/notifications/stats` | Get notification statistics |
| POST | `/api/notifications/test` | Send test notification |

### 6. Comprehensive Tests
- **Unit Tests**: MockEmailServiceTest, TemplateServiceTest, NotificationServiceTest
- **Integration Tests**: NotificationControllerIntegrationTest
- **Repository Tests**: NotificationRepositoryTest
- **Listener Tests**: SeatEventListenerTest, PaymentEventListenerTest

## Configuration

### application.yml
```yaml
# Mock Email Configuration
email:
  mock:
    enabled: true
    log-to-console: true
  from: noreply@skyhigh-airlines.com

# Thymeleaf Configuration
spring:
  thymeleaf:
    cache: false
    mode: HTML
    prefix: classpath:/templates/
    suffix: .html

# RabbitMQ Configuration
rabbitmq:
  exchange:
    notification: notification.exchange
  queues:
    notification: notification.queue
  routing-keys:
    notification: notification.#  # Wildcard to receive all events
```

## How to Use

### 1. Start the Service

```bash
cd notification-service
mvn spring-boot:run
```

The service will start on `http://localhost:8085`

### 2. Verify Service is Running

Access Swagger UI:
```
http://localhost:8085/swagger-ui.html
```

Access H2 Console:
```
http://localhost:8085/h2-console
JDBC URL: jdbc:h2:mem:notification-db
Username: sa
Password: (empty)
```

### 3. Trigger Notifications

#### Option A: Through Complete Booking Flow
1. Start all services (seat, payment, notification)
2. Use the frontend to make a booking
3. Complete payment
4. Watch the notification-service console for email logs

#### Option B: Send Test Notification via API
```bash
curl -X POST http://localhost:8085/api/notifications/test \
  -H "Content-Type: application/json" \
  -d '{
    "recipient": "test@example.com",
    "subject": "Test Email",
    "content": "This is a test notification",
    "bookingReference": "TEST123"
  }'
```

#### Option C: Publish RabbitMQ Event Manually
Use RabbitMQ Management UI at `http://localhost:15672` to publish events.

### 4. View Notifications

#### Check Console Logs
All emails are logged to the console with clear formatting.

#### Query API
```bash
# Get all notifications
curl http://localhost:8085/api/notifications

# Get notifications by recipient
curl http://localhost:8085/api/notifications/recipient/test@example.com

# Get statistics
curl http://localhost:8085/api/notifications/stats
```

#### Check Database
1. Open H2 Console: `http://localhost:8085/h2-console`
2. Run query:
```sql
SELECT * FROM NOTIFICATIONS ORDER BY CREATED_AT DESC;
```

## Email Template Customization

Templates are located in `src/main/resources/templates/email/`

To customize:
1. Edit the HTML template
2. Use Thymeleaf syntax: `th:text="${variableName}"`
3. Restart the service
4. Templates are automatically reloaded (cache disabled)

Example:
```html
<p th:text="${passengerId}">P12345</p>
<p th:text="${seatNumber}">12A</p>
```

## Transition to Production

When ready to send real emails, simply:

1. **Add SMTP Configuration** to `application.yml`:
```yaml
spring:
  mail:
    host: smtp.gmail.com
    port: 587
    username: your-email@gmail.com
    password: your-app-password
    properties:
      mail:
        smtp:
          auth: true
          starttls:
            enable: true
```

2. **Update MockEmailService** to use `JavaMailSender`:
```java
@Autowired
private JavaMailSender mailSender;

public void sendEmail(String to, String subject, String htmlContent) {
    MimeMessage message = mailSender.createMimeMessage();
    MimeMessageHelper helper = new MimeMessageHelper(message, true);
    helper.setFrom(fromEmail);
    helper.setTo(to);
    helper.setSubject(subject);
    helper.setText(htmlContent, true);
    mailSender.send(message);
}
```

3. **Set mock.enabled to false**:
```yaml
email:
  mock:
    enabled: false
```

All other code remains unchanged!

## Testing

### Run All Tests
```bash
mvn test
```

### Run Specific Test
```bash
mvn test -Dtest=NotificationServiceTest
```

### Test Coverage
Current test coverage includes:
- ✅ Service layer unit tests
- ✅ Repository integration tests
- ✅ Controller integration tests
- ✅ Event listener unit tests
- ✅ Template rendering tests

Target: 80%+ line coverage (configured in pom.xml)

## Troubleshooting

### Issue: No emails appearing in logs

**Solution**: Check the following:
1. Is the notification service running?
2. Is RabbitMQ running and accessible?
3. Are events being published to the correct exchange?
4. Check RabbitMQ Management UI for queue bindings
5. Verify logging level: `logging.level.com.skyhigh.notification: DEBUG`

### Issue: RabbitMQ connection failed

**Solution**:
1. Ensure RabbitMQ is running: `docker ps`
2. Check connection details in `application.yml`
3. Verify network connectivity to RabbitMQ

### Issue: Template not found

**Solution**:
1. Verify template exists in `src/main/resources/templates/email/`
2. Check template name matches what's in code
3. Ensure template has `.html` extension
4. Rebuild project: `mvn clean install`

## API Examples

### Get All Notifications (with pagination)
```bash
curl "http://localhost:8085/api/notifications?page=0&size=10&sortBy=createdAt&sortDir=DESC"
```

### Get Notifications by Status
```bash
curl http://localhost:8085/api/notifications/status/SENT
curl http://localhost:8085/api/notifications/status/FAILED
```

### Get Notifications by Booking Reference
```bash
curl http://localhost:8085/api/notifications/booking/SKY123456
```

### Get Statistics
```bash
curl http://localhost:8085/api/notifications/stats
```

Response:
```json
{
  "total": 25,
  "sent": 22,
  "failed": 2,
  "pending": 1
}
```

## Dependencies

All dependencies are already configured in `pom.xml`:

- Spring Boot Starter Web
- Spring Boot Starter Data JPA
- Spring Boot Starter AMQP (RabbitMQ)
- Spring Boot Starter Thymeleaf
- Spring Boot Starter Mail
- H2 Database
- Lombok
- SpringDoc OpenAPI (Swagger)
- JUnit 5 & Mockito (Testing)

## Database Schema

The `NOTIFICATIONS` table is automatically created by Hibernate with the following structure:

```sql
CREATE TABLE NOTIFICATIONS (
    ID BIGINT AUTO_INCREMENT PRIMARY KEY,
    TYPE VARCHAR(255) NOT NULL,
    CHANNEL VARCHAR(255) NOT NULL,
    RECIPIENT VARCHAR(255) NOT NULL,
    SUBJECT VARCHAR(255) NOT NULL,
    CONTENT TEXT,
    STATUS VARCHAR(255) NOT NULL,
    SENT_AT TIMESTAMP,
    CREATED_AT TIMESTAMP NOT NULL,
    UPDATED_AT TIMESTAMP,
    BOOKING_REFERENCE VARCHAR(255),
    EVENT_TYPE VARCHAR(255),
    ERROR_MESSAGE VARCHAR(255)
);
```

## Monitoring & Metrics

### Actuator Endpoints
Access at: `http://localhost:8085/actuator`

Available endpoints:
- `/actuator/health` - Service health status
- `/actuator/info` - Service information
- `/actuator/metrics` - Application metrics
- `/actuator/prometheus` - Prometheus metrics

### Logging

Logs are configured to show:
- All notification processing steps
- Email sending attempts
- RabbitMQ message consumption
- Template rendering
- Errors and exceptions

Log pattern:
```
2024-01-15 10:30:45 [main] DEBUG c.s.n.service.NotificationService - Processing seat confirmation notification for passenger: P12345
```

## Best Practices Implemented

✅ **Clean Architecture** - Separation of concerns (Controller, Service, Repository)  
✅ **Dependency Injection** - Using Spring's @Autowired and constructor injection  
✅ **Exception Handling** - Graceful error handling with try-catch blocks  
✅ **Database Transactions** - @Transactional on service methods  
✅ **Event-Driven** - Asynchronous processing via RabbitMQ  
✅ **RESTful API** - Following REST conventions  
✅ **API Documentation** - Swagger/OpenAPI integration  
✅ **Comprehensive Testing** - Unit, integration, and repository tests  
✅ **Configuration Management** - Externalized configuration  
✅ **Logging** - Structured logging with SLF4J

## Summary

This notification service implementation provides a **complete, production-ready email notification system** using a **mock approach** that's perfect for:

- ✅ Academic assignments
- ✅ Development and testing
- ✅ Demonstrations and presentations
- ✅ Learning microservices architecture

The implementation is **fully functional**, **well-tested**, and **easy to transition to production** when real email sending is needed.

**No external services required. No costs. Fully verifiable through logs and database.**

---

## Questions?

For support or questions about this implementation, check:
1. Console logs for detailed operation traces
2. H2 Console for database inspection
3. Swagger UI for API testing
4. RabbitMQ Management UI for message flow

Happy coding! ✈️
