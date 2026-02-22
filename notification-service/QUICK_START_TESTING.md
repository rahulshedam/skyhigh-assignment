# Quick Start Guide - Notification Service Testing

## 🚀 Quick Start (5 Minutes)

### Step 1: Start Required Services
```bash
# Start RabbitMQ (if not already running)
docker-compose up -d rabbitmq

# Start Notification Service
cd notification-service
mvn spring-boot:run
```

Wait for: `Started NotificationServiceApplication in X seconds`

### Step 2: Verify Service Health
Open in browser:
- Service: http://localhost:8085/api/notifications/hello
- Swagger UI: http://localhost:8085/swagger-ui.html
- H2 Console: http://localhost:8085/h2-console

### Step 3: Send Test Email
```bash
curl -X POST http://localhost:8085/api/notifications/test \
  -H "Content-Type: application/json" \
  -d '{
    "recipient": "passenger@example.com",
    "subject": "Test Flight Booking",
    "content": "Your test booking has been confirmed!",
    "bookingReference": "TEST001"
  }'
```

### Step 4: Check Results

**Console Output** - You should see:
```
================================================================================
📧 MOCK EMAIL SENT
================================================================================
From: noreply@skyhigh-airlines.com
To: passenger@example.com
Subject: Test Flight Booking
...
```

**Database Check**:
1. Go to http://localhost:8085/h2-console
2. Connect with:
   - JDBC URL: `jdbc:h2:mem:notification-db`
   - Username: `sa`
   - Password: (leave empty)
3. Run query:
```sql
SELECT * FROM NOTIFICATIONS ORDER BY CREATED_AT DESC;
```

**API Check**:
```bash
# Get all notifications
curl http://localhost:8085/api/notifications

# Get statistics
curl http://localhost:8085/api/notifications/stats
```

## 🧪 Testing Complete Flow

### Test 1: Seat Confirmation Email

Simulate a `SeatConfirmedEvent`:

1. Open RabbitMQ Management: http://localhost:15672 (guest/guest)
2. Go to "Queues" → "notification.queue"
3. Click "Publish message"
4. Set properties:
   - Headers: Add `__TypeId__` = `com.skyhigh.notification.event.SeatConfirmedEvent`
5. Paste payload:
```json
{
  "seatId": 1,
  "seatNumber": "12A",
  "flightId": 100,
  "flightNumber": "SK123",
  "passengerId": "P12345",
  "bookingReference": "SKY123456",
  "confirmedAt": "2024-01-15T10:30:00",
  "timestamp": "2024-01-15T10:30:00"
}
```
6. Click "Publish message"
7. Check console for email log

### Test 2: Payment Confirmation Email

```json
{
  "paymentReference": "PAY789012",
  "passengerId": "P12345",
  "bookingReference": "SKY123456",
  "amount": 250.00,
  "currency": "USD",
  "timestamp": "2024-01-15T10:35:00",
  "eventType": "PAYMENT_COMPLETED",
  "metadata": null
}
```

### Test 3: Payment Failure Email

```json
{
  "paymentReference": "PAY789013",
  "passengerId": "P12345",
  "bookingReference": "SKY123456",
  "amount": 250.00,
  "currency": "USD",
  "timestamp": "2024-01-15T10:40:00",
  "eventType": "PAYMENT_FAILED",
  "metadata": null
}
```

## 📊 Verify Different Scenarios

### Scenario 1: Multiple Notifications for Same Booking
```bash
# Get notifications by booking reference
curl http://localhost:8085/api/notifications/booking/SKY123456
```

Expected: Multiple notifications (seat hold, payment, confirmation)

### Scenario 2: Failed Notifications
Check failed notifications:
```bash
curl http://localhost:8085/api/notifications/status/FAILED
```

### Scenario 3: Notifications by Recipient
```bash
curl http://localhost:8085/api/notifications/recipient/p12345@passenger.skyhigh.com
```

## 🔍 Monitoring Console Output

### What to Look For:

**Successful Email:**
```
2024-01-15 10:30:45 [main] INFO  c.s.n.l.SeatEventListener - Received SeatConfirmedEvent for passenger: P12345, booking: SKY123456
2024-01-15 10:30:45 [main] INFO  c.s.n.s.NotificationService - Processing seat confirmation notification for passenger: P12345
2024-01-15 10:30:45 [main] DEBUG c.s.n.s.TemplateService - Successfully rendered template: email/seat-confirmation

================================================================================
📧 MOCK EMAIL SENT
================================================================================
From: noreply@skyhigh-airlines.com
To: p12345@passenger.skyhigh.com
Subject: SkyHigh Airlines - Seat Confirmation
...

2024-01-15 10:30:45 [main] INFO  c.s.n.s.NotificationService - Seat confirmation notification sent successfully to: p12345@passenger.skyhigh.com
```

