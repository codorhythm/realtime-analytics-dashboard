# Real-time Analytics Dashboard

An event-driven order analytics service built with Spring Boot, Kafka, and PostgreSQL. Orders flow through a Kafka topic, get persisted asynchronously by a consumer, and are queryable via REST endpoints that expose revenue, regional breakdowns, and per-minute throughput.

This is a learning/portfolio project — built to deeply understand event-driven architecture, async processing, and the operational concerns that come with running Kafka in production (deserialization failures, poison pills, mTLS, replication factor).

---

## Architecture

```text
HTTP POST  ──►  Spring Controller  ──►  OrderService  ──►  KafkaProducer
                                                                 │
                                                                 ▼
                                                          Aiven Kafka
                                                       (order-events topic)
                                                                 │
                                                                 ▼
   GET endpoints  ◄──  PostgreSQL  ◄──  KafkaConsumer  ◄────────┘
   (analytics)         (Neon)          (saves to DB)
```

The producer fires-and-forgets: the HTTP request returns `202 Accepted` as soon as the event is queued for Kafka, decoupling the API response time from database writes. Persistence happens out-of-band via the consumer, which means the API stays fast even under load.

---

## Tech stack

| Layer       | Choice                          | Why                                                                 |
|-------------|----------------------------------|---------------------------------------------------------------------|
| Runtime     | Java 17, Spring Boot 3.5         | LTS, broad ecosystem, current stable Spring Boot                    |
| Messaging   | Aiven Kafka (free tier, mTLS)    | Real Kafka protocol, no credit card, certificate-based auth         |
| Database    | Neon PostgreSQL 17 (serverless)  | Managed Postgres with generous free tier, scale-to-zero             |
| ORM         | Spring Data JPA + Hibernate      | Standard for Spring; entity mapping is straightforward for orders   |
| Build       | Maven                            | Familiar, plays well with Spring Boot starters                      |

---

## Design decisions worth discussing

**Why Kafka instead of synchronous DB writes?**  
The original use case is bursty: order events can spike during sales or product launches. Writing to Postgres synchronously on every request couples API latency to DB throughput. Kafka decouples them — the API only has to enqueue, and a separate consumer drains at whatever rate the DB can handle. It also gives us a replayable event log for free, which is useful for rebuilding read models or debugging.

**Why mTLS for Kafka instead of SASL?**  
Aiven's free tier exposes Kafka over mTLS by default. mTLS authenticates the client at the TLS layer using a certificate, instead of sending a username/password each connection. It means the keystore IS the credential — there's no way to leak a password in a log line. Slightly more setup (PKCS12 + JKS keystores), better security posture.

**Why `ErrorHandlingDeserializer` wrapping `JsonDeserializer`?**  
Bare `JsonDeserializer` will infinite-loop on a poison-pill message: deserialization fails, the offset isn't committed, Kafka redelivers, fails again. `ErrorHandlingDeserializer` catches the deserialization exception, logs it, and lets the listener move on. Caught this the hard way during initial setup.

**Why `replication factor = 2`?**  
Aiven's free tier requires `min RF = 2` to prevent data loss from a single broker termination. For a hobby project I'd run RF=1, but the platform forces a more production-realistic setup. Free education.

**Why disable Spring's docker-compose integration?**  
The project ships a `compose.yaml` from the initial scaffolding, but everything runs against managed cloud services now (Neon, Aiven). Spring Boot would otherwise try to start Docker on every run and fail. Removed the dependency entirely so the failure mode is impossible.

---

## Endpoints

| Method | Path                                    | Description                              |
|--------|-----------------------------------------|------------------------------------------|
| POST   | `/api/orders/event`                     | Publish an order event to Kafka          |
| GET    | `/api/orders`                           | List all persisted orders                |
| GET    | `/api/orders/status/{status}`           | Filter by status                         |
| GET    | `/api/orders/analytics/revenue`         | Total revenue in last N hours            |
| GET    | `/api/orders/analytics/by-status`       | Order count grouped by status            |
| GET    | `/api/orders/analytics/by-region`       | Revenue grouped by region                |
| GET    | `/api/orders/analytics/per-minute`      | Order count per minute                   |

Example POST body:
```json
{
  "orderId": "ORD-001",
  "customerId": "CUST-001",
  "productId": "PROD-042",
  "productName": "Wireless Headphones",
  "amount": 99.99,
  "quantity": 2,
  "status": "PLACED",
  "region": "APAC"
}
```

---

## Running locally

Requires Java 17+, Maven, and accounts at [aiven.io](https://aiven.io) (free Kafka tier) and [neon.tech](https://neon.tech) (free Postgres).

1. Provision Kafka and Postgres in their respective consoles.
2. Download Aiven's `service.key`, `service.cert`, `ca.pem` into `src/main/resources/kafka-certs/`.
3. Generate Java keystores:
```bash
   cd src/main/resources/kafka-certs
   openssl pkcs12 -export -in service.cert -inkey service.key \
     -out client.keystore.p12 -name kafka-client -password pass:changeit
   keytool -import -file ca.pem -alias aiven-ca \
     -keystore client.truststore.jks -storepass changeit -noprompt
```
4. Set environment variables (see `run-local.sh.example`).
5. `./mvnw spring-boot:run`

---

## Roadmap

- [x] Phase 1: Domain model, Kafka producer/consumer, Postgres persistence, REST endpoints
- [x] Phase 2: Scheduled order simulator + analytics endpoint validation
- [ ] Phase 3: WebSocket push for live order feed
- [ ] Phase 4: Grafana Cloud dashboard backed by Postgres
- [ ] Phase 5: Deploy to Render/Railway

---

## What I learned

- Kafka client tuning: trusted packages, default type, and the difference between `__TypeId__` headers and explicit deserialization
- mTLS setup with `keytool` + `openssl` and the surprisingly fiddly difference between PKCS12 and JKS
- Spring Boot's `@KafkaListener` lifecycle and how unhandled exceptions cause infinite redelivery
- Why replication factor matters even on a one-broker free tier (it doesn't, until your provider enforces it)
- The boring but real cost of free-tier constraints (250 KiB/s, 5 topics × 2 partitions, 24h idle shutdown)
- `@ConditionalOnProperty` lets feature-flagged code ship cleanly: the simulator stays dormant in prod unless explicitly enabled, no commenting out beans
- Weighted random distributions matter more than they sound — uniform random looks fake, but a 50/20/15/10/3/2 split feels real