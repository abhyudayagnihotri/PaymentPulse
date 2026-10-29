# PaymentPulse — Distributed Payment Reliability & Observability Platform

PaymentPulse is a backend-focused payment reliability and observability platform built with Java, Spring Boot, and PostgreSQL. It monitors simulated payment transactions, detects reliability degradation, identifies suspicious gateway-bank combinations, and manages incidents through recovery.

The project is being developed to explore distributed systems, event-driven architecture, observability, anomaly detection, and resilient payment processing.

## Current Status

**Implemented and tested**

- Transaction ingestion and simulation
- PostgreSQL persistence using Spring Data JPA
- Global and rolling-window payment metrics
- Historical reliability baselines
- Failure-rate and latency anomaly detection
- Per-gateway reliability analysis
- Automated incident creation and updates
- Recovery tracking with three consecutive healthy evaluations
- Gateway-bank failure correlation
- Root-cause analysis (RCA) evidence and confidence levels
- Persistent RCA snapshots and incident history
- Automated unit tests using JUnit and Mockito

**Planned**

- Kafka-based event-driven transaction processing
- Improved failure correlation and root-cause analysis
- Prometheus and Grafana observability
- Docker-based deployment
- Load testing and performance benchmarking
- Intelligent gateway routing and failover

## Architecture

### Current implementation

```text
Transaction Simulator / REST API
              |
              v
     Transaction Processing
              |
              v
          PostgreSQL
              |
              v
       Metrics Service
              |
              v
    Reliability Baselines
              |
              v
    Anomaly Detection
              |
              v
      Incident Engine
              |
              v
   Gateway-Bank Correlation
              |
              v
    RCA Evidence History
```

### Planned event-driven architecture

```text
Transaction API
      |
      v
Kafka: payment-transactions
      |
      v
Transaction Consumer
      |
      v
PostgreSQL + Reliability Services
      |
      v
Incident Detection + RCA
      |
      v
Metrics and Observability
```

Kafka integration is planned and is not yet part of the current implementation.

## Key Features

### 1. Transaction ingestion and simulation

The platform generates simulated payment transactions containing fields such as:

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

The simulator supports normal traffic and simulated gateway incidents to exercise the reliability engine.

### 2. Payment metrics

The metrics service calculates:

- Total transactions
- Successful and failed transactions
- Success and failure rates
- Average latency
- Gateway, bank, merchant, location, and payment-method breakdowns
- Response-code distribution

Rolling-window metrics include:

- Transaction throughput (TPS)
- P50 latency
- P95 latency
- P99 latency
- Window-specific success and failure rates

### 3. Reliability baselines

The reliability baseline service compares current performance with historical windows.

It calculates baseline statistics for metrics such as:

- Failure rate
- P95 latency
- Throughput
- Standard deviation

These baselines provide context for identifying unusual changes in payment performance.

### 4. Anomaly detection

The anomaly detection service evaluates current payment performance against historical baselines.

It detects degradation associated with elevated failure rates and latency. Severity is assigned according to the configured anomaly rules.

### 5. Gateway reliability analysis

The gateway reliability service evaluates individual gateways using current-window metrics and historical baseline information.

It identifies gateways showing suspicious failure-rate or latency behavior and provides candidate gateways for incident investigation.

### 6. Incident lifecycle management

The incident engine supports:

- Incident creation when an anomaly is detected
- Updates to an existing active incident
- Severity and incident-type assignment
- Suspected-gateway identification
- Consecutive healthy-evaluation tracking
- Automatic resolution after three consecutive healthy evaluations
- Resolution timestamps and incident history

If an anomaly returns during recovery, the consecutive healthy-evaluation counter resets.

### 7. Gateway-bank correlation and RCA

PaymentPulse analyzes gateway-bank combinations within a rolling transaction window.

It evaluates:

- Number of transactions in each combination
- Number of failures
- Pair-level failure rate
- Overall failure rate
- Difference between pair and overall failure rates
- Minimum sample-size requirements

The system records a suspicious signal when the configured sample-size and failure-rate-gap thresholds are satisfied.

RCA confidence levels distinguish between stronger suspicious signals and less conclusive candidates. A correlation is not proof of causation.

### 8. Persistent RCA history

Each incident evaluation that enters the anomaly-handling path can save an RCA snapshot to PostgreSQL.

Snapshots preserve information such as:

- Incident ID
- Evaluation timestamp
- Suspected gateway and bank
- Pair and overall failure rates
- Failure-rate gap
- Transaction and failure counts
- Confidence level
- Evidence summary

The RCA history endpoint returns snapshots in descending evaluation-time order.

## Technology Stack

| Component | Technology |
|---|---|
| Language | Java 21 |
| Backend framework | Spring Boot 4.1.1 |
| REST API | Spring Web MVC |
| Persistence | Spring Data JPA / Hibernate |
| Database | PostgreSQL |
| Build tool | Maven |
| Unit testing | JUnit 6 and Mockito |
| Planned event streaming | Apache Kafka |
| Planned observability | Prometheus and Grafana |
| Planned containerization | Docker |

