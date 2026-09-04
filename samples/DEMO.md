# Employee Ingestion Platform — Demonstration Guide

This guide demonstrates authentication, Excel ingestion, batch processing, employee searching, Kafka ingestion, retry handling, and dead-letter topic processing.

## Prerequisites

Install:

* Java 25
* Docker Desktop
* Git

Run all commands from the project root:

```bash
cd ~/Documents/TavantAssignment/employee-ingestion-platform
```

## 1. Start the complete platform

Build and start PostgreSQL, Kafka, and the Spring Boot application:

```bash
docker compose up -d --build
```

Check the containers:

```bash
docker compose ps
```

Expected containers:

* `employee-application`
* `employee-postgres`
* `employee-kafka`

Check application health:

```bash
curl -i http://localhost:8080/actuator/health
```

Expected HTTP status:

```text
HTTP/1.1 200
```

## 2. Log in as administrator

```bash
curl -s -X POST \
  http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"Admin@123"}'
```

Copy the returned `accessToken` and store it in a terminal variable:

```bash
export EMPLOYEE_TOKEN='paste-access-token-here'
```

Verify that the variable contains the token:

```bash
echo "$EMPLOYEE_TOKEN"
```

The token must be supplied as a Bearer token when calling protected APIs.

## 3. Upload the partially valid Excel file

This file contains valid and invalid employee rows, allowing the rejection flow to be demonstrated.

```bash
curl -s -X POST \
  http://localhost:8080/api/v1/employees/upload \
  -H "Authorization: Bearer $EMPLOYEE_TOKEN" \
  -F "file=@samples/excel/employees-partial-failure.xlsx"
```

Expected response:

```json
{
  "trackingId": "generated-uuid",
  "fileName": "employees-partial-failure.xlsx",
  "status": "PENDING",
  "message": "File uploaded successfully and is awaiting processing"
}
```

Copy the returned `trackingId`:

```bash
export TRACKING_ID='paste-tracking-id-here'
```

## 4. Check the initial upload status

```bash
curl -s \
  "http://localhost:8080/api/v1/employees/uploads/$TRACKING_ID" \
  -H "Authorization: Bearer $EMPLOYEE_TOKEN"
```

Before processing, the expected status is:

```text
PENDING
```

## 5. Process the Excel file manually

```bash
curl -s -X POST \
  "http://localhost:8080/api/v1/employees/uploads/$TRACKING_ID/process" \
  -H "Authorization: Bearer $EMPLOYEE_TOKEN"
```

The Spring Batch job reads the spreadsheet and processes employees in chunks.

The configured chunk size is:

```text
500
```

## 6. Check the final processing status

```bash
curl -s \
  "http://localhost:8080/api/v1/employees/uploads/$TRACKING_ID" \
  -H "Authorization: Bearer $EMPLOYEE_TOKEN"
```

Possible statuses:

| Status                | Meaning                                      |
| --------------------- | -------------------------------------------- |
| `PENDING`             | File is waiting to be processed              |
| `PROCESSING`          | Batch processing has started                 |
| `COMPLETED`           | Every row was processed successfully         |
| `PARTIALLY_COMPLETED` | Some rows succeeded and others were rejected |
| `FAILED`              | The batch job failed                         |

For `employees-partial-failure.xlsx`, the expected final status is:

```text
PARTIALLY_COMPLETED
```

## 7. View rejected Excel rows

```bash
curl -s \
  "http://localhost:8080/api/v1/employees/uploads/$TRACKING_ID/rejections?page=0&size=20" \
  -H "Authorization: Bearer $EMPLOYEE_TOKEN"
```

The response contains:

* Excel row number
* Employee ID, when available
* Rejection reason
* Rejection timestamp
* Pagination information

## 8. Upload the 10,000-row Excel file

```bash
curl -s -X POST \
  http://localhost:8080/api/v1/employees/upload \
  -H "Authorization: Bearer $EMPLOYEE_TOKEN" \
  -F "file=@samples/excel/employees-valid-10000.xlsx"
```

Copy the new tracking ID:

```bash
export LARGE_UPLOAD_TRACKING_ID='paste-tracking-id-here'
```

Process it manually:

```bash
curl -s -X POST \
  "http://localhost:8080/api/v1/employees/uploads/$LARGE_UPLOAD_TRACKING_ID/process" \
  -H "Authorization: Bearer $EMPLOYEE_TOKEN"
```

Check its status:

```bash
curl -s \
  "http://localhost:8080/api/v1/employees/uploads/$LARGE_UPLOAD_TRACKING_ID" \
  -H "Authorization: Bearer $EMPLOYEE_TOKEN"
```

The expected final status is:

```text
COMPLETED
```

## 9. Retrieve employees with pagination

```bash
curl -s \
  "http://localhost:8080/api/v1/employees?page=0&size=20" \
  -H "Authorization: Bearer $EMPLOYEE_TOKEN"
```

Pagination begins at page `0`.

The maximum allowed page size is `100`.

## 10. Filter and sort employees

Filter by department and source:

