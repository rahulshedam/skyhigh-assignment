# Product Requirements Document (PRD)
# SkyHigh Core – Digital Check-In System

## 1. Overview

### 1.1 Problem Statement

SkyHigh Airlines faces significant challenges during peak check-in hours when hundreds of passengers simultaneously attempt to:
- Browse and select seats
- Add baggage and pay associated fees
- Complete their check-in process

Current systems struggle with seat conflicts, inconsistent state management, and poor performance under concurrent load. The airline needs a robust, scalable backend service that ensures data consistency, prevents double-bookings, and maintains high performance during traffic spikes.

### 1.2 Product Vision

Build **SkyHigh Core**, a high-performance backend service that provides a seamless, conflict-free digital check-in experience capable of handling hundreds of concurrent users during peak hours while maintaining data integrity and system reliability.

## 2. Goals and Objectives

### 2.1 Primary Goals

1. **Zero Seat Conflicts**: Guarantee that no two passengers can book the same seat simultaneously, regardless of concurrent request volume
2. **High Performance**: Deliver seat map data within P95 < 1 second during peak usage
3. **Automated Lifecycle Management**: Handle time-bound seat reservations (120 seconds) automatically without manual intervention
4. **Business Rule Enforcement**: Validate baggage limits and pause check-in for payment when rules are violated
5. **System Protection**: Detect and prevent abusive access patterns and bot attacks

### 2.2 Success Criteria

- **0% double-booking** rate under concurrent load testing
- **P95 latency < 1 second** for seat map retrieval
- **100% automatic expiry** of held seats after 120 seconds
- **Successful waitlist assignment** within 5 seconds of seat availability
- **Rate limit enforcement** triggering within 2 seconds of abuse detection

## 3. Key Users and Stakeholders

### 3.1 Primary Users

| User Type | Description | Key Needs |
|-----------|-------------|-----------|
| **Passengers** | Airline customers checking in for flights | Fast seat selection, clear status updates, reliable booking |
| **System Administrators** | Technical staff managing the platform | Monitoring tools, abuse detection alerts, system health metrics |
| **Airport Staff** | Ground crew assisting passengers | Override capabilities, passenger status visibility |

### 3.2 Stakeholders

- **SkyHigh Airlines Business Team**: Revenue optimization, customer satisfaction
- **Operations Team**: System reliability, incident management
- **Compliance Team**: Data integrity, audit trails

## 4. Functional Requirements

### 4.1 Seat Lifecycle Management

#### 4.1.1 Seat States

The system must support the following seat state transitions:

**State Definitions:**
- **AVAILABLE**: Seat is open and can be selected by any passenger
- **HELD**: Seat is temporarily reserved for a specific passenger (120-second timer active)
- **CONFIRMED**: Seat is permanently assigned to a passenger after successful check-in
- **CANCELLED**: Previously confirmed seat released back to available pool

#### 4.1.2 State Transition Rules

| Current State | Allowed Transition | Trigger |
|---------------|-------------------|---------|
| AVAILABLE | → HELD | Passenger selects seat |
| HELD | → CONFIRMED | Passenger completes check-in within 120s |
| HELD | → AVAILABLE | 120-second timer expires OR passenger abandons |
| CONFIRMED | → CANCELLED | Passenger cancels check-in |
| CANCELLED | → AVAILABLE | System processes cancellation |

#### 4.1.3 Business Rules

- **BR-001**: A seat can transition to HELD only if current state is AVAILABLE
- **BR-002**: A HELD seat is exclusive to exactly one passenger
- **BR-003**: Only CONFIRMED seats can be CANCELLED
- **BR-004**: State history must be maintained for audit purposes

### 4.2 Time-Bound Seat Hold

#### 4.2.1 Hold Duration

- **Timer Duration**: Exactly 120 seconds (2 minutes)
- **Timer Start**: Moment seat transitions to HELD state
- **Timer Action**: Automatic transition to AVAILABLE on expiry

#### 4.2.2 Requirements

- **REQ-001**: System must prevent other passengers from reserving/confirming a HELD seat
- **REQ-002**: Timer must work reliably under high concurrent load
- **REQ-003**: No manual intervention required for timer expiry
- **REQ-004**: Passenger should receive countdown/warning notifications (optional enhancement)

### 4.3 Conflict-Free Seat Assignment

#### 4.3.1 Core Guarantee

**Critical Requirement**: When multiple passengers attempt to reserve the same seat simultaneously, exactly ONE reservation must succeed; all others must fail gracefully.

#### 4.3.2 Technical Requirements

- **REQ-005**: Implement optimistic or pessimistic locking mechanism
- **REQ-006**: Use database-level constraints to prevent duplicate assignments
- **REQ-007**: Maintain consistency under race conditions
- **REQ-008**: Provide clear error messages to unsuccessful passengers
- **REQ-009**: Suggest alternative available seats on conflict