## API Endpoints

The following endpoints are implemented in the current application.

| Method | Endpoint | Purpose |
|---|---|---|
| GET | `/api/metrics` | Retrieve aggregate payment metrics |
| GET | `/api/metrics/window?minutes=10` | Retrieve rolling-window metrics |
| GET | `/api/metrics/anomaly?windowMinutes=10&windows=6` | Evaluate anomalies |
| GET | `/api/gateways/reliability?windowMinutes=10&windows=6` | Analyze gateway reliability |
| GET | `/api/reliability/correlation?windowMinutes=10` | Analyze gateway-bank correlations |
| POST | `/api/incidents/evaluate?windowMinutes=10&windows=6` | Evaluate and update incident state |
| GET | `/api/incidents/active` | Retrieve the active incident |
| GET | `/api/incidents` | Retrieve incident history |
| GET | `/api/incidents/{incidentId}/rca-history` | Retrieve RCA snapshots for an incident |
| POST | `/api/simulator/generate?count=200` | Generate normal simulated transactions |
| POST | `/api/simulator/incident?gatewayId=G03&count=100` | Simulate gateway degradation |

The endpoints use the application's configured port, which defaults to `8080` unless overridden.

## Prerequisites

- JDK 21
- IntelliJ IDEA or another Java IDE
- PostgreSQL
- Maven, or the Maven Wrapper included in the repository

## Local Setup

### 1. Clone the repository

```bash
git clone <YOUR_REPOSITORY_URL>
cd PaymentPulse
```

Replace `<YOUR_REPOSITORY_URL>` with the actual Git repository URL.

### 2. Configure PostgreSQL

Create a database named `paymentpulse`.

Configure `src/main/resources/application.properties` with your local database credentials:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/paymentpulse
spring.datasource.username=postgres
spring.datasource.password=${DB_PASSWORD}
spring.datasource.driver-class-name=org.postgresql.Driver

spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
```

Set the `DB_PASSWORD` environment variable to your PostgreSQL password before starting the application. Do not commit real credentials to Git.

### 3. Build the project

On Windows PowerShell:

```powershell
.\mvnw.cmd clean compile
```

### 4. Run automated tests

```powershell
.\mvnw.cmd test
```

The current unit-test suite contains 10 passing tests covering incident creation and updates, recovery behavior, RCA selection, and insufficient-data handling.

### 5. Start the application

Run `PaymentpulseApplication` from IntelliJ, or use:

```powershell
.\mvnw.cmd spring-boot:run
```

The application should be available at `http://localhost:8080`, unless a different port is configured.

## Testing the Incident Lifecycle

A typical manual test sequence is:

1. Generate normal simulated transactions.
2. Generate an incident on a selected gateway.
3. Check rolling-window metrics.
4. Evaluate the incident engine.
5. Inspect the incident and RCA response.
6. Retrieve RCA history.
7. Stop incident simulation and generate normal traffic.
8. Evaluate recovery until three consecutive healthy evaluations resolve the incident.

Example:

```http
POST /api/simulator/generate?count=200
POST /api/simulator/incident?gatewayId=G03&count=100
POST /api/incidents/evaluate?windowMinutes=10&windows=6
GET  /api/incidents
GET  /api/incidents/INC-0001/rca-history
```

Use the actual incident ID returned by the API. The simulator's timestamp behavior and rolling-window boundaries affect whether generated transactions contribute to current metrics.

## Roadmap

- [x] Transaction model, ingestion, and PostgreSQL persistence
- [x] Aggregate and rolling-window metrics
- [x] Historical reliability baselines
- [x] Anomaly detection
- [x] Gateway reliability analysis
- [x] Incident lifecycle and automatic recovery
- [x] Gateway-bank correlation and RCA evidence
- [x] RCA snapshot persistence
- [x] Initial automated unit-test suite
- [ ] More comprehensive persistence and integration tests
- [ ] Kafka event-driven transaction processing
- [ ] Idempotent event consumption and retry handling
- [ ] Prometheus metrics and Grafana dashboards
- [ ] Docker-based local deployment
- [ ] Load testing and reliability benchmarks
- [ ] Intelligent routing and failover
- [ ] Production deployment

## Engineering Goals

The longer-term goal is to evolve PaymentPulse into a distributed payment reliability platform that explores:

- Asynchronous transaction processing
- Event-driven service architecture
- Failure isolation and recovery
- Time-window-based anomaly detection
- Gateway reliability scoring
- Evidence-based incident investigation
- Operational metrics and observability
- Throughput, latency, and fault-tolerance benchmarking

## Disclaimer

PaymentPulse currently uses simulated payment transactions. Its reliability analysis and RCA outputs are intended for engineering experimentation and do not establish the actual cause of a real payment failure.

## Author

Abhyuday Agnihotri
