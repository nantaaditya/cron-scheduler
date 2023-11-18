create table if not exists client_request
(
    id            varchar(64),
    created_by    varchar(50),
    created_date  date,
    created_time  time,
    modified_by   varchar(50),
    modified_date date,
    modified_time time,
    version       bigint,
    client_name   varchar(255),
    http_method   varchar(10),
    base_url      varchar(255),
    api_path      varchar(255),
    path_params   jsonb,
    query_params  jsonb,
    headers       jsonb,
    payload       jsonb,
    primary key (id)
);

create table if not exists job_executor
(
    id            varchar(64),
    created_by    varchar(50),
    created_date  date,
    created_time  time,
    modified_by   varchar(50),
    modified_date date,
    modified_time time,
    version       bigint,
    client_id     varchar(64),
    job_name      varchar(255),
    job_group     varchar(50),
    job_data      jsonb,
    trigger_cron  varchar(15),
    active        boolean,
    primary key (id)
);

create table if not exists job_history
(
    id              varchar(64),
    created_by      varchar(50),
    created_date    date,
    created_time    time,
    modified_by     varchar(50),
    modified_date   date,
    modified_time   time,
    version         bigint,
    job_executor_id varchar(64),
    executed_date   date,
    executed_time   time,
    status          varchar(15),
    trigger_cron    varchar(10),
    primary key (id)
);

create table if not exists job_history_detail
(
    id              varchar(64),
    created_by      varchar(50),
    created_date    date,
    created_time    time,
    modified_by     varchar(50),
    modified_date   date,
    modified_time   time,
    version         bigint,
    job_history_id  varchar(64),
    job_executor_id varchar(64),
    client_request  jsonb,
    result_detail   jsonb,
    primary key (id)
);

create index if not exists idx_clientrequest_name client_request(client_name);
create index if not exists idx_jobexecutor_name job_executor(job_name);