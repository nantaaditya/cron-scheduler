create table client_request
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

create table job_executor
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