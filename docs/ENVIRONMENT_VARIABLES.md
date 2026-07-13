# Environment Variables Reference

> Auto-generated from `src/main/resources/application.yml`.
> Last updated: 2026-07-13

## Legend

| Symbol | Meaning                                           |
|--------|---------------------------------------------------|
| 🔒     | Secret — never commit; rotate if exposed          |
| 🔑     | PCI-DSS relevant — restrict access                |
| ✅      | Required — no default; app won't start without it |

---

## Go-Live Checklist

Before deploying to production, verify:

- [ ] `DB_URL`, `DB_USER`, `DB_PASS` point to the production R2DBC database
- [ ] `DB_PASS` is rotated from the default placeholder value
- [ ] `DB_POOL_SIZE` / `DB_MAX_POOL_SIZE` are sized for expected production load
- [ ] `APP_LOG_LEVEL` is not left at `DEBUG`/`TRACE` in production
- [ ] `QUARTZ_JOB_STORE_CLASS` — confirm `RAMJobStore` (in-memory, non-persistent) is acceptable, or switch to a JDBC job store if job state must survive restarts
- [ ] `QUARTZ_THREAD_COUNT` matches the concurrency the scheduler is expected to handle
- [ ] `CONNECT_TIME_OUT`, `RESPONSE_TIME_OUT`, `READ_TIME_OUT`, `WRITE_TIME_OUT` are tuned for production network conditions
- [ ] `management.endpoints.web.exposure.include: '*'` (all Actuator endpoints exposed) is reviewed and restricted for production

---

## 1. Server

| Variable    | Description                          | Type    | Default | Required | Sensitivity | Nonprod Value | Prod Value | Notes |
|-------------|--------------------------------------|---------|---------|----------|-------------|---------------|------------|-------|
| SERVER_PORT | HTTP port the application listens on | Integer | `1000`  | No       |             |               |            |       |

---

## 2. Database (R2DBC)

| Variable         | Description                                      | Type              | Default                                     | Required | Sensitivity | Nonprod Value | Prod Value | Notes                                                 |
|------------------|--------------------------------------------------|-------------------|---------------------------------------------|----------|-------------|---------------|------------|-------------------------------------------------------|
| DB_URL           | R2DBC connection URL for the PostgreSQL database | URL               | `r2dbc:postgresql://localhost:5432/cron_db` | No       |             |               |            |                                                       |
| DB_USER          | Database username                                | String            | `user`                                      | No       | 🔒 Secret   |               |            |                                                       |
| DB_PASS          | Database password                                | String            | `password`                                  | No       | 🔒 Secret   |               |            | ⚠️ Rotate before prod — default is a weak placeholder |
| DB_POOL_SIZE     | Initial R2DBC connection pool size               | Integer           | `5`                                         | No       |             |               |            |                                                       |
| DB_MAX_POOL_SIZE | Maximum R2DBC connection pool size               | Integer           | `10`                                        | No       |             |               |            |                                                       |
| DB_IDLE_TIME     | Maximum time a pooled connection may sit idle    | String (duration) | `30s`                                       | No       |             |               |            | Duration string, e.g. `30s`                           |
| DB_LIFE_TIME     | Maximum lifetime of a pooled connection          | String (duration) | `60S`                                       | No       |             |               |            | Duration string, e.g. `60S`                           |

---

## 3. Logging

| Variable                 | Description                                                            | Type    | Default              | Required | Sensitivity | Nonprod Value | Prod Value | Notes                                              |
|--------------------------|------------------------------------------------------------------------|---------|----------------------|----------|-------------|---------------|------------|----------------------------------------------------|
| APP_LOG_LEVEL            | Root logging level                                                     | String  | `INFO`               | No       |             |               |            | Avoid `DEBUG`/`TRACE` in production                |
| WEB_CLIENT_JOB_LOG_LEVEL | Logging level for `reactor.netty.http.client` (WebClient HTTP traffic) | String  | `DEBUG`              | No       |             |               |            | Verbose in nonprod; consider raising in production |
| LOG_NAME                 | Log file name written under `logs/`                                    | String  | `cron-scheduler.log` | No       |             |               |            |                                                    |
| MAX_LOG_HISTORY          | Number of days of rolled log files to retain                           | Integer | `14`                 | No       |             |               |            |                                                    |

---

## 4. Web Client Job Configuration

| Variable          | Description                                                     | Type    | Default | Required | Sensitivity | Nonprod Value | Prod Value | Notes |
|-------------------|-----------------------------------------------------------------|---------|---------|----------|-------------|---------------|------------|-------|
| CONNECT_TIME_OUT  | WebClient connect timeout (seconds) for job HTTP calls          | Integer | `10`    | No       |             |               |            |       |
| RESPONSE_TIME_OUT | WebClient overall response timeout (seconds) for job HTTP calls | Integer | `10`    | No       |             |               |            |       |
| READ_TIME_OUT     | WebClient read timeout (seconds) for job HTTP calls             | Integer | `10`    | No       |             |               |            |       |
| WRITE_TIME_OUT    | WebClient write timeout (seconds) for job HTTP calls            | Integer | `10`    | No       |             |               |            |       |

---

## 5. Quartz Scheduler Configuration

| Variable                 | Description                                                         | Type         | Default                             | Required | Sensitivity | Nonprod Value | Prod Value | Notes                                                                                                  |
|--------------------------|---------------------------------------------------------------------|--------------|-------------------------------------|----------|-------------|---------------|------------|--------------------------------------------------------------------------------------------------------|
| QUARTZ_INSTANCE_NAME     | Quartz scheduler instance name                                      | String       | `cron-scheduler`                    | No       |             |               |            |                                                                                                        |
| QUARTZ_THREAD_POOL_CLASS | Fully-qualified class name of the Quartz thread pool implementation | String       | `org.quartz.simpl.SimpleThreadPool` | No       |             |               |            |                                                                                                        |
| QUARTZ_THREAD_NAME       | Base name for Quartz worker threads                                 | String       | `cron-scheduler`                    | No       |             |               |            |                                                                                                        |
| QUARTZ_THREAD_COUNT      | Number of worker threads in the Quartz thread pool                  | Integer      | `20`                                | No       |             |               |            |                                                                                                        |
| QUARTZ_THREAD_PRIORITY   | Thread priority for Quartz worker threads                           | Integer      | `5`                                 | No       |             |               |            |                                                                                                        |
| QUARTZ_JOB_STORE_CLASS   | Fully-qualified class name of the Quartz job store implementation   | String       | `org.quartz.simpl.RAMJobStore`      | No       |             |               |            | In-memory store — job state does not survive a restart unless changed to a persistent (JDBC) job store |
| QUARTZ_MISFIRE_THRESHOLD | Quartz misfire threshold                                            | Integer (ms) | `60000`                             | No       |             |               |            | Value is in milliseconds                                                                               |
