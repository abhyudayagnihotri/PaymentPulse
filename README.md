# PaymentPulse

### Distributed Payment Reliability & Observability Platform

PaymentPulse is a backend-focused payment reliability platform designed to monitor, analyze, and detect reliability issues in distributed payment systems.

Instead of treating payments as simple transactions, PaymentPulse focuses on **system reliability** — identifying abnormal failure rates, latency degradation, gateway performance issues, and potential payment incidents using real-time metrics and statistical analysis.

The project is being developed incrementally toward a production-style distributed architecture using **Java, Spring Boot, PostgreSQL, Kafka, Redis, Docker, Kubernetes, Prometheus, and Grafana**.

---

## 🚀 Project Vision

Modern payment systems depend on multiple components such as:

- Payment gateways
- Banks
- Merchants
- Payment methods
- Network services
- Transaction processing services

A failure in any of these components can cause increased transaction failures or latency.

PaymentPulse aims to detect these problems early and provide actionable reliability insights.

### Target Architecture

```text
                    ┌─────────────────────┐
                    │   Payment Client    │
                    └──────────┬──────────┘
                               │
                               ▼
                    ┌─────────────────────┐
                    │ Transaction Ingest  │
                    └──────────┬──────────┘
                               │
                               ▼
                    ┌─────────────────────┐
                    │ Kafka Event Stream  │
                    └──────────┬──────────┘
                               │
                               ▼
                ┌────────────────────────────┐
                │   Payment Processing       │
                └─────────────┬──────────────┘
                              │
                              ▼
              ┌───────────────────────────────┐
              │   Payment Reliability Engine  │
              │                               │
              │ • Metrics                     │
              │ • Baselines                   │
              │ • Anomaly Detection           │
              │ • Incident Detection          │
              │ • Failure Correlation          │
              │ • Gateway Analysis             │
              │ • Intelligent Routing          │
              │ • Recovery / Failover          │
              └──────────────┬────────────────┘
                             │
                             ▼
                 ┌─────────────────────────┐
                 │ Observability Dashboard │
                 │ Prometheus + Grafana     │
                 └─────────────────────────┘
```

---

# ✨ Current Features

## 1. Transaction Management

PaymentPulse currently supports transaction ingestion and storage using PostgreSQL.

Each transaction contains information such as:

- Transaction ID
- Merchant ID
- Bank ID
- Gateway ID
- Amount
- Location
- Payment method
- Transaction status
- Response code
- Processing latency
- Timestamp

---

## 2. Transaction Metrics

The system calculates global transaction reliability metrics including:

- Total transactions
- Successful transactions
- Failed transactions
- Success rate
- Failure rate
- Average latency

Example:

```json
{
  "totalTransactions": 2004,
  "successfulTransactions": 1823,
  "failedTransactions": 181,
  "successRate": 90.97,
  "failureRate": 9.03,
  "averageLatency": 405.69
}
```

---

## 3. Multi-Dimensional Reliability Analysis

PaymentPulse can analyze transaction reliability across multiple dimensions.

### Gateway

```text
G01
G02
G03
```

### Bank

```text
B12
B15
B18
B21
```

### Merchant

```text
M1001
M1002
...
```

### Payment Method

```text
CARD
UPI
NET_BANKING
```

### Location

```text
Delhi
Mumbai
Bangalore
Hyderabad
Chennai
Pune
```

Each breakdown provides:

- Transaction count
- Successful transactions
- Failed transactions
- Failure rate
- Average latency

This allows PaymentPulse to identify components with comparatively poor reliability.

---

# 📊 Time-Window Metrics

PaymentPulse supports rolling time-window analysis.

Example:

```http
GET /api/metrics/window?minutes=60
```

The endpoint calculates:

- Transactions
- Success rate
- Failure rate
- TPS
- Average latency
- P50 latency
- P95 latency
- P99 latency

Example:

```json
{
  "windowMinutes": 60,
  "transactions": 992,
  "successfulTransactions": 904,
  "failedTransactions": 88,
  "successRate": 91.13,
  "failureRate": 8.87,
  "tps": 0.2755,
  "averageLatency": 402.92,
  "p50Latency": 264.0,
  "p95Latency": 1889.0,
  "p99Latency": 2623.16
}
```

---

# 📈 Reliability Baselines

PaymentPulse calculates historical reliability baselines using previous time windows.

For example:

```http
GET /api/metrics/baseline?windowMinutes=10&windows=6
```

The baseline currently includes:

- Baseline failure rate
- Baseline P95 latency
- Baseline TPS
- Failure-rate standard deviation
- Latency standard deviation

Example:

```json
{
  "windowMinutes": 10,
  "requestedWindows": 6,
  "availableWindows": 6,
  "baselineFailureRate": 10.61,
  "baselineP95Latency": 1922.72,
  "baselineTps": 0.244,
  "failureRateStdDev": 2.63,
  "latencyStdDev": 43.74
}
```

