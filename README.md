# SkyHigh Core – Digital Check-In System

A high-performance, microservices-based digital check-in platform for SkyHigh Airlines. The system handles seat selection, baggage validation, payment processing, and notifications with conflict-free seat assignment, distributed locking, and event-driven architecture. *This run uses open endpoints for demo; production would require JWT/RBAC and threat hardening (see ARCHITECTURE.md).*

## Table of Contents

- [Overview](#overview)
- [Architecture](#architecture)
- [Prerequisites](#prerequisites)
- [Infrastructure Setup](#infrastructure-setup)
  - [Option A: Docker Compose (Recommended)](#option-a-docker-compose-recommended)
  - [Option B: Manual Setup](#option-b-manual-setup)
    - [Redis](#redis)
    - [RabbitMQ](#rabbitmq)
    - [Database (H2)](#database-h2)
- [Running the Application](#running-the-application)
  - [Quick Start with Docker Compose](#quick-start-with-docker-compose)
  - [Local Development (Manual)](#local-development-manual)
- [Background Workers](#background-workers)
- [Service Endpoints](#service-endpoints)
- [Health Checks](#health-checks)
- [Troubleshooting](#troubleshooting)

---

## Overview

| Component | Technology |
|-----------|------------|
| **Backend** | Java 21, Spring Boot 3.5 |
| **Frontend** | React 18, TypeScript, Vite, Material-UI |
| **Database** | H2 (in-memory / file-based per service) |
| **Cache** | Redis |
| **Message Queue** | RabbitMQ |
| **Build** | Maven 3.9+ |

### Key Features

- **Conflict-free seat assignment** – Optimistic locking, distributed locks (Redis)
- **Time-bound seat holds** – 120-second TTL with automatic expiry
- **Waitlist management** – FIFO queue with automatic assignment
- **Rate limiting** – Abuse and bot detection (50 requests / 2 seconds)
- **Event-driven notifications** – Seat, payment, and baggage events via RabbitMQ

---

## Architecture

```
┌─────────────┐     ┌──────────────────┐     ┌─────────────────┐
│   Frontend  │────▶│  Check-in Service │────▶│ Seat Management │
│  (React)    │     │   (Orchestrator)  │     │     Service     │
└─────────────┘     └──────────────────┘     └────────┬────────┘
       │                        │                      │
       │                        ├──────────────────────┼──────────┐
       │                        │                      │          │
       │                        ▼                      ▼          ▼
       │               ┌────────────────┐      ┌───────────┐  ┌──────┐
       │               │ Baggage Service│      │   Redis   │  │RabbitMQ│
       │               └────────────────┘      └───────────┘  └───┬──┘
       │                        │                                 │
       │                        ▼                                 │
       │               ┌────────────────┐                         │
       └──────────────▶│ Payment Service│◀────────────────────────┘
                       └────────────────┘
                                │
                                ▼
                       ┌────────────────────┐
                       │ Notification Svc   │
                       │ (RabbitMQ Listener)│
                       └────────────────────┘
```

---

## Prerequisites

| Tool | Version |
|------|---------|
| **Java** | 21 |
| **Maven** | 3.9+ |
| **Node.js** | 18+ |
| **Docker** | 20.10+ (for infrastructure or full Docker run) |
| **Docker Compose** | 2.0+ |

---

## Infrastructure Setup

### Option A: Docker Compose (Recommended)

Start Redis and RabbitMQ with one command:

```bash
docker compose up -d redis rabbitmq
```

This brings up:

| Service  | Port(s)       | Purpose                    |
|----------|---------------|----------------------------|
| Redis    | 6379          | Caching, distributed locks |
| RabbitMQ | 5672 (AMQP)   | Message queue              |
| RabbitMQ | 15672 (HTTP)  | Management UI              |

**RabbitMQ Management UI:** `http://localhost:15672`  
**Credentials:** `guest` / `guest`

---

### Option B: Manual Setup

#### Redis

**Install (Windows – via Chocolatey):**
```powershell
choco install redis-64
```

**Install (macOS):**
```bash
brew install redis
brew services start redis
```

**Install (Linux):**
```bash
sudo apt install redis-server   # Ubuntu/Debian
sudo systemctl start redis
```

**Verify:**
```bash
redis-cli ping
# Expected: PONG
```

**Default:** `localhost:6379`

---

#### RabbitMQ

**Install (Windows – via Chocolatey):**
```powershell
choco install rabbitmq
# Start: rabbitmq-server
```

**Install (macOS):**
```bash
brew install rabbitmq
brew services start rabbitmq
```

**Install (Linux):**
```bash
sudo apt install rabbitmq-server   # Ubuntu/Debian
sudo systemctl start rabbitmq-server
```

**Enable Management Plugin (recommended):**
```bash
rabbitmq-plugins enable rabbitmq_management
```

**Default connection:** `localhost:5672`  
**Management UI:** `http://localhost:15672`  
**Credentials:** `guest` / `guest`

---

#### Database (H2)

Each service uses H2:

- **Seat Management:** file `./data/seat-management-db` (or in-memory in Docker)
- **Check-in:** file `./data/checkin-service/checkin-db`
- **Baggage:** file `./data/baggage-service/baggage-db`
- **Payment:** file `./data/payment-db`
- **Notification:** in-memory `notification-db`

H2 starts automatically with each service. No manual DB setup is required.

---

## Running the Application

### Quick Start with Docker Compose

From the `skyhigh-assignment` directory:

```bash
docker compose up -d
```

This starts:

1. Redis  
2. RabbitMQ  
3. Seat Management Service (8091)  
4. Check-in Service (8082)  
5. Baggage Service (8083)  
6. Payment Service (8084)  
7. Notification Service (8085)  
8. Frontend (3000)  

**Access the app:** `http://localhost:3000`

---

### Local Development (Manual)

**1. Start infrastructure**

```bash
docker compose up -d redis rabbitmq
```

**2. Build the common module**

```bash
cd common
mvn clean install
cd ..
```

**3. Start backend services** (in separate terminals, from project root)

```bash
# Seat Management Service
cd seat-management-service
mvn spring-boot:run

# Check-in Service (new terminal)
cd checkin-service
mvn spring-boot:run

# Baggage Service (new terminal)
cd baggage-service
mvn spring-boot:run

# Payment Service (new terminal)
cd payment-service
mvn spring-boot:run

# Notification Service (new terminal)
cd notification-service
mvn spring-boot:run
```

**4. Start the frontend**

```bash
cd frontend
npm install
npm run dev
```

**Access:** `http://localhost:3000`

For local runs, ensure Redis and RabbitMQ use `localhost:6379` and `localhost:5672`.

---

## Background Workers

Background processing is built into the services; there are no separate worker processes.

### Seat Management Service

| Worker | Type | Interval | Description |
|--------|------|----------|-------------|
| **Seat Expiry Scheduler** | `@Scheduled` | 10 seconds | Releases held seats that have exceeded 120 seconds |
| **Waitlist Processor Scheduler** | `@Scheduled` | 5 seconds | Assigns freed seats to the next waitlisted passengers (FIFO) |

These run automatically when the Seat Management Service is up. **Seat hold expiry – fail-safe:** Release is guaranteed by (1) the scheduler batch run and (2) a confirm-time check: if a client tries to confirm after the hold TTL, the request is rejected (409) even if the scheduler run was delayed.

### Notification Service

| Worker | Type | Description |
|--------|------|-------------|
| **Seat Event Listener** | RabbitMQ consumer | Handles SeatHeld, SeatConfirmed, SeatReleased, WaitlistAssigned |
| **Payment Event Listener** | RabbitMQ consumer | Handles PaymentSuccess, PaymentFailure |

These run as part of the Notification Service; no extra process is needed.

**Requirement:** RabbitMQ must be running for the notification listeners to work.

---

## Configuration and Operation of Advanced Features

### Waitlist

- **Join:** `POST /api/seats/{seatId}/waitlist` with `passengerId`, `bookingReference`. Assignment is automatic when the seat becomes available (released or expired).
- **Status:** `GET /api/seats/waitlist/{passengerId}` returns all waitlist entries for the passenger.
- **Remove:** `DELETE /api/seats/waitlist/{waitlistId}`.
- Scheduler interval: WaitlistProcessorScheduler runs every 5 seconds (configurable via Spring scheduling if needed).

### Abuse detection / rate limiting

- **Where:** Seat Management Service only (all `/api/seats/*` and related endpoints).
- **Limit:** 50 requests per 2 seconds per IP (Bucket4j). Response: HTTP 429 Too Many Requests.
- **Observation:** Rate-limit violations are logged (WARN); audit records can be written to `rate_limit_audit` (see ARCHITECTURE.md). Supports abuse/bot detection analysis.

### Logging and observability

- **Logged:** Rate-limit violations, seat state changes (seat_history), check-in state transitions (checkin_history). See ARCHITECTURE.md for audit tables and emitted events (seat.held, seat.confirmed, seat.released, waitlist.assigned, payment.success/failure, checkin.completed).

---

## Service Endpoints

| Service | Port | Base URL | Swagger UI |
|---------|------|----------|------------|
| Seat Management | 8091 | `http://localhost:8091` | `/swagger-ui.html` |
| Check-in | 8082 | `http://localhost:8082` | `/swagger-ui.html` |
| Baggage | 8083 | `http://localhost:8083` | `/swagger-ui.html` |
| Payment | 8084 | `http://localhost:8084` | `/swagger-ui.html` |
| Notification | 8085 | `http://localhost:8085` | `/swagger-ui.html` |
| Frontend | 3000 | `http://localhost:3000` | — |

### H2 Console (per service)

- **Path:** `/h2-console`
- **JDBC URL:** Shown in service logs (e.g. `jdbc:h2:mem:notification-db`)
- **Username:** `sa`
- **Password:** *(empty)*

---

## Health Checks

```bash
curl http://localhost:8091/actuator/health   # Seat Management
curl http://localhost:8082/actuator/health   # Check-in
curl http://localhost:8083/actuator/health   # Baggage
curl http://localhost:8084/actuator/health   # Payment
curl http://localhost:8085/actuator/health   # Notification
```

---

## Troubleshooting

### Redis connection refused

- Ensure Redis is running: `docker ps` or `redis-cli ping`
- Confirm `spring.data.redis.host` is `localhost` (or `redis` in Docker)

### RabbitMQ connection failed

- Ensure RabbitMQ is running: `docker ps` or visit `http://localhost:15672`
- Default: host `localhost`, port `5672`, user `guest`, password `guest`

### Seat Management fails to start

- Redis and RabbitMQ must be available
- In Docker, wait for Redis and RabbitMQ health checks before starting dependent services

### Check-in service can't reach other services

- For local runs, ensure Seat Management (8091), Baggage (8083), and Payment (8084) are up
- In Docker Compose, service URLs use hostnames: `seat-management`, `baggage-service`, `payment-service`

### Notification service not receiving events

- Confirm RabbitMQ is running
- Check RabbitMQ Management UI: queues and bindings for `notification.queue`
- Ensure exchanges/queues are created on startup (they are configured in the service)

### Port already in use

Use the following ports and stop any conflicting processes:

- 3000 (Frontend)
- 5672 (RabbitMQ AMQP)
- 6379 (Redis)
- 8082–8085 (Backend services)
- 8091 (Seat Management)
- 15672 (RabbitMQ Management)

---

## Project Structure

```
skyhigh-assignment/
├── common/                 # Shared DTOs, exception handling
├── seat-management-service/   # Seats, waitlist, expiry
├── checkin-service/          # Check-in orchestration
├── baggage-service/          # Baggage validation
├── payment-service/          # Payment (simulated)
├── notification-service/     # Event listeners, mock email
├── frontend/                 # React + Vite + TypeScript
├── docker-compose.yml
├── PRD.md
├── PROJECT_STRUCTURE.md
└── ARCHITECTURE.md
```

---

## Additional Documentation

- [PRD.md](PRD.md) – Product requirements
- [PROJECT_STRUCTURE.md](PROJECT_STRUCTURE.md) – Code layout
- [ARCHITECTURE.md](ARCHITECTURE.md) – Technical design
- [notification-service/NOTIFICATION_SERVICE_README.md](notification-service/NOTIFICATION_SERVICE_README.md) – Notification details
