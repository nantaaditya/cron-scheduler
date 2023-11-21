# CRON-SCHEDULER

spring project for cron scheduler, by default it only rest api call job using web client.

![img](./static/erd.png)

## Configuration Properties
Application Configuration Properties

| Property Name                                  | Property Type | Default Value | Description                                |
|------------------------------------------------|---------------|---------------|--------------------------------------------|
| cron.quartz.thread-pool                        | int           | 20            | quartz thread pool size                    |
| job.configuration.web-client.connect-time-out  | int           | 10            | web client connect time out configuration  |
| job.configuration.web-client.response-time-out | int           | 10            | web client response time out configuration |
| job.configuration.web-client.read-time-out     | int           | 10            | web client read time out configuration     |
| job.configuration.web-client.write-time-out    | int           | 10            | web client write time out configuration    |

## Environment Variable

| Environment Variable Name | Default Value                             | Description                        |
|---------------------------|-------------------------------------------|------------------------------------|
| SERVER_PORT               | 1000                                      | application port                   |
| DB_URL                    | r2dbc:postgresql://localhost:5432/cron_db | application db host                |
| DB_USER                   | user                                      | application db username credential |
| DB_PASS                   | password                                  | application db password credential |
| LOG_NAME                  | cron-scheduler.log                        | application log name               |
| MAX_LOG_HISTORY           | 14                                        | max application log history        |
| CONNECT_TIME_OUT          | 10                                        | web client job connect time out    |
| RESPONSE_TIME_OUT         | 10                                        | web client job response time out   |
| READ_TIME_OUT             | 10                                        | web client job read time out       |
| WRITE_TIME_OUT            | 10                                        | web client job write time out      |
| QUARTZ_THREAD_POOL        | 20                                        | quartz thread pool                 |

## Swagger URL
swagger url format `http://HOST:PORT/swagger-ui.html`, <b>[Swagger URL](http://localhost:1000/swagger-ui.html)</b>