```bash
curl -s \
  "http://localhost:8080/api/v1/employees?page=0&size=20&department=Engineering&source=EXCEL&sort=empId,asc" \
  -H "Authorization: Bearer $EMPLOYEE_TOKEN"
```

Filter by employee ID:

```bash
curl -s \
  "http://localhost:8080/api/v1/employees?empId=E00001" \
  -H "Authorization: Bearer $EMPLOYEE_TOKEN"
```

Filter by email:

```bash
curl -s \
  "http://localhost:8080/api/v1/employees?email=employee00001@example.com" \
  -H "Authorization: Bearer $EMPLOYEE_TOKEN"
```

Filter by salary range:

```bash
curl -s \
  "http://localhost:8080/api/v1/employees?minSalary=50000&maxSalary=150000&sort=salary,desc" \
  -H "Authorization: Bearer $EMPLOYEE_TOKEN"
```

## 11. Send a valid Kafka employee event

The Kafka console producer treats each input line as a separate event. Therefore, the JSON file is converted into one line before it is sent.

```bash
{ tr -d '\n' < samples/kafka/valid-employee.json; echo; } | \
docker exec -i employee-kafka \
  /opt/kafka/bin/kafka-console-producer.sh \
  --bootstrap-server localhost:9092 \
  --topic employee-events
```

Wait a few seconds for the consumer to process the event.

## 12. Verify Kafka ingestion in PostgreSQL

```bash
docker exec employee-postgres \
  psql -U employee_user -d employee_db \
  -c "SELECT emp_id, first_name, last_name, email, department, salary, source FROM employees WHERE emp_id = 'KAFKA90001';"
```

Expected source:

```text
KAFKA
```

This demonstrates the following flow:

```text
JSON event
    → employee-events topic
    → Spring Kafka consumer
    → validation
    → employee service
    → PostgreSQL
```

## 13. Send an invalid Kafka event

```bash
{ tr -d '\n' < samples/kafka/invalid-employee.json; echo; } | \
docker exec -i employee-kafka \
  /opt/kafka/bin/kafka-console-producer.sh \
  --bootstrap-server localhost:9092 \
  --topic employee-events
```

The event should:

1. Fail validation.
2. Be retried according to the configured retry policy.
3. Be published to `employee-events.DLT`.

Check recent application logs:

```bash
docker logs employee-application --tail 150
```

## 14. Read the dead-letter topic

```bash
docker exec employee-kafka \
  /opt/kafka/bin/kafka-console-consumer.sh \
  --bootstrap-server localhost:9092 \
  --topic employee-events.DLT \
  --from-beginning \
  --timeout-ms 5000
```

Confirm that the invalid employee was not inserted:

```bash
docker exec employee-postgres \
  psql -U employee_user -d employee_db \
  -c "SELECT emp_id, email, source FROM employees WHERE emp_id = 'KAFKA90002';"
```

Expected result:

```text
(0 rows)
```

## 15. Test duplicate Kafka protection

The valid event created employee `KAFKA90001`. Send another event containing the same employee ID:

```bash
{ tr -d '\n' < samples/kafka/duplicate-employee.json; echo; } | \
docker exec -i employee-kafka \
  /opt/kafka/bin/kafka-console-producer.sh \
  --bootstrap-server localhost:9092 \
  --topic employee-events
```

Check that only one database record exists:

```bash
docker exec employee-postgres \
  psql -U employee_user -d employee_db \
  -c "SELECT COUNT(*) FROM employees WHERE emp_id = 'KAFKA90001';"
```

Expected result:

```text
count
-------
1
```

The duplicate event should follow the configured retry and DLT flow.

## 16. Test correlation IDs

Supply a custom correlation ID:

```bash
curl -i \
  "http://localhost:8080/api/v1/employees?page=0&size=5" \
  -H "Authorization: Bearer $EMPLOYEE_TOKEN" \
  -H "X-Correlation-ID: assignment-demo-001"
```

The response headers should include:

```text
X-Correlation-ID: assignment-demo-001
```

The same value can be used to trace the request in application logs.

## 17. Run automated tests

Run the complete Maven verification lifecycle:

```bash
./mvnw clean verify
```

Expected result:

```text
BUILD SUCCESS
```

This command:

* Compiles production code.
* Compiles test code.
* Runs automated tests.
* Creates the executable JAR.
* Generates the JaCoCo report.
* Checks the configured coverage threshold.

## 18. Open the JaCoCo coverage report

On macOS:

```bash
open target/site/jacoco/index.html
```

The configured line-coverage threshold must pass during `mvn verify`.

## 19. Review application logs

```bash
docker logs employee-application --tail 200
```

Follow logs continuously:

```bash
docker logs -f employee-application
```

Stop following logs with `Control + C`.

## 20. Stop the platform

Stop and remove the containers while retaining PostgreSQL data:

```bash
docker compose down
```

## 21. Reset the demonstration environment

The following command removes the containers and PostgreSQL volume:

```bash
docker compose down -v
```

Warning: this permanently deletes the local PostgreSQL data stored in the Docker volume.

Rebuild a fresh environment with:

```bash
docker compose up -d --build
```