**Failed Email:**
```
2024-01-15 10:30:45 [main] ERROR c.s.n.s.NotificationService - Failed to send seat confirmation email for passenger: P12345
java.lang.RuntimeException: Template not found
...
```

## 🧰 Useful Commands

### Check RabbitMQ Queues
```bash
# List queues
docker exec rabbitmq rabbitmqctl list_queues

# Check specific queue
docker exec rabbitmq rabbitmqctl list_queues notification.queue
```

### Check Service Logs
```bash
# Follow logs in real-time
mvn spring-boot:run | grep -E "MOCK EMAIL|Received.*Event|notification"
```

### Database Queries
```sql
-- Count notifications by status
SELECT STATUS, COUNT(*) as COUNT 
FROM NOTIFICATIONS 
GROUP BY STATUS;

-- Recent notifications
SELECT RECIPIENT, SUBJECT, STATUS, CREATED_AT 
FROM NOTIFICATIONS 
ORDER BY CREATED_AT DESC 
LIMIT 10;

-- Notifications by booking
SELECT * 
FROM NOTIFICATIONS 
WHERE BOOKING_REFERENCE = 'SKY123456' 
ORDER BY CREATED_AT;

-- Failed notifications with errors
SELECT RECIPIENT, SUBJECT, ERROR_MESSAGE, CREATED_AT 
FROM NOTIFICATIONS 
WHERE STATUS = 'FAILED';
```

## 🎯 Expected Test Results

### After Complete Flow (Seat + Payment):

**Statistics:**
```json
{
  "total": 3,
  "sent": 3,
  "failed": 0,
  "pending": 0
}
```

**Console:** 3 formatted email logs
**Database:** 3 notification records
**RabbitMQ:** Messages consumed from notification.queue

## ⚠️ Troubleshooting

### Problem: No emails in console

**Check:**
1. Service running? `curl http://localhost:8085/actuator/health`
2. RabbitMQ connection? Check logs for connection errors
3. Message format? Verify JSON payload
4. Queue binding? Check RabbitMQ UI bindings tab

### Problem: Template not found

**Fix:**
```bash
# Rebuild project
mvn clean install

# Verify template exists
ls src/main/resources/templates/email/
```

### Problem: Database empty

**Possible causes:**
- Service not receiving events
- Events failing before database save
- Check console for exceptions

## 📝 Test Checklist

- [ ] Service starts without errors
- [ ] Health check returns 200 OK
- [ ] Swagger UI accessible
- [ ] H2 Console accessible
- [ ] Test notification sends successfully
- [ ] Email appears in console logs
- [ ] Notification saved in database
- [ ] API returns notifications
- [ ] Statistics endpoint works
- [ ] RabbitMQ events processed
- [ ] Multiple events handled correctly
- [ ] Failed notifications logged properly

## 🎓 For Assignment Demo

**Demonstrate:**
1. ✅ Service startup and health check
2. ✅ Send test notification via API
3. ✅ Show formatted email in console
4. ✅ Query notification via API
5. ✅ Show database records in H2 Console
6. ✅ Send real event via RabbitMQ
7. ✅ Show statistics endpoint
8. ✅ Explain mock vs production approach

**Talking Points:**
- "No external services needed - perfect for development"
- "All emails verifiable through logs and database"
- "Production-ready architecture, just swap MockEmailService"
- "Comprehensive test coverage for reliability"
- "Event-driven, loosely coupled design"

---

## Need Help?

**Quick Debug:**
```bash
# Check if service is running
curl http://localhost:8085/actuator/health

# Check RabbitMQ
curl -u guest:guest http://localhost:15672/api/overview

# View recent logs
tail -f logs/notification-service.log
```

**Common Issues:**
1. Port 8085 in use → Change in application.yml
2. RabbitMQ not running → `docker-compose up -d rabbitmq`
3. Template errors → Check file names and paths
4. Database issues → Delete data: `DELETE FROM NOTIFICATIONS;`

🎉 **You're all set!** The notification service is ready for testing and demonstration.
