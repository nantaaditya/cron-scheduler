# cron-scheduler

A reactive Spring Boot service that schedules and executes HTTP callbacks on a cron trigger, backed by Quartz for scheduling and WebFlux/R2DBC for a fully non-blocking pipeline.

---

## Table of Contents

- [Overview](#overview)
- [Architecture](#architecture)
- [Project Structure](#project-structure)
- [Features](#features)
- [API Reference](#api-reference)
- [Configuration](#configuration)
- [Local Development](#local-development)
- [Database](#database)

---

## Overview

`cron-scheduler` lets you register an HTTP request template (a **Client Request**) and a cron trigger (a **Job Executor**) that fires it. When the trigger runs, the service calls the configured endpoint via `WebClient`, records the outcome (`JobHistory` / `JobHistoryDetail`), and invokes a pluggable `NotificationCallback` on success or failure. Everything from the REST API to the job execution and database access is reactive (Project Reactor) — no blocking calls anywhere in the request or job path.

Secondary responsibilities:
- Cron-trigger lifecycle management (create/update/delete/enable/disable/instant-run) materialized into live Quartz triggers
- Execution history with retention-based purge
- Pluggable success/failure notification callbacks
- Trace/span-correlated logging across concurrent, decoupled job executions

**Runtime**: Spring Boot 3.5 · Java 21 · PostgreSQL (R2DBC)

---

## Architecture

```
                    ┌───────────────────────┐
   REST API  ─────► │  Controller layer     │  (ClientRequest / JobExecutor / JobHistory)
                    └──────────┬────────────┘
                               │
                    ┌──────────▼────────────┐
                    │  Service layer         │  validation, quartz scheduling, persistence
                    └──────────┬────────────┘
                               │
        ┌──────────────────────┼───────────────────────┐
        │                      │                        │
┌───────▼───────┐   ┌──────────▼──────────┐   ┌─────────▼─────────┐
│ Quartz Trigger │──►│ WebClientJob        │──►│ ReactorEventBus /  │
│ (cron_trigger) │   │ (Quartz Job)        │   │ Sinks.Many          │
└────────────────┘   └──────────┬──────────┘   └─────────┬─────────┘
                                 │                        │
                                 ▼                        ▼
                       ┌───────────────────────────────────────┐
                       │ WebClientJobListener                  │
                       │  createJobHistory → execute (WebClient │
                       │  call w/ timeout) → handleResponse     │
                       └──────────┬──────────────────┬─────────┘
                                  │                  │
                          ┌───────▼──────┐   ┌────────▼─────────┐
                          │ R2DBC (Postgres)│ │ NotificationCallback│
                          │ job_history*    │ │ (pluggable bean)     │
                          └────────────────┘ └──────────────────────┘
```

### Key design decisions

**Event-bus decoupling of the Quartz thread pool**
`WebClientJob` (the Quartz job) doesn't call the downstream endpoint directly — it publishes a `JobExecutionContext` onto a Reactor `Sinks.Many` event bus (`ReactorEventBus`). `WebClientJobListener` subscribes to that bus once at startup and drives the whole pipeline: create a `JobHistory` row → run the HTTP call with a timeout (`ReactorJobExecutor`) → persist the result (`JobHistoryDetail`) → invoke `NotificationCallback`. This keeps Quartz's own thread pool free of reactive/HTTP work.

**Quartz-owned scheduling, DB-backed source of truth**
`JobExecutor` entities are the source of truth; Quartz triggers are materialized from them on startup (`QuartzInitializerConfiguration`) and kept in sync on every create/update/delete/toggle via `JobExecutorServiceImpl` calling into `QuartzUtil`.

**Per-job trace correlation across thread hops**
`reqId` / `traceId` / `spanId` are carried through Reactor `Context` (`ReactorLogContext`) and re-synced into SLF4J `MDC` at every thread hop, so concurrent job executions — each running on its own reactive chain — don't cross-contaminate each other's log lines.

**Resilient execution path**
Every downstream call runs under a per-execution timeout (`ReactorJobExecutor`). Failures still get a `JobHistory`/`JobHistoryDetail` record and a `notifyFailed` callback instead of the job silently disappearing.

---

## Project Structure

```
src/main/java/com/nantaaditya/cronscheduler/
├── configuration/   # Spring config: Quartz, Netty/WebClient, R2DBC, OpenAPI, observability
├── controller/      # REST controllers (ClientRequest, JobExecutor, JobHistory) + exception handler
├── entity/          # R2DBC entities (BaseEntity, ClientRequest, JobExecutor, JobHistory, JobHistoryDetail)
├── job/             # Quartz Job implementation (WebClientJob) that publishes to the event bus
├── listener/        # Quartz + Reactor listeners; WebClientJobListener drives the execution pipeline
├── model/
│   ├── constant/     # Enums / constants (JobStatus, JobDataMapKey, exceptions)
│   ├── dto/          # Internal DTOs (EventContext, JobResponse, NotificationCallbackDTO)
│   ├── request/      # Inbound request DTOs (Create/Update *RequestDTO)
│   └── response/     # Outbound response DTOs + the Response<T> envelope
├── properties/       # @ConfigurationProperties classes (JobProperties, QuartzProperties)
├── repository/       # R2DBC repositories + custom repository implementations
├── service/          # Business logic (ClientRequestService, JobExecutorService, JobHistoryService, NotificationCallback)
├── util/             # Reactor/MDC helpers, JSON helper, ID generation
└── validation/       # Custom bean-validation annotations (cron expression, HTTP method)
```

Tests mirror this structure under `src/test/java`, plus `src/test/resources` for ddl/fixtures.

---

## Features

### Client Request management

**`/api/client_request`**
CRUD for reusable HTTP request templates — HTTP method, base URL, path, path/query params, headers, JSON payload, and timeout. A Client Request is the reusable "what to call" half of a scheduled job.

### Job Executor management

**`/api/job_executor`**
CRUD for cron-triggered jobs bound to a Client Request. Includes an enable/disable toggle (`PUT /{id}/_toggle/{enable}`) and an on-demand instant run (`GET /{id}/_run`) that fires the job immediately, outside its cron schedule.

### Job History

**`/api/job_history`**
Every execution is recorded (`job_history` + `job_history_detail`, including the request payload sent and the response captured), with a retention-based cleanup endpoint (`DELETE /?retentionDays=30`).

### Operational utilities

| Feature | Description |
|---|---|
| Pluggable notifications | Implement `NotificationCallback` and expose it as the `@Primary` bean to receive success/failure callbacks (email, Slack, webhook, etc.) |
| Resilience | Per-execution timeout via `ReactorJobExecutor`; failed executions still record history and notify instead of being dropped silently |
| Tracing-aware logging | B3 trace/span headers are propagated to the downstream HTTP call and mirrored into MDC (`ReactorLogContext`) for correlated logs across concurrent jobs |
| OpenAPI/Swagger UI | Interactive API docs out of the box |
| Actuator + Prometheus + tracing | Health, metrics, and distributed tracing endpoints out of the box |

### Extending notifications

Implement `NotificationCallback` and register it as the primary bean to plug in your own delivery channel:

```java
import com.nantaaditya.cronscheduler.model.dto.NotificationCallbackDTO;

@Service
@Primary
public class EmailNotificationCallback implements NotificationCallback {

  private final EmailService emailService;

  @Override
  public Mono<Boolean> notifySuccess(NotificationCallbackDTO notificationCallback) {
    return emailService.send(notificationCallback);
  }

  @Override
  public Mono<Boolean> notifyFailed(NotificationCallbackDTO notificationCallback) {
    return emailService.send(notificationCallback);
  }
}
```

---

## API Reference

Base path: `/api`. All responses are wrapped in `Response<T>` (`com.nantaaditya.cronscheduler.model.response.Response`), serialized with non-null fields only:

```json
{
  "success": true,
  "data": {}
}
```

**Response shapes**

| HTTP Status | `success` | Description |
|---|---|---|
| 200 | `true` | Request processed successfully — `data` is populated |
| 400 | `false` | Business validation failure (thrown as `InvalidParameterException`) — `errors` is a `field → [reasonCode]` map |
| 500 | — | Unexpected error — default Spring WebFlux error body, not the `Response` envelope |

Error shape example:
```json
{
  "success": false,
  "errors": {
    "clientName": ["AlreadyExists"]
  }
}
```

Interactive docs: `http://localhost:{SERVER_PORT}/swagger-ui.html`

---

### Client Request — `/api/client_request`

#### `POST /api/client_request`

Creates a reusable HTTP request template.

**Request**
```json
{
  "clientName": "weather-alert-webhook",
  "httpMethod": "POST",
  "baseUrl": "https://api.example.com",
  "apiPath": "/v1/notify",
  "pathParams": {},
  "queryParams": { "source": "cron-scheduler" },
  "headers": { "Authorization": ["Bearer <token>"] },
  "payload": { "message": "daily weather check" },
  "timeoutInMillis": 5000
}
```

**Response `200`**
```json
{
  "success": true,
  "data": {
    "id": "0h3x9k2qz0001",
    "clientName": "weather-alert-webhook",
    "httpMethod": "POST",
    "baseUrl": "https://api.example.com",
    "apiPath": "/v1/notify",
    "queryParams": { "source": "cron-scheduler" },
    "headers": { "Authorization": ["Bearer <token>"] },
    "timeoutInMillis": 5000,
    "payload": { "message": "daily weather check" }
  }
}
```

**Response `400`** — `clientName` already registered
```json
{ "success": false, "errors": { "clientName": ["AlreadyExists"] } }
```

#### `PUT /api/client_request`

Updates a client request template, looked up by **`clientName`** (not id). Cascades any cron trigger changes to linked job executors.

**Response `400`** — no template with this `clientName` exists: `{ "clientName": ["NotExists"] }`

#### `GET /api/client_request?page=0&size=10`

Lists client request templates.

#### `GET /api/client_request/{clientId}`

Fetches one template by id. **Response `400`**: `{ "clientId": ["NotExists"] }`

#### `DELETE /api/client_request/{clientId}`

Deletes a template and cascades deletion (and Quartz unscheduling) to its linked job executors. **Response `400`**: `{ "clientId": ["NotExists"] }`

---

### Job Executor — `/api/job_executor`

#### `POST /api/job_executor`

Creates a cron-triggered job bound to an existing Client Request and schedules it in Quartz.

**Request**
```json
{
  "clientId": "0h3x9k2qz0001",
  "cronTriggerExpression": "0 */5 * * * ?",
  "enable": true,
  "jobName": "weather-alert-every-5-min"
}
```

**Response `200`**
```json
{
  "success": true,
  "data": {
    "jobExecutorId": "0h3xa1r4z0002",
    "jobName": "weather-alert-every-5-min",
    "jobGroup": "WEB_CLIENT_JOB_GROUP",
    "triggerCron": "0 */5 * * * ?",
    "enable": true,
    "clientRequest": { "id": "0h3x9k2qz0001", "clientName": "weather-alert-webhook" }
  }
}
```

**Response `400`** — `{ "clientId": ["NotExists"] }` and/or `{ "jobName": ["AlreadyExists"] }`

#### `PUT /api/job_executor`

Updates a job executor (identified by `jobExecutorId`) and re-syncs the Quartz trigger. **Response `400`**: `clientId`/`jobExecutorId` `NotExists`.

#### `GET /api/job_executor?page=0&size=10`

Lists job executors with their linked client request.

#### `GET /api/job_executor/{id}`

Fetches one job executor by id. **Response `400`**: `{ "id": ["NotExists"] }`

#### `DELETE /api/job_executor/{id}`

Deletes a job executor and unschedules its Quartz trigger. **Response `400`**: `{ "id": ["NotExists"] }`

#### `PUT /api/job_executor/{id}/_toggle/{enable}`

Enables or disables a job executor without deleting it (e.g. `PUT /api/job_executor/0h3xa1r4z0002/_toggle/false`).

#### `GET /api/job_executor/{id}/_run`

Triggers an instant, one-off run outside the cron schedule.

---

### Job History — `/api/job_history`

#### `GET /api/job_history?page=0&size=10`

Lists execution history, including the request snapshot and captured result per run.

**Response `200`**
```json
{
  "success": true,
  "data": [
    {
      "id": "0h3xb7m9z0003",
      "jobExecutorId": "0h3xa1r4z0002",
      "executedDate": "2026-07-13",
      "executedTime": "09:05:00",
      "status": "SUCCESS",
      "triggerCron": "0 */5 * * * ?",
      "clientRequest": { "message": "daily weather check" },
      "result": { "status": 200 }
    }
  ]
}
```

#### `DELETE /api/job_history?retentionDays=30`

Purges history (and its detail rows) older than `retentionDays` (default `30`).

---

## Configuration

All values are injectable via environment variables. Defaults are shown; every variable in this project has a default, so nothing is strictly required to start locally.

### Server

| Variable | Default | Description |
|---|---|---|
| `SERVER_PORT` | `1000` | HTTP port the application listens on |

### Database (R2DBC)

| Variable | Default | Description |
|---|---|---|
| `DB_URL` | `r2dbc:postgresql://localhost:5432/cron_db` | R2DBC connection URL |
| `DB_USER` | `user` | Database username |
| `DB_PASS` | `password` | Database password |
| `DB_POOL_SIZE` | `5` | Initial R2DBC connection pool size |
| `DB_MAX_POOL_SIZE` | `10` | Maximum R2DBC connection pool size |
| `DB_IDLE_TIME` | `30s` | Maximum time a pooled connection may sit idle |
| `DB_LIFE_TIME` | `60S` | Maximum lifetime of a pooled connection |

### Logging

| Variable | Default | Description |
|---|---|---|
| `APP_LOG_LEVEL` | `INFO` | Root logging level |
| `WEB_CLIENT_JOB_LOG_LEVEL` | `DEBUG` | Logging level for `reactor.netty.http.client` (WebClient HTTP traffic) |
| `LOG_NAME` | `cron-scheduler.log` | Log file name written under `logs/` |
| `MAX_LOG_HISTORY` | `14` | Number of days of rolled log files to retain |

### Web Client Job

| Variable | Default | Description |
|---|---|---|
| `CONNECT_TIME_OUT` | `10` | WebClient connect timeout (seconds) for job HTTP calls |
| `RESPONSE_TIME_OUT` | `10` | WebClient overall response timeout (seconds) |
| `READ_TIME_OUT` | `10` | WebClient read timeout (seconds) |
| `WRITE_TIME_OUT` | `10` | WebClient write timeout (seconds) |

### Quartz Scheduler

| Variable | Default | Description |
|---|---|---|
| `QUARTZ_INSTANCE_NAME` | `cron-scheduler` | Quartz scheduler instance name |
| `QUARTZ_THREAD_POOL_CLASS` | `org.quartz.simpl.SimpleThreadPool` | Quartz thread pool implementation |
| `QUARTZ_THREAD_NAME` | `cron-scheduler` | Base name for Quartz worker threads |
| `QUARTZ_THREAD_COUNT` | `20` | Number of worker threads in the Quartz thread pool |
| `QUARTZ_THREAD_PRIORITY` | `5` | Thread priority for Quartz worker threads |
| `QUARTZ_JOB_STORE_CLASS` | `org.quartz.simpl.RAMJobStore` | Quartz job store implementation — in-memory, does not survive a restart unless changed to a persistent JDBC store |
| `QUARTZ_MISFIRE_THRESHOLD` | `60000` | Quartz misfire threshold (ms) |

---

## Local Development

**Prerequisites**: JDK 21, Maven (or the bundled `./mvnw`), a running PostgreSQL instance (or Docker).

Every setting has a default, so the app runs locally with zero environment variables — schema is applied automatically from `src/main/resources/ddl.sql` on startup.

```shell
# start a local Postgres matching the application.yml defaults
docker run --name cron-scheduler-db -e POSTGRES_DB=cron_db \
  -e POSTGRES_USER=user -e POSTGRES_PASSWORD=password \
  -p 5432:5432 -d postgres:16

# run the app
./mvnw spring-boot:run

# run tests
./mvnw test
```

Once running:
- Swagger UI: `http://localhost:{SERVER_PORT}/swagger-ui.html`
- Actuator: `http://localhost:{SERVER_PORT}/actuator`

### Build & deploy image

Build Jar
```shell
mvn install -DskipTests
```

Build Image
```shell
docker build -f .docker/Dockerfile -t nantaaditya/cron-scheduler:0.0.1 .
```

Run Image
```shell
docker run --env-file .deployment/.env.local --name cron-scheduler -p 1000:1000 -m512m nantaaditya/cron-scheduler:0.0.1
```

### Build & deploy OpenJ9 image

Build Jar
```shell
mvn install -DskipTests
```

Build Image
```shell
docker build -f .docker/Dockerfile-Openj9 -t nantaaditya/cron-scheduler:0.0.1-o9 .
```

Run Image
```shell
docker run --env-file .deployment/.env.local --name cron-scheduler-o9 -p 1000:1000 -m512m nantaaditya/cron-scheduler:0.0.1-o9
```

### Build & deploy GraalVM image

Build Image
```shell
docker buildx build -f .docker/Dockerfile-GraalVM -t nantaaditya/cron-scheduler:0.0.1-gvm .
```

Run Image
```shell
docker run --env-file .deployment/.env.local --name cron-scheduler-gvm -p 1000:1000 nantaaditya/cron-scheduler:0.0.1-gvm
```

---

## Database

PostgreSQL, accessed reactively via R2DBC. Schema is defined in `src/main/resources/ddl.sql` (source diagram: `.diagram/cron-erd.puml`) and applied on startup — there is no Flyway migration chain in this service.

![erd](./static/erd.png)

| Table | Purpose |
|---|---|
| `client_request` | Reusable HTTP request template: method, base URL, path, params, headers, payload, timeout |
| `job_executor` | Cron-triggered job bound to a `client_request` (`client_id`), with its trigger cron expression and active flag |
| `job_history` | One row per job execution, tracking `job_executor_id`, execution time, status, and the cron expression used |
| `job_history_detail` | Per-execution detail: the actual `client_request` payload sent and the `result_detail` response captured |

All tables share the same audit columns (`created_by`, `created_date`, `created_time`, `modified_by`, `modified_date`, `modified_time`, `version`) via `BaseEntity`, and use TSID-generated `varchar(64)` primary keys.

Indexes: `client_request.client_name`, `job_executor.job_name`, `job_history_detail.job_history_id`.