### 4.4 Check-In Cancellation

#### 4.4.1 Requirements

- **REQ-010**: Passengers can cancel CONFIRMED check-ins before flight departure
- **REQ-011**: Cancelled seats transition immediately to AVAILABLE or trigger waitlist assignment
- **REQ-012**: Cancellation must be atomic (all-or-nothing operation)
- **REQ-013**: Generate cancellation confirmation for passenger

### 4.5 Waitlist Management

#### 4.5.1 Waitlist Behavior

- **REQ-014**: Allow passengers to join waitlist when desired seat is unavailable
- **REQ-015**: Implement FIFO (First-In-First-Out) waitlist queue per seat
- **REQ-016**: Automatically assign seat to next waitlisted passenger when it becomes AVAILABLE
- **REQ-017**: Notify waitlisted passenger immediately upon seat assignment
- **REQ-018**: Handle waitlist expiry if passenger doesn't respond within timeframe

#### 4.5.2 Priority Rules

- Waitlist assignment order: Registration timestamp (earliest first)
- Passengers can be on multiple waitlists simultaneously
- Assigned passenger has limited time to accept (e.g., 60 seconds)

### 4.6 Baggage Validation and Payment Flow

#### 4.6.1 Baggage Rules

- **Maximum Free Baggage Weight**: 25kg
- **Overage**: Any baggage > 25kg requires additional payment

#### 4.6.2 Check-In States

The system must clearly distinguish between:

1. **In Progress**: Passenger is actively checking in
2. **Waiting for Payment**: Baggage weight exceeded; pending payment completion
3. **Completed**: Check-in successfully finished

#### 4.6.3 Integration Requirements

- **REQ-019**: Integrate with Weight Service to validate baggage weight
- **REQ-020**: Integrate with Payment Service for fee processing
- **REQ-021**: Pause check-in flow when payment is required
- **REQ-022**: Resume check-in automatically upon successful payment
- **REQ-023**: Handle payment failures gracefully (release seat hold, notify passenger)

#### 4.6.4 Flow
    1. Passenger enters baggage details
    2. System calls Weight Service
    3. IF weight > 25kg:
        * Calculate additional fee
        * Pause check-in (seat remains HELD)
        * Invoke Payment Service
        * IF payment successful: Resume check-in
        * IF payment fails: Release seat, notify passenger
    4. ELSE: Continue check-in
    
### 4.7 High-Performance Seat Map Access

#### 4.7.1 Performance Requirements

- **REQ-024**: P95 latency < 1 second for seat map retrieval during peak hours
- **REQ-025**: Support 500+ concurrent seat map requests
- **REQ-026**: Seat availability data must be near real-time (< 2 seconds staleness)

#### 4.7.2 Optimization Strategies

- Implement caching layer for seat map data
- Use database indexing on frequently queried fields
- Consider read replicas for seat map queries
- Optimize query patterns to minimize database load

### 4.8 Abuse and Bot Detection

#### 4.8.1 Abuse Scenario

**Trigger**: Single source (IP address/user ID) accessing 50 different seat maps within 2 seconds

#### 4.8.2 Requirements

- **REQ-027**: Implement rate limiting per user/IP
- **REQ-028**: Detect rapid seat map access patterns (threshold: 50 maps in 2 seconds)
- **REQ-029**: Temporarily block/throttle detected abusive sources
- **REQ-030**: Log all abuse detection events for audit and analysis
- **REQ-031**: Provide mechanism to whitelist legitimate high-frequency users

#### 4.8.3 Actions on Detection

1. **Immediate**: Return 429 (Too Many Requests) status
2. **Short-term**: Apply temporary block (e.g., 5-15 minutes)
3. **Logging**: Record event with timestamp, source, and access pattern
4. **Notification**: Alert system administrators for review

## 5. Non-Functional Requirements (NFRs)

### 5.1 Performance

| Metric | Requirement |
|--------|-------------|
| Seat Hold API Response Time | P95 < 500ms |
| Seat Map Retrieval | P95 < 1 second |
| Check-in Completion | P95 < 2 seconds |
| Concurrent Users | Support 500+ simultaneous users |
| Throughput | Handle 100+ seat bookings per second |

### 5.2 Reliability

- **Availability**: 99.9% uptime during operational hours
- **Data Consistency**: Zero tolerance for double-bookings
- **Automatic Recovery**: Expired seat holds must auto-recover without manual intervention
- **Fault Tolerance**: Graceful degradation if external services (Payment/Weight) are unavailable

### 5.3 Scalability

- **Horizontal Scaling**: System must support adding more application instances
- **Database Scaling**: Design schema to support read replicas
- **Peak Load**: Handle 5x normal traffic during peak check-in windows