These historical values provide the reference point for anomaly detection.

---

# 🚨 Statistical Anomaly Detection

PaymentPulse currently uses an explainable **3-sigma statistical detection approach**.

The current window is compared against historical reliability baselines.

### Failure Rate

```text
threshold =
baseline failure rate
+
3 × failure-rate standard deviation
```

### P95 Latency

```text
threshold =
baseline P95 latency
+
3 × latency standard deviation
```

An anomaly is generated when the current system behavior exceeds these thresholds.

Endpoint:

```http
GET /api/metrics/anomaly?windowMinutes=10&windows=6
```

---

## Example Anomaly

During a controlled gateway incident, PaymentPulse detected:

```json
{
  "anomaly": true,
  "severity": "HIGH",
  "windowMinutes": 10,
  "transactions": 168,
  "currentFailureRate": 26.19,
  "baselineFailureRate": 12.63,
  "failureRateThreshold": 22.60,
  "currentP95Latency": 4913.3,
  "latencyThreshold": 2301.04,
  "reasons": [
    "Failure rate exceeded baseline threshold",
    "P95 latency exceeded baseline threshold"
  ]
}
```

This demonstrates that the system can detect a degradation in payment reliability based on **multiple independent signals**.

---

# 🧪 Controlled Incident Simulation

PaymentPulse includes a transaction simulator for development and reliability testing.

Normal traffic can be generated using:

```http
POST /api/simulator/generate?count=1000
```

A controlled gateway incident can be simulated using:

```http
POST /api/simulator/incident?gatewayId=G01&count=100
```

The incident simulator introduces:

- Increased failure rate
- Increased latency
- Realistic response codes
- Gateway-specific degradation

This allows the reliability engine to be tested against reproducible failure scenarios instead of relying on random failures.

---

# 🏗️ Technology Stack

### Current

| Technology | Purpose |
|---|---|
| Java 21 | Backend development |
| Spring Boot | REST API and application framework |
| Spring Data JPA | Persistence layer |
| PostgreSQL | Transaction storage |
| Maven | Dependency management |
| REST APIs | Application interface |

### Planned

| Technology | Purpose |
|---|---|
| Apache Kafka | Event streaming |
| Redis | Fast state / reliability data |
| Docker | Containerization |
| Kubernetes | Distributed deployment |
| Prometheus | Metrics collection |
| Grafana | Observability dashboards |
| React / Vite | Frontend dashboard |

Technologies will be introduced only where they provide meaningful architectural value.

---

# 📁 Project Structure

The current project follows a layered Spring Boot architecture:

```text
src/
└── main/
    └── java/
        └── com/
            └── paymentpulse/
                └── paymentpulse/
                    ├── controller/
                    │   ├── MetricsController.java
                    │   └── SimulatorController.java
                    │
                    ├── model/
                    │   └── Transaction.java
                    │
                    ├── repository/
                    │   └── TransactionRepository.java
                    │
                    ├── service/
                    │   ├── MetricsService.java
                    │   ├── ReliabilityBaselineService.java
                    │   └── AnomalyDetectionService.java
                    │
                    └── simulator/
                        └── TransactionSimulator.java
```

---

# 🔌 API Endpoints

## Metrics

### Overall metrics

```http
GET /api/metrics
```

### Time-window metrics

```http
GET /api/metrics/window?minutes=10
```

### Reliability baseline

```http
GET /api/metrics/baseline?windowMinutes=10&windows=6
```

### Anomaly detection

```http
GET /api/metrics/anomaly?windowMinutes=10&windows=6
```

---

## Simulator

### Generate normal transactions

```http
POST /api/simulator/generate?count=1000
```

### Generate controlled gateway incident

```http
POST /api/simulator/incident?gatewayId=G01&count=100
```

---

# ⚙️ Running Locally

## Prerequisites

Install:

- Java 21
- Maven
- PostgreSQL
- Git

Verify Java:

```bash
java -version
```

Verify Maven:

```bash
mvn -version
```

---

## Database

Create a PostgreSQL database:

```sql
CREATE DATABASE paymentpulse;
```

Configure your database connection in:

```text
src/main/resources/application.properties
```

Example:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/paymentpulse
spring.datasource.username=YOUR_USERNAME
spring.datasource.password=YOUR_PASSWORD

spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=false
```

Do not commit real database credentials.

---

# ▶️ Running the Application

Clone the repository:

```bash
git clone https://github.com/YOUR_USERNAME/PaymentPulse.git
cd PaymentPulse
```

Build:

```bash
mvn clean package
```

Run:

```bash
mvn spring-boot:run
```

The application will start on:

```text
http://localhost:8080
```

---

# 🔬 Reliability Testing

A basic reliability test can be performed using the following workflow:

### Step 1 — Generate normal traffic

```http
POST /api/simulator/generate?count=2000
```

### Step 2 — Establish baseline

```http
GET /api/metrics/baseline?windowMinutes=10&windows=6
```

### Step 3 — Check current system state

```http
GET /api/metrics/anomaly?windowMinutes=10&windows=6
```

### Step 4 — Inject gateway incident

```http
POST /api/simulator/incident?gatewayId=G01&count=100
```

### Step 5 — Run anomaly detection again

```http
GET /api/metrics/anomaly?windowMinutes=10&windows=6
```

The system should detect elevated failure rates and/or latency.

---

# 🛣️ Roadmap

PaymentPulse is being developed in phases.

### Phase 1 — Foundation

- [x] Transaction model
- [x] PostgreSQL persistence
- [x] Transaction APIs
- [x] Basic metrics

### Phase 2 — Reliability Analytics

- [x] Gateway breakdown
- [x] Bank breakdown
- [x] Merchant breakdown
- [x] Payment-method analysis
- [x] Location analysis
- [x] Response-code analysis
- [x] Time-window metrics
- [x] TPS
- [x] P50/P95/P99 latency
- [x] Historical reliability baseline
- [x] Statistical anomaly detection
- [x] Controlled incident simulation

### Phase 3 — Event-Driven Architecture

- [ ] Apache Kafka integration
- [ ] Transaction event model
- [ ] Producers
- [ ] Consumers
- [ ] Partitioning strategy
- [ ] Consumer groups
- [ ] Event-processing metrics
- [ ] Kafka consumer lag monitoring

### Phase 4 — Incident Management

- [ ] Incident engine
- [ ] Incident lifecycle
- [ ] Incident severity
- [ ] Incident persistence
- [ ] Incident history
- [ ] MTTR calculation
- [ ] Recovery detection

### Phase 5 — Failure Correlation & Root Cause Analysis

- [ ] Gateway-level anomaly detection
- [ ] Bank-level anomaly detection
- [ ] Response-code correlation
- [ ] Cross-component correlation
- [ ] Root-cause candidates
- [ ] Failure propagation analysis

### Phase 6 — Intelligent Routing & Recovery

- [ ] Gateway reliability scoring
- [ ] Dynamic gateway selection
- [ ] Failure-aware routing
- [ ] Circuit breaker
- [ ] Retry strategy
- [ ] Failover
- [ ] Recovery policies

### Phase 7 — Observability

- [ ] Prometheus integration
- [ ] Grafana dashboards
- [ ] System health metrics
- [ ] Transaction metrics
- [ ] Incident dashboards
- [ ] Gateway health dashboards
- [ ] Alerting

### Phase 8 — Distributed Deployment

- [ ] Docker
- [ ] Docker Compose
- [ ] Kubernetes
- [ ] Horizontal scaling
- [ ] Service discovery
- [ ] Health checks
- [ ] Resource monitoring

### Phase 9 — Performance & Reliability Benchmarking

- [ ] Load testing
- [ ] Throughput benchmarks
- [ ] Latency benchmarks
- [ ] Failure injection
- [ ] Recovery-time measurement
- [ ] Kafka throughput testing
- [ ] Horizontal scaling experiments

---

# 🎯 Long-Term Goal

The final goal of PaymentPulse is to evolve into a **distributed payment reliability platform** capable of answering questions such as:

> Is the payment system healthy?

> Which gateway is currently degrading?

> Are failures increasing beyond normal behavior?

> Is latency increasing even though transactions are succeeding?

> Is the problem isolated to a gateway, bank, merchant, location, or payment method?

> When did the incident start?

> What caused it?

> Which gateway should receive the next transaction?

> Has the system recovered?

This moves PaymentPulse from a simple transaction analytics application toward a system focused on **distributed systems, reliability engineering, observability, fault detection, and automated recovery**.

---

# 📚 Key Engineering Concepts

The project is designed to explore practical concepts including:

- Distributed systems
- Event-driven architecture
- Kafka
- Reliability engineering
- Statistical anomaly detection
- Fault detection
- Failure correlation
- Root-cause analysis
- Circuit breakers
- Failover
- Load balancing
- Observability
- Metrics and telemetry
- Containerization
- Kubernetes
- Performance engineering

---

# 📌 Project Status

**Current status: Phase 2 — Reliability Analytics**

PaymentPulse currently has:

```text
Transaction ingestion
        ↓
PostgreSQL
        ↓
Metrics
        ↓
Time-window analysis
        ↓
Historical baseline
        ↓
Statistical anomaly detection
        ↓
Controlled failure simulation
```

The next major milestone is the **Incident Engine**, followed by Kafka-based event processing and deeper distributed reliability capabilities.

---

## 👨‍💻 Author

**Abhyuday Agnihotri**

Built as a long-term backend/distributed-systems project focused on payment reliability and observability.