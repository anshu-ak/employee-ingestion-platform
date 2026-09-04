# Employee Ingestion Platform

A Spring Boot platform for ingesting employee records through Excel files and Kafka events.

## Technology Stack

- Java 25
- Spring Boot 4.1.1
- Spring Batch
- Spring Data JPA
- Spring Security and JWT
- Apache Kafka
- PostgreSQL
- Flyway
- Apache POI
- Docker Compose
- JUnit 5 and Mockito
- JaCoCo

## Features

- JWT-based authentication and role authorization
- Paginated employee search with filters
- Excel file upload and tracking
- Chunk-oriented batch processing with chunk size 500
- Validation and rejected-record tracking
- Manual and scheduled upload processing
- Kafka-based employee ingestion
- Kafka retry and dead-letter topic handling
- Correlation IDs for request tracing
- Consistent API error responses
- Flyway-managed database schema
- Dockerized application, PostgreSQL and Kafka
- Automated test coverage enforcement

## Architecture

The platform supports two employee-ingestion paths:

### Excel ingestion

1. A client uploads an `.xlsx` file.
2. The file is stored under `data/uploads`.
3. An `upload_tracking` record is created with `PENDING` status.
4. Processing is launched manually or by the scheduler.
5. Spring Batch reads, validates and writes records in chunks of 500.
6. Invalid records are stored in `rejected_records`.
7. The upload status becomes `COMPLETED`, `PARTIALLY_COMPLETED` or `FAILED`.

### Kafka ingestion

1. A producer publishes an employee event to `employee-events`.
2. The Kafka consumer validates the event.
3. Valid employees are saved with source `KAFKA`.
4. Failed events are retried.
5. Events that exhaust retries are published to `employee-events.DLT`.

## Prerequisites

- Docker Desktop
- Java 25
- Maven Wrapper is included

Verify:

```bash
java -version
docker --version
docker compose version