### 5.4 Security

- **Authentication**: Secure passenger identity verification
- **Authorization**: Role-based access control (Passenger, Admin, Staff)
- **Data Protection**: Encrypt sensitive data at rest and in transit
- **Rate Limiting**: Protect against DDoS and abuse

### 5.5 Observability

- **Logging**: Comprehensive structured logging for all critical operations
- **Metrics**: Real-time metrics for:
  - Seat assignment success/failure rates
  - API response times
  - Rate limiting triggers
  - Payment success/failure rates
- **Tracing**: Distributed tracing for end-to-end request flow
- **Alerting**: Automated alerts for anomalies and failures

### 5.6 Maintainability

- **Code Quality**: Minimum 80% test coverage
- **Documentation**: Comprehensive API documentation
- **Deployment**: Automated CI/CD pipeline
- **Monitoring**: Health check endpoints for all services

### 5.7 Data Integrity

- **Audit Trail**: Complete history of seat state transitions
- **Idempotency**: API operations must be idempotent where applicable
- **Transaction Management**: ACID compliance for critical operations
- **Backup & Recovery**: Regular automated backups with tested recovery procedures

## 6. Constraints and Assumptions

### 6.1 Technical Constraints

- Must integrate with existing Weight Service (external)
- Must integrate with Payment Service (can be mocked/simulated)
- All components must be containerized (Docker)
- Must provide single docker-compose.yml for deployment

### 6.2 Assumptions

- Passengers have valid booking references before check-in
- Flight seat maps are pre-configured in the system
- Each flight has a fixed check-in window (e.g., 24 hours before departure)
- Notification delivery (email/SMS) is handled by external service

## 7. Success Metrics

### 7.1 Key Performance Indicators (KPIs)

| KPI | Target | Measurement Method |
|-----|--------|-------------------|
| Double-booking Rate | 0% | Automated conflict detection tests |
| Seat Map Load Time (P95) | < 1 second | APM monitoring |
| Successful Check-ins | > 95% | Success rate tracking |
| Seat Hold Expiry Accuracy | 100% | Timer accuracy tests |
| Rate Limit Triggers | < 1% of users | Abuse detection logs |
| System Uptime | 99.9% | Health check monitoring |

### 7.2 Business Metrics

- Customer satisfaction score for check-in experience
- Reduction in airport counter check-ins
- Peak-hour system performance stability

## 8. Out of Scope (v1.0)

The following features are explicitly out of scope for the initial release:

- Payment gateway integration (use mock/simulation)
- Email/SMS notification delivery (assume external service)
- Multi-language support
- Mobile app development (backend only)
- Seat upgrade recommendations
- Meal preferences and special requests
- Integration with existing airline reservation systems
- Passenger profile management

## 9. Dependencies

### 9.1 External Services

- **Weight Service**: Baggage weight validation
- **Payment Service**: Fee processing (can be simulated)
- **Notification Service**: Passenger alerts (assumed available)

### 9.2 Infrastructure

- Container orchestration platform (Docker Compose)
- Relational database (PostgreSQL/MySQL)
- Caching layer (Redis recommended)
- Background job processor for timer management

## 10. Acceptance Criteria

The system will be considered complete when:

1. ✅ All seat state transitions work correctly under concurrent load
2. ✅ Zero double-booking incidents in load testing (1000+ concurrent users)
3. ✅ Seat hold timer automatically expires after exactly 120 seconds
4. ✅ P95 latency for seat map retrieval < 1 second
5. ✅ Waitlist assignment triggers within 5 seconds of seat availability
6. ✅ Baggage validation and payment flow prevents check-in completion for unpaid overages
7. ✅ Rate limiting blocks sources accessing 50+ seat maps in 2 seconds
8. ✅ Unit test coverage > 80%
9. ✅ All components run successfully via docker-compose.yml
10. ✅ Complete API documentation provided

## 11. Risks and Mitigation

| Risk | Impact | Mitigation Strategy |
|------|--------|-------------------|
| Database deadlocks under high concurrency | High | Implement optimistic locking, retry logic, proper indexing |
| Timer service failure causing held seats to never expire | High | Redundant timer mechanisms, health monitoring, automatic recovery |
| External service (Payment/Weight) downtime | Medium | Implement circuit breakers, fallback mechanisms, timeouts |
| Cache invalidation issues causing stale seat data | Medium | Use cache-aside pattern with TTL, event-driven invalidation |
| Rate limiting blocking legitimate users | Low | Configurable thresholds, whitelist mechanism, gradual enforcement |

---

**Document Version**: 1.0  
**Last Updated**: February 7, 2026  
**Owner**: Development Team  
**Stakeholders**: SkyHigh Airlines Product, Engineering, and Operations Teams