# PaymentPulse
### Distributed Payment Reliability & Observability Platform

PaymentPulse is a backend-focused distributed payment reliability and observability platform designed to simulate payment transactions, process them through an event-driven architecture, and monitor payment system reliability.

The project focuses on exploring distributed systems concepts such as asynchronous processing, Kafka-based event streaming, idempotent consumers, failure handling, transaction persistence, and payment reliability monitoring.

---

## 1. Project Overview

Modern payment systems process thousands of transactions across multiple payment gateways, banks, merchants, and locations.

These systems must handle:
- Payment failures and gateway degradation.
- High transaction volumes.
- Delayed or duplicated events.
- Unreliable downstream services.
- Transaction processing and monitoring.

PaymentPulse simulates this environment and provides a foundation for analyzing payment reliability through a distributed backend architecture.

## 2. Architecture

PaymentPulse currently uses an event-driven transaction ingestion pipeline.

```text
                 Transaction Simulator
                         |
                         v
                Transaction Producer
                         |
                         v
                 Apache Kafka
              payment-transactions
                         |
                         v
                Kafka Consumer
                         |
                         v
             Transaction Persistence
                         |
                 +-------+-------+
                 |               |
                 v               v
          Processed Events   PostgreSQL
          (Idempotency)     (Transactions)
```

### Event Failure Handling

```text
             Kafka Consumer
                    |
                    v
             Processing Error
                    |
                    v
              Retry Mechanism
                    |
             +------+------+
             |             |
          Success        Failure
             |             |
             v             v
          Continue      Dead Letter
                        Topic (DLT)
```

The dead-letter topic currently uses Kafka's default naming convention:

`payment-transactions-dlt`

---

## 3. Technology Stack

| Component | Technology |
|---|---|
| Backend | Java 21 |
| Framework | Spring Boot 4.1.1 |
| Build Tool | Maven |
| Messaging | Apache Kafka |
| Database | PostgreSQL |
| ORM | Spring Data JPA / Hibernate |
| Event Serialization | Jackson JSON |
| Testing | JUnit 5, Mockito |
| Containerization | Docker |

---

## 4. Implemented Features

### 4.1 Transaction Simulation

A transaction simulator generates realistic payment transaction data.

Each transaction contains:
- Transaction ID
- Merchant ID
- Bank ID
- Gateway ID
- Transaction amount
- Location
- Payment method
- Transaction status
- Response code
- Processing latency
- Timestamp

**Simulation characteristics:**

- 90% successful transactions under normal conditions.
- 10% failed transactions under normal conditions.
- Random transaction amounts.
- Multiple payment gateways and banks.
- Multiple payment methods and locations.
- Time-distributed transaction generation over a 70-minute window.

Supported payment methods:
- CARD
- UPI
- NET_BANKING

### 4.2 Gateway Incident Simulation

PaymentPulse can simulate gateway degradation.

The incident simulator:
- Targets a specific gateway.
- Generates a configurable number of transactions.
- Increases the failure probability to 40%.
- Introduces severe transaction latency.
- Generates incident-related response codes.

This helps simulate real-world gateway reliability problems.

### 4.3 Event-Driven Transaction Processing

Transactions are published to Apache Kafka instead of being written directly to PostgreSQL by the simulator.

The processing pipeline consists of:

1. Transaction generation.
2. Event creation.
3. Kafka event publishing.
4. Kafka event consumption.
5. Transaction persistence.

Each event receives a unique UUID-based event ID.

Kafka messages use the gateway ID as the message key, allowing transactions belonging to the same gateway to be routed to the same Kafka partition.

**Kafka topic:** `payment-transactions`

**Partitions:** 3

### 4.4 Idempotent Event Processing

PaymentPulse implements event-level idempotency to prevent duplicate transaction persistence.

A separate `processed_events` table tracks processed event IDs.

Before persisting a transaction, the persistence service attempts to claim its event ID using:

```sql
INSERT INTO processed_events (event_id, processed_at)
VALUES (:eventId, :processedAt)
ON CONFLICT (event_id) DO NOTHING;
```

If the event ID has already been processed, the event is skipped.

This protects the database against duplicate event delivery and processing attempts.

### 4.5 Retry Mechanism

Kafka consumer failures are handled through Spring Kafka's `DefaultErrorHandler`.

Current retry configuration:

```java
FixedBackOff backOff = new FixedBackOff(1000L, 3L);
```

This provides:
- One initial processing attempt.
- Three additional retry attempts.
- A one-second delay between retries.

### 4.6 Dead Letter Topic (DLT)

Events that continue failing after the configured retries are published to a Dead Letter Topic.

Current configuration:
- Original topic: `payment-transactions`
- Dead Letter Topic: `payment-transactions-dlt`
- Retry attempts: 3
- Retry interval: 1 second

Dead-letter publishing has been tested successfully by deliberately introducing a processing failure.

### 4.7 PostgreSQL Persistence

Processed transactions are stored in PostgreSQL using Spring Data JPA and Hibernate.

The database currently includes:

**transactions**
- Stores transaction information.
- Contains payment, gateway, merchant, bank, status, response code, latency, and timestamp fields.

**processed_events**
- Stores processed event IDs.
- Maintains processing timestamps.
- Enforces event ID uniqueness.

---

## 5. Reliability and Observability Features

The project also includes an initial reliability monitoring and incident analysis foundation.

Implemented components include:

| Component | Responsibility |
|---|---|
| MetricsService | Transaction metrics |
| ReliabilityBaselineService | Historical reliability baselines |
| AnomalyDetectionService | Detects unusual payment behavior |
| GatewayReliabilityService | Gateway-level reliability analysis |
| IncidentService | Incident evaluation and lifecycle |
| FailureCorrelationService | Failure correlation and RCA evidence |

### Metrics

The platform includes transaction metrics such as:
- Total transactions.
- Successful transactions.
- Failed transactions.
- Success rate.
- Failure rate.
- Average latency.
- TPS.
- Latency percentiles.

### Anomaly Detection

Historical baselines are used to evaluate payment system behavior and identify abnormal changes in failure rates and latency.

### Gateway Reliability

Payment gateway performance can be evaluated using historical transaction behavior.

### Incident Management

The incident engine supports incident evaluation, active incident retrieval, incident history, and RCA evidence.

---

## 6. API Endpoints

The following endpoints are currently implemented.

| Method | Endpoint | Description |
|---|---|---|
| GET | `/api/metrics` | Overall transaction metrics |
| GET | `/api/metrics/window?minutes=10` | Time-window metrics |
| GET | `/api/metrics/anomaly?windowMinutes=10&windows=6` | Anomaly detection |
| GET | `/api/gateways/reliability?windowMinutes=10&windows=6` | Gateway reliability |
| GET | `/api/incidents/active` | Retrieve active incidents |
| GET | `/api/incidents` | Retrieve incidents |
| GET | `/api/incidents/{id}/rca-history` | Retrieve RCA history |
| GET | `/api/reliability/correlation?windowMinutes=10` | Failure correlation |
| GET | `/api/incidents/evaluate?windowMinutes=10&windows=6` | Evaluate incidents |
| POST | `/api/simulator/generate?count=10` | Generate transactions |
| POST | `/api/simulator/incident?gatewayId=G03&count=100` | Simulate gateway degradation |
| POST | `/api/kafka/test` | Publish a test Kafka event |

---

## 7. Project Structure

```text
com.paymentpulse.paymentpulse
│
├── config
│   └── KafkaErrorHandlerConfig
│
├── controller
│   ├── IncidentController
│   ├── KafkaTestController
│   ├── MetricsController
│   └── TransactionController
│
├── event
│   └── TransactionCreatedEvent
│
├── model
│   ├── Incident
│   ├── IncidentRcaEvidence
│   ├── ProcessedEvent
│   └── Transaction
│
├── repository
│   ├── IncidentRcaEvidenceRepository
│   ├── IncidentRepository
│   ├── ProcessedEventRepository
│   └── TransactionRepository
│
├── service
│   ├── AnomalyDetectionService
│   ├── FailureCorrelationService
│   ├── GatewayReliabilityService
│   ├── IncidentService
│   ├── MetricsService
│   ├── ReliabilityBaselineService
│   ├── TransactionConsumerService
│   ├── TransactionPersistenceService
│   ├── TransactionProducerService
│   └── TransactionService
│
├── simulator
│   ├── SimulatorController
│   └── TransactionSimulator
│
└── PaymentpulseApplication
```

---

## 8. Running the Project Locally

### Prerequisites

Install the following:

- JDK 21
- Maven
- PostgreSQL
- Docker Desktop

### Step 1: Clone the repository

```bash
git clone https://github.com/abhyudayagnihotri/PaymentPulse
cd PaymentPulse
```

### Step 2: Configure PostgreSQL

Create a PostgreSQL database:

```sql
CREATE DATABASE paymentpulse;
```

Configure your database credentials in `application.properties`.

### Step 3: Start Kafka

Start Docker Desktop and run your Kafka container.

The application currently expects Kafka at:

```properties
spring.kafka.bootstrap-servers=localhost:9092
```

The Kafka topic used by the application is:

```text
payment-transactions
```

### Step 4: Configure Spring Boot

The application currently runs on port 8081.

Relevant configuration:

```properties
server.port=8081

spring.kafka.bootstrap-servers=localhost:9092

spring.kafka.consumer.group-id=paymentpulse-group
spring.kafka.consumer.auto-offset-reset=earliest
```

Configure your PostgreSQL connection and remaining Kafka serializer/deserializer properties before starting the application.

### Step 5: Build the project

Windows:

```powershell
.\mvnw.cmd clean install
```

### Step 6: Run the application

```powershell
.\mvnw.cmd spring-boot:run
```

The application will be available at:

```text
http://localhost:8081
```

---

## 9. Verifying Kafka Processing

Generate 10 transactions:

```http
POST http://localhost:8081/api/simulator/generate?count=10
```

Expected response:

```json
{
  "message": "Transaction generated successfully",
  "generated": 10
}
```

Check the Kafka consumer group:

```bash
docker exec -it paymentpulse-kafka \
/opt/kafka/bin/kafka-consumer-groups.sh \
--bootstrap-server localhost:9092 \
--describe \
--group paymentpulse-group
```

Inspect persisted transactions:

```sql
SELECT *
FROM transactions
ORDER BY id DESC
LIMIT 10;
```

Inspect processed events:

```sql
SELECT *
FROM processed_events
ORDER BY processed_at DESC
LIMIT 10;
```

Inspect the Dead Letter Topic:

```bash
docker exec -it paymentpulse-kafka \
/opt/kafka/bin/kafka-console-consumer.sh \
--bootstrap-server localhost:9092 \
--topic payment-transactions-dlt \
--from-beginning
```

---

## 10. Current Implementation Status

| Feature | Status |
|---|---|
| Transaction generation | Completed |
| Gateway incident simulation | Completed |
| PostgreSQL persistence | Completed |
| Transaction metrics | Implemented |
| Historical reliability baselines | Implemented |
| Anomaly detection | Implemented |
| Gateway reliability analysis | Implemented |
| Incident management | Implemented |
| Failure correlation and RCA evidence | Implemented |
| Kafka producer | Completed |
| Kafka consumer | Completed |
| JSON event serialization | Completed |
| Gateway-based partitioning | Completed |
| Idempotent event processing | Completed |
| Retry mechanism | Completed |
| Dead Letter Topic | Tested |
| Event-driven simulator integration | Completed |

---

## 11. Future Roadmap

The next development milestones are:

### Phase 1: Performance and Load Testing
- Generate 1,000, 5,000, and 10,000 transaction workloads.
- Measure transaction throughput.
- Measure average and P95 processing latency.
- Monitor Kafka consumer lag.
- Evaluate database persistence performance.

### Phase 2: Observability Infrastructure
- Integrate Prometheus.
- Build Grafana dashboards.
- Monitor Kafka producer and consumer metrics.
- Track transaction processing performance.
- Monitor application health.

### Phase 3: Distributed Reliability Improvements
- Improve failure detection.
- Add intelligent payment gateway routing.
- Introduce automated recovery and failover.
- Improve incident correlation and root cause analysis.

### Phase 4: Deployment and Documentation
- Dockerize the application.
- Develop a complete deployment setup.
- Add architecture and performance documentation.
- Conduct load and failure testing.

---

## 12. Project Objective

PaymentPulse aims to demonstrate practical understanding of backend engineering and distributed systems through a simulated payment processing environment.

The primary engineering focus areas are:

- Event-driven architecture.
- Distributed messaging.
- Idempotent processing.
- Failure handling and recovery.
- Transaction reliability.
- Performance monitoring.
- Observability and incident management.

**PaymentPulse — Building reliability into distributed payment systems.